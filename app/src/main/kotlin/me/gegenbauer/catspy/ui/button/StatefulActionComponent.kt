package me.gegenbauer.catspy.ui.button

interface StatefulActionComponent {

    var buttonDisplayMode: ButtonDisplayMode?

    fun setDisplayMode(mode: ButtonDisplayMode?)
}