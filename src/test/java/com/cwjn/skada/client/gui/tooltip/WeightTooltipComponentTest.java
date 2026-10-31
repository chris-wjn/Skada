package com.cwjn.skada.client.gui.tooltip;

import com.cwjn.skada.data.SkadaData;
import com.cwjn.skada.data.damage.AttackSelection;
import com.cwjn.skada.data.registry.AttackType;
import com.cwjn.skada.util.EquipmentWeightUtil;
import com.cwjn.skada.util.Keybinds;
import com.cwjn.skada.util.UtilText;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.settings.KeyModifier;
import org.joml.Matrix4f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Guards fixed attack-icon positions, selection and separation from item mass. */
class WeightTooltipComponentTest {
  /** Initializes vanilla item codecs before mocking ItemStack. */
  @BeforeAll
  static void bootstrapMinecraft() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }

  /** Cycling must retain every icon's position while its single shortcut cue stays clear of the mass. */
  @Test
  void attackIconsStayStationaryAndClearOfWeight() throws ReflectiveOperationException {
    Minecraft client = mock(Minecraft.class);
    Font font = mock(Font.class);
    var fontField = Minecraft.class.getDeclaredField("font");
    fontField.setAccessible(true);
    fontField.set(client, font);
    var lineHeightField = Font.class.getDeclaredField("lineHeight");
    lineHeightField.setAccessible(true);
    lineHeightField.set(font, 9);
    when(font.width(any(FormattedText.class))).thenAnswer(call ->
        ((FormattedText) call.getArgument(0)).getString().startsWith("skada.icon.attack_type.") ? 8 : 30);
    KeyMapping previousModifier = Keybinds.attackScrollModifier;
    KeyMapping previousCycle = Keybinds.cycleAttackType;
    KeyMapping modifier = mock(KeyMapping.class);
    when(modifier.getKey()).thenReturn(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F12));
    when(modifier.getKeyModifier()).thenReturn(KeyModifier.NONE);
    when(modifier.getTranslatedKeyMessage()).thenReturn(Component.literal("F12"));
    Keybinds.attackScrollModifier = modifier;
    Keybinds.cycleAttackType = mock(KeyMapping.class);
    try (var minecraft = mockStatic(Minecraft.class);
        var formatter = mockStatic(UtilText.class);
        var selection = mockStatic(AttackSelection.class);
        var weight = mockStatic(EquipmentWeightUtil.class)) {
      minecraft.when(Minecraft::getInstance).thenReturn(client);
      formatter.when(() -> UtilText.pixelFontComponent(any(MutableComponent.class)))
          .thenAnswer(call -> ((MutableComponent) call.getArgument(0)).copy());
      for (int count : new int[]{0, 1, 2, 3, 5}) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getHoverName()).thenReturn(Component.literal("Test").withStyle(style -> style.withColor(0x79B9C6)));
        when(stack.hasTag()).thenReturn(true);
        CompoundTag tag = new CompoundTag();
        tag.put(count == 0 ? SkadaData.ARMOUR_INFO_TAG_KEY : SkadaData.WEAPON_INFO_TAG_KEY, new CompoundTag());
        when(stack.getTag()).thenReturn(tag);
        weight.when(() -> EquipmentWeightUtil.getStackWeightKg(stack)).thenReturn(1.5);
        List<AttackType> types = new ArrayList<>();
        for (int i = 0; i < count; i++) types.add(new AttackType("type" + i, null, null, null, null));
        int[] selectedIndex = {0};
        if (count > 0) {
          selection.when(() -> AttackSelection.resolve(stack)).thenAnswer(call ->
              new AttackSelection(null, types.get(selectedIndex[0]), null, types, selectedIndex[0], true));
        }
        List<String> icons = new ArrayList<>();
        List<String> selected = new ArrayList<>();
        List<Float> iconPositions = new ArrayList<>();
        List<Float> iconTops = new ArrayList<>();
        float[] massX = {Float.NaN};
        float[] massTop = {Float.NaN};
        List<String> attackNames = new ArrayList<>();
        float[] attackNameX = {Float.NaN};
        when(font.drawInBatch(any(Component.class), anyFloat(), anyFloat(), anyInt(), anyBoolean(),
            any(), any(), any(), anyInt(), anyInt())).thenAnswer(call -> {
              String label = ((Component) call.getArgument(0)).getString();
              if (label.startsWith("skada.icon.attack_type.")) {
                icons.add(label);
                iconPositions.add(call.getArgument(1));
                iconTops.add(call.getArgument(2));
                if ((int) call.getArgument(3) == 0xFFFFFFFF) selected.add(label);
              } else if (label.equals("Type0")) {
                attackNames.add(label);
                attackNameX[0] = call.getArgument(1);
              } else {
                massX[0] = call.getArgument(1);
                massTop[0] = call.getArgument(2);
              }
              return 0;
            });
        List<Float> fixedPositions = null;
        GuiGraphics graphics = mock(GuiGraphics.class);
        var buffers = mock(MultiBufferSource.BufferSource.class);
        when(graphics.bufferSource()).thenReturn(buffers);
        when(buffers.getBuffer(any())).thenReturn(mock(VertexConsumer.class, RETURNS_SELF));
        when(graphics.pose()).thenReturn(new PoseStack());
        List<int[]> pixels = new ArrayList<>();
        doAnswer(call -> {
          pixels.add(new int[]{call.getArgument(0), call.getArgument(1), call.getArgument(2), call.getArgument(3), call.getArgument(4)});
          return null;
        }).when(graphics).fill(anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        List<Integer> shortcutPositions = new ArrayList<>();
        when(graphics.drawString(eq(font), any(Component.class), anyInt(), anyInt(), anyInt(), eq(false)))
            .thenAnswer(call -> {
              assertEquals("F12", ((Component) call.getArgument(1)).getString(), "Only the current primary binding should appear");
              shortcutPositions.add(call.getArgument(2));
              return 0;
            });
        for (int index = 0; index < Math.max(count, 1); index++) {
          selectedIndex[0] = index;
          icons.clear();
          attackNames.clear();
          selected.clear();
          iconPositions.clear();
          iconTops.clear();
          pixels.clear();
          shortcutPositions.clear();
          WeightTooltipComponent footer = new WeightTooltipComponent(stack);
          int naturalWidth = footer.getWidth(font);
          footer.setTooltipWidth(naturalWidth + 50);
          footer.renderText(font, 20, 30, new Matrix4f(), null);
          footer.renderImage(font, 20, 30, graphics);
          assertEquals(types.stream().map(type -> "skada.icon.attack_type." + type.name()).toList(), icons,
              "Every supported type must keep its authored position");
          assertFalse(Float.isNaN(massX[0]));
          int contentTop = 30 + 3;
          int contentBottom = 30 + footer.getHeight();
          float centeredMassOrigin = massTop[0] - 1;
          assertTrue(Math.abs((centeredMassOrigin - 1 - contentTop)
              - (contentBottom - (centeredMassOrigin + font.lineHeight))) <= 1,
              "The weight icon and label must sit one pixel below the shared centre");
          if (count > 0) {
            assertEquals(List.of("skada.icon.attack_type.type" + index), selected);
            assertTrue(iconPositions.stream().allMatch(x -> x + 8 < massX[0]), "Icons must not collide with mass");
          }
          if (count == 1) {
            assertTrue(pixels.isEmpty(), "A single attack needs no underline, arrows or shortcut cue");
            float topGap = iconTops.get(0) - 1 - contentTop;
            float bottomGap = contentBottom - (iconTops.get(0) - 1 + 8);
            assertTrue(Math.abs(topGap - bottomGap) <= 1, "The lone attack icon must be vertically centred");
            assertEquals(topGap + 1, iconPositions.get(0) - (20 - 2),
                "The lone icon must sit one pixel right of its equal-padding position");
            assertEquals(List.of("Type0"), attackNames, "A lone attack must show its readable name");
            assertTrue(attackNameX[0] > iconPositions.get(0) + 8, "The name must sit beside the icon");
            assertTrue(attackNameX[0] + 30 < massX[0], "The attack name must remain clear of the weight");
          }
          if (count != 1) assertTrue(attackNames.isEmpty(), "Only a single attack needs a name in the footer");
          if (count > 1) {
            int[] underline = pixels.stream().filter(rect -> rect[4] == 0xC079B9C6).findFirst().orElseThrow();
            assertTrue(iconTops.stream().allMatch(top -> Math.abs((top - 2 - 1 - contentTop)
                - (contentBottom - (underline[3] - 2))) <= 1),
                "Attack icons and their selection underline must sit two pixels below the shared centre");
          }
          assertEquals(count > 1 ? 1 : 0, shortcutPositions.size(), "Only weapons with a choice need one shortcut cue");
          if (count > 1) {
            assertTrue(shortcutPositions.get(0) > iconPositions.get(count - 1) + 8, "The shortcut belongs to the right of all attack types");
            assertTrue(pixels.stream().allMatch(rect -> rect[2] < massX[0]), "The shortcut artwork must stay clear of mass");
            assertTrue(pixels.stream().allMatch(rect -> rect[1] >= 30 && rect[3] <= 30 + footer.getHeight()),
                "The keycap and wheel must fit within the existing footer height");
            int[] keycap = pixels.stream().filter(rect -> rect[4] == 0xFF202730).findFirst().orElseThrow();
            assertEquals(keycap[1] - (30 + 3), 30 + footer.getHeight() - keycap[3],
                "The shortcut must have equal padding below the divider and above the tooltip bottom");
            List<int[]> arrows = pixels.stream().filter(rect -> rect[4] == 0x9979B9C6).toList();
            int arrowTop = arrows.stream().mapToInt(rect -> rect[1]).min().orElseThrow();
            int arrowBottom = arrows.stream().mapToInt(rect -> rect[3]).max().orElseThrow();
            assertTrue(Math.abs((arrowTop - 1 - contentTop) - (contentBottom - (arrowBottom - 1))) <= 1,
                "The attack-cycle arrows must sit one pixel below the shared centre");
            assertTrue(arrows.stream().anyMatch(rect -> rect[2] <= iconPositions.get(0)),
                "The previous-attack chevron must sit left of the attack types");
            assertTrue(arrows.stream().anyMatch(rect -> rect[0] >= iconPositions.get(count - 1) + 8 && rect[2] < keycap[0]),
                "The next-attack chevron must fit between the attack types and the shortcut");
          }
          if (fixedPositions == null) fixedPositions = List.copyOf(iconPositions);
          else assertEquals(fixedPositions, iconPositions, "Changing selection must never move the icons");
          assertEquals(naturalWidth + 50, footer.getWidth(font), "The footer must still align under a wide title");
        }
      }
    } finally {
      Keybinds.attackScrollModifier = previousModifier;
      Keybinds.cycleAttackType = previousCycle;
    }
  }
}
