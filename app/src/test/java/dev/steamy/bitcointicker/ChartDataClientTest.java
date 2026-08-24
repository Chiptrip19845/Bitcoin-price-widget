package dev.steamy.bitcointicker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ChartDataClientTest {
    @Test
    public void candlesAreFilteredAndSorted() throws Exception {
        long now = 1_800_000_000_000L;
        long nowSeconds = now / 1000L;
        String json = "[[" + nowSeconds + ",1,2,1,120,3],"
                + "[" + (nowSeconds - 300) + ",1,2,1,110,3],"
                + "[" + (nowSeconds - 7200) + ",1,2,1,90,3]]";

        ChartSeries result = ChartDataClient.parseCandles(json, ChartRange.HOUR, now);

        assertEquals(2, result.points.size());
        assertEquals(110.0, result.points.get(0).price, 0.001);
        assertEquals(120.0, result.points.get(1).price, 0.001);
    }

    @Test
    public void blockchainUsesRateFromSameOrPreviousBusinessDay() throws Exception {
        String chart = "{\"values\":["
                + "{\"x\":1609459200,\"y\":100},"
                + "{\"x\":1609545600,\"y\":200}]}";
        String rates = "{\"rates\":{\"2021-01-01\":{\"EUR\":0.8}}}";

        ChartSeries result = ChartDataClient.parseBlockchain(chart, rates);

        assertEquals(80.0, result.points.get(0).price, 0.001);
        assertEquals(160.0, result.points.get(1).price, 0.001);
    }

    @Test
    public void blockchainUsdKeepsNativePrices() throws Exception {
        String chart = "{\"values\":["
                + "{\"x\":1609459200,\"y\":100},"
                + "{\"x\":1609545600,\"y\":200}]}";

        ChartSeries result = ChartDataClient.parseBlockchainUsd(chart);

        assertEquals(100.0, result.points.get(0).price, 0.001);
        assertEquals(200.0, result.points.get(1).price, 0.001);
    }
}
