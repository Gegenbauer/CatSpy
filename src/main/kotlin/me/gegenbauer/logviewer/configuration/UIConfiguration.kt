package me.gegenbauer.logviewer.configuration

// TODO 一些窗口属性设置未生效
data class UI(
    var frameX: Int = 0,
    var frameY: Int = 0,
    var frameWidth: Int = 0,
    var frameHeight: Int = 0,
    var frameExtendedState: Int = 0,
    var rotation: Int = 0,
    var dividerLocation: Int = 0,
    var lastDividerLocation: Int = 0,
)