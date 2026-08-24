package dev.steamy.bitcointicker;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class CurrencyPreferenceTest {
    @Test
    public void invalidOrMissingValueDefaultsToBoth() {
        assertEquals(CurrencyPreference.BOTH, CurrencyPreference.fromStored(null));
        assertEquals(CurrencyPreference.BOTH, CurrencyPreference.fromStored("unknown"));
    }

    @Test
    public void storedValuesRoundTrip() {
        for (CurrencyPreference preference : CurrencyPreference.values()) {
            assertEquals(preference, CurrencyPreference.fromStored(preference.name()));
        }
    }

    @Test
    public void cyclesBothEurUsd() {
        assertEquals(CurrencyPreference.EUR, CurrencyPreference.BOTH.next());
        assertEquals(CurrencyPreference.USD, CurrencyPreference.EUR.next());
        assertEquals(CurrencyPreference.BOTH, CurrencyPreference.USD.next());
    }
}
