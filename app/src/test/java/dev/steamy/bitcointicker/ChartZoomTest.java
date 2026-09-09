package dev.steamy.bitcointicker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public final class ChartZoomTest {
    @Test public void sliderAnchorsSelectExpectedSources() {
        assertEquals(ChartRange.HOUR, ChartZoom.sourceFor(0));
        assertEquals(ChartRange.DAY, ChartZoom.sourceFor(200));
        assertEquals(ChartRange.FOUR_DAYS, ChartZoom.sourceFor(400));
        assertEquals(ChartRange.FOURTEEN_DAYS, ChartZoom.sourceFor(600));
        assertEquals(ChartRange.FOUR_WEEKS, ChartZoom.sourceFor(800));
        assertEquals(ChartRange.TEN_MONTHS, ChartZoom.sourceFor(801));
        assertEquals(ChartRange.ALL, ChartZoom.sourceFor(900));
    }

    @Test public void durationIsContinuousAndMonotonic() {
        long previous = 0;
        for (int progress = 0; progress <= ChartZoom.MAX_PROGRESS; progress++) {
            long duration = ChartZoom.durationSeconds(progress, 16L * 365 * 24 * 60 * 60);
            assertTrue(duration >= previous);
            previous = duration;
        }
    }

    @Test public void markerPositionsRoundTripToTheirDurations() {
        long allTime = 17L * 365 * 24 * 60 * 60;
        long[] durations = {24 * 60 * 60L, 7 * 24 * 60 * 60L,
                14 * 24 * 60 * 60L, 30 * 24 * 60 * 60L};
        for (long duration : durations) {
            int progress = ChartZoom.progressForDuration(duration, allTime);
            long result = ChartZoom.durationSeconds(progress, allTime);
            assertTrue(Math.abs(result - duration) < duration * 0.03);
        }
    }

    @Test public void windowKeepsNewestPointsAndAtLeastTwo() {
        List<ChartPoint> points = new ArrayList<>();
        for (int hour = 0; hour < 10; hour++) {
            points.add(new ChartPoint(hour * 3_600_000L, 100 + hour));
        }
        ChartSeries window = ChartZoom.window(new ChartSeries(points), 2 * 60 * 60L);
        assertEquals(3, window.points.size());
        assertEquals(7 * 3_600_000L, window.points.get(0).timestampMillis);
        assertEquals(9 * 3_600_000L, window.points.get(2).timestampMillis);
    }
}
