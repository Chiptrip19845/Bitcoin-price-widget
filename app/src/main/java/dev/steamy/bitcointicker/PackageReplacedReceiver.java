package dev.steamy.bitcointicker;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/** Migrates existing widget installations to the current WorkManager schedule. */
public final class PackageReplacedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(
                new ComponentName(context, BitcoinWidgetProvider.class));
        if (ids != null && ids.length > 0) {
            WidgetScheduler.schedulePeriodic(context);
            WidgetScheduler.scheduleNow(context);
        }
    }
}
