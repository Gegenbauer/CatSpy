package me.gegenbauer.catspy.view.button

interface StatefulActionComponent {

    var buttonDisplayMode: ButtonDisplayMode?

    fun setDisplayMode(mode: ButtonDisplayMode?)
}