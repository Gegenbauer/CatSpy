package me.gegenbauer.catspy.configuration

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.extras.FlatAnimatedLafChange

fun updateUIWithAnim(updateUI: () -> Unit = {}) {
    FlatAnimatedLafChange.showSnapshot()
    updateUI()
    FlatLaf.updateUI()
    FlatAnimatedLafChange.hideSnapshotWithAnimation()
}