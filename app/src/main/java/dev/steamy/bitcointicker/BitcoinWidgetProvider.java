package dev.steamy.bitcointicker;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public final class BitcoinWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_REFRESH =
            "dev.steamy.bitcointicker.action.REFRESH";

    @Override
    public void onEnabled(Context context) {
        WidgetScheduler.schedulePeriodic(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        try {
            WidgetRenderer.renderCached(context, manager, appWidgetIds, false);
        } catch (RuntimeException ignored) {
            // A launcher render failure must not prevent future updates.
        }
        WidgetScheduler.schedulePeriodic(context);
        WidgetScheduler.scheduleNow(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_REFRESH.equals(intent.getAction())) {
            WidgetScheduler.scheduleNow(context);
            return;
        }

        super.onReceive(context, intent);
    }

    @Override
    public void onDisabled(Context context) {
        WidgetScheduler.cancelPeriodic(context);
    }
}
