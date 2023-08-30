package me.gegenbauer.catspy.view.button

interface StatefulActionComponent {

    var buttonDisplayMode: me.gegenbauer.catspy.view.button.ButtonDisplayMode?

    fun setDisplayMode(mode: me.gegenbauer.catspy.view.button.ButtonDisplayMode?)
}