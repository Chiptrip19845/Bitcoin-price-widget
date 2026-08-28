package dev.steamy.bitcointicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class BitcoinChartView extends View {
    private static final int ORANGE = Color.rgb(247, 147, 26);
    private static final int GREEN = Color.rgb(64, 201, 137);
    private static final int RED = Color.rgb(244, 99, 109);
    private static final int MUTED = Color.rgb(111, 120, 130);

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionBoxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private ChartSeries series;
    private ChartRange range = ChartRange.DAY;
    private ChartCurrency currency = ChartCurrency.EUR;
    private Boolean dayChangePositive;
    private boolean error;
    private int touchedIndex = -1;

    BitcoinChartView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2.4f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(dp(8));
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStrokeJoin(Paint.Join.ROUND);
        gridPaint.setColor(Color.argb(24, 255, 255, 255));
        gridPaint.setStrokeWidth(dp(1));
        labelPaint.setColor(MUTED);
        labelPaint.setTextSize(dp(10));
        labelPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        dotPaint.setStyle(Paint.Style.FILL);
        selectionBoxPaint.setColor(Color.rgb(39, 44, 52));
        setContentDescription(context.getString(R.string.chart_content_description));
    }

    void setSeries(ChartSeries series, ChartRange range) {
        setSeries(series, range, currency);
    }

    void setSeries(ChartSeries series, ChartRange range, ChartCurrency currency) {
        this.series = series;
        this.range = range;
        this.currency = currency;
        error = false;
        touchedIndex = -1;
        invalidate();
    }

    void showError() {
        error = true;
        series = null;
        invalidate();
    }

    void setDayChangePositive(boolean positive) {
        dayChangePositive = positive;
        if (range == ChartRange.DAY) invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (series == null || series.points.size() < 2) {
            if (error) drawCentered(canvas, getContext().getString(R.string.chart_no_data));
            return;
        }

        float left = dp(13);
        float right = getWidth() - dp(24);
        float top = dp(20);
        float bottom = getHeight() - dp(29);
        if (right <= left || bottom <= top) return;

        for (int row = 1; row <= 3; row++) {
            float y = top + (bottom - top) * row / 4f;
            canvas.drawLine(left, y, right, y, gridPaint);
        }

        List<ChartPoint> points = series.points;
        double min = series.minPrice();
        double max = series.maxPrice();
        double padding = Math.max((max - min) * 0.10, max * 0.002);
        min -= padding;
        max += padding;
        long start = points.get(0).timestampMillis;
        long end = points.get(points.size() - 1).timestampMillis;

        linePath.reset();
        fillPath.reset();
        for (int index = 0; index < points.size(); index++) {
            ChartPoint point = points.get(index);
            float x = map(point.timestampMillis, start, end, left, right);
            float y = map(point.price, min, max, bottom, top);
            if (index == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, bottom);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }
        fillPath.lineTo(right, bottom);
        fillPath.close();

        boolean positive = range == ChartRange.DAY && dayChangePositive != null
                ? dayChangePositive
                : points.get(points.size() - 1).price >= points.get(0).price;
        int color = range == ChartRange.ALL ? ORANGE : (positive ? GREEN : RED);
        linePaint.setColor(color);
        linePaint.setShadowLayer(dp(10), 0, dp(2), Color.argb(105,
                Color.red(color), Color.green(color), Color.blue(color)));
        glowPaint.setColor(Color.argb(25, Color.red(color), Color.green(color),
                Color.blue(color)));
        fillPaint.setShader(new LinearGradient(0, top, 0, bottom,
                Color.argb(74, Color.red(color), Color.green(color), Color.blue(color)),
                Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, glowPaint);
        canvas.drawPath(linePath, linePaint);

        ChartPoint latest = points.get(points.size() - 1);
        float latestX = map(latest.timestampMillis, start, end, left, right);
        float latestY = map(latest.price, min, max, bottom, top);
        dotPaint.setColor(Color.argb(45, Color.red(color), Color.green(color), Color.blue(color)));
        canvas.drawCircle(latestX, latestY, dp(9), dotPaint);
        dotPaint.setColor(color);
        canvas.drawCircle(latestX, latestY, dp(4), dotPaint);
        dotPaint.setStyle(Paint.Style.STROKE);
        dotPaint.setStrokeWidth(dp(1.5f));
        dotPaint.setColor(Color.WHITE);
        canvas.drawCircle(latestX, latestY, dp(4), dotPaint);
        dotPaint.setStyle(Paint.Style.FILL);

        drawTimeLabels(canvas, start, end, left, right, bottom);
        if (touchedIndex >= 0 && touchedIndex < points.size()) {
            drawSelection(canvas, points.get(touchedIndex), start, end, min, max,
                    left, right, top, bottom, color);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (series == null || series.points.isEmpty()) return false;
        if (event.getAction() == MotionEvent.ACTION_DOWN
                || event.getAction() == MotionEvent.ACTION_MOVE) {
            float fraction = Math.max(0f, Math.min(1f,
                    (event.getX() - dp(13)) / Math.max(1f, getWidth() - dp(26))));
            touchedIndex = Math.round(fraction * (series.points.size() - 1));
            invalidate();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
            performClick();
            touchedIndex = -1;
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void drawTimeLabels(Canvas canvas, long start, long end,
                                float left, float right, float bottom) {
        String pattern;
        if (range == ChartRange.TEN_MINUTES || range == ChartRange.HOUR
                || range == ChartRange.DAY) pattern = "HH:mm";
        else if (range == ChartRange.FOUR_DAYS
                || range == ChartRange.FOURTEEN_DAYS) pattern = "EEE";
        else pattern = "yyyy";
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
        float y = bottom + dp(21);
        canvas.drawText(format.format(new Date(start)), left, y, labelPaint);
        if (range == ChartRange.FOUR_DAYS || range == ChartRange.FOURTEEN_DAYS) {
            // Four evenly spaced weekday labels for multi-day ranges.
            String second = format.format(new Date(start + (end - start) / 3));
            String third = format.format(new Date(start + 2 * (end - start) / 3));
            canvas.drawText(second,
                    left + (right - left) / 3f - labelPaint.measureText(second) / 2f, y, labelPaint);
            canvas.drawText(third,
                    left + 2f * (right - left) / 3f - labelPaint.measureText(third) / 2f, y, labelPaint);
        } else {
            String middle = format.format(new Date(start + (end - start) / 2));
            canvas.drawText(middle, (left + right - labelPaint.measureText(middle)) / 2, y, labelPaint);
        }
        String last = format.format(new Date(end));
        canvas.drawText(last, right - labelPaint.measureText(last), y, labelPaint);
    }

    private void drawSelection(Canvas canvas, ChartPoint point, long start, long end,
                               double min, double max, float left, float right,
                               float top, float bottom, int color) {
        float x = map(point.timestampMillis, start, end, left, right);
        float y = map(point.price, min, max, bottom, top);
        Paint guide = gridPaint;
        guide.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawLine(x, top, x, bottom, guide);
        guide.setColor(Color.argb(24, 255, 255, 255));
        dotPaint.setColor(color);
        canvas.drawCircle(x, y, dp(5), dotPaint);
        dotPaint.setStyle(Paint.Style.STROKE);
        dotPaint.setStrokeWidth(dp(2));
        dotPaint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, dp(5), dotPaint);
        dotPaint.setStyle(Paint.Style.FILL);

        String priceLabel = currency.format(point.price);
        boolean german = Locale.GERMAN.getLanguage().equals(Locale.getDefault().getLanguage());
        String datePattern = range == ChartRange.ALL
                ? (german ? "dd. MMM yyyy" : "MMM dd, yyyy")
                : range == ChartRange.FOUR_DAYS || range == ChartRange.FOURTEEN_DAYS
                        ? "EEE, HH:mm" : "HH:mm";
        String dateLabel = new SimpleDateFormat(datePattern, Locale.getDefault())
                .format(new Date(point.timestampMillis));
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(dp(12));
        float width = Math.max(labelPaint.measureText(priceLabel),
                labelPaint.measureText(dateLabel)) + dp(18);
        float boxLeft = Math.max(left, Math.min(right - width, x - width / 2));
        canvas.drawRoundRect(boxLeft, top, boxLeft + width, top + dp(43),
                dp(10), dp(10), selectionBoxPaint);
        canvas.drawText(priceLabel, boxLeft + dp(9), top + dp(17), labelPaint);
        labelPaint.setTextSize(dp(9));
        labelPaint.setColor(Color.rgb(158, 168, 180));
        canvas.drawText(dateLabel, boxLeft + dp(9), top + dp(34), labelPaint);
        labelPaint.setColor(MUTED);
        labelPaint.setTextSize(dp(10));
    }

    private void drawCentered(Canvas canvas, String message) {
        float width = labelPaint.measureText(message);
        canvas.drawText(message, (getWidth() - width) / 2f, getHeight() / 2f, labelPaint);
    }

    private static float map(double value, double inMin, double inMax,
                             float outMin, float outMax) {
        if (inMax == inMin) return (outMin + outMax) / 2f;
        return (float) (outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
