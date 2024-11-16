package com.example.chargercharts2.utils

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

fun Fragment.isDarkTheme(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

fun <T> chooseValue(condition: Boolean, trueValue: T, falseValue: T): T {
    return if (condition) trueValue else falseValue
}

fun updateViewMarginBottom(view: View, bottomMarginDp: Int, context: Context?) {
    val layoutParams = view.layoutParams as? ViewGroup.MarginLayoutParams
    if (layoutParams != null) {
        layoutParams.bottomMargin = dpToPx(bottomMarginDp, context)
        view.layoutParams = layoutParams
    }
}

fun dpToPx(dp: Int, context: Context?): Int {
    return dp * (context?.resources?.displayMetrics?.density ?: 1f).toInt()
}