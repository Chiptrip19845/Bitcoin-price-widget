package dev.steamy.bitcointicker;

import java.util.Locale;

enum ChartCurrency {
    EUR("EUR", Locale.GERMANY),
    USD("USD", Locale.US);

    final String code;
    final Locale locale;

    ChartCurrency(String code, Locale locale) {
        this.code = code;
        this.locale = locale;
    }
}
