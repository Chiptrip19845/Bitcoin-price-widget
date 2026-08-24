package dev.steamy.bitcointicker;

import org.json.JSONObject;

final class CoinbaseResponseParser {
    private CoinbaseResponseParser() {}

    static PriceSnapshot parse(String eurJson, String usdJson, long updatedAt)
            throws Exception {
        JSONObject eur = new JSONObject(eurJson);
        JSONObject usd = new JSONObject(usdJson);
        double eurLast = eur.getDouble("last");
        double usdLast = usd.getDouble("last");
        double eurOpen = eur.getDouble("open");
        double usdOpen = usd.getDouble("open");
        return new PriceSnapshot(
                eurLast,
                usdLast,
                percentChange(eurLast, eurOpen),
                percentChange(usdLast, usdOpen),
                eur.getDouble("high"),
                eur.getDouble("low"),
                usd.getDouble("high"),
                usd.getDouble("low"),
                updatedAt
        );
    }

    private static double percentChange(double last, double open) {
        return open == 0.0 ? 0.0 : ((last - open) / open) * 100.0;
    }
}
