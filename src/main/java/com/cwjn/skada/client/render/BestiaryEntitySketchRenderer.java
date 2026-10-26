package com.cwjn.skada.client.render;

import com.cwjn.skada.util.UtilColour;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds shader-processed bestiary snapshots into off-screen textures.
 *
 * The returned texture location points at a renderer-owned dynamic texture in
 * normal GUI top-down orientation. It remains valid until the next refresh or
 * {@link #close()} call.
 */
@OnlyIn(Dist.CLIENT)
public final class BestiaryEntitySketchRenderer {

  private static final ResourceLocation BESTIARY_SKETCH_SHADER = new ResourceLocation("skada",
      "bestiary_entity_sketch");
  private static final AtomicInteger NEXT_DYNAMIC_TEXTURE_ID = new AtomicInteger();
  private static final float PREVIEW_YAW_DEGREES = 14.0F;
  private static final float PREVIEW_PITCH_DEGREES = 8.0F;
  private static final float PREVIEW_BODY_ROT = 180.0F;
  private static final float CAPTURE_SIZE_MULTIPLIER = 2.5F;
  private static final float CALIBRATION_SCALE_MULTIPLIER = 0.35F;
  private static final float CALIBRATION_CANVAS_MULTIPLIER = 1.5F;
  private static final float MIN_EXPECTED_VISIBLE_RATIO = 0.35F;
  private static final float MIN_SKETCH_DIMENSION_RATIO = 0.55F;
  private static final float MIN_SKETCH_AREA_RATIO = 0.35F;
  private static final float MIN_CAPTURE_DIMENSION = 0.5F;
  private static final int CAPTURE_PADDING = 96;
  private static final int CALIBRATION_EXTRA_CANVAS = 256;
  private static final int MIN_CAPTURE_SIZE = 256;
  private static final int MAX_CAPTURE_TARGET_SIZE = 2048;
  private static final int RETARGET_MARGIN = 72;
  private static final int MAX_CAPTURE_RETRIES = 5;
  private static final int SNAPSHOT_ALPHA_THRESHOLD = 0;
  private static final int PREFERRED_PIXELS_PER_BLOCK = 96;
  private static final int INK_COLOUR = UtilColour.UI_TEXT_COLOUR;
  private static final int PAGE_COLOUR = 0xE8B982;
  private static final float INK_RED = (float) ((INK_COLOUR >> 16) & 0xFF) / 255.0F;
  private static final float INK_GREEN = (float) ((INK_COLOUR >> 8) & 0xFF) / 255.0F;
  private static final float INK_BLUE = (float) (INK_COLOUR & 0xFF) / 255.0F;
  private static final float PAGE_RED = (float) ((PAGE_COLOUR >> 16) & 0xFF) / 255.0F;
  private static final float PAGE_GREEN = (float) ((PAGE_COLOUR >> 8) & 0xFF) / 255.0F;
  private static final float PAGE_BLUE = (float) (PAGE_COLOUR & 0xFF) / 255.0F;

  @Nullable
  private static ShaderInstance bestiarySketchShader;

  private final Minecraft minecraft = Minecraft.getInstance();
  private final ResourceLocation snapshotTextureLocation = new ResourceLocation("skada",
      "dynamic/bestiary_snapshot_" + NEXT_DYNAMIC_TEXTURE_ID.incrementAndGet());
  @Nullable
  private DynamicTexture snapshotTexture;

  @Nullable
  private TextureTarget captureTarget;
  @Nullable
  private TextureTarget sketchTarget;
  @Nullable
  private LivingEntity cachedEntity;
  @Nullable
  private SnapshotRequest cachedRequest;
  @Nullable
  private SketchSnapshot cachedSnapshot;

  /**
   * Registers the active sketch composite shader.
   *
   * @param event the mod-bus shader registration event
   * @throws IOException if the shader resources cannot be loaded
   */
  public static void registerShaders(RegisterShadersEvent event) throws IOException {
    event.registerShader(new ShaderInstance(event.getResourceProvider(), BESTIARY_SKETCH_SHADER,
        DefaultVertexFormat.POSITION_TEX), shader -> bestiarySketchShader = shader);
  }

  /**
   * Describes the off-screen capture layout for one entity snapshot.
   *
    * The capture canvas is sized from the entity's measured dimensions, then the
    * entity is rendered at the supplied GUI anchor with a fixed pose.
   */
  private record SnapshotRequest(int captureWidth, int captureHeight, int renderX, int renderY, int renderScale) {

    public SnapshotRequest {
      if (captureWidth <= 0 || captureHeight <= 0 || renderScale <= 0) {
        throw new IllegalArgumentException("Snapshot dimensions and scale must be positive");
      }
    }
  }

  /**
   * Holds one inclusive visible rectangle inside a render target.
   */
  private record SnapshotBounds(int minX, int minY, int maxX, int maxY) {

    private static final SnapshotBounds EMPTY = new SnapshotBounds(0, 0, -1, -1);

    /**
     * Returns whether the rectangle contains any pixels.
     *
     * @return {@code true} when the rectangle is not empty
     */
    private boolean hasVisiblePixels() {
      return maxX >= minX && maxY >= minY;
    }

    /**
     * Returns the rectangle width in pixels.
     *
     * @return the inclusive width, or {@code 0} when empty
     */
    private int width() {
      return hasVisiblePixels() ? maxX - minX + 1 : 0;
    }

    /**
     * Returns the rectangle height in pixels.
     *
     * @return the inclusive height, or {@code 0} when empty
     */
    private int height() {
      return hasVisiblePixels() ? maxY - minY + 1 : 0;
    }
  }

  /**
   * Returns the processed texture metadata for one cached snapshot.
   *
   * The returned texture location is registered with Minecraft's texture
   * manager and points at this renderer's internal cropped target, so callers
   * can draw it through {@link GuiGraphics#blit(ResourceLocation, int, int, int,
   * int, float, float, int, int, int, int)}.
   */
  public record SketchSnapshot(ResourceLocation textureLocation, int width, int height) {

    public SketchSnapshot {
      if (textureLocation == null) {
        throw new IllegalArgumentException("Snapshot texture location must not be null");
      }
      if (width <= 0 || height <= 0) {
        throw new IllegalArgumentException("Snapshot dimensions must be positive");
      }
    }
  }

  /**
   * Returns one shader-processed snapshot for the supplied entity.
   *
   * The pipeline is intentionally simple: render the entity into an off-screen
   * target through {@link InventoryScreen#renderEntityInInventory(GuiGraphics,
   * int, int, int, Quaternionf, Quaternionf, LivingEntity)}, run the custom
   * sketch shader across that full target, then crop the final texture to the
   * measured visible alpha bounds.
   *
   * @param entity the entity to capture
   * @return the processed snapshot metadata, or {@code null} when no visible
   *     snapshot could be produced
   */
  @Nullable
  public SketchSnapshot getSnapshot(LivingEntity entity) {
    if (bestiarySketchShader == null) {
      return null;
    }

    SnapshotRequest request = createSnapshotRequest(entity);
    if (cachedEntity != entity || !request.equals(cachedRequest) || cachedSnapshot == null) {
      renderSnapshot(entity, request);
      cachedEntity = entity;
      cachedRequest = request;
    }

    return cachedSnapshot;
  }

  /**
   * Releases the capture and processed snapshot targets when the screen closes.
   */
  public void close() {
    minecraft.getTextureManager().release(snapshotTextureLocation);
    snapshotTexture = null;
    if (captureTarget != null) {
      captureTarget.destroyBuffers();
      captureTarget = null;
    }
    if (sketchTarget != null) {
      sketchTarget.destroyBuffers();
      sketchTarget = null;
    }

    cachedEntity = null;
    cachedRequest = null;
    cachedSnapshot = null;
  }

  /**
   * Uploads one freshly cropped snapshot image into the renderer-owned dynamic
   * texture registration.
   *
   * @param snapshotImage the top-down cropped snapshot pixels
   */
  private void uploadSnapshotTexture(NativeImage snapshotImage) {
    snapshotTexture = new DynamicTexture(snapshotImage);
    minecraft.getTextureManager().register(snapshotTextureLocation, snapshotTexture);
  }

  /**
    * Builds the initial off-screen capture request for one entity.
   *
    * The canvas grows with the measured entity bounds, then a centered
    * calibration pass can rebuild that request from the entity's actual visible
    * silhouette before normal retry logic takes over. A generous safety
    * multiplier covers models that extend beyond their collision box.
   *
   * @param entity the entity to snapshot
   * @return the capture layout used for the raw render
   */
  private SnapshotRequest createSnapshotRequest(LivingEntity entity) {
    float entityWidth = getMeasuredEntityWidth(entity);
    float entityHeight = getMeasuredEntityHeight(entity);
    float safeWidth = Math.max(MIN_CAPTURE_DIMENSION, entityWidth * CAPTURE_SIZE_MULTIPLIER);
    float safeHeight = Math.max(MIN_CAPTURE_DIMENSION, entityHeight * CAPTURE_SIZE_MULTIPLIER);
    int maxTextureSize = getMaxCaptureTextureSize();
    int captureWidth = clampCaptureSize(Math.round(safeWidth * PREFERRED_PIXELS_PER_BLOCK) + CAPTURE_PADDING * 2,
        maxTextureSize);
    int captureHeight = clampCaptureSize(Math.round(safeHeight * PREFERRED_PIXELS_PER_BLOCK) + CAPTURE_PADDING * 2,
        maxTextureSize);
    float availableWidth = Math.max(1.0F, captureWidth - CAPTURE_PADDING * 2.0F);
    float availableHeight = Math.max(1.0F, captureHeight - CAPTURE_PADDING * 2.0F);
    int renderScale = Math.max(1, (int) Math.floor(Math.min(availableWidth / safeWidth, availableHeight / safeHeight)));
    return new SnapshotRequest(captureWidth, captureHeight, captureWidth / 2,
        captureHeight - CAPTURE_PADDING, renderScale);
  }

  /**
   * Builds the reduced-scale centered request used for the calibration pass.
   *
   * Centering the render anchor avoids assuming the entity origin behaves like a
   * feet-at-bottom model, which is exactly what breaks on floating or
   * asymmetric renderers such as squid, ghast, or dragon previews.
   *
   * @param request the initial full-scale request
   * @return the centered calibration request
   */
  private SnapshotRequest createCalibrationRequest(SnapshotRequest request, int maxTextureSize) {
    int calibrationWidth = clampCaptureSize(
      Mth.ceil((float) request.captureWidth() * CALIBRATION_CANVAS_MULTIPLIER) + CALIBRATION_EXTRA_CANVAS,
      maxTextureSize);
    int calibrationHeight = clampCaptureSize(
      Mth.ceil((float) request.captureHeight() * CALIBRATION_CANVAS_MULTIPLIER) + CALIBRATION_EXTRA_CANVAS,
      maxTextureSize);
    int calibrationScale = Math.max(1,
        Mth.floor((float) request.renderScale() * CALIBRATION_SCALE_MULTIPLIER));
    return new SnapshotRequest(calibrationWidth, calibrationHeight, calibrationWidth / 2,
        calibrationHeight / 2, calibrationScale);
  }

    /**
     * Returns the widest useful entity footprint for snapshot sizing.
     *
     * Renderer-scale and multipart oddities do not always match the collision box,
     * so sizing takes the largest width from the normal box, culling box, and pose
     * dimensions.
     *
     * @param entity the entity being captured
     * @return the widest measured footprint in world blocks
     */
    private float getMeasuredEntityWidth(LivingEntity entity) {
    AABB boundingBox = entity.getBoundingBox();
    AABB cullingBox = entity.getBoundingBoxForCulling();
    EntityDimensions dimensions = entity.getDimensions(entity.getPose());
    return (float) Math.max(
      Math.max(Math.max(boundingBox.getXsize(), boundingBox.getZsize()),
        Math.max(cullingBox.getXsize(), cullingBox.getZsize())),
      dimensions.width);
    }

    /**
     * Returns the tallest useful entity footprint for snapshot sizing.
     *
     * @param entity the entity being captured
     * @return the tallest measured footprint in world blocks
     */
    private float getMeasuredEntityHeight(LivingEntity entity) {
    AABB boundingBox = entity.getBoundingBox();
    AABB cullingBox = entity.getBoundingBoxForCulling();
    EntityDimensions dimensions = entity.getDimensions(entity.getPose());
    return (float) Math.max(Math.max(boundingBox.getYsize(), cullingBox.getYsize()), dimensions.height);
    }

  /**
   * Runs the reduced-scale centered calibration pass and rebuilds the first
   * full-scale request from the measured silhouette.
   *
   * The render scale stays unchanged. Only the initial capture footprint and
   * render anchor are corrected so the existing full-scale retry and crop logic
   * can continue operating as-is.
   *
   * @param entity the entity being captured
   * @param request the initial dimension-derived request
   * @param maxTextureSize the current hardware texture-size limit
   * @return the calibrated request, or the original request when calibration fails
   */
  private SnapshotRequest calibrateSnapshotRequest(LivingEntity entity, SnapshotRequest request, int maxTextureSize) {
    SnapshotRequest calibrationRequest = createCalibrationRequest(request, maxTextureSize);
    ensureTargets(calibrationRequest.captureWidth(), calibrationRequest.captureHeight());
    if (captureTarget == null) {
      return request;
    }

    renderToTarget(captureTarget, () -> renderCapture(entity, calibrationRequest));
    SnapshotBounds calibrationBounds = measureVisibleBounds(captureTarget, SNAPSHOT_ALPHA_THRESHOLD);
    if (!calibrationBounds.hasVisiblePixels()) {
      return request;
    }

    return createSnapshotRequestFromMeasuredBounds(request, calibrationRequest, calibrationBounds, maxTextureSize);
  }

  /**
   * Rebuilds one full-scale request from a centered calibration capture.
   *
   * The measured silhouette is converted back into offsets around the render
   * anchor, then scaled up to the intended full-scale render. That corrects the
   * capture origin for entities whose renderer origin is not aligned with their
   * collision box or feet position.
   *
   * @param request the original full-scale request
   * @param calibrationRequest the centered calibration request
   * @param measuredBounds the visible bounds from the calibration render
   * @param maxTextureSize the current hardware texture-size limit
   * @return the rebuilt full-scale request
   */
  private SnapshotRequest createSnapshotRequestFromMeasuredBounds(SnapshotRequest request,
                                                                 SnapshotRequest calibrationRequest,
                                                                 SnapshotBounds measuredBounds,
                                                                 int maxTextureSize) {
    SnapshotBounds guiMeasuredBounds = toGuiSpaceBounds(measuredBounds, calibrationRequest.captureHeight());
    float scaleRatio = (float) request.renderScale() / (float) calibrationRequest.renderScale();
    float minOffsetX = (float) guiMeasuredBounds.minX() - (float) calibrationRequest.renderX();
    float maxOffsetX = (float) guiMeasuredBounds.maxX() - (float) calibrationRequest.renderX();
    float minOffsetY = (float) guiMeasuredBounds.minY() - (float) calibrationRequest.renderY();
    float maxOffsetY = (float) guiMeasuredBounds.maxY() - (float) calibrationRequest.renderY();

    int scaledMinOffsetX = Mth.floor(minOffsetX * scaleRatio);
    int scaledMaxOffsetX = Mth.ceil(maxOffsetX * scaleRatio);
    int scaledMinOffsetY = Mth.floor(minOffsetY * scaleRatio);
    int scaledMaxOffsetY = Mth.ceil(maxOffsetY * scaleRatio);

    int desiredWidth = scaledMaxOffsetX - scaledMinOffsetX + 1 + CAPTURE_PADDING * 2;
    int desiredHeight = scaledMaxOffsetY - scaledMinOffsetY + 1 + CAPTURE_PADDING * 2;
    int calibratedWidth = Math.max(request.captureWidth(), clampCaptureSize(desiredWidth, maxTextureSize));
    int calibratedHeight = Math.max(request.captureHeight(), clampCaptureSize(desiredHeight, maxTextureSize));
    int renderX = Mth.clamp(CAPTURE_PADDING - scaledMinOffsetX, 0, calibratedWidth - 1);
    int renderY = Mth.clamp(CAPTURE_PADDING - scaledMinOffsetY, 0, calibratedHeight - 1);

    SnapshotRequest calibratedRequest = new SnapshotRequest(calibratedWidth, calibratedHeight, renderX, renderY,
        request.renderScale());
    return calibratedRequest.equals(request) ? request : calibratedRequest;
  }

  /**
   * Clamps one desired capture dimension to the supported texture range.
   *
   * @param desiredSize the measured size requirement
   * @param maxTextureSize the current hardware texture-size limit
   * @return a legal render target dimension
   */
  private int clampCaptureSize(int desiredSize, int maxTextureSize) {
    return Math.max(MIN_CAPTURE_SIZE, Math.min(desiredSize, maxTextureSize));
  }

  /**
   * Returns the maximum off-screen texture size this preview renderer should use.
   *
   * The bestiary preview is ultimately drawn into a relatively small UI region,
   * so very large boss snapshots do not benefit from multi-thousand-pixel
   * render targets. Capping the working size keeps first-time snapshot
   * generation responsive while still preserving more than enough detail for the
   * page.
   *
   * @return the effective capture texture-size cap for preview rendering
   */
  private int getMaxCaptureTextureSize() {
    return Math.max(MIN_CAPTURE_SIZE,
        Math.min(RenderSystem.maxSupportedTextureSize(), MAX_CAPTURE_TARGET_SIZE));
  }

  /**
   * Renders a fresh raw capture, runs the sketch shader, then crops the final
   * texture down to the visible entity footprint.
   *
    * The normal full-scale capture path remains the default. A reduced-scale
    * centered calibration pass is only used when that first capture shows a
    * margin shortfall that suggests the renderer origin does not align with the
    * expected feet-at-bottom anchor. After that, each full-scale retry can still
    * expand the canvas further if the capture hits a render-target edge.
   *
   * @param entity the entity being captured
   * @param initialRequest the derived capture layout
   */
  private void renderSnapshot(LivingEntity entity, SnapshotRequest initialRequest) {
    if (bestiarySketchShader == null) {
      cachedSnapshot = null;
      return;
    }

    int maxTextureSize = getMaxCaptureTextureSize();
    SnapshotRequest request = initialRequest;
    boolean calibrationAttempted = false;
    for (int attempt = 0; attempt <= MAX_CAPTURE_RETRIES; attempt++) {
      ensureTargets(request.captureWidth(), request.captureHeight());
      if (captureTarget == null || sketchTarget == null) {
        cachedSnapshot = null;
        return;
      }

      SnapshotRequest renderRequest = request;
      renderToTarget(captureTarget, () -> renderCapture(entity, renderRequest));
      SnapshotBounds captureBounds = measureVisibleBounds(captureTarget, SNAPSHOT_ALPHA_THRESHOLD);
      renderToTarget(sketchTarget, this::renderSketchPass);

      SnapshotBounds sketchBounds = measureVisibleBounds(sketchTarget, SNAPSHOT_ALPHA_THRESHOLD);
      boolean rawCaptureFallback = shouldUseRawCaptureFallback(captureBounds, sketchBounds);
      SnapshotBounds sketchCropBounds = unionBounds(captureBounds, sketchBounds, sketchTarget.width, sketchTarget.height);
      SnapshotBounds outputBounds = rawCaptureFallback ? captureBounds : sketchCropBounds;
      if (!outputBounds.hasVisiblePixels()) {
        if (!calibrationAttempted) {
          SnapshotRequest calibratedRequest = calibrateSnapshotRequest(entity, initialRequest, maxTextureSize);
          calibrationAttempted = true;
          if (!calibratedRequest.equals(request)) {
            request = calibratedRequest;
            continue;
          }
        }

        cachedSnapshot = null;
        return;
      }

      if (!calibrationAttempted
          && (hasMarginShortfall(request, outputBounds)
          || hasSuspiciouslySmallFootprint(entity, request, outputBounds))) {
        SnapshotRequest calibratedRequest = calibrateSnapshotRequest(entity, initialRequest, maxTextureSize);
        calibrationAttempted = true;
        if (!calibratedRequest.equals(request)) {
          request = calibratedRequest;
          continue;
        }
      }

      SnapshotRequest refinedRequest = refineSnapshotRequest(request, outputBounds, maxTextureSize);
      if (attempt == MAX_CAPTURE_RETRIES || refinedRequest.equals(request)) {
        cropSnapshotTarget(rawCaptureFallback ? captureTarget : sketchTarget, outputBounds);
        return;
      }

      request = refinedRequest;
    }

    cachedSnapshot = null;
  }

  /**
   * Returns whether the current capture is short on safety margin on any side.
   *
   * This is used to decide when the centered calibration path is warranted.
   * Stable mobs that already fit comfortably should stay on the original
   * bottom-biased request so their established framing does not move.
   *
   * @param request the request used for the current capture
   * @param bounds the measured visible bounds from that capture
   * @return {@code true} when any side has less than the desired safety margin
   */
  private boolean hasMarginShortfall(SnapshotRequest request, SnapshotBounds bounds) {
    SnapshotBounds guiBounds = toGuiSpaceBounds(bounds, request.captureHeight());
    if (!guiBounds.hasVisiblePixels()) {
      return false;
    }

    int leftMargin = guiBounds.minX();
    int topMargin = guiBounds.minY();
    int rightMargin = request.captureWidth() - 1 - guiBounds.maxX();
    int bottomMargin = request.captureHeight() - 1 - guiBounds.maxY();
    return leftMargin < RETARGET_MARGIN || topMargin < RETARGET_MARGIN || rightMargin < RETARGET_MARGIN
        || bottomMargin < RETARGET_MARGIN;
  }

  /**
   * Returns whether the visible capture is implausibly small for the entity.
   *
   * When only a shadow or a clipped center fragment survives the first pass,
   * the capture can look comfortably inside the target while still missing most
   * of the model. Comparing the measured silhouette against the expected
   * projected entity footprint catches those cases and routes them into the
   * centered calibration path.
   *
   * @param entity the entity being captured
   * @param request the request used for the current capture
   * @param bounds the measured visible bounds from that capture
   * @return {@code true} when the visible footprint is unexpectedly small
   */
  private boolean hasSuspiciouslySmallFootprint(LivingEntity entity, SnapshotRequest request, SnapshotBounds bounds) {
    if (!bounds.hasVisiblePixels()) {
      return false;
    }

    float expectedWidth = Math.max(1.0F, getMeasuredEntityWidth(entity) * (float) request.renderScale());
    float expectedHeight = Math.max(1.0F, getMeasuredEntityHeight(entity) * (float) request.renderScale());
    float widthRatio = (float) bounds.width() / expectedWidth;
    float heightRatio = (float) bounds.height() / expectedHeight;
    return widthRatio < MIN_EXPECTED_VISIBLE_RATIO || heightRatio < MIN_EXPECTED_VISIBLE_RATIO;
  }

  /**
   * Returns whether the processed sketch lost too much of the raw silhouette.
   *
   * Most entities survive the sketch pass with only minor edge loss. When the
   * processed bounds collapse far below the raw capture bounds, the stylization
   * is no longer representing the model faithfully and the renderer should fall
   * back to the raw capture for that snapshot.
   *
   * @param captureBounds the visible bounds from the raw entity capture
   * @param sketchBounds the visible bounds from the processed sketch target
   * @return {@code true} when the processed sketch underfills the raw capture too much
   */
  private boolean shouldUseRawCaptureFallback(SnapshotBounds captureBounds, SnapshotBounds sketchBounds) {
    if (!captureBounds.hasVisiblePixels()) {
      return false;
    }
    if (!sketchBounds.hasVisiblePixels()) {
      return true;
    }

    float widthRatio = (float) sketchBounds.width() / (float) captureBounds.width();
    float heightRatio = (float) sketchBounds.height() / (float) captureBounds.height();
    float captureArea = (float) captureBounds.width() * (float) captureBounds.height();
    float sketchArea = (float) sketchBounds.width() * (float) sketchBounds.height();
    float areaRatio = sketchArea / Math.max(1.0F, captureArea);
    return widthRatio < MIN_SKETCH_DIMENSION_RATIO || heightRatio < MIN_SKETCH_DIMENSION_RATIO
        || areaRatio < MIN_SKETCH_AREA_RATIO;
  }

  /**
   * Rebuilds one capture request from the measured silhouette of the last pass.
   *
    * The render scale stays fixed, but the next pass can expand the canvas and
    * shift the entity origin only when a side is short on margin. That keeps
    * already-good captures stable while still adding space exactly where clipped
    * geometry needs it.
   *
   * @param request the request used for the current capture
   * @param bounds the measured visible bounds from that capture
   * @param maxTextureSize the current hardware texture-size limit
   * @return a refined request, or the original request when no adjustment is needed
   */
  private SnapshotRequest refineSnapshotRequest(SnapshotRequest request, SnapshotBounds bounds, int maxTextureSize) {
    SnapshotBounds guiBounds = toGuiSpaceBounds(bounds, request.captureHeight());
    int leftMargin = guiBounds.minX();
    int topMargin = guiBounds.minY();
    int rightMargin = request.captureWidth() - 1 - guiBounds.maxX();
    int bottomMargin = request.captureHeight() - 1 - guiBounds.maxY();

    int extraLeft = Math.max(0, RETARGET_MARGIN - leftMargin);
    int extraTop = Math.max(0, RETARGET_MARGIN - topMargin);
    int extraRight = Math.max(0, RETARGET_MARGIN - rightMargin);
    int extraBottom = Math.max(0, RETARGET_MARGIN - bottomMargin);
    if (extraLeft == 0 && extraTop == 0 && extraRight == 0 && extraBottom == 0) {
      return request;
    }

    int refinedWidth = clampCaptureSize(request.captureWidth() + extraLeft + extraRight, maxTextureSize);
    int refinedHeight = clampCaptureSize(request.captureHeight() + extraTop + extraBottom, maxTextureSize);
    int appliedWidthGrowth = Math.max(0, refinedWidth - request.captureWidth());
    int appliedHeightGrowth = Math.max(0, refinedHeight - request.captureHeight());
    int refinedRenderX = Mth.clamp(request.renderX() + Math.min(extraLeft, appliedWidthGrowth), 0, refinedWidth - 1);
    int refinedRenderY = Mth.clamp(request.renderY() + Math.min(extraTop, appliedHeightGrowth), 0,
        refinedHeight - 1);

    SnapshotRequest refinedRequest = new SnapshotRequest(refinedWidth, refinedHeight, refinedRenderX, refinedRenderY,
        request.renderScale());
    return refinedRequest.equals(request) ? request : refinedRequest;
  }

  /**
   * Ensures the raw capture target and processed target match the requested
   * off-screen canvas size.
   *
   * @param width the desired target width in pixels
   * @param height the desired target height in pixels
   */
  private void ensureTargets(int width, int height) {
    captureTarget = ensureTarget(captureTarget, width, height, GL11.GL_NEAREST);
    sketchTarget = ensureTarget(sketchTarget, width, height, GL11.GL_NEAREST);
  }

  /**
   * Creates or resizes one internal render target as needed.
   *
   * @param target the current target instance, if any
   * @param width the required target width
   * @param height the required target height
   * @param filterMode the texture filter to apply after resize
   * @return a target matching the requested size and filter mode
   */
  private TextureTarget ensureTarget(@Nullable TextureTarget target, int width, int height, int filterMode) {
    if (target == null) {
      target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
      target.setFilterMode(filterMode);
      return target;
    }

    if (target.width != width || target.height != height) {
      target.resize(width, height, Minecraft.ON_OSX);
    }
    target.setFilterMode(filterMode);
    return target;
  }

  /**
   * Flushes the shared GUI buffer before a render target switch.
   */
  private void flushGuiBuffers() {
    RenderSystem.disableDepthTest();
    minecraft.renderBuffers().bufferSource().endBatch();
    RenderSystem.enableDepthTest();
  }

  /**
   * Clears and binds an internal render target, applies the shared GUI
   * projection setup, and runs one off-screen render pass.
   *
   * @param target the off-screen target to render into
   * @param renderPass the rendering work to run while the target is bound
   */
  private void renderToTarget(TextureTarget target, Runnable renderPass) {
    RenderTarget mainTarget = minecraft.getMainRenderTarget();
    flushGuiBuffers();

    target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
    target.clear(Minecraft.ON_OSX);
    target.bindWrite(true);
    RenderSystem.enableDepthTest();
    RenderSystem.backupProjectionMatrix();
    RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, (float) target.width,
        (float) target.height, 0.0F, 1000.0F, ForgeHooksClient.getGuiFarPlane()),
        VertexSorting.ORTHOGRAPHIC_Z);

    PoseStack modelViewStack = RenderSystem.getModelViewStack();
    modelViewStack.pushPose();
    modelViewStack.setIdentity();
    modelViewStack.translate(0.0D, 0.0D, 1000.0D - ForgeHooksClient.getGuiFarPlane());
    RenderSystem.applyModelViewMatrix();

    try {
      renderPass.run();
    } finally {
      modelViewStack.popPose();
      RenderSystem.applyModelViewMatrix();
      RenderSystem.restoreProjectionMatrix();
      mainTarget.bindWrite(true);
      RenderSystem.disableDepthTest();
    }
  }

  /**
   * Renders the raw entity capture into the current off-screen target.
   *
   * @param entity the entity being captured
   * @param request the derived capture layout
   */
  private void renderCapture(LivingEntity entity, SnapshotRequest request) {
    GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
    renderStaticEntitySnapshot(graphics, entity, request);
  }

  /**
   * Builds the fixed preview pose used for snapshot capture.
   *
   * @return the model rotation used for the off-screen render
   */
  private static Quaternionf createSnapshotPose() {
    return new Quaternionf().rotateZ((float) Math.PI)
        .rotateY((float) Math.toRadians(-PREVIEW_YAW_DEGREES))
        .rotateX((float) Math.toRadians(-PREVIEW_PITCH_DEGREES));
  }

  /**
   * Builds the camera orientation paired with the fixed preview pose.
   *
   * @return the camera orientation before vanilla conjugates it
   */
  private static Quaternionf createCameraOrientation() {
    return new Quaternionf().rotateY((float) Math.toRadians(PREVIEW_YAW_DEGREES))
        .rotateX((float) Math.toRadians(PREVIEW_PITCH_DEGREES));
  }

  /**
   * Draws the entity through the vanilla inventory helper after temporarily
   * normalizing its pose state.
   *
   * @param graphics the off-screen GUI graphics context
   * @param entity the entity to render
   * @param request the derived capture layout
   */
  private void renderStaticEntitySnapshot(GuiGraphics graphics, LivingEntity entity, SnapshotRequest request) {
    var entityRenderDispatcher = minecraft.getEntityRenderDispatcher();
    float previousBodyRot = entity.yBodyRot;
    float previousBodyRotO = entity.yBodyRotO;
    float previousYRot = entity.getYRot();
    float previousYRotO = entity.yRotO;
    float previousXRot = entity.getXRot();
    float previousXRotO = entity.xRotO;
    double previousXOld = entity.xOld;
    double previousYOld = entity.yOld;
    double previousZOld = entity.zOld;
    double previousXo = entity.xo;
    double previousYo = entity.yo;
    double previousZo = entity.zo;
    float previousHeadRotO = entity.yHeadRotO;
    float previousHeadRot = entity.yHeadRot;
    int previousTickCount = entity.tickCount;
    boolean previousBatResting = entity instanceof Bat bat && bat.isResting();
    float previousSquidXBodyRot = 0.0F;
    float previousSquidXBodyRotO = 0.0F;
    float previousSquidZBodyRot = 0.0F;
    float previousSquidZBodyRotO = 0.0F;
    float previousSquidTentacleMovement = 0.0F;
    float previousSquidOldTentacleMovement = 0.0F;
    float previousSquidTentacleAngle = 0.0F;
    float previousSquidOldTentacleAngle = 0.0F;
    if (entity instanceof Squid squid) {
      previousSquidXBodyRot = squid.xBodyRot;
      previousSquidXBodyRotO = squid.xBodyRotO;
      previousSquidZBodyRot = squid.zBodyRot;
      previousSquidZBodyRotO = squid.zBodyRotO;
      previousSquidTentacleMovement = squid.tentacleMovement;
      previousSquidOldTentacleMovement = squid.oldTentacleMovement;
      previousSquidTentacleAngle = squid.tentacleAngle;
      previousSquidOldTentacleAngle = squid.oldTentacleAngle;
    }

    entity.yBodyRot = PREVIEW_BODY_ROT;
    entity.yBodyRotO = PREVIEW_BODY_ROT;
    entity.setYRot(PREVIEW_BODY_ROT);
    entity.yRotO = PREVIEW_BODY_ROT;
    entity.setXRot(0.0F);
    entity.xRotO = 0.0F;
    entity.setOldPosAndRot();
    entity.yHeadRot = entity.getYRot();
    entity.yHeadRotO = entity.getYRot();
    entity.tickCount = Math.max(1, previousTickCount);

    if (entity instanceof Bat bat) {
      bat.setResting(false);
    }
    if (entity instanceof Squid squid) {
      squid.xBodyRot = 0.0F;
      squid.xBodyRotO = 0.0F;
      squid.zBodyRot = 0.0F;
      squid.zBodyRotO = 0.0F;
      squid.tentacleMovement = 0.0F;
      squid.oldTentacleMovement = 0.0F;
      squid.tentacleAngle = 0.0F;
      squid.oldTentacleAngle = 0.0F;
    }

    entityRenderDispatcher.setRenderShadow(false);
    try {
      InventoryScreen.renderEntityInInventory(graphics, request.renderX(), request.renderY(), request.renderScale(),
          createSnapshotPose(), createCameraOrientation(), entity);
    } finally {
      entityRenderDispatcher.setRenderShadow(true);
      entity.yBodyRot = previousBodyRot;
      entity.yBodyRotO = previousBodyRotO;
      entity.setYRot(previousYRot);
      entity.yRotO = previousYRotO;
      entity.setXRot(previousXRot);
      entity.xRotO = previousXRotO;
      entity.xOld = previousXOld;
      entity.yOld = previousYOld;
      entity.zOld = previousZOld;
      entity.xo = previousXo;
      entity.yo = previousYo;
      entity.zo = previousZo;
      entity.yHeadRotO = previousHeadRotO;
      entity.yHeadRot = previousHeadRot;
      entity.tickCount = previousTickCount;

      if (entity instanceof Bat bat) {
        bat.setResting(previousBatResting);
      }
      if (entity instanceof Squid squid) {
        squid.xBodyRot = previousSquidXBodyRot;
        squid.xBodyRotO = previousSquidXBodyRotO;
        squid.zBodyRot = previousSquidZBodyRot;
        squid.zBodyRotO = previousSquidZBodyRotO;
        squid.tentacleMovement = previousSquidTentacleMovement;
        squid.oldTentacleMovement = previousSquidOldTentacleMovement;
        squid.tentacleAngle = previousSquidTentacleAngle;
        squid.oldTentacleAngle = previousSquidOldTentacleAngle;
      }
    }
  }

  /**
   * Applies the sketch shader across the raw capture target.
   */
  private void renderSketchPass() {
    if (captureTarget == null || sketchTarget == null || bestiarySketchShader == null) {
      return;
    }

    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    RenderSystem.setShaderTexture(0, captureTarget.getColorTextureId());
    RenderSystem.setShader(() -> bestiarySketchShader);
    bestiarySketchShader.setSampler("DepthSampler", captureTarget.getDepthTextureId());
    bestiarySketchShader.safeGetUniform("InputSize").set((float) captureTarget.width, (float) captureTarget.height);
    bestiarySketchShader.safeGetUniform("SourceEdgeFadeFlags").set(0.0F, 0.0F, 0.0F, 0.0F);
    bestiarySketchShader.safeGetUniform("ToneDebugMode").set(0.0F);
    bestiarySketchShader.safeGetUniform("InkColor").set(INK_RED, INK_GREEN, INK_BLUE, 1.0F);
    bestiarySketchShader.safeGetUniform("PageColor").set(PAGE_RED, PAGE_GREEN, PAGE_BLUE, 1.0F);

    Matrix4f matrix = new Matrix4f();
    BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
    bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
    bufferBuilder.vertex(matrix, 0.0F, 0.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
    bufferBuilder.vertex(matrix, 0.0F, sketchTarget.height, 0.0F).uv(0.0F, 0.0F).endVertex();
    bufferBuilder.vertex(matrix, sketchTarget.width, sketchTarget.height, 0.0F).uv(1.0F, 0.0F).endVertex();
    bufferBuilder.vertex(matrix, sketchTarget.width, 0.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
    BufferUploader.drawWithShader(bufferBuilder.end());

    RenderSystem.disableBlend();
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
  }

  /**
   * Crops the processed target to the measured visible bounds.
   *
   * @param cropBounds the visible rectangle inside the processed target
   */
  private void cropSnapshotTarget(@Nullable RenderTarget sourceTarget, SnapshotBounds cropBounds) {
    if (sourceTarget == null || !cropBounds.hasVisiblePixels()) {
      cachedSnapshot = null;
      return;
    }

    try (NativeImage sourceImage = new NativeImage(sourceTarget.width, sourceTarget.height, false)) {
      sourceTarget.bindRead();
      try {
        sourceImage.downloadTexture(0, false);
      } finally {
        sourceTarget.unbindRead();
      }

      NativeImage snapshotImage = copyCroppedSnapshot(sourceImage, cropBounds);
      uploadSnapshotTexture(snapshotImage);
      cachedSnapshot = new SketchSnapshot(snapshotTextureLocation, snapshotImage.getWidth(), snapshotImage.getHeight());
    }
  }

  /**
   * Copies one downloaded render-target crop into a top-down GUI texture image.
   *
   * NativeImage data downloaded from a render target uses OpenGL's bottom-up
   * row order, so this helper flips rows while copying into the returned image.
   *
   * @param sourceImage the full downloaded sketch target image
   * @param cropBounds the inclusive visible bounds inside that image
   * @return a cropped image in standard GUI top-down orientation
   */
  private NativeImage copyCroppedSnapshot(NativeImage sourceImage, SnapshotBounds cropBounds) {
    NativeImage snapshotImage = new NativeImage(cropBounds.width(), cropBounds.height(), false);
    int cropWidth = cropBounds.width();
    int cropHeight = cropBounds.height();

    for (int sourceY = cropBounds.minY(); sourceY <= cropBounds.maxY(); sourceY++) {
      int destY = cropHeight - 1 - (sourceY - cropBounds.minY());
      for (int sourceX = cropBounds.minX(); sourceX <= cropBounds.maxX(); sourceX++) {
        int destX = sourceX - cropBounds.minX();
        snapshotImage.setPixelRGBA(destX, destY, sourceImage.getPixelRGBA(sourceX, sourceY));
      }
    }

    return snapshotImage;
  }

  /**
   * Converts measured render-target bounds into the same top-down coordinate
   * space used by GUI preview anchors.
   *
   * {@link NativeImage#downloadTexture(int, boolean)} reads render-target rows
   * in OpenGL's bottom-up order. Cropping still needs those native bounds, but
   * calibration and margin math must compare against the GUI-space render
   * anchor, which means the Y axis must be flipped first.
   *
   * @param bounds the measured bounds in downloaded render-target space
   * @param targetHeight the render target height the bounds were measured from
   * @return the equivalent bounds in top-down GUI pixel space
   */
  private SnapshotBounds toGuiSpaceBounds(SnapshotBounds bounds, int targetHeight) {
    if (!bounds.hasVisiblePixels() || targetHeight <= 0) {
      return bounds;
    }

    return new SnapshotBounds(bounds.minX(), targetHeight - 1 - bounds.maxY(), bounds.maxX(),
        targetHeight - 1 - bounds.minY());
  }

  /**
   * Returns the union of the raw and processed visible bounds.
   *
   * The shader's lightest tone is transparent on purpose, so the raw capture
   * bounds remain the safety net that keeps bright parts of the silhouette from
   * being trimmed out of the final texture.
   *
   * @param captureBounds the visible bounds from the raw entity capture
   * @param sketchBounds the visible bounds from the processed sketch target
   * @param width the processed target width
   * @param height the processed target height
   * @return the combined visible bounds, or the non-empty input when only one exists
   */
  private SnapshotBounds unionBounds(SnapshotBounds captureBounds, SnapshotBounds sketchBounds, int width,
                                     int height) {
    if (!captureBounds.hasVisiblePixels()) {
      return sketchBounds;
    }
    if (!sketchBounds.hasVisiblePixels()) {
      return captureBounds;
    }

    return new SnapshotBounds(
        Math.max(0, Math.min(captureBounds.minX(), sketchBounds.minX())),
        Math.max(0, Math.min(captureBounds.minY(), sketchBounds.minY())),
        Math.min(width - 1, Math.max(captureBounds.maxX(), sketchBounds.maxX())),
        Math.min(height - 1, Math.max(captureBounds.maxY(), sketchBounds.maxY())));
  }

  /**
   * Measures the visible alpha rectangle inside the supplied render target.
   *
   * @param target the target to measure
   * @param alphaThreshold the minimum alpha that counts as visible
   * @return the visible alpha bounds, or {@link SnapshotBounds#EMPTY} when the
   *     target is fully transparent
   */
  private SnapshotBounds measureVisibleBounds(RenderTarget target, int alphaThreshold) {
    if (target.width <= 0 || target.height <= 0) {
      return SnapshotBounds.EMPTY;
    }

    try (NativeImage snapshot = new NativeImage(target.width, target.height, false)) {
      target.bindRead();
      snapshot.downloadTexture(0, false);

      int minX = snapshot.getWidth();
      int minY = snapshot.getHeight();
      int maxX = -1;
      int maxY = -1;

      for (int y = 0; y < snapshot.getHeight(); y++) {
        for (int x = 0; x < snapshot.getWidth(); x++) {
          int alpha = FastColor.ABGR32.alpha(snapshot.getPixelRGBA(x, y));
          if (alpha <= alphaThreshold) {
            continue;
          }

          minX = Math.min(minX, x);
          minY = Math.min(minY, y);
          maxX = Math.max(maxX, x);
          maxY = Math.max(maxY, y);
        }
      }

      if (maxX < minX || maxY < minY) {
        return SnapshotBounds.EMPTY;
      }

      return new SnapshotBounds(minX, minY, maxX, maxY);
    } finally {
      target.unbindRead();
    }
  }

}