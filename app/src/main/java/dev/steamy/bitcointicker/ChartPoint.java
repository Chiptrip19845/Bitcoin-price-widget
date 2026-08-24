package dev.steamy.bitcointicker;

final class ChartPoint {
    final long timestampMillis;
    final double price;

    ChartPoint(long timestampMillis, double price) {
        this.timestampMillis = timestampMillis;
        this.price = price;
    }
}
