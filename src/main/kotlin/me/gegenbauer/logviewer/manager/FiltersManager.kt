package me.gegenbauer.logviewer.manager

import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.ui.log.LogPanel
import java.awt.event.*
import javax.swing.JList
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener

class FiltersManager (mainUI: MainUI, logPanel: LogPanel): CustomListManager(mainUI, logPanel){
    private val configManager = ConfigManager.getInstance()

    private val listSelectionHandler = ListSelectionHandler()
    private val mouseHandler = MouseHandler()
    private val keyHandler = KeyHandler()

    init {
        dialogTitle = "Filters Manager"
    }

    override fun loadList(): ArrayList<CustomElement> {
        return configManager.loadFilters()
    }

    override fun saveList(list: ArrayList<CustomElement>) {
        configManager.saveFilters(list)
    }

    override fun getFirstElement(): CustomElement {
        return CustomElement(CURRENT_FILTER, mainUI.getTextShowLogCombo(), false)
    }

    override fun getListSelectionListener(): ListSelectionListener {
        return listSelectionHandler
    }

    override fun getListMouseListener(): MouseListener {
        return mouseHandler
    }

    override fun getListKeyListener(): KeyListener {
        return keyHandler
    }

    internal inner class ListSelectionHandler : ListSelectionListener {
        override fun valueChanged(event: ListSelectionEvent) {
            if (!event.valueIsAdjusting) {
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal inner class MouseHandler: MouseAdapter() {
        override fun mouseClicked(mouseEvent: MouseEvent) {
            super.mouseClicked(mouseEvent)
            if (mouseEvent.clickCount == 2) {
                shouMainUIText(mouseEvent)
            }
        }
    }

    private fun shouMainUIText(event: InputEvent) {
        val list = event.source as JList<CustomElement>
        val selection = list.selectedValue
        if ((InputEvent.CTRL_DOWN_MASK and event.modifiersEx) != 0) {
            val filterText = mainUI.getTextShowLogCombo()
            if (filterText.isEmpty()) {
                mainUI.setTextShowLogCombo(selection.value)
            } else {
                if (filterText.substring(filterText.length - 1) == "|") {
                    mainUI.setTextShowLogCombo(filterText + selection.value)
                }
                else {
                    mainUI.setTextShowLogCombo(filterText + "|" + selection.value)
                }
            }
        }
        else {
            mainUI.setTextShowLogCombo(selection.value)
        }
        mainUI.applyShowLogCombo()
    }

    @Suppress("UNCHECKED_CAST")
    internal inner class KeyHandler: KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            if (event.keyCode == KeyEvent.VK_ENTER) {
                shouMainUIText(event)
            }
        }
    }

    companion object {
        const val MAX_FILTERS = 20
        private const val CURRENT_FILTER = "Current"
    }
}
