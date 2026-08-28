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

    /** Device-locale-aware number formatting with consistent symbol placement. */
    String format(double value) {
        return format(value, Locale.getDefault());
    }

    String format(double value, Locale locale) {
        java.text.NumberFormat format = java.text.NumberFormat.getNumberInstance(locale);
        format.setMaximumFractionDigits(0);
        format.setMinimumFractionDigits(0);
        boolean symbolAfterNumber = Locale.GERMAN.getLanguage().equals(locale.getLanguage());
        return symbolAfterNumber
                ? format.format(value) + " " + symbol
                : symbol + format.format(value);
    }
}
