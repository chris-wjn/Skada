package com.cwjn.skada.client.gui.tooltip;

import com.cwjn.skada.util.UtilText;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Guards column overlap and full-tooltip alignment without requiring a graphics context. */
class EquipmentTooltipComponentTest {
  /** Initializes the registries used by ItemStack's static codecs before mocking a stack. */
  @BeforeAll
  static void bootstrapMinecraft() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }

  /** Maxima on different rows must both fit, and a longer title must move every value together. */
  @Test
  void differentRowWidthsNeverOverlapAndValuesFollowTheTitleEdge() throws ReflectiveOperationException {
    Minecraft client = mock(Minecraft.class);
    Font font = mock(Font.class);
    var fontField = Minecraft.class.getDeclaredField("font");
    fontField.setAccessible(true);
    fontField.set(client, font);
    ItemStack stack = mock(ItemStack.class);
    when(stack.getHoverName()).thenReturn(Component.literal("Test").withStyle(style -> style.withColor(0x79B9C6)));
    when(font.width(any(FormattedText.class)))
        .thenAnswer(call -> ((FormattedText) call.getArgument(0)).getString().length() * 6);
    try (var minecraft = mockStatic(Minecraft.class); var formatter = mockStatic(UtilText.class)) {
      minecraft.when(Minecraft::getInstance).thenReturn(client);
      formatter.when(() -> UtilText.pixelFontComponent(any(MutableComponent.class)))
          .thenAnswer(call -> ((MutableComponent) call.getArgument(0)).copy());
      var layout = new EquipmentTooltipComponent(stack) {};
      layout.stat("", Component.literal("B"), Component.literal("99999999"));
      layout.branchStat("", Component.literal("Long stat label"), Component.literal("1"));
      layout.branchStat("", Component.literal("C"), Component.literal("2"));
      layout.line(Component.literal("Hint"));
      Map<String, Float> positions = new HashMap<>();
      Map<String, Float> tops = new HashMap<>();
      when(font.drawInBatch(any(Component.class), anyFloat(), anyFloat(), anyInt(), anyBoolean(),
          any(), any(), any(), anyInt(), anyInt())).thenAnswer(call -> {
            positions.put(((Component) call.getArgument(0)).getString(), call.getArgument(1));
            tops.put(((Component) call.getArgument(0)).getString(), call.getArgument(2));
            return 0;
          });
      int naturalWidth = layout.getWidth(font);
      layout.renderText(font, 20, 30, new Matrix4f(), null);
      assertTrue(positions.get(" Long stat label") > positions.get(" B"), "Damage children must be indented");
      assertEquals(positions.get(" Long stat label"), positions.get(" C"));
      assertTrue(positions.get(" Long stat label") + font.width(Component.literal(" Long stat label"))
          < positions.get("99999999"), "Even the widest label must leave room for the widest value");
      float firstRight = positions.get("1") + font.width(Component.literal("1"));
      assertEquals(firstRight, positions.get("99999999") + font.width(Component.literal("99999999")));

      layout.setTooltipWidth(naturalWidth + 80);
      layout.renderText(font, 20, 30, new Matrix4f(), null);
      assertEquals(firstRight + 80, positions.get("1") + font.width(Component.literal("1")));
      assertEquals(firstRight + 80, positions.get("99999999") + font.width(Component.literal("99999999")));

      GuiGraphics graphics = mock(GuiGraphics.class);
      var buffers = mock(MultiBufferSource.BufferSource.class);
      var vertices = mock(VertexConsumer.class, RETURNS_SELF);
      when(graphics.bufferSource()).thenReturn(buffers);
      when(buffers.getBuffer(any())).thenReturn(vertices);
      when(graphics.pose()).thenReturn(new PoseStack());
      List<int[]> rectangles = new ArrayList<>();
      doAnswer(call -> {
        rectangles.add(new int[]{call.getArgument(0), call.getArgument(1), call.getArgument(2), call.getArgument(3)});
        return null;
      }).when(graphics).fill(anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
      layout.renderImage(font, 20, 30, graphics);
      var trunks = rectangles.stream().filter(rect -> rect[2] - rect[0] == 1 && rect[3] - rect[1] > 1).toList();
      assertFalse(trunks.isEmpty(), "Nested damage rows need a vertical connector");
      assertTrue(trunks.stream().anyMatch(rect -> rect[1] < tops.get(" Long stat label")),
          "The first branch must connect towards its parent row");
      assertTrue(trunks.stream().allMatch(rect -> rect[3] < tops.get("Hint")),
          "The connector must stop before the following non-damage content");
    }
  }

  /** The colour already carried by an elemental value must survive the neutral value palette. */
  @Test
  void semanticValueColourSurvivesFormatting() {
    try (var formatter = mockStatic(UtilText.class)) {
      formatter.when(() -> UtilText.pixelFontComponent(any(MutableComponent.class)))
          .thenAnswer(call -> Component.literal(((MutableComponent) call.getArgument(0)).getString()));
      var value = Component.literal("50%").withStyle(style -> style.withColor(0xE56B46));
      var formatted = EquipmentTooltipComponent.text(value, EquipmentTooltipComponent.VALUE);
      assertEquals(0xE56B46, formatted.getStyle().getColor().getValue());
    }
  }

}
