package com.cwjn.skada;

import com.cwjn.skada.client.gui.StatHelp;
import com.cwjn.skada.util.UtilText;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Exercises the exact tooltip formatter call so Java overload selection and legacy colour parsing are covered. */
class KnockbackTooltipFormattingTest {
    /** Disabling the custom font must retain vanilla glyphs and category colours, including Shift details. */
    @Test void vanillaFontRetainsCategoryColours() throws Exception {
        checkFormatting(false);
    }

    /** Enabling the custom font must preserve category colours rather than stripping legacy formatting codes. */
    @Test void customFontRetainsCategoryColours() throws Exception {
        checkFormatting(true);
    }

    /** Loads real tooltip translations while replacing only the running client and language-service dependencies. */
    private void checkFormatting(boolean customFont) throws Exception {
        Language previousLanguage = Language.getInstance();
        Map<String, String> translations;
        try (var input = getClass().getResourceAsStream("/assets/skada/lang/en_us.json")) {
            assertNotNull(input);
            translations = new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, String>>() {}.getType());
        }
        Language language = mock(Language.class, CALLS_REAL_METHODS);
        when(language.has(anyString())).thenAnswer(call -> translations.containsKey(call.getArgument(0)));
        when(language.getOrDefault(anyString())).thenAnswer(call ->
                translations.getOrDefault(call.getArgument(0), call.getArgument(0)));
        Minecraft client = mock(Minecraft.class, RETURNS_DEEP_STUBS);
        when(client.getLanguageManager().getSelected()).thenReturn("en_us");
        ClientConfig.SPEC.setConfig(CommentedConfig.inMemory());
        boolean previousFont = ClientConfig.USE_MDU_FONT.get();
        ClientConfig.USE_MDU_FONT.set(customFont);
        Language.inject(language);
        try (var minecraft = mockStatic(Minecraft.class); var i18n = mockStatic(I18n.class)) {
            minecraft.when(Minecraft::getInstance).thenReturn(client);
            i18n.when(() -> I18n.get(anyString())).thenAnswer(call -> call.getArgument(0));
            for (boolean detailed : new boolean[]{false, true}) {
                // Keep this expression identical to the real call site: its static return type chooses the overload.
                var formatted = UtilText.pixelFontComponent(StatHelp.knockbackTooltip(0.5, detailed));
                AtomicReference<Style> categoryStyle = new AtomicReference<>();
                AtomicReference<Style> numberStyle = new AtomicReference<>();
                formatted.visit((style, text) -> {
                    if (text.contains("N")) categoryStyle.set(style);
                    if (text.contains("0")) numberStyle.set(style);
                    return Optional.empty();
                }, Style.EMPTY);
                assertNotNull(categoryStyle.get(), "Expected the Normal category in the rendered row");
                assertEquals(customFont ? new ResourceLocation("skada", "minimal_pixel_bitmap") : Style.DEFAULT_FONT,
                        categoryStyle.get().getFont(), "The row must respect useNewFont");
                assertNotNull(categoryStyle.get().getColor(), "The category must retain its colour");
                assertEquals(ChatFormatting.GREEN.getColor(), categoryStyle.get().getColor().getValue());
                if (detailed) {
                    assertNotNull(numberStyle.get(), "Shift must retain the raw value");
                    assertNotNull(numberStyle.get().getColor());
                    assertEquals(ChatFormatting.GRAY.getColor(), numberStyle.get().getColor().getValue());
                }
            }
        } finally {
            Language.inject(previousLanguage);
            ClientConfig.USE_MDU_FONT.set(previousFont);
        }
    }
}
