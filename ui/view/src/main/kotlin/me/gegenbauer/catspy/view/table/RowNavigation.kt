package me.gegenbauer.catspy.view.table

interface RowNavigation {
    fun moveToNextRow()

    fun moveToPreviousRow()

    fun moveToFirstRow()

    fun moveToLastRow()

    fun moveToRow(row: Int, setSelected: Boolean)

    fun scrollToEnd()

    fun moveToNextSeveralRows()

    fun moveToPreviousSeveralRows()
}