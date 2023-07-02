package me.gegenbauer.catspy

import me.gegenbauer.catspy.script.parser.DirectRule
import me.gegenbauer.catspy.script.parser.RegexRule

/**
 * 从下面这个字符串中提取出 com.google.android.apps.nexuslauncher/.NexusLauncherActivity
 *   mLayoutSeq=75
 *   mCurrentFocus=Window{f047612 u0 com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity}
 *   mFocusedApp=ActivityRecord{18f3268 u0 com.google.android.apps.nexuslauncher/.NexusLauncherActivity} t24}
 *
 *   mHoldScreenWindow=null
 */
fun main() {
    val input = """
        mLayoutSeq=75
        mCurrentFocus=Window{f047612 u0 com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity}
        mFocusedApp=ActivityRecord{18f3268 u0 com.google.android.apps.nexuslauncher/.NexusLauncherActivity} t24}
        mHoldScreenWindow=null
    """.trimIndent()
    val rule = RegexRule("mCurrentFocus=Window\\{[0-9a-z]+ [0-9a-z]+ (.*)\\}", DirectRule())
    val result = rule.parse(input)
    println(result)
}