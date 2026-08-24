package dev.steamy.bitcointicker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

final class WidgetScheduler {
    private static final String PERIODIC_WORK_NAME = "bitcoin-widget-periodic-refresh";
    private static final String IMMEDIATE_WORK_NAME = "bitcoin-widget-immediate-refresh";
    private static final long REFRESH_INTERVAL_MINUTES = 15L;

    // v1.9.1-v1.9.3 migration identifiers.
    private static final int LEGACY_PERIODIC_JOB_ID = 0xB17C01;
    private static final int LEGACY_REFRESH_ALARM_REQUEST_CODE = 0xB17C02;
    private static final String LEGACY_ALARM_ACTION =
            "dev.steamy.bitcointicker.action.SCHEDULED_REFRESH";
    private static final String LEGACY_ALARM_RECEIVER =
            "dev.steamy.bitcointicker.RefreshAlarmReceiver";

    private WidgetScheduler() {}

    static void schedulePeriodic(Context context) {
        Context appContext = context.getApplicationContext();
        cancelLegacySchedules(appContext);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                PriceUpdateWorker.class,
                REFRESH_INTERVAL_MINUTES,
                TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(new Data.Builder()
                        .putBoolean(PriceUpdateWorker.KEY_RETRY_ON_FAILURE, true)
                        .build())
                .build();

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request);
    }

    static void scheduleNow(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(
                PriceUpdateWorker.class)
                .setConstraints(constraints)
                .setInputData(new Data.Builder()
                        .putBoolean(PriceUpdateWorker.KEY_RETRY_ON_FAILURE, false)
                        .build())
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request);
    }

    static void cancelPeriodic(Context context) {
        Context appContext = context.getApplicationContext();
        WorkManager workManager = WorkManager.getInstance(appContext);
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME);
        workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME);
        cancelLegacySchedules(appContext);
    }

    private static void cancelLegacySchedules(Context context) {
        try {
            JobScheduler jobs = (JobScheduler) context.getSystemService(
                    Context.JOB_SCHEDULER_SERVICE);
            jobs.cancel(LEGACY_PERIODIC_JOB_ID);
            jobs.cancel(LEGACY_PERIODIC_JOB_ID + 1);
        } catch (RuntimeException ignored) {
            // Migration cleanup must not prevent WorkManager scheduling.
        }

        try {
            Intent intent = new Intent()
                    .setComponent(new ComponentName(context, LEGACY_ALARM_RECEIVER))
                    .setAction(LEGACY_ALARM_ACTION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    LEGACY_REFRESH_ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pendingIntent != null) {
                AlarmManager alarms = (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE);
                alarms.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        } catch (RuntimeException ignored) {
            // The old alarm will disappear automatically if cleanup is rejected.
        }
    }
}
