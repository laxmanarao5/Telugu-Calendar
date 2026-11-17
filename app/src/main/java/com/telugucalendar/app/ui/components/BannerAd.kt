package com.telugucalendar.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.telugucalendar.app.BuildConfig

/**
 * Google AdMob adaptive banner. The unit id comes from secrets.properties (ADMOB_BANNER_UNIT_ID); when it is blank the build uses
 * Google's public test unit, so a fresh checkout shows harmless "Test Ad" banners. The strip takes no space when no ad is available.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    var failed by remember { mutableStateOf(false) }

    val adView = remember(widthDp) {
        AdView(context).apply {
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
            adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) { failed = true }
                override fun onAdLoaded() { failed = false }
            }
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }

    if (!failed) {
        AndroidView(factory = { adView }, modifier = modifier.fillMaxWidth())
    }
}
