package com.example.numberformatwatcher

import android.content.res.Configuration
import android.text.Editable
import android.text.Selection
import android.text.TextWatcher
import android.widget.EditText
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

/**
 * Mask for EditText that formats the entered number in the "###,##0.##" format.
 *
 * Ignores symbols that are not related to numeric values.
 *
 * [EditText.setInputType] must be set to "text". If the "number" input type is set, a crash will
 * take place, since "number" input type does not support symbols like ".", " ", etc.
 *
 * If the numeric keyboard is required use [EditText.setRawInputType], setting the
 * [Configuration.KEYBOARD_12KEY] parameter.
 *
 * @param textEnding suffix ending for the input. Can be a currency symbol (e.g., $), or any other
 * string value. Example, "2 300.3 Bonuses".
 *
 * @param isDecimal flag that sets this TextWatcher to accept up to 2 decimal places in a number.
 *
 * @param minAmount minimal Double value that can be entered to the input field.
 *
 * @param maxAmount maximal Double value that can be entered to the input field.
 *
 * @param isDecimalGroupingEnabled flag that enables decimal grouping. (E.g., "100 000" - grouped,
 * "100000" - not grouped).
 *
 * @param fieldCanBeEmpty flag that enables an empty input. If set to false, empty string will
 * always be replaced with "0" + [textEnding].
 *
 * @param usFormatSymbols formatting symbols for grouping and decimal places representation. By
 * default, set to the US locale with " " whitespace being used for the grouping separation and "."
 * for the decimal places separation.
 *
 * @param onTextChangedListener listener that returns the entered value in Double.
 */
class NumberFormatWatcher(
    private val textEnding: String = "",
    private var isDecimal: Boolean = false,
    private var minAmount: Double = 0.0,
    private var maxAmount: Double = Int.MAX_VALUE.toDouble(),
    private val isDecimalGroupingEnabled: Boolean = true,
    private val fieldCanBeEmpty: Boolean = false,
    usFormatSymbols: DecimalFormatSymbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ' '
        decimalSeparator = '.'
    },
    private val onTextChangedListener: (amount: Double) -> Unit
) : TextWatcher {

    private val numberFormatter = DecimalFormat("###,##0.##", usFormatSymbols).apply {
        roundingMode = RoundingMode.DOWN
        isGroupingUsed = isDecimalGroupingEnabled
    }

    // Symbols that are not allowed for the input, i.e. only digits, dots and commas are allowed for
    // the user input.
    private val exclusiveRegex = """[^0-9.,]+""".toRegex()

    private var currentText: String = ""

    /**
     * Flag showing that the changes in input takes places because of calling
     * [setTextWithSelection] method. Necessary to avoid recursive input and self-formatting.
     */
    @Volatile
    private var isSelfChanging = false

    // Suffix which is empty if no textEnding was provided. Otherwise, is made of a whitespace and
    // the suffix.
    private val suffix = if (textEnding.isEmpty()) "" else " $textEnding"

    // Default text value for the empty inputs. If fieldCanBeEmpty == true, an empty string is set
    // to the input. Otherwise, the minimal numeric amount + suffix is set to the input field.
    private val defaultTextValue: String
        get() {
            return if (fieldCanBeEmpty) {
                ""
            } else if (textEnding.isEmpty()) {
                "${minAmount.toInt()}"
            } else {
                "${minAmount.toInt()} $textEnding"
            }
        }

    private val suffixLength = if (textEnding.isEmpty()) 0 else suffix.length

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(
        s: CharSequence,
        start: Int,
        before: Int,
        count: Int
    ) = Unit

    @Synchronized
    override fun afterTextChanged(s: Editable?) {
        val enteredString = s.toString()

        if (enteredString == currentText || isSelfChanging || s == null) {
            return
        }

        // If the input is empty or contains no digits and only the suffix, replace the input with
        // the default text value.
        if (s.isEmpty() || s.toString() == suffix) {
            currentText = defaultTextValue
            s.setTextWithSelection(currentText)
            onTextChangedListener(minAmount)
            return
        }

        // If the input has already the suffix at the end, remove it from the input to have pure
        // numeric value only.
        val pureEnteredValue = if (enteredString.contains(suffix)) {
            enteredString.dropLast(suffixLength)
        } else {
            enteredString
        }

        // If the entered value does not match the formatted number (contains symbols other than
        // digits, spaces, dots, commas or has more than 1 dot or comma), then the input must be
        // ignored and current entered value must be left in the input field.
        // This is useful for the cases when a user tries to paste forbidden symbols.
        if (!pureEnteredValue.matches(formattedNumberRegex)) {
            s.setTextWithSelection(currentText)
            return
        }

        reformatText(s)
    }

    /**
     * Set the max limit for the numeric input.
     */
    fun setMaxAmount(amount: Double) {
        maxAmount = amount
    }

    /**
     * Set the min limit for the numeric input.
     */
    fun setMinAmount(amount: Double) {
        minAmount = amount
    }

    /**
     * Enable/disable decimal inputs.
     */
    fun setIsDecimal(isDecimal: Boolean) {
        this.isDecimal = isDecimal
    }

    private fun reformatText(s: Editable) {
        synchronized(this) {
            isSelfChanging = true

            // necessary to remove the whitespaces from the already formatted inputs
            var cleanString: String = s.replace(exclusiveRegex, "")

            // replacing commas with dots to convert the string to Int/Double
            cleanString = cleanString.replace(',', '.')

            if (isDecimal) {
                val parsedDouble: Double = cleanString.toDoubleWithinBounds(minAmount, maxAmount)
                val formattedNumber = numberFormatter.format(parsedDouble)

                currentText = when {
                    cleanString.endsWith('.') && parsedDouble < maxAmount -> {
                        "$formattedNumber.$suffix"
                    }

                    cleanString.endsWith(".0") -> {
                        "$formattedNumber.0$suffix"
                    }

                    else -> {
                        formattedNumber + suffix
                    }
                }

                s.setTextWithSelection(currentText)
                onTextChangedListener(parsedDouble)
            } else {
                // If the input of a dot and/or decimal fraction value is being attempted, the
                // fraction value and the dot must be cleared. Here only non-fraction inputs are
                // enabled.
                val decimalValue = cleanString.substringAfter('.')
                cleanString = cleanString.replace(".$decimalValue", "")

                val parsedInt: Int = cleanString.toIntWithinBounds(
                    minValue = minAmount.toInt(),
                    maxValue = maxAmount.toInt()
                )
                currentText = numberFormatter.format(parsedInt) + suffix

                s.setTextWithSelection(currentText)
                onTextChangedListener(parsedInt.toDouble())
            }

            isSelfChanging = false
        }
    }

    private fun Editable.setTextWithSelection(text: String) {
        val newEditable = this.replace(
            /* st = */ 0,
            /* en = */ this.length,
            /* text = */ text
        )
        val currentSelectionIndex = Selection.getSelectionEnd(newEditable)

        val newSelectionIndex = if (currentSelectionIndex < newEditable.length) {
            currentSelectionIndex
        } else {
            (newEditable.length - suffixLength).coerceAtLeast(0)
        }
        Selection.setSelection(newEditable, newSelectionIndex)
    }
}