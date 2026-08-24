package dev.steamy.bitcointicker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class WidgetRenderer {
    private static final int POSITIVE = Color.rgb(69, 214, 128);
    private static final int NEGATIVE = Color.rgb(255, 94, 94);
    private static final int NEUTRAL = Color.rgb(183, 190, 200);
    private static final int AGING = Color.rgb(247, 147, 26);
    private static final long FRESH_AGE_MS = 20L * 60L * 1000L;
    private static final long AGING_AGE_MS = 60L * 60L * 1000L;

    private WidgetRenderer() {}

    static void renderAll(Context context, boolean refreshing) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, BitcoinWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        renderCached(context, manager, ids, refreshing);
    }

    static void renderCached(Context context, AppWidgetManager manager,
                             int[] ids, boolean refreshing) {
        SharedPreferences prefs = context.getSharedPreferences(
                PriceUpdater.PREFS, Context.MODE_PRIVATE);
        boolean hasPrice = prefs.contains(PriceUpdater.EUR)
                && prefs.contains(PriceUpdater.USD);
        CurrencyPreference currencyPreference = CurrencyPreference.fromStored(
                prefs.getString(PriceUpdater.CURRENCY_PREFERENCE, null));

        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.bitcoin_widget);
            setClickActions(context, views);
            applyCurrencyVisibility(views, currencyPreference);

            if (hasPrice) {
                double eur = Double.parseDouble(prefs.getString(PriceUpdater.EUR, "0"));
                double usd = Double.parseDouble(prefs.getString(PriceUpdater.USD, "0"));
                double changeEur = Double.parseDouble(
                        prefs.getString(PriceUpdater.CHANGE_EUR, "0"));
                double changeUsd = Double.parseDouble(
                        prefs.getString(PriceUpdater.CHANGE_USD, "0"));
                double highEur = readDouble(prefs, PriceUpdater.HIGH_EUR);
                double lowEur = readDouble(prefs, PriceUpdater.LOW_EUR);
                double highUsd = readDouble(prefs, PriceUpdater.HIGH_USD);
                double lowUsd = readDouble(prefs, PriceUpdater.LOW_USD);
                long updatedAt = prefs.getLong(PriceUpdater.UPDATED_AT, 0L);

                views.setTextViewText(R.id.price_eur, ChartCurrency.EUR.format(eur));
                views.setTextViewText(R.id.price_usd, ChartCurrency.USD.format(usd));
                setChange(views, currencyPreference == CurrencyPreference.USD
                        ? changeUsd : changeEur);
                views.setTextViewText(R.id.range_eur,
                        formatRange(context, highEur, lowEur));
                views.setTextViewText(R.id.range_usd,
                        formatRange(context, highUsd, lowUsd));
                views.setImageViewBitmap(R.id.range_eur_meter,
                        createRangeMeter(context, rangePosition(eur, highEur, lowEur), false));
                views.setImageViewBitmap(R.id.range_usd_meter,
                        createRangeMeter(context, rangePosition(usd, highUsd, lowUsd), true));
                // Keep the last successful update time visible during refreshes.
                // Some launchers otherwise retain the temporary ellipsis indefinitely.
                views.setTextViewText(R.id.status, formatTime(updatedAt));
                setFreshness(views, updatedAt);
            } else {
                views.setTextViewText(R.id.price_eur, "€ —");
                views.setTextViewText(R.id.price_usd, "$ —");
                views.setTextViewText(R.id.change_24h, "");
                views.setTextViewText(R.id.trend_marker, "•");
                views.setTextColor(R.id.trend_marker, NEUTRAL);
                views.setTextViewText(R.id.range_eur,
                        context.getString(R.string.widget_range_empty));
                views.setTextViewText(R.id.range_usd,
                        context.getString(R.string.widget_range_empty));
                views.setImageViewBitmap(R.id.range_eur_meter,
                        createRangeMeter(context, 0, false));
                views.setImageViewBitmap(R.id.range_usd_meter,
                        createRangeMeter(context, 0, true));
                boolean hasError = prefs.contains(PriceUpdater.LAST_ERROR);
                String errorDetail = prefs.getString(PriceUpdater.LAST_ERROR, "");
                views.setTextViewText(R.id.status, hasError
                        ? context.getString(R.string.widget_error_detail, errorDetail)
                        : context.getString(R.string.updating_short));
                views.setTextColor(R.id.freshness_dot, hasError ? NEGATIVE : NEUTRAL);
            }
            manager.updateAppWidget(id, views);
        }
    }

    private static void applyCurrencyVisibility(RemoteViews views,
                                                CurrencyPreference preference) {
        views.setViewVisibility(R.id.price_eur,
                preference == CurrencyPreference.USD ? View.GONE : View.VISIBLE);
        views.setViewVisibility(R.id.range_eur_group,
                preference == CurrencyPreference.USD ? View.GONE : View.VISIBLE);
        views.setViewVisibility(R.id.price_usd,
                preference == CurrencyPreference.EUR ? View.GONE : View.VISIBLE);
        views.setViewVisibility(R.id.range_usd_group,
                preference == CurrencyPreference.EUR ? View.GONE : View.VISIBLE);
    }

    private static void setClickActions(Context context, RemoteViews views) {
        Intent refreshIntent = new Intent(context, BitcoinWidgetProvider.class)
                .setAction(BitcoinWidgetProvider.ACTION_REFRESH);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                100,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.refresh_panel, refreshPendingIntent);

        Intent appIntent = new Intent(context, MainActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent appPendingIntent = PendingIntent.getActivity(
                context,
                101,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent);
        views.setOnClickPendingIntent(R.id.price_panel, appPendingIntent);
    }

    private static void setChange(RemoteViews views, double change) {
        String arrow = change > 0.005 ? "▲" : change < -0.005 ? "▼" : "•";
        int color = change > 0.005 ? POSITIVE : change < -0.005 ? NEGATIVE : NEUTRAL;
        views.setTextViewText(R.id.trend_marker, arrow);
        views.setTextColor(R.id.trend_marker, color);
        views.setTextViewText(R.id.change_24h,
                String.format(Locale.getDefault(), "%+.2f%%", change));
        views.setTextColor(R.id.change_24h, color);
    }

    private static int rangePosition(double current, double high, double low) {
        if (high <= low) {
            return 0;
        }
        double position = (current - low) / (high - low);
        return (int) Math.round(Math.max(0.0, Math.min(1.0, position)) * 100.0);
    }

    private static Bitmap createRangeMeter(Context context, int progress, boolean muted) {
        float density = context.getResources().getDisplayMetrics().density;
        int width = Math.max(1, Math.round(102f * density));
        int height = Math.max(1, Math.round(5f * density));
        float centerY = height / 2f;
        float dotRadius = Math.max(1f, 2f * density);
        float startX = dotRadius;
        float endX = width - dotRadius;
        float markerX = startX + (endX - startX) * progress / 100f;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(1f, density));
        paint.setColor(muted ? Color.rgb(41, 49, 60) : Color.rgb(48, 57, 70));
        canvas.drawLine(startX, centerY, endX, centerY, paint);

        paint.setColor(muted ? Color.rgb(183, 110, 33) : Color.rgb(247, 147, 26));
        paint.setStrokeWidth(Math.max(1f, 1.4f * density));
        canvas.drawLine(startX, centerY, markerX, centerY, paint);
        canvas.drawCircle(markerX, centerY, dotRadius, paint);
        return bitmap;
    }

    private static void setFreshness(RemoteViews views, long updatedAt) {
        long age = Math.max(0L, System.currentTimeMillis() - updatedAt);
        int color = age <= FRESH_AGE_MS ? POSITIVE : age <= AGING_AGE_MS ? AGING : NEGATIVE;
        views.setTextViewText(R.id.freshness_dot, "•");
        views.setTextColor(R.id.freshness_dot, color);
    }

    private static double readDouble(SharedPreferences prefs, String key) {
        return Double.parseDouble(prefs.getString(key, "0"));
    }

    private static String formatRange(Context context, double high, double low) {
        return context.getString(R.string.widget_range,
                formatCompact(high), formatCompact(low));
    }

    private static String formatCompact(double value) {
        if (value >= 1000.0) {
            return String.format(Locale.getDefault(), "%.1fk", value / 1000.0);
        }
        return String.format(Locale.getDefault(), "%.0f", value);
    }

    private static String formatTime(long timestamp) {
        if (timestamp <= 0L) {
            return "—";
        }
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }
}
