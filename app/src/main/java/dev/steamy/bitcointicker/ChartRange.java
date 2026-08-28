package dev.steamy.bitcointicker;

import android.content.Context;

enum ChartRange {
    TEN_MINUTES(R.string.range_ten_minutes, 10 * 60L, 60),
    HOUR(R.string.range_hour, 60 * 60L, 300),
    DAY(R.string.range_day, 24 * 60 * 60L, 900),
    FOUR_DAYS(R.string.range_four_days, 4 * 24 * 60 * 60L, 3600),
    FOURTEEN_DAYS(R.string.range_fourteen_days, 14 * 24 * 60 * 60L, 21600),
    ALL(R.string.range_all, 0L, 0);

    private final int labelResource;
    final long durationSeconds;
    final int granularitySeconds;

    ChartRange(int labelResource, long durationSeconds, int granularitySeconds) {
        this.labelResource = labelResource;
        this.durationSeconds = durationSeconds;
        this.granularitySeconds = granularitySeconds;
    }

    String label(Context context) {
        return context.getString(labelResource);
    }
}
