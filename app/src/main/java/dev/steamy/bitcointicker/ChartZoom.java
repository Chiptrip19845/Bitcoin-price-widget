package dev.steamy.bitcointicker;

import java.util.ArrayList;
import java.util.List;

/** Pure zoom math shared by the slider and chart rendering. */
final class ChartZoom {
    static final int MAX_PROGRESS = 1000;
    static final long ASSUMED_ALL_TIME_SECONDS = 17L * 365 * 24 * 60 * 60;
    private static final long DENSE_HISTORY_SECONDS = 300L * 24 * 60 * 60;
    private static final int SEGMENT = 200;
    private static final long[] ANCHORS_SECONDS = {
            60 * 60L,
            24 * 60 * 60L,
            4 * 24 * 60 * 60L,
            14 * 24 * 60 * 60L,
            28 * 24 * 60 * 60L
    };

    private ChartZoom() {}

    static long durationSeconds(int progress, long allTimeSeconds) {
        int clamped = Math.max(0, Math.min(MAX_PROGRESS, progress));
        if (clamped == MAX_PROGRESS) return Math.max(ANCHORS_SECONDS[4], allTimeSeconds);
        int segment = Math.min(4, clamped / SEGMENT);
        double fraction = (clamped % SEGMENT) / (double) SEGMENT;
        long start = ANCHORS_SECONDS[segment];
        long end = segment == 4
                ? Math.max(start, allTimeSeconds)
                : ANCHORS_SECONDS[segment + 1];
        // Logarithmic interpolation feels even across hours, days, months and years.
        return Math.round(Math.exp(Math.log(start)
                + fraction * (Math.log(end) - Math.log(start))));
    }

    static ChartRange sourceFor(int progress) {
        int clamped = Math.max(0, Math.min(MAX_PROGRESS, progress));
        if (clamped <= 0) return ChartRange.HOUR;
        if (clamped <= 200) return ChartRange.DAY;
        if (clamped <= 400) return ChartRange.FOUR_DAYS;
        if (clamped <= 600) return ChartRange.FOURTEEN_DAYS;
        if (clamped <= 800) return ChartRange.FOUR_WEEKS;
        if (clamped <= progressForDuration(DENSE_HISTORY_SECONDS,
                ASSUMED_ALL_TIME_SECONDS)) return ChartRange.TEN_MONTHS;
        return ChartRange.ALL;
    }

    static int progressForDuration(long durationSeconds, long allTimeSeconds) {
        long target = Math.max(ANCHORS_SECONDS[0], durationSeconds);
        if (target >= Math.max(ANCHORS_SECONDS[4], allTimeSeconds)) return MAX_PROGRESS;
        for (int segment = 0; segment < 5; segment++) {
            long start = ANCHORS_SECONDS[segment];
            long end = segment == 4
                    ? Math.max(start, allTimeSeconds)
                    : ANCHORS_SECONDS[segment + 1];
            if (target <= end) {
                double fraction = (Math.log(target) - Math.log(start))
                        / (Math.log(end) - Math.log(start));
                return Math.max(0, Math.min(MAX_PROGRESS,
                        segment * SEGMENT + (int) Math.round(fraction * SEGMENT)));
            }
        }
        return MAX_PROGRESS;
    }

    static ChartSeries window(ChartSeries source, long durationSeconds) {
        if (source == null || source.points.size() < 2 || durationSeconds <= 0) return source;
        List<ChartPoint> points = source.points;
        long end = points.get(points.size() - 1).timestampMillis;
        long cutoff = end - durationSeconds * 1000L;
        int first = 0;
        while (first < points.size() - 2 && points.get(first).timestampMillis < cutoff) first++;
        return new ChartSeries(new ArrayList<>(points.subList(first, points.size())));
    }
}
