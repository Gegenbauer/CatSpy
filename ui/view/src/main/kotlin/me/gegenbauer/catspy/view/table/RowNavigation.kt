package me.gegenbauer.catspy.view.table

interface RowNavigation {
    fun moveToNextRow()

    fun moveToPreviousRow()

    fun moveToFirstRow()

    fun moveToLastRow()

    fun moveToRow(row: Int)

    fun moveToNextSeveralRows()

    fun moveToPreviousSeveralRows()
}