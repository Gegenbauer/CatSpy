package me.gegenbauer.catspy.demo

import me.gegenbauer.catspy.demo.base.BaseComponentDemo
import me.gegenbauer.catspy.demo.base.DemoExecutor
import java.awt.BorderLayout
import java.io.File
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

fun main() {
    DemoExecutor.show(FileTreeDemo())
}

class FileTreeDemo: BaseComponentDemo() {
    override val demoName: String
        get() = "File Tree Demo"

    init {
        val fileSystemView = File.listRoots()[0]

        val root = DefaultMutableTreeNode(FileNode(fileSystemView))
        val treeModel = DefaultTreeModel(root)

        val fileTree = JTree(treeModel)
        createChildren(fileSystemView, root)
        fileTree.addTreeWillExpandListener(object: TreeWillExpandListener {

            override fun treeWillExpand(event: TreeExpansionEvent) {
                val node = event.path.lastPathComponent as DefaultMutableTreeNode
                val fileNode = node.userObject as FileNode

                if (node.childCount == 0) {
                    fileNode.file.listFiles()?.forEach {
                        node.add(DefaultMutableTreeNode(FileNode(it)))
                    }
                }
            }

            override fun treeWillCollapse(event: TreeExpansionEvent) {
                TODO("Not yet implemented")
            }
        })

        add(JScrollPane(fileTree), BorderLayout.CENTER)
    }

   private fun createChildren(file: File, node: DefaultMutableTreeNode) {
        val files = file.listFiles()
        if (files != null) {
            for (childFile in files) {
                if (childFile.isDirectory) {
                    val childNode = DefaultMutableTreeNode(FileNode(childFile))
                    node.add(childNode)
                }
            }
        }
    }

    class FileNode(val file: File) {
        override fun toString(): String = file.name
    }
}