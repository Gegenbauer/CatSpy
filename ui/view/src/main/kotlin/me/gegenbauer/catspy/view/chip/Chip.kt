package me.gegenbauer.catspy.view.chip

import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.utils.ui.getSizeWithPadding
import me.gegenbauer.catspy.utils.ui.horizontalPadding
import me.gegenbauer.catspy.utils.ui.isLeftClick
import me.gegenbauer.catspy.utils.ui.verticalPadding
import me.gegenbauer.catspy.view.border.RoundedBorder
import me.gegenbauer.catspy.view.button.CloseButton
import me.gegenbauer.catspy.view.label.EllipsisLabel
import me.gegenbauer.catspy.view.panel.HorizontalFlexibleHeightLayout
import me.gegenbauer.catspy.view.panel.HoverStateAwarePanel
import java.awt.Color
import java.awt.Dimension
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.UIManager

class Chip(text: String, deleteButtonVisible: Boolean = true) : HoverStateAwarePanel(),
    MouseListener {
    private val label = EllipsisLabel(text, maxWidth = 200)
    private val border = RoundedBorder(20, normalBackgroundColor)
    private val deleteButton = CloseButton()

    private val hoverBackgroundColor: Color
        get() = UIManager.getColor("Chip.hoverBackground") ?: Color.WHITE
    private val normalBackgroundColor: Color
        get() = UIManager.getColor("Chip.background") ?: Color.WHITE
    private val focusBorderColor: Color
        get() = SettingsManager.settings.themeSettings.getAccentColor()

    private val focusListener = object : FocusAdapter() {
        override fun focusGained(e: FocusEvent) {
            border.strokeColor = focusBorderColor
            repaint()
        }

        override fun focusLost(e: FocusEvent) {
            border.strokeColor = null
            repaint()
        }
    }

    private var onFilterChipClickedListener: OnChipClickedListener? = null

    private var _text: String = text
        set(value) {
            field = value
            label.text = value
        }

    init {
        layout = HorizontalFlexibleHeightLayout(2)
        isOpaque = false
        isFocusable = true
        setBorder(border)
        isFocusable = true

        add(label)
        add(deleteButton)
        deleteButton.border = null
        deleteButton.isVisible = deleteButtonVisible
        addMouseListener(this)
        label.addMouseListener(this)
        label.addFocusListener(focusListener)
        addFocusListener(focusListener)
    }

    fun setText(text: String) {
        this._text = text
    }

    fun setTooltip(tooltip: String) {
        val composedTooltip = "$_text: $tooltip"
        label.toolTipText = composedTooltip
        toolTipText = composedTooltip
    }

    fun setOnDeleteClicked(onDeleteClicked: () -> Unit) {
        deleteButton.onClose = onDeleteClicked
    }

    fun setOnChipClickedListener(listener: OnChipClickedListener) {
        onFilterChipClickedListener = listener
    }

    override fun onHoverStateChanged(isHover: Boolean, e: MouseEvent) {
        super.onHoverStateChanged(isHover, e)
        if (isHover) {
            border.fillColor = hoverBackgroundColor
            repaint()
        } else {
            border.fillColor = normalBackgroundColor
            repaint()
        }
    }

    override fun getPreferredSize(): Dimension {
        val labelSize = label.getSizeWithPadding(label.preferredSize)
        val deleteButtonSize = deleteButton.getSizeWithPadding(deleteButton.preferredSize)
        val horizontalGap = if (deleteButton.isVisible) 2 else 0
        return Dimension(
            labelSize.width + deleteButtonSize.width + horizontalPadding() + horizontalGap,
            labelSize.height + verticalPadding()
        )
    }

    override fun getMaximumSize(): Dimension {
        return getPreferredSize()
    }

    override fun getMinimumSize(): Dimension {
        return getPreferredSize()
    }

    override fun updateUI() {
        super.updateUI()
        if (border != null) {
            border.fillColor = normalBackgroundColor
        }
    }

    override fun mouseClicked(e: MouseEvent) {
        if (e.isLeftClick && e.source != deleteButton) {
            onFilterChipClickedListener?.onChipClicked(this)
            requestFocus()
        }
    }

    override fun mousePressed(e: MouseEvent) {
        // no-op
    }

    override fun mouseReleased(e: MouseEvent) {
        // no-op
    }

    override fun mouseEntered(e: MouseEvent) {
        mouseMoved(e)
    }

    override fun mouseExited(e: MouseEvent) {
        mouseMoved(e)
    }
}

fun interface OnChipClickedListener {
    fun onChipClicked(chip: Chip)
}