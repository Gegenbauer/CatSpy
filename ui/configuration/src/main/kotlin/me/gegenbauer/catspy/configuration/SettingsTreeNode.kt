package me.gegenbauer.catspy.configuration

import javax.swing.tree.DefaultMutableTreeNode

class SettingsTreeNode(val group: ISettingsGroup) : DefaultMutableTreeNode() {

    override fun toString(): String {
        return group.title
    }
}