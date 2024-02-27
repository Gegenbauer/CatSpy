package me.gegenbauer.catspy.configuration

import me.gegenbauer.catspy.strings.STRINGS
import java.util.*
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.ExpandVetoException
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class SettingsTree : JTree() {

    fun init(groupPanel: JPanel, groups: List<ISettingsGroup>, selectedGroupIndex: Int) {
        val treeRoot = DefaultMutableTreeNode(STRINGS.ui.preferences)
        addGroups(treeRoot, groups)
        model = DefaultTreeModel(treeRoot)
        selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        isFocusable = false
        addTreeSelectionListener { switchGroup(groupPanel) }
        // expand all nodes and disallow collapsing
        setNodeExpandedState(this, treeRoot, true)
        addTreeWillExpandListener(DisableRootCollapseListener(treeRoot))
        addSelectionRow(selectedGroupIndex)
    }

    private fun addGroups(base: DefaultMutableTreeNode, groups: List<ISettingsGroup>) {
        for (group in groups) {
            val node = SettingsTreeNode(group)
            base.add(node)
            addGroups(node, group.subGroups)
        }
    }

    private fun switchGroup(groupPanel: JPanel) {
        val selected = lastSelectedPathComponent
        groupPanel.removeAll()
        if (selected is SettingsTreeNode) {
            groupPanel.add(selected.group.buildComponent())
        }
        groupPanel.updateUI()
    }

    companion object {
        private fun setNodeExpandedState(tree: JTree, node: TreeNode, expanded: Boolean) {
            val list = Collections.list(node.children())
            for (treeNode in list) {
                setNodeExpandedState(tree, treeNode, expanded)
            }
            val mutableTreeNode = node as DefaultMutableTreeNode
            if (!expanded && mutableTreeNode.isRoot) {
                return
            }
            val path = TreePath(mutableTreeNode.path)
            if (expanded) {
                tree.expandPath(path)
            } else {
                tree.collapsePath(path)
            }
        }
    }

    private class DisableRootCollapseListener(private val treeRoot: DefaultMutableTreeNode) : TreeWillExpandListener {
        override fun treeWillExpand(event: TreeExpansionEvent) {
            // no-op
        }

        @Throws(ExpandVetoException::class)
        override fun treeWillCollapse(event: TreeExpansionEvent) {
            val current = event.path.lastPathComponent
            if (Objects.equals(current, treeRoot)) {
                throw ExpandVetoException(event, "Root collapsing not allowed")
            }
        }
    }
}