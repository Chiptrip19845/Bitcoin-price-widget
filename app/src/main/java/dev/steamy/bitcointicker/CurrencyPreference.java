package dev.steamy.bitcointicker;

enum CurrencyPreference {
    EUR,
    USD,
    BOTH;

    static CurrencyPreference fromStored(String value) {
        if (value != null) {
            try {
                return valueOf(value);
            } catch (IllegalArgumentException ignored) {
                // Fall through to the backwards-compatible default.
            }
        }
        return BOTH;
    }

    CurrencyPreference next() {
        switch (this) {
            case BOTH:
                return EUR;
            case EUR:
                return USD;
            default:
                return BOTH;
        }
    }
}
