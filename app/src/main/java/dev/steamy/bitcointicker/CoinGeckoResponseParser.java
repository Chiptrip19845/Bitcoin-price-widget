package dev.steamy.bitcointicker;

import org.json.JSONObject;
import org.json.JSONArray;

final class CoinGeckoResponseParser {
    private CoinGeckoResponseParser() {}

    static PriceSnapshot parseMarkets(String eurJson, String usdJson, long updatedAt)
            throws Exception {
        JSONObject eur = new JSONArray(eurJson).getJSONObject(0);
        JSONObject usd = new JSONArray(usdJson).getJSONObject(0);
        return new PriceSnapshot(
                eur.getDouble("current_price"),
                usd.getDouble("current_price"),
                eur.getDouble("price_change_percentage_24h"),
                usd.getDouble("price_change_percentage_24h"),
                eur.getDouble("high_24h"),
                eur.getDouble("low_24h"),
                usd.getDouble("high_24h"),
                usd.getDouble("low_24h"),
                updatedAt
        );
    }
}
