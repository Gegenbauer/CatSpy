package me.gegenbauer.catspy

import java.awt.Dimension
import java.io.File
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

fun main() {
    val frame = JFrame("File Browser")
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

    frame.add(JScrollPane(fileTree))
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.size = Dimension(320, 240)
    frame.isVisible = true
}

fun createChildren(file: File, node: DefaultMutableTreeNode) {
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