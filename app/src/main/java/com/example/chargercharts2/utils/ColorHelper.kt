package com.example.chargercharts2.utils

import androidx.core.graphics.ColorUtils
import android.graphics.Color

fun Int.setAlpha(alpha: Int = 0) : Int{
    return ColorUtils.setAlphaComponent(this, alpha)
}