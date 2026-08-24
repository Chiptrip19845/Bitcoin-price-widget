package dev.steamy.bitcointicker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CoinGeckoResponseParserTest {
    @Test
    public void parsesBothMarketsAndRanges() throws Exception {
        String eur = "[{\"current_price\":66123.0,\"price_change_percentage_24h\":2.5," +
                "\"high_24h\":67000.0,\"low_24h\":64000.0}]";
        String usd = "[{\"current_price\":77123.0,\"price_change_percentage_24h\":2.7," +
                "\"high_24h\":78000.0,\"low_24h\":74000.0}]";

        PriceSnapshot result = CoinGeckoResponseParser.parseMarkets(eur, usd, 1234L);

        assertEquals(66123.0, result.eur, 0.001);
        assertEquals(77123.0, result.usd, 0.001);
        assertEquals(2.5, result.changeEur, 0.001);
        assertEquals(2.7, result.changeUsd, 0.001);
        assertEquals(67000.0, result.highEur, 0.001);
        assertEquals(64000.0, result.lowEur, 0.001);
        assertEquals(78000.0, result.highUsd, 0.001);
        assertEquals(74000.0, result.lowUsd, 0.001);
        assertEquals(1234L, result.updatedAt);
    }
}
