package dev.steamy.bitcointicker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public final class PriceUpdateWorker extends Worker {
    static final String KEY_RETRY_ON_FAILURE = "retry_on_failure";

    public PriceUpdateWorker(@NonNull Context appContext,
                             @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            if (PriceUpdater.update(getApplicationContext())) {
                return Result.success();
            }
            return retryOrFail();
        } catch (Throwable error) {
            PriceUpdater.recordUnexpectedError(getApplicationContext(), error);
            return retryOrFail();
        }
    }

    private Result retryOrFail() {
        return getInputData().getBoolean(KEY_RETRY_ON_FAILURE, false)
                ? Result.retry()
                : Result.failure();
    }
}
