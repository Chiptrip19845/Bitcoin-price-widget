package dev.steamy.bitcointicker;

import java.util.Collections;
import java.util.List;

final class ChartSeries {
    final List<ChartPoint> points;

    ChartSeries(List<ChartPoint> points) {
        this.points = Collections.unmodifiableList(points);
    }

    double minPrice() {
        double result = Double.POSITIVE_INFINITY;
        for (ChartPoint point : points) result = Math.min(result, point.price);
        return result;
    }

    double maxPrice() {
        double result = Double.NEGATIVE_INFINITY;
        for (ChartPoint point : points) result = Math.max(result, point.price);
        return result;
    }
}
