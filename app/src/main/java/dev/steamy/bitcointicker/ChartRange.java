package dev.steamy.bitcointicker;

enum ChartRange {
    TEN_MINUTES("10 Min", 10 * 60L, 60),
    HOUR("1 Std", 60 * 60L, 300),
    DAY("24 Std", 24 * 60 * 60L, 900),
    FOUR_DAYS("4 Tage", 4 * 24 * 60 * 60L, 3600),
    ALL("Gesamt", 0L, 0);

    final String label;
    final long durationSeconds;
    final int granularitySeconds;

    ChartRange(String label, long durationSeconds, int granularitySeconds) {
        this.label = label;
        this.durationSeconds = durationSeconds;
        this.granularitySeconds = granularitySeconds;
    }
}
