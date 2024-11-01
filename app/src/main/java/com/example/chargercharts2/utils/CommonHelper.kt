package com.example.chargercharts2.utils

import android.content.res.Configuration
import androidx.fragment.app.Fragment

fun Fragment.isDarkTheme(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

fun <T> chooseValue(condition: Boolean, trueValue: T, falseValue: T): T {
    return if (condition) trueValue else falseValue
}