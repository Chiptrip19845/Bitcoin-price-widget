package dev.steamy.bitcointicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

/** Subtle landmarks aligned to the logarithmic zoom slider. */
final class ZoomMarkerView extends View {
    private static final long[] DURATIONS = {
            24 * 60 * 60L,
            7 * 24 * 60 * 60L,
            14 * 24 * 60 * 60L,
            30 * 24 * 60 * 60L
    };

    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String[] labels;

    ZoomMarkerView(Context context) {
        super(context);
        labels = new String[]{
                context.getString(R.string.zoom_marker_day),
                context.getString(R.string.zoom_marker_week),
                context.getString(R.string.zoom_marker_two_weeks),
                context.getString(R.string.zoom_marker_month)
        };
        tickPaint.setColor(Color.rgb(77, 86, 98));
        tickPaint.setStrokeWidth(dp(1));
        labelPaint.setColor(Color.rgb(105, 115, 128));
        labelPaint.setTextSize(dp(8));
        labelPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        setContentDescription(context.getString(R.string.zoom_markers_description));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float edge = dp(16);
        float width = Math.max(1, getWidth() - edge * 2);
        for (int index = 0; index < DURATIONS.length; index++) {
            int progress = ChartZoom.progressForDuration(
                    DURATIONS[index], ChartZoom.ASSUMED_ALL_TIME_SECONDS);
            float x = edge + width * progress / ChartZoom.MAX_PROGRESS;
            canvas.drawLine(x, 0, x, dp(5), tickPaint);
            canvas.drawText(labels[index], x, dp(15), labelPaint);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
