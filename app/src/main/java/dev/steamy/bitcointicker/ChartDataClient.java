package dev.steamy.bitcointicker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

final class ChartDataClient {
    private static final String COINBASE_CANDLES =
            "https://api.exchange.coinbase.com/products/BTC-%s/candles?granularity=%d";
    private static final String BLOCKCHAIN_ALL =
            "https://api.blockchain.info/charts/market-price?timespan=all&format=json";
    private static final String EXCHANGE_HISTORY =
            "https://api.frankfurter.app/2009-01-01..?from=USD&to=EUR";
    private static final String USER_AGENT = "BitcoinTickerWidget/2.0";

    private ChartDataClient() {}

    static ChartSeries fetch(ChartRange range, ChartCurrency currency) throws Exception {
        return range == ChartRange.ALL ? fetchAllTime(currency) : fetchCandles(range, currency);
    }

    static ChartSeries parseCandles(String json, ChartRange range, long nowMillis)
            throws Exception {
        JSONArray candles = new JSONArray(json);
        long cutoffMillis = nowMillis - range.durationSeconds * 1000L;
        List<ChartPoint> points = new ArrayList<>();
        for (int index = 0; index < candles.length(); index++) {
            JSONArray candle = candles.getJSONArray(index);
            long timestamp = candle.getLong(0) * 1000L;
            double close = candle.getDouble(4);
            if (timestamp >= cutoffMillis - range.granularitySeconds * 1000L
                    && close > 0 && Double.isFinite(close)) {
                points.add(new ChartPoint(timestamp, close));
            }
        }
        points.sort(Comparator.comparingLong(point -> point.timestampMillis));
        ensureEnoughPoints(points);
        return new ChartSeries(points);
    }

    static ChartSeries parseBlockchain(String json, String exchangeJson) throws Exception {
        JSONArray values = new JSONObject(json).getJSONArray("values");
        JSONObject rateValues = new JSONObject(exchangeJson).getJSONObject("rates");
        NavigableMap<LocalDate, Double> rates = new TreeMap<>();
        Iterator<String> dates = rateValues.keys();
        while (dates.hasNext()) {
            String date = dates.next();
            rates.put(LocalDate.parse(date), rateValues.getJSONObject(date).getDouble("EUR"));
        }
        if (rates.isEmpty()) throw new IllegalStateException("No exchange rates");
        List<ChartPoint> points = new ArrayList<>();
        int stride = Math.max(1, values.length() / 260);
        for (int index = 0; index < values.length(); index += stride) {
            JSONObject item = values.getJSONObject(index);
            long timestamp = item.getLong("x") * 1000L;
            LocalDate date = Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toLocalDate();
            java.util.Map.Entry<LocalDate, Double> rate = rates.floorEntry(date);
            if (rate == null) rate = rates.firstEntry();
            double eur = item.getDouble("y") * rate.getValue();
            if (eur > 0 && Double.isFinite(eur)) {
                points.add(new ChartPoint(timestamp, eur));
            }
        }
        ensureEnoughPoints(points);
        return new ChartSeries(points);
    }

    static ChartSeries parseBlockchainUsd(String json) throws Exception {
        JSONArray values = new JSONObject(json).getJSONArray("values");
        List<ChartPoint> points = new ArrayList<>();
        int stride = Math.max(1, values.length() / 260);
        for (int index = 0; index < values.length(); index += stride) {
            JSONObject item = values.getJSONObject(index);
            double usd = item.getDouble("y");
            if (usd > 0 && Double.isFinite(usd)) {
                points.add(new ChartPoint(item.getLong("x") * 1000L, usd));
            }
        }
        ensureEnoughPoints(points);
        return new ChartSeries(points);
    }

    private static ChartSeries fetchCandles(ChartRange range, ChartCurrency currency)
            throws Exception {
        String endpoint = String.format(Locale.US, COINBASE_CANDLES,
                currency.code, range.granularitySeconds);
        return parseCandles(get(endpoint), range, System.currentTimeMillis());
    }

    private static ChartSeries fetchAllTime(ChartCurrency currency) throws Exception {
        String history = get(BLOCKCHAIN_ALL);
        return currency == ChartCurrency.USD
                ? parseBlockchainUsd(history)
                : parseBlockchain(history, get(EXCHANGE_HISTORY));
    }

    private static String get(String endpoint) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(4_000);
            connection.setReadTimeout(7_000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", USER_AGENT);
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + status);
            }
            return readFully(connection.getInputStream());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String readFully(InputStream stream) throws Exception {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        }
        return result.toString();
    }

    private static void ensureEnoughPoints(List<ChartPoint> points) {
        if (points.size() < 2) throw new IllegalStateException("Not enough chart data");
    }
}
