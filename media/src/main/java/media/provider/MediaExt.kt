package media.provider

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import media.provider.model.MediaStoreImage

fun MediaStoreImage.getCompressedSize(context: Context): Pair<Double, Double> {

    val deviceWith = 3000
    val originalW = originalWidth
    val originalH = originalHeight


    val minimumImageCount = 2
    val maximumImageCount = 5
    val deviceWidth = getDeviceWidth(context)


    return Pair(0.0, 0.0)
}

fun getDeviceWidth(context: Context): Float {

    val displayMetrics = DisplayMetrics()
    val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.widthPixels.toFloat()
}