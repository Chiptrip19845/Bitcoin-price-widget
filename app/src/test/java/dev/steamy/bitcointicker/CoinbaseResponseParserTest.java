package dev.steamy.bitcointicker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CoinbaseResponseParserTest {
    @Test
    public void parsesFallbackStatsAndComputesChange() throws Exception {
        String eur = "{\"open\":\"60000\",\"high\":\"67000\",\"low\":\"59000\",\"last\":\"63000\"}";
        String usd = "{\"open\":\"70000\",\"high\":\"78000\",\"low\":\"69000\",\"last\":\"73500\"}";

        PriceSnapshot result = CoinbaseResponseParser.parse(eur, usd, 1234L);

        assertEquals(63000.0, result.eur, 0.001);
        assertEquals(73500.0, result.usd, 0.001);
        assertEquals(5.0, result.changeEur, 0.001);
        assertEquals(5.0, result.changeUsd, 0.001);
        assertEquals(67000.0, result.highEur, 0.001);
        assertEquals(69000.0, result.lowUsd, 0.001);
    }
}
