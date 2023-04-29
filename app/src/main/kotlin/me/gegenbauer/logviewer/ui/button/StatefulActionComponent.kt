package me.gegenbauer.logviewer.ui.button

interface StatefulActionComponent {

    var buttonDisplayMode: ButtonDisplayMode?

    fun setDisplayMode(mode: ButtonDisplayMode?)
}