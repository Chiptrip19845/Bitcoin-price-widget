package dev.steamy.bitcointicker;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

public final class ChartCurrencyTest {
    @Test
    public void germanPlacesBothCurrencySymbolsAfterThePrice() {
        assertEquals("68.093 €", ChartCurrency.EUR.format(68093, Locale.GERMANY));
        assertEquals("79.282 $", ChartCurrency.USD.format(79282, Locale.GERMANY));
    }

    @Test
    public void englishPlacesBothCurrencySymbolsBeforeThePrice() {
        assertEquals("€68,093", ChartCurrency.EUR.format(68093, Locale.US));
        assertEquals("$79,282", ChartCurrency.USD.format(79282, Locale.US));
    }
}
