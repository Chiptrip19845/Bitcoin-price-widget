package dev.steamy.bitcointicker;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

/**
 * Standard Android app-widget configuration flow. Launchers open this
 * activity automatically when a new widget is placed, which also takes a
 * freshly installed app out of Android's stopped state.
 */
public final class WidgetSetupActivity extends Activity {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        Intent sourceIntent = getIntent();
        if (sourceIntent != null && sourceIntent.getExtras() != null) {
            appWidgetId = sourceIntent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        TextView status = createStatusView();
        setContentView(status);

        Context appContext = getApplicationContext();
        WidgetScheduler.schedulePeriodic(appContext);
        new Thread(() -> {
            boolean success = false;
            try {
                success = PriceUpdater.update(appContext);
            } catch (Throwable error) {
                PriceUpdater.recordUnexpectedError(appContext, error);
            }
            boolean updateSucceeded = success;
            mainHandler.post(() -> completeSetup(status, updateSucceeded));
        }, "bitcoin-widget-setup").start();
    }

    private TextView createStatusView() {
        TextView status = new TextView(this);
        status.setText("₿  Bitcoin Ticker\n\nWidget wird eingerichtet …");
        status.setTextSize(18f);
        status.setTextColor(Color.WHITE);
        status.setBackgroundColor(Color.rgb(18, 21, 27));
        int padding = (int) (32 * getResources().getDisplayMetrics().density);
        status.setPadding(padding, padding, padding, padding);
        return status;
    }

    private void completeSetup(TextView status, boolean success) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            WidgetRenderer.renderCached(this, manager, new int[]{appWidgetId}, false);
        } catch (RuntimeException ignored) {
            // The launcher will request another render; setup may still complete.
        }

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, result);
        status.setText(success
                ? "₿  Bitcoin Ticker\n\nWidget ist bereit."
                : "₿  Bitcoin Ticker\n\nWidget hinzugefügt. Kurs folgt bei bestehender Verbindung.");
        mainHandler.postDelayed(this::finish, 650L);
    }
}
