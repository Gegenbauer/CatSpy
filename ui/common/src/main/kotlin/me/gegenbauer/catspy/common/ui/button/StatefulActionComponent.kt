package me.gegenbauer.catspy.common.ui.button

interface StatefulActionComponent {

    var buttonDisplayMode: ButtonDisplayMode?

    fun setDisplayMode(mode: ButtonDisplayMode?)
}