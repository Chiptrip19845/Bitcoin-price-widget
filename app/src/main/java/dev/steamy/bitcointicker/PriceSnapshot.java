package dev.steamy.bitcointicker;

final class PriceSnapshot {
    final double eur;
    final double usd;
    final double changeEur;
    final double changeUsd;
    final double highEur;
    final double lowEur;
    final double highUsd;
    final double lowUsd;
    final long updatedAt;

    PriceSnapshot(double eur, double usd, double changeEur, double changeUsd,
                  double highEur, double lowEur, double highUsd, double lowUsd,
                  long updatedAt) {
        this.eur = eur;
        this.usd = usd;
        this.changeEur = changeEur;
        this.changeUsd = changeUsd;
        this.highEur = highEur;
        this.lowEur = lowEur;
        this.highUsd = highUsd;
        this.lowUsd = lowUsd;
        this.updatedAt = updatedAt;
    }
}
