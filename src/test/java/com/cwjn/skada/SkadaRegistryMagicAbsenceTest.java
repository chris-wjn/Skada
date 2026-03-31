package com.cwjn.skada;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Guards against re-introduction of the removed skada:magic AttackType.
 * Uses reflection and source-text scanning so no Forge bootstrap is needed.
 */
class SkadaRegistryMagicAbsenceTest {

    @Test
    void skadaRegistryHasNoMagicField() {
        boolean hasMagic = Arrays.stream(SkadaRegistry.class.getDeclaredFields())
                .map(Field::getName)
                .anyMatch(name -> name.equalsIgnoreCase("MAGIC"));

        assertFalse(hasMagic, "SkadaRegistry must not contain a field named MAGIC — skada:magic has been removed");
    }

    @Test
    void skadaRegistryFieldNamesContainNoMagic() {
        boolean hasMagicField = Arrays.stream(SkadaRegistry.class.getDeclaredFields())
                .map(Field::getName)
                .anyMatch(name -> name.toLowerCase().contains("magic"));

        assertFalse(hasMagicField,
                "SkadaRegistry must not contain any field whose name contains 'magic' — skada:magic has been removed");
    }

    @Test
    void skadaRegistrySourceDoesNotRegisterMagicAttackType() throws IOException {
        Path source = Path.of("src/main/java/com/cwjn/skada/SkadaRegistry.java");
        String text = Files.readString(source);

        assertFalse(text.contains("attackType(\"magic\""),
                "SkadaRegistry.java must not call attackType(\"magic\") — skada:magic has been removed");
    }
}
