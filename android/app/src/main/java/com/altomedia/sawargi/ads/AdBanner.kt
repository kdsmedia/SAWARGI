package com.altomedia.sawargi.ads

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.altomedia.sawargi.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * AdMob banner rendered inside Compose via AndroidView.
 * Reads the banner ad unit id from strings.xml.
 */
@SuppressLint("VisibleForTests")
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
) {
    val adUnitId = context.getString(R.string.admob_banner)
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            AdView(ctx).apply {
                this.adUnitId = adUnitId
                setAdSize(AdSize.BANNER)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}