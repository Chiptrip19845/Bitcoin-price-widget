package dev.steamy.bitcointicker;

import java.util.Locale;

enum ChartCurrency {
    EUR("EUR", "€"),
    USD("USD", "$");

    final String code;
    final String symbol;

    ChartCurrency(String code, String symbol) {
        this.code = code;
        this.symbol = symbol;
    }

    /** Device-locale-aware number formatting with the correct currency symbol. */
    String format(double value) {
        java.text.NumberFormat format = java.text.NumberFormat.getNumberInstance(Locale.getDefault());
        format.setMaximumFractionDigits(0);
        format.setMinimumFractionDigits(0);
        boolean german = Locale.GERMAN.getLanguage().equals(Locale.getDefault().getLanguage());
        if (this == USD) {
            return symbol + format.format(value);
        }
        // EUR: suffix in German contexts, prefix elsewhere
        return german ? format.format(value) + " " + symbol : symbol + format.format(value);
    }
}
