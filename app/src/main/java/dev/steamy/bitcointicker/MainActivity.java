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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;

/** Focused Bitcoin market screen backing the home-screen widget. */
public final class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(8, 11, 15);
    private static final int SURFACE = Color.rgb(17, 22, 29);
    private static final int SURFACE_RAISED = Color.rgb(23, 29, 38);
    private static final int BORDER = Color.rgb(40, 48, 59);
    private static final int TEXT_PRIMARY = Color.rgb(247, 248, 250);
    private static final int TEXT_MUTED = Color.rgb(135, 145, 158);
    private static final int FOOTER_TEXT = Color.rgb(172, 182, 195);
    private static final int BITCOIN = Color.rgb(247, 147, 26);
    private static final int GREEN = Color.rgb(64, 201, 137);
    private static final int RED = Color.rgb(244, 99, 109);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final ChartRange[] PRESET_RANGES = {ChartRange.HOUR, ChartRange.DAY,
            ChartRange.FOUR_DAYS, ChartRange.FOURTEEN_DAYS,
            ChartRange.FOUR_WEEKS, ChartRange.ALL};
    private final EnumMap<ChartRange, TextView> presetButtons =
            new EnumMap<>(ChartRange.class);
    private final EnumMap<ChartCurrency, TextView> currencyButtons =
            new EnumMap<>(ChartCurrency.class);
    private final EnumMap<ChartCurrency, EnumMap<ChartRange, ChartSeries>> cache =
            new EnumMap<>(ChartCurrency.class);
    private final EnumMap<ChartCurrency, EnumSet<ChartRange>> loading =
            new EnumMap<>(ChartCurrency.class);
    private static final int INITIAL_ZOOM_PROGRESS = 200;
    private int zoomProgress = INITIAL_ZOOM_PROGRESS;
    private ChartCurrency selectedCurrency = ChartCurrency.EUR;
    private CurrencyPreference currencyPreference = CurrencyPreference.BOTH;

    private TextView priceText;
    private TextView marketPairText;
    private TextView alternatePriceText;
    private TextView changeText;
    private TextView chartRangeText;
    private TextView lowText;
    private TextView highText;
    private TextView footerText;
    private TextView currencyPreferenceText;
    private View currencySelector;
    private BitcoinChartView chartView;
    private ProgressBar progress;
    private SeekBar zoomSlider;
    private View sliderControls;
    private View presetControls;
    private TextView presetModeButton;
    private TextView sliderModeButton;
    private boolean sliderMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        for (ChartCurrency currency : ChartCurrency.values()) {
            cache.put(currency, new EnumMap<>(ChartRange.class));
            loading.put(currency, EnumSet.noneOf(ChartRange.class));
        }
        SharedPreferences prefs = getSharedPreferences(PriceUpdater.PREFS, Context.MODE_PRIVATE);
        currencyPreference = CurrencyPreference.fromStored(
                prefs.getString(PriceUpdater.CURRENCY_PREFERENCE, null));
        if (currencyPreference == CurrencyPreference.USD) {
            selectedCurrency = ChartCurrency.USD;
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
        loadChart(ChartRange.DAY, true);
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
        page.addView(buildZoomSelector());

        footerText = text(getString(R.string.public_market_data), 11,
                FOOTER_TEXT, Typeface.NORMAL);
        footerText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = wrapMatch();
        footerParams.topMargin = dp(12);
        page.addView(footerText, footerParams);

        TextView donate = text(getString(R.string.donation),
                11, FOOTER_TEXT, Typeface.NORMAL);
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

        currencyPreferenceText = text(currencyPreferenceLabel(), 11,
                FOOTER_TEXT, Typeface.NORMAL);
        currencyPreferenceText.setGravity(Gravity.CENTER);
        currencyPreferenceText.setPadding(dp(8), dp(6), dp(8), dp(2));
        currencyPreferenceText.setOnClickListener(v ->
                applyCurrencyPreference(currencyPreference.next()));
        page.addView(currencyPreferenceText, wrapMatch());

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
        TextView subtitle = text(getString(R.string.market_overview), 10,
                TEXT_MUTED, Typeface.BOLD);
        subtitle.setLetterSpacing(0.14f);
        LinearLayout.LayoutParams subParams = wrapWrap();
        subParams.topMargin = dp(4);
        names.addView(title);
        names.addView(subtitle, subParams);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameParams.leftMargin = dp(12);
        row.addView(names, nameParams);

        TextView market = text(getString(R.string.market_badge), 10, BITCOIN, Typeface.BOLD);
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
        marketPairText = text(getString(R.string.market_pair, selectedCurrency.code), 11,
                Color.rgb(206, 159, 102), Typeface.BOLD);
        marketPairText.setLetterSpacing(0.12f);
        labelRow.addView(marketPairText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        currencySelector = buildCurrencySelector();
        currencySelector.setVisibility(currencyPreference == CurrencyPreference.BOTH
                ? View.VISIBLE : View.GONE);
        labelRow.addView(currencySelector);
        card.addView(labelRow);

        priceText = text(getString(selectedCurrency == ChartCurrency.EUR
                ? R.string.empty_eur : R.string.empty_usd), 43, TEXT_PRIMARY, Typeface.BOLD);
        LinearLayout.LayoutParams priceParams = wrapWrap();
        priceParams.topMargin = dp(9);
        card.addView(priceText, priceParams);

        changeText = text(getString(R.string.range_day) + "  —", 13,
                TEXT_MUTED, Typeface.BOLD);
        LinearLayout.LayoutParams changeParams = wrapWrap();
        changeParams.topMargin = dp(5);
        card.addView(changeText, changeParams);

        ChartCurrency alternate = selectedCurrency == ChartCurrency.EUR
                ? ChartCurrency.USD : ChartCurrency.EUR;
        alternatePriceText = text(getString(R.string.empty_alternate_price, alternate.code),
                11, TEXT_MUTED, Typeface.BOLD);
        alternatePriceText.setVisibility(currencyPreference == CurrencyPreference.BOTH
                ? View.VISIBLE : View.GONE);
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
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(5), 0, dp(5), 0);
        TextView label = text(getString(R.string.chart_history), 10, TEXT_MUTED, Typeface.BOLD);
        label.setLetterSpacing(0.13f);
        header.addView(label);
        chartRangeText = text(getString(R.string.range_day).toUpperCase(Locale.getDefault()), 10,
                BITCOIN, Typeface.BOLD);
        chartRangeText.setLetterSpacing(0.08f);
        chartRangeText.setGravity(Gravity.END);
        chartRangeText.setSingleLine(true);
        header.addView(chartRangeText);
        card.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(43)));

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
        lowText = stat(getString(R.string.low), "—", Gravity.START);
        highText = stat(getString(R.string.high), "—", Gravity.END);
        stats.addView(lowText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        stats.addView(highText, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(stats);
        return card;
    }

    private View buildZoomSelector() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(4), dp(12), dp(4));
        panel.setBackground(ShapeFactory.roundedBorder(Color.rgb(13, 17, 22),
                Color.rgb(30, 37, 46), 17, 1));

        LinearLayout modeSelector = new LinearLayout(this);
        modeSelector.setOrientation(LinearLayout.HORIZONTAL);
        modeSelector.setPadding(dp(2), dp(2), dp(2), dp(2));
        modeSelector.setBackground(ShapeFactory.roundedBorder(Color.rgb(20, 23, 29),
                Color.rgb(48, 55, 65), 12, 1));
        presetModeButton = modeButton(getString(R.string.zoom_mode_presets), false);
        sliderModeButton = modeButton(getString(R.string.zoom_mode_slider), true);
        presetModeButton.setOnClickListener(view -> setSliderMode(false));
        sliderModeButton.setOnClickListener(view -> setSliderMode(true));
        modeSelector.addView(presetModeButton, new LinearLayout.LayoutParams(0, dp(25), 1f));
        modeSelector.addView(sliderModeButton, new LinearLayout.LayoutParams(0, dp(25), 1f));
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(29));
        modeParams.leftMargin = dp(75);
        modeParams.rightMargin = dp(75);
        modeParams.bottomMargin = dp(2);
        panel.addView(modeSelector, modeParams);

        presetControls = buildPresetControls();
        presetControls.setVisibility(View.GONE);
        panel.addView(presetControls);

        LinearLayout sliderPanel = new LinearLayout(this);
        sliderPanel.setOrientation(LinearLayout.VERTICAL);
        zoomSlider = new SeekBar(this);
        zoomSlider.setMax(ChartZoom.MAX_PROGRESS);
        zoomSlider.setProgress(zoomProgress);
        zoomSlider.setProgressTintList(ColorStateList.valueOf(BITCOIN));
        zoomSlider.setThumbTintList(ColorStateList.valueOf(Color.rgb(255, 177, 66)));
        zoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progressValue,
                                                    boolean fromUser) {
                zoomProgress = progressValue;
                updatePresetButtons();
                renderZoomWindow();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        sliderPanel.addView(zoomSlider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(34)));

        sliderPanel.addView(new ZoomMarkerView(this), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(17)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.HORIZONTAL);
        TextView near = text(getString(R.string.zoom_out), 9, TEXT_MUTED, Typeface.BOLD);
        TextView far = text(getString(R.string.zoom_in), 9, TEXT_MUTED, Typeface.BOLD);
        far.setGravity(Gravity.END);
        labels.addView(near, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        labels.addView(far, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        sliderPanel.addView(labels);
        sliderControls = sliderPanel;
        panel.addView(sliderControls);
        return panel;
    }

    private TextView modeButton(String label, boolean selected) {
        TextView button = text(label, 9,
                selected ? Color.rgb(20, 16, 12) : TEXT_MUTED, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(true);
        button.setBackground(selected
                ? ShapeFactory.roundedGradient(Color.rgb(255, 177, 66), BITCOIN, 10)
                : ShapeFactory.rounded(Color.TRANSPARENT, 10));
        return button;
    }

    private View buildPresetControls() {
        LinearLayout ranges = new LinearLayout(this);
        ranges.setOrientation(LinearLayout.HORIZONTAL);
        ranges.setGravity(Gravity.CENTER);
        for (ChartRange range : PRESET_RANGES) {
            TextView button = text(range.label(this), 9, TEXT_MUTED, Typeface.BOLD);
            button.setGravity(Gravity.CENTER);
            button.setSingleLine(true);
            button.setPadding(dp(1), dp(8), dp(1), dp(8));
            button.setOnClickListener(view -> selectPreset(range));
            ranges.addView(button, new LinearLayout.LayoutParams(0, dp(38), 1f));
            presetButtons.put(range, button);
        }
        updatePresetButtons();
        return ranges;
    }

    private void setSliderMode(boolean useSlider) {
        if (sliderMode == useSlider) return;
        sliderMode = useSlider;
        sliderControls.setVisibility(useSlider ? View.VISIBLE : View.GONE);
        presetControls.setVisibility(useSlider ? View.GONE : View.VISIBLE);
        if (!useSlider) selectPreset(nearestPreset(zoomProgress));
        updateModeButtons();
    }

    private void updateModeButtons() {
        styleModeButton(presetModeButton, !sliderMode);
        styleModeButton(sliderModeButton, sliderMode);
    }

    private void styleModeButton(TextView button, boolean selected) {
        button.setTextColor(selected ? Color.rgb(20, 16, 12) : TEXT_MUTED);
        button.setBackground(selected
                ? ShapeFactory.roundedGradient(Color.rgb(255, 177, 66), BITCOIN, 10)
                : ShapeFactory.rounded(Color.TRANSPARENT, 10));
    }

    private void selectPreset(ChartRange range) {
        int progressValue = presetProgress(range);
        zoomProgress = progressValue;
        zoomSlider.setProgress(progressValue);
        updatePresetButtons();
        renderZoomWindow();
    }

    private void updatePresetButtons() {
        for (ChartRange range : PRESET_RANGES) {
            TextView button = presetButtons.get(range);
            if (button == null) continue;
            boolean selected = presetProgress(range) == zoomProgress;
            button.setTextColor(selected ? Color.rgb(20, 16, 12) : TEXT_MUTED);
            button.setBackground(selected
                    ? ShapeFactory.roundedGradient(Color.rgb(255, 177, 66), BITCOIN, 11)
                    : ShapeFactory.rounded(Color.TRANSPARENT, 11));
        }
    }

    private ChartRange nearestPreset(int progressValue) {
        ChartRange closest = ChartRange.HOUR;
        int distance = Integer.MAX_VALUE;
        for (ChartRange range : PRESET_RANGES) {
            int candidateDistance = Math.abs(progressValue - presetProgress(range));
            if (candidateDistance < distance) {
                closest = range;
                distance = candidateDistance;
            }
        }
        return closest;
    }

    private int presetProgress(ChartRange range) {
        switch (range) {
            case HOUR: return 0;
            case DAY: return 200;
            case FOUR_DAYS: return 400;
            case FOURTEEN_DAYS: return 600;
            case FOUR_WEEKS: return 800;
            case ALL: return ChartZoom.MAX_PROGRESS;
            default: throw new IllegalArgumentException("Not a visible preset: " + range);
        }
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

    private void selectCurrency(ChartCurrency currency) {
        if (currencyPreference != CurrencyPreference.BOTH
                && !currencyPreference.name().equals(currency.name())) return;
        if (selectedCurrency == currency) return;
        selectedCurrency = currency;
        marketPairText.setText(getString(R.string.market_pair, currency.code));
        updateCurrencyButtons();
        renderStoredPrice();
        ChartRange source = ChartZoom.sourceFor(zoomProgress);
        ChartSeries cached = cache.get(currency).get(source);
        if (cached != null) {
            renderZoomWindow();
            progress.setVisibility(View.GONE);
            renderFooter();
        } else {
            loadChart(source, true);
        }
    }

    private void applyCurrencyPreference(CurrencyPreference preference) {
        currencyPreference = preference;
        getSharedPreferences(PriceUpdater.PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(PriceUpdater.CURRENCY_PREFERENCE, preference.name())
                .apply();
        if (currencyPreferenceText != null) {
            currencyPreferenceText.setText(currencyPreferenceLabel());
        }
        if (currencySelector != null) {
            currencySelector.setVisibility(preference == CurrencyPreference.BOTH
                    ? View.VISIBLE : View.GONE);
        }
        if (alternatePriceText != null) {
            alternatePriceText.setVisibility(preference == CurrencyPreference.BOTH
                    ? View.VISIBLE : View.GONE);
        }

        ChartCurrency target = preference == CurrencyPreference.USD
                ? ChartCurrency.USD : preference == CurrencyPreference.EUR
                ? ChartCurrency.EUR : selectedCurrency;
        if (target != selectedCurrency) {
            selectedCurrency = target;
            marketPairText.setText(getString(R.string.market_pair, target.code));
            priceText.setText(target == ChartCurrency.EUR
                    ? R.string.empty_eur : R.string.empty_usd);
            ChartCurrency alternate = target == ChartCurrency.EUR
                    ? ChartCurrency.USD : ChartCurrency.EUR;
            alternatePriceText.setText(getString(
                    R.string.empty_alternate_price, alternate.code));
            updateCurrencyButtons();
            renderStoredPrice();
            loadChart(ChartZoom.sourceFor(zoomProgress), true);
        } else {
            renderStoredPrice();
        }
        try {
            WidgetRenderer.renderAll(getApplicationContext(), false);
        } catch (RuntimeException ignored) {
            // The preference is saved even if a launcher rejects an immediate redraw.
        }
    }

    private String currencyPreferenceLabel() {
        String value = currencyPreference == CurrencyPreference.BOTH
                ? getString(R.string.currency_both) : currencyPreference.name();
        return getString(R.string.currency_preference, value);
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

    private void loadChart(ChartRange range, boolean showBusy) {
        ChartCurrency currency = selectedCurrency;
        if (cache.get(currency).containsKey(range) || loading.get(currency).contains(range)) return;
        loading.get(currency).add(range);
        if (showBusy) {
            progress.setVisibility(View.VISIBLE);
            footerText.setText(R.string.chart_loading);
        }
        new Thread(() -> {
            ChartSeries series = null;
            try {
                series = ChartDataClient.fetch(range, currency);
            } catch (Exception ignored) {
                // A calm inline error is more useful here than raw network details.
            }
            ChartSeries result = series;
            handler.post(() -> {
                loading.get(currency).remove(range);
                if (isFinishing() || isDestroyed()) return;
                if (result != null && !result.points.isEmpty()) {
                    cache.get(currency).put(range, result);
                    if (currency == selectedCurrency
                            && range == ChartZoom.sourceFor(zoomProgress)) {
                        progress.setVisibility(View.GONE);
                        renderZoomWindow();
                        renderFooter();
                    }
                    prefetchNext(currency, range);
                } else if (currency == selectedCurrency
                        && range == ChartZoom.sourceFor(zoomProgress)
                        && chartView.hasSeries() == false) {
                    progress.setVisibility(View.GONE);
                    chartView.showError();
                    footerText.setText(R.string.chart_unavailable);
                }
            });
        }, "bitcoin-chart-" + range.name().toLowerCase(Locale.US)).start();
    }

    private void renderZoomWindow() {
        ChartRange source = ChartZoom.sourceFor(zoomProgress);
        ChartSeries full = cache.get(selectedCurrency).get(source);
        if (full == null) {
            loadChart(source, true);
            updateZoomLabel(null, zoomDurationSeconds());
            return;
        }
        long duration = zoomDurationSeconds();
        ChartSeries visible = ChartZoom.window(full, duration);
        chartView.setSeries(visible, source, selectedCurrency);
        showChartStats(visible);
        updateZoomLabel(visible, duration);
    }

    private long zoomDurationSeconds() {
        ChartSeries all = cache.get(selectedCurrency).get(ChartRange.ALL);
        long allTime = ChartZoom.ASSUMED_ALL_TIME_SECONDS;
        if (all != null && all.points.size() >= 2) {
            allTime = Math.max(28L * 24 * 60 * 60,
                    (all.points.get(all.points.size() - 1).timestampMillis
                            - all.points.get(0).timestampMillis) / 1000L);
        }
        return ChartZoom.durationSeconds(zoomProgress, allTime);
    }

    private void updateZoomLabel(ChartSeries visible, long durationSeconds) {
        String duration = formatZoomDuration(durationSeconds);
        if (visible == null || visible.points.size() < 2) {
            chartRangeText.setText(duration.toUpperCase(Locale.getDefault()));
            return;
        }
        long start = visible.points.get(0).timestampMillis;
        long end = visible.points.get(visible.points.size() - 1).timestampMillis;
        boolean german = Locale.GERMAN.getLanguage().equals(Locale.getDefault().getLanguage());
        String pattern = german ? "dd.MM.yy" : "MMM d, yy";
        SimpleDateFormat date = new SimpleDateFormat(pattern, Locale.getDefault());
        chartRangeText.setText((duration + "  ·  " + date.format(new Date(start))
                + " – " + date.format(new Date(end))).toUpperCase(Locale.getDefault()));
    }

    private String formatZoomDuration(long seconds) {
        if (zoomProgress == ChartZoom.MAX_PROGRESS) return getString(R.string.zoom_period_all);
        long hours = Math.max(1, Math.round(seconds / 3600.0));
        if (hours < 48) return getString(R.string.zoom_period_hours, hours);
        long days = Math.round(hours / 24.0);
        if (days < 14) return getString(R.string.zoom_period_days, days);
        if (days < 70) return getString(R.string.zoom_period_weeks, Math.round(days / 7.0));
        if (days < 730) return getString(R.string.zoom_period_months, Math.round(days / 30.44));
        return getString(R.string.zoom_period_years, Math.round(days / 365.25));
    }

    private void prefetchNext(ChartCurrency currency, ChartRange loadedRange) {
        if (currency != selectedCurrency) return;
        ChartRange[] order = {ChartRange.HOUR, ChartRange.FOUR_DAYS,
                ChartRange.FOURTEEN_DAYS, ChartRange.FOUR_WEEKS,
                ChartRange.TEN_MONTHS, ChartRange.ALL};
        for (ChartRange candidate : order) {
            if (!cache.get(currency).containsKey(candidate)
                    && !loading.get(currency).contains(candidate)) {
                loadChart(candidate, false);
                return;
            }
        }
    }

    private void showChartStats(ChartSeries series) {
        lowText.setText(getString(R.string.low) + "\n" + selectedCurrency.format(series.minPrice()));
        lowText.setTextColor(Color.rgb(153, 163, 175));
        highText.setText(getString(R.string.high) + "\n" + selectedCurrency.format(series.maxPrice()));
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
            priceText.setText(selectedCurrency.format(selectedPrice));
        }
        if (!Double.isNaN(alternatePrice)) {
            ChartCurrency alternate = selectedCurrency == ChartCurrency.EUR
                    ? ChartCurrency.USD : ChartCurrency.EUR;
            alternatePriceText.setText(alternate.code + "  " + alternate.format(alternatePrice));
        }
        if (!Double.isNaN(change)) {
            boolean positive = change >= 0;
            changeText.setText(getString(R.string.change_24_hours,
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
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date(updatedAt));
            footerText.setText(getString(R.string.updated_market_data, time));
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
