package me.gegenbauer.catspy.common.ui.table

interface RowNavigation {
    fun moveToNextRow()

    fun moveToPreviousRow()

    fun moveToFirstRow()

    fun moveToLastRow()

    fun moveToRow(row: Int)

    fun moveToNextSeveralRows()

    fun moveToPreviousSeveralRows()
}