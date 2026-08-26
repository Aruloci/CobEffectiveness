package com.cobeffectiveness.type;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TypeTest {

    @Test
    void resolvesTheCasingCobblemonUses() {
        assertEquals(Type.FIRE, Type.fromName("Fire"));
        assertEquals(Type.FIRE, Type.fromName("fire"));
        assertEquals(Type.FIRE, Type.fromName("FIRE"));
    }

    @Test
    void returnsNullForMissingOrUnknownNames() {
        assertNull(Type.fromName(null));
        assertNull(Type.fromName(ChartJson.PLACEHOLDER));
        assertNull(Type.fromName("banana"));
    }

    @Test
    void ignoresTheDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            // A locale sensitive toUpperCase maps a lowercase i to a dotted capital on a Turkish
            // JVM, which used to drop every type whose name contains an i.
            assertEquals(Type.FIRE, Type.fromName("Fire"));
            assertEquals(Type.ICE, Type.fromName("Ice"));
            assertEquals(Type.FIGHTING, Type.fromName("Fighting"));
            assertEquals(Type.FAIRY, Type.fromName("Fairy"));
        } finally {
            Locale.setDefault(original);
        }
    }
}
