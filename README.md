# Bitcoin Ticker Widget

A deliberately tiny Android home-screen widget showing Bitcoin in EUR and USD.

- no ads, account, analytics or tracking
- public CoinGecko price endpoint with Coinbase fallback; no API key
- one shared 24-hour change, high/low and last successful update
- WorkManager refresh approximately every 15–30 minutes, subject to Android's
  battery and standby scheduling
- tap a widget price to open the chart; the dedicated refresh area triggers a
  manual refresh and a failed attempt never blocks later taps
- focused market screen and no unnecessary permissions
- native in-app EUR chart for 10 minutes, 1 hour, 24 hours, 4 days and all time
- Android 8.0 or newer

## Install

Install the current `BitcoinTicker` APK, then long-press an empty part of the Android
home screen, choose **Widgets**, find **Bitcoin Ticker**, and drag it into place.

Because the APK is locally built rather than Play-Store signed, Android may ask
you to allow installation from the app used to open the file.

## Build

The project requires JDK 17 and Android SDK 35.

```sh
./gradlew test lint assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
`assembleRelease` creates an unsigned, minified APK in
`app/build/outputs/apk/release/`, which is also the build path used by F-Droid.

For an upstream-signed release, install Android SDK Build Tools 34.0.0, add the
local `signing/signing.properties`, and run:

```sh
./gradlew clean signReleaseApk
```

This deliberately signs with `apksigner` 34 because Build Tools 35 signatures
cannot currently be verified by F-Droid's `apksigcopier`. The signed output is
`app/build/outputs/apk/release/app-release.apk`.

## Data source and privacy

The widget first connects to CoinGecko's public HTTPS market endpoint. If that
is unavailable, it falls back to Coinbase's public BTC-EUR and BTC-USD stats
endpoints. No exchange account is used.

The in-app chart uses public Coinbase candles for the short ranges. Its
all-time EUR view combines Blockchain.com's daily USD history with the matching
historical USD/EUR reference rates from Frankfurter. No API key is required.

`https://api.coingecko.com/api/v3/coins/markets?ids=bitcoin&amp;price_change_percentage=24h&amp;vs_currency=eur`

No identifier, account information or API key is sent. The only requested
Android permission is internet access.
