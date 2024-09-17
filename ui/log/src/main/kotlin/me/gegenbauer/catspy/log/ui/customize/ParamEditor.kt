package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.utils.ui.Key
import me.gegenbauer.catspy.utils.ui.keyEventInfo
import me.gegenbauer.catspy.utils.ui.registerStrokeWhenFocused
import me.gegenbauer.catspy.utils.ui.released
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.undo.UndoManager

open class ParamEditor(
    tooltip: String? = null,
    maxCharCount: Int = Int.MAX_VALUE
) : WidthConstrainedTextField(tooltip, maxCharCount), EditEventSource, ParamVerifier, Editor {

    private val editListeners = mutableListOf<EditEventListener>()
    private var inputVerifier: ParamVerifier = ParamVerifier.default
    private val undo = UndoManager()

    init {
        // must set tooltip before setting JInputValidator
        toolTipText = tooltip
        addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                if (e.keyEventInfo == Key.ENTER.released() && isEditable) {
                    commitEdit()
                }
            }
        })
        addFocusListener(object : FocusAdapter() {
            private var lastValue = text

            override fun focusLost(e: FocusEvent) {
                if (isEditable && lastValue != text) {
                    commitEdit()
                }
            }

            override fun focusGained(e: FocusEvent?) {
                lastValue = text
            }
        })
        setJIInputVerifier()
        configureMemo()
        isEditable = false
    }

    private fun configureMemo() {
        document.addUndoableEditListener {
            undo.addEdit(it.edit)
        }
        registerStrokeWhenFocused(Key.C_Z, "Undo") {
            runCatching {
                if (undo.canUndo()) {
                    undo.undo()
                }
            }
        }
        registerStrokeWhenFocused(Key.C_Y, "Redo") {
            runCatching {
                if (undo.canRedo()) {
                    undo.redo()
                }
            }
        }
    }

    protected open fun setJIInputVerifier() {
        setJIInputVerifier(this)
    }

    override fun startEditing() {
        isEditable = true
    }

    override fun stopEditing() {
        isEditable = false
    }

    override fun isEditValid(): Boolean {
        return !isEditable || inputVerifier.verify(this).isValid
    }

    fun setVerifier(verifier: ParamVerifier) {
        inputVerifier = verifier
        reVerify()
    }

    protected fun reVerify() {
        if (isEditable) {
            getInputVerifier().verify(this)
        }
    }

    private fun commitEdit() {
        if (isEditValid()) {
            notifyEditDone()
        }
    }

    private fun notifyEditDone() {
        editListeners.forEach { it.onEditDone(this) }
    }

    override fun addEditEventListener(listener: EditEventListener) {
        editListeners.add(listener)
    }

    override fun verify(input: JComponent): ParamVerifier.Result {
        return if (isEditable) {
            inputVerifier.verify(input)
        } else {
            ParamVerifier.Result.Valid
        }
    }
}