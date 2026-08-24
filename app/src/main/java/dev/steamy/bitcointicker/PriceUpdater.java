package dev.steamy.bitcointicker;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

final class PriceUpdater {
    static final String PREFS = "prices";
    static final String EUR = "eur";
    static final String USD = "usd";
    static final String CHANGE_EUR = "change_eur";
    static final String CHANGE_USD = "change_usd";
    static final String HIGH_EUR = "high_eur";
    static final String LOW_EUR = "low_eur";
    static final String HIGH_USD = "high_usd";
    static final String LOW_USD = "low_usd";
    static final String UPDATED_AT = "updated_at";
    static final String LAST_ERROR = "last_error";
    static final String CURRENCY_PREFERENCE = "currency_preference";

    private static final String COINGECKO_MARKETS =
            "https://api.coingecko.com/api/v3/coins/markets"
                    + "?ids=bitcoin&price_change_percentage=24h&vs_currency=";
    private static final String COINBASE_STATS =
            "https://api.exchange.coinbase.com/products/BTC-%s/stats";
    private static final long TOTAL_TIMEOUT_SECONDS = 12L;
    private static final AtomicBoolean UPDATE_IN_PROGRESS = new AtomicBoolean(false);

    private PriceUpdater() {}

    static boolean update(Context context) {
        if (!UPDATE_IN_PROGRESS.compareAndSet(false, true)) {
            // Another entry point is already fetching the same data.
            return true;
        }
        FutureTask<PriceSnapshot> request = new FutureTask<>(PriceUpdater::fetch);
        Thread requestThread = new Thread(request, "bitcoin-price-http");
        requestThread.start();
        try {
            PriceSnapshot snapshot = request.get(TOTAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(EUR, Double.toString(snapshot.eur))
                    .putString(USD, Double.toString(snapshot.usd))
                    .putString(CHANGE_EUR, Double.toString(snapshot.changeEur))
                    .putString(CHANGE_USD, Double.toString(snapshot.changeUsd))
                    .putString(HIGH_EUR, Double.toString(snapshot.highEur))
                    .putString(LOW_EUR, Double.toString(snapshot.lowEur))
                    .putString(HIGH_USD, Double.toString(snapshot.highUsd))
                    .putString(LOW_USD, Double.toString(snapshot.lowUsd))
                    .putLong(UPDATED_AT, snapshot.updatedAt)
                    .remove(LAST_ERROR)
                    .apply();
            renderSafely(context);
            return true;
        } catch (TimeoutException error) {
            request.cancel(true);
            saveError(context, "Timeout");
            return false;
        } catch (InterruptedException error) {
            request.cancel(true);
            Thread.currentThread().interrupt();
            saveError(context, "Interrupted");
            return false;
        } catch (ExecutionException error) {
            Throwable cause = error.getCause();
            saveError(context, cause == null ? "Network" : cause.getClass().getSimpleName());
            return false;
        } finally {
            UPDATE_IN_PROGRESS.set(false);
        }
    }

    static void recordUnexpectedError(Context context, Throwable error) {
        String name = error == null ? "Unexpected" : error.getClass().getSimpleName();
        saveError(context, name);
    }

    private static void saveError(Context context, String errorName) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(LAST_ERROR, errorName)
                .apply();
        renderSafely(context);
    }

    private static void renderSafely(Context context) {
        try {
            WidgetRenderer.renderAll(context, false);
        } catch (RuntimeException ignored) {
            // The fetched data remains cached even if a launcher rejects one render.
        }
    }

    private static PriceSnapshot fetch() throws Exception {
        Exception primaryError;
        try {
            return CoinGeckoResponseParser.parseMarkets(
                    get(COINGECKO_MARKETS + "eur", "BitcoinTickerWidget/1.3"),
                    get(COINGECKO_MARKETS + "usd", "BitcoinTickerWidget/1.3"),
                    System.currentTimeMillis());
        } catch (Exception error) {
            primaryError = error;
        }

        try {
            return CoinbaseResponseParser.parse(
                    get(String.format(Locale.US, COINBASE_STATS, "EUR"),
                            "BitcoinTickerWidget/1.3"),
                    get(String.format(Locale.US, COINBASE_STATS, "USD"),
                            "BitcoinTickerWidget/1.3"),
                    System.currentTimeMillis());
        } catch (Exception fallbackError) {
            fallbackError.addSuppressed(primaryError);
            throw fallbackError;
        }
    }

    private static String get(String endpoint, String userAgent) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2_500);
            connection.setReadTimeout(2_500);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", userAgent);

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + status);
            }
            return readFully(connection.getInputStream());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readFully(InputStream stream) throws Exception {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }
}
