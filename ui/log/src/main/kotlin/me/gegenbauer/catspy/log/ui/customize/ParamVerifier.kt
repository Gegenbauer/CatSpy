package me.gegenbauer.catspy.log.ui.customize

import com.alexandriasoftware.swing.JInputValidator
import com.alexandriasoftware.swing.JInputValidatorPreferences
import com.alexandriasoftware.swing.Validation
import me.gegenbauer.catspy.file.FILE_NAME_INVALID_CHARS
import me.gegenbauer.catspy.file.isValidFileName
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import javax.swing.JComponent
import javax.swing.JTextField

fun interface ParamVerifier {
    fun verify(input: JComponent): Result

    sealed class Result(val isValid: Boolean, val warning: String = EMPTY_STRING) {
        object Valid : Result(true)
        class Invalid(warning: String) : Result(false, warning)
    }

    companion object {
        val default = ParamVerifier { Result.Valid }
    }
}

class IntVerifier(private val min: Int, private val max: Int) : ParamVerifier {
    private val invalidInputWarning = STRINGS.toolTip.intVerifierWarning.get(min, max)

    override fun verify(input: JComponent): ParamVerifier.Result {
        val text = (input as JTextField).text
        val number = text.toIntOrNull()
        val isValid = number != null && number in min until max
        return if (isValid) {
            ParamVerifier.Result.Valid
        } else {
            ParamVerifier.Result.Invalid(invalidInputWarning)
        }
    }
}

class DoubleVerifier(private val min: Double, private val max: Double) : ParamVerifier {
    private val invalidInputWarning = STRINGS.toolTip.doubleVerifierWarning.get(min, max)

    override fun verify(input: JComponent): ParamVerifier.Result {
        val text = (input as JTextField).text
        val number = text.toDoubleOrNull()
        val isValid = number != null && number >= min && number <= max
        return if (isValid) {
            ParamVerifier.Result.Valid
        } else {
            ParamVerifier.Result.Invalid(invalidInputWarning)
        }
    }
}

class CharVerifier : ParamVerifier {
    private val invalidInputWarning = STRINGS.toolTip.charVerifierWarning

    override fun verify(input: JComponent): ParamVerifier.Result {
        val text = (input as JTextField).text
        return if (text.length <= 1) {
            ParamVerifier.Result.Valid
        } else {
            ParamVerifier.Result.Invalid(invalidInputWarning)
        }
    }
}

open class NameVerifier : ParamVerifier {

    override fun verify(input: JComponent): ParamVerifier.Result {
        input as JTextField
        val logType = input.text
        return if (logType.isBlank()) {
            ParamVerifier.Result.Invalid(STRINGS.toolTip.contentBlankWarning)
        } else if (!isValidFileName(logType)) {
            ParamVerifier.Result.Invalid(STRINGS.toolTip.nameInvalidWarning.get(FILE_NAME_INVALID_CHARS))
        } else {
            ParamVerifier.Result.Valid
        }
    }
}

fun JComponent.setJIInputVerifier(verifier: ParamVerifier) {
    setInputVerifier(object : JInputValidator(this, true, true) {
        override fun getValidation(p0: JComponent, p1: JInputValidatorPreferences?): Validation {
            val result = verifier.verify(p0)
            return if (result.isValid) {
                noneValidation
            } else {
                Validation(Validation.Type.DANGER, result.warning)
            }
        }
    })
}

fun JComponent.setTableInputVerifier(
    verifier: ParamVerifier,
) {
    setInputVerifier(object : TableCellValidator(this) {
        override fun getValidation(p0: JComponent, p1: JInputValidatorPreferences?): Validation {
            val result = verifier.verify(p0)
            return if (result.isValid) {
                Validation(Validation.Type.SUCCESS, EMPTY_STRING)
            } else {
                Validation(Validation.Type.DANGER, result.warning)
            }
        }
    })
}