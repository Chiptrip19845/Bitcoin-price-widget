package dev.steamy.bitcointicker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.Locale;

/** Focused Bitcoin market screen backing the home-screen widget. */
public final class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(8, 11, 15);
    private static final int SURFACE = Color.rgb(17, 22, 29);
    private static final int SURFACE_RAISED = Color.rgb(23, 29, 38);
    private static final int BORDER = Color.rgb(40, 48, 59);
    private static final int TEXT_PRIMARY = Color.rgb(247, 248, 250);
    private static final int TEXT_MUTED = Color.rgb(135, 145, 158);
    private static final int BITCOIN = Color.rgb(247, 147, 26);
    private static final int GREEN = Color.rgb(64, 201, 137);
    private static final int RED = Color.rgb(244, 99, 109);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final EnumMap<ChartRange, TextView> rangeButtons = new EnumMap<>(ChartRange.class);
    private final EnumMap<ChartCurrency, TextView> currencyButtons =
            new EnumMap<>(ChartCurrency.class);
    private final EnumMap<ChartCurrency, EnumMap<ChartRange, ChartSeries>> cache =
            new EnumMap<>(ChartCurrency.class);
    private int requestGeneration;
    private ChartRange selectedRange = ChartRange.DAY;
    private ChartCurrency selectedCurrency = ChartCurrency.EUR;

    private TextView priceText;
    private TextView marketPairText;
    private TextView alternatePriceText;
    private TextView changeText;
    private TextView chartRangeText;
    private TextView lowText;
    private TextView highText;
    private TextView footerText;
    private BitcoinChartView chartView;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        for (ChartCurrency currency : ChartCurrency.values()) {
            cache.put(currency, new EnumMap<>(ChartRange.class));
        }
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(0);
        setContentView(buildContent());

        Context appContext = getApplicationContext();
        try {
            WidgetScheduler.schedulePeriodic(appContext);
        } catch (RuntimeException ignored) {
            // A vendor-specific WorkManager failure must never close the app.
        }
        renderStoredPrice();
        loadChart(selectedRange);
        refreshPriceInBackground();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            WidgetScheduler.schedulePeriodic(getApplicationContext());
        } catch (RuntimeException ignored) {
            // Keep the market screen usable on broken vendor WorkManager builds.
        }
        renderStoredPrice();
    }

    private View buildContent() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(19), dp(20), dp(16));
        page.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(dp(20), dp(19) + insets.getSystemWindowInsetTop(),
                    dp(20), dp(16) + insets.getSystemWindowInsetBottom());
            return insets;
        });
        page.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(30, 22, 14), BACKGROUND, Color.rgb(8, 13, 19)}));

        page.addView(buildHeader());
        page.addView(buildHeroCard());
        page.addView(buildChartCard(), chartCardParams());
        page.addView(buildRangeSelector());

        footerText = text("Öffentliche Marktdaten  ·  keine Anmeldung", 11,
                TEXT_MUTED, Typeface.NORMAL);
        footerText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = wrapMatch();
        footerParams.topMargin = dp(12);
        page.addView(footerText, footerParams);

        TextView donate = text("☕  App unterstützen  ·  paypal.me/SimonKirschner",
                11, TEXT_MUTED, Typeface.NORMAL);
        donate.setGravity(Gravity.CENTER);
        donate.setPadding(dp(8), dp(6), dp(8), dp(2));
        donate.setOnClickListener(v -> {
            try {
                startActivity(new android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://www.paypal.me/SimonKirschner")));
            } catch (android.content.ActivityNotFoundException ignored) {
                // No browser available — stay silent, donation must never nag.
            }
        });
        page.addView(donate, wrapMatch());

        updateRangeButtons();
        return page;
    }

    private View buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView coin = text("₿", 25, BITCOIN, Typeface.BOLD);
        coin.setGravity(Gravity.CENTER);
        coin.setBackground(ShapeFactory.roundedBorder(Color.rgb(32, 24, 16),
                Color.rgb(110, 68, 24), 15, 1));
        row.addView(coin, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("Bitcoin", 20, TEXT_PRIMARY, Typeface.BOLD);
        TextView subtitle = text("MARKET OVERVIEW", 10, TEXT_MUTED, Typeface.BOLD);
        subtitle.setLetterSpacing(0.14f);
        LinearLayout.LayoutParams subParams = wrapWrap();
        subParams.topMargin = dp(4);
        names.addView(title);
        names.addView(subtitle, subParams);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameParams.leftMargin = dp(12);
        row.addView(names, nameParams);

        TextView market = text("●  MARKET", 10, BITCOIN, Typeface.BOLD);
        market.setLetterSpacing(0.08f);
        market.setGravity(Gravity.CENTER);
        market.setPadding(dp(11), 0, dp(11), 0);
        market.setBackground(ShapeFactory.roundedBorder(Color.rgb(29, 23, 17),
                Color.rgb(82, 55, 25), 14, 1));
        row.addView(market, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(29)));
        return row;
    }

    private View buildHeroCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(16), dp(20), dp(16));
        card.setBackground(ShapeFactory.roundedGradient(
                Color.rgb(44, 30, 18), Color.rgb(18, 22, 29), 22));
        LinearLayout.LayoutParams cardParams = wrapMatch();
        cardParams.topMargin = dp(18);
        card.setLayoutParams(cardParams);

        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        marketPairText = text("BTC / EUR", 11, Color.rgb(206, 159, 102), Typeface.BOLD);
        marketPairText.setLetterSpacing(0.12f);
        labelRow.addView(marketPairText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        labelRow.addView(buildCurrencySelector());
        card.addView(labelRow);

        priceText = text("€ —", 43, TEXT_PRIMARY, Typeface.BOLD);
        LinearLayout.LayoutParams priceParams = wrapWrap();
        priceParams.topMargin = dp(9);
        card.addView(priceText, priceParams);

        changeText = text("24 Std.  —", 13, TEXT_MUTED, Typeface.BOLD);
        LinearLayout.LayoutParams changeParams = wrapWrap();
        changeParams.topMargin = dp(5);
        card.addView(changeText, changeParams);

        alternatePriceText = text("USD  —", 11, TEXT_MUTED, Typeface.BOLD);
        LinearLayout.LayoutParams alternateParams = wrapWrap();
        alternateParams.topMargin = dp(7);
        card.addView(alternatePriceText, alternateParams);
        return card;
    }

    private View buildCurrencySelector() {
        LinearLayout selector = new LinearLayout(this);
        selector.setOrientation(LinearLayout.HORIZONTAL);
        selector.setPadding(dp(2), dp(2), dp(2), dp(2));
        selector.setBackground(ShapeFactory.roundedBorder(Color.rgb(20, 23, 29),
                Color.rgb(63, 52, 38), 13, 1));
        for (ChartCurrency currency : ChartCurrency.values()) {
            TextView button = text(currency.code, 10, TEXT_MUTED, Typeface.BOLD);
            button.setGravity(Gravity.CENTER);
            button.setOnClickListener(view -> selectCurrency(currency));
            selector.addView(button, new LinearLayout.LayoutParams(dp(47), dp(25)));
            currencyButtons.put(currency, button);
        }
        updateCurrencyButtons();
        return selector;
    }

    private View buildChartCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(13), dp(12), dp(10));
        card.setBackground(ShapeFactory.roundedBorder(SURFACE, BORDER, 22, 1));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(5), 0, dp(5), 0);
        TextView label = text("KURSVERLAUF", 10, TEXT_MUTED, Typeface.BOLD);
        label.setLetterSpacing(0.13f);
        header.addView(label, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        chartRangeText = text(selectedRange.label.toUpperCase(Locale.GERMANY), 10,
                BITCOIN, Typeface.BOLD);
        chartRangeText.setLetterSpacing(0.08f);
        header.addView(chartRangeText);
        card.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(25)));

        chartView = new BitcoinChartView(this);
        card.addView(chartView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        progress.setIndeterminateTintList(ColorStateList.valueOf(BITCOIN));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(22), dp(22));
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.topMargin = -dp(39);
        progressParams.bottomMargin = dp(17);
        card.addView(progress, progressParams);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setPadding(dp(3), dp(5), dp(3), 0);
        lowText = stat("TIEF", "—", Gravity.START);
        highText = stat("HOCH", "—", Gravity.END);
        stats.addView(lowText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        stats.addView(highText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(stats);
        return card;
    }

    private View buildRangeSelector() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFillViewport(true);
        scroll.setPadding(dp(4), dp(4), dp(4), dp(4));
        scroll.setBackground(ShapeFactory.roundedBorder(Color.rgb(13, 17, 22),
                Color.rgb(30, 37, 46), 17, 1));
        LinearLayout ranges = new LinearLayout(this);
        ranges.setOrientation(LinearLayout.HORIZONTAL);
        ranges.setGravity(Gravity.CENTER);
        for (ChartRange range : ChartRange.values()) {
            TextView button = text(range.label, 12, TEXT_MUTED, Typeface.BOLD);
            button.setGravity(Gravity.CENTER);
            button.setMinWidth(dp(58));
            button.setPadding(dp(10), dp(10), dp(10), dp(10));
            button.setOnClickListener(view -> selectRange(range));
            ranges.addView(button, new LinearLayout.LayoutParams(0, dp(38), 1f));
            rangeButtons.put(range, button);
        }
        scroll.addView(ranges, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.MATCH_PARENT,
                HorizontalScrollView.LayoutParams.WRAP_CONTENT));
        return scroll;
    }

    private TextView stat(String label, String value, int gravity) {
        TextView view = text(label + "\n" + value, 10, TEXT_MUTED, Typeface.BOLD);
        view.setGravity(gravity);
        view.setLineSpacing(dp(3), 1f);
        return view;
    }

    private LinearLayout.LayoutParams chartCardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        params.topMargin = dp(15);
        params.bottomMargin = dp(13);
        return params;
    }

    private void selectRange(ChartRange range) {
        if (selectedRange == range) return;
        selectedRange = range;
        chartRangeText.setText(range.label.toUpperCase(Locale.GERMANY));
        updateRangeButtons();
        ChartSeries cached = cache.get(selectedCurrency).get(range);
        if (cached != null) {
            requestGeneration++;
            chartView.setSeries(cached, range, selectedCurrency);
            showChartStats(cached);
            progress.setVisibility(View.GONE);
        } else {
            loadChart(range);
        }
    }

    private void selectCurrency(ChartCurrency currency) {
        if (selectedCurrency == currency) return;
        selectedCurrency = currency;
        marketPairText.setText("BTC / " + currency.code);
        updateCurrencyButtons();
        renderStoredPrice();
        ChartSeries cached = cache.get(currency).get(selectedRange);
        if (cached != null) {
            requestGeneration++;
            chartView.setSeries(cached, selectedRange, selectedCurrency);
            showChartStats(cached);
            progress.setVisibility(View.GONE);
            renderFooter();
        } else {
            loadChart(selectedRange);
        }
    }

    private void updateCurrencyButtons() {
        for (ChartCurrency currency : ChartCurrency.values()) {
            TextView button = currencyButtons.get(currency);
            if (button == null) continue;
            boolean selected = currency == selectedCurrency;
            button.setTextColor(selected ? Color.rgb(20, 16, 12) : TEXT_MUTED);
            button.setBackground(selected
                    ? ShapeFactory.roundedGradient(Color.rgb(255, 177, 66), BITCOIN, 11)
                    : ShapeFactory.rounded(Color.TRANSPARENT, 11));
        }
    }

    private void updateRangeButtons() {
        for (ChartRange range : ChartRange.values()) {
            TextView button = rangeButtons.get(range);
            boolean selected = range == selectedRange;
            button.setTextColor(selected ? Color.rgb(20, 16, 12) : TEXT_MUTED);
            button.setBackground(selected
                    ? ShapeFactory.roundedGradient(Color.rgb(255, 177, 66), BITCOIN, 13)
                    : ShapeFactory.rounded(Color.TRANSPARENT, 13));
            button.setElevation(selected ? dp(3) : 0f);
        }
    }

    private void loadChart(ChartRange range) {
        ChartCurrency currency = selectedCurrency;
        int generation = ++requestGeneration;
        progress.setVisibility(View.VISIBLE);
        lowText.setText("TIEF\n—");
        highText.setText("HOCH\n—");
        footerText.setText("Kursverlauf wird geladen …");
        new Thread(() -> {
            ChartSeries series = null;
            try {
                series = ChartDataClient.fetch(range, currency);
            } catch (Exception ignored) {
                // A calm inline error is more useful here than raw network details.
            }
            ChartSeries result = series;
            handler.post(() -> {
                if (isFinishing() || isDestroyed() || generation != requestGeneration) return;
                progress.setVisibility(View.GONE);
                if (result != null && !result.points.isEmpty()) {
                    cache.get(currency).put(range, result);
                    chartView.setSeries(result, range, currency);
                    showChartStats(result);
                    renderFooter();
                } else {
                    chartView.showError();
                    footerText.setText("Kursverlauf momentan nicht verfügbar");
                }
            });
        }, "bitcoin-chart-" + range.name().toLowerCase(Locale.US)).start();
    }

    private void showChartStats(ChartSeries series) {
        NumberFormat format = NumberFormat.getCurrencyInstance(selectedCurrency.locale);
        format.setMaximumFractionDigits(0);
        lowText.setText("TIEF\n" + format.format(series.minPrice()));
        lowText.setTextColor(Color.rgb(153, 163, 175));
        highText.setText("HOCH\n" + format.format(series.maxPrice()));
        highText.setTextColor(Color.rgb(214, 220, 227));
    }

    private void refreshPriceInBackground() {
        Context appContext = getApplicationContext();
        new Thread(() -> {
            try {
                PriceUpdater.update(appContext);
            } catch (Throwable ignored) {
                // Cached prices and chart remain useful if the live refresh fails.
            }
            handler.post(() -> {
                if (!isFinishing() && !isDestroyed()) renderStoredPrice();
            });
        }, "bitcoin-ticker-initial-refresh").start();
    }

    private void renderStoredPrice() {
        SharedPreferences prefs = getSharedPreferences(PriceUpdater.PREFS, Context.MODE_PRIVATE);
        double eur = readDouble(prefs, PriceUpdater.EUR);
        double usd = readDouble(prefs, PriceUpdater.USD);
        double changeEur = readDouble(prefs, PriceUpdater.CHANGE_EUR);
        double changeUsd = readDouble(prefs, PriceUpdater.CHANGE_USD);
        double selectedPrice = selectedCurrency == ChartCurrency.EUR ? eur : usd;
        double alternatePrice = selectedCurrency == ChartCurrency.EUR ? usd : eur;
        double change = selectedCurrency == ChartCurrency.EUR ? changeEur : changeUsd;
        if (!Double.isNaN(selectedPrice)) {
            NumberFormat price = NumberFormat.getCurrencyInstance(selectedCurrency.locale);
            price.setMaximumFractionDigits(0);
            priceText.setText(price.format(selectedPrice));
        }
        if (!Double.isNaN(alternatePrice)) {
            ChartCurrency alternate = selectedCurrency == ChartCurrency.EUR
                    ? ChartCurrency.USD : ChartCurrency.EUR;
            NumberFormat price = NumberFormat.getCurrencyInstance(alternate.locale);
            price.setMaximumFractionDigits(0);
            alternatePriceText.setText(alternate.code + "  " + price.format(alternatePrice));
        }
        if (!Double.isNaN(change)) {
            boolean positive = change >= 0;
            changeText.setText(String.format(Locale.GERMANY, "%s  %.2f %%  ·  24 STD.",
                    positive ? "▲" : "▼", Math.abs(change)));
            changeText.setTextColor(positive ? GREEN : RED);
            chartView.setDayChangePositive(positive);
        }
        renderFooter();
    }

    private void renderFooter() {
        SharedPreferences prefs = getSharedPreferences(PriceUpdater.PREFS, Context.MODE_PRIVATE);
        long updatedAt = prefs.getLong(PriceUpdater.UPDATED_AT, 0L);
        if (updatedAt > 0 && footerText != null) {
            String time = new SimpleDateFormat("HH:mm", Locale.GERMANY).format(new Date(updatedAt));
            footerText.setText("Aktualisiert " + time + "  ·  öffentliche Marktdaten");
        }
    }

    private static double readDouble(SharedPreferences prefs, String key) {
        try {
            return Double.parseDouble(prefs.getString(key, ""));
        } catch (RuntimeException ignored) {
            return Double.NaN;
        }
    }

    private TextView text(String value, float sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(getResources().getFont(R.font.space_grotesk_bold), style);
        view.setIncludeFontPadding(false);
        return view;
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapMatch() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
