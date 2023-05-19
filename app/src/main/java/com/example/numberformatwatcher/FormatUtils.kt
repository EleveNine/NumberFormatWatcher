package com.example.numberformatwatcher


const val DIGITS_ONLY_REGEX = "^\\d*\$"

/**
 * Регулярка для проверки соответствия паттерну ###,##0.##
 *
 * [0-9\s]{1,12} - наличие от 1 до 12 цифр или пробелов
 * [.,]? - опциональное наличие точки либо запятой
 * [0-9]{0,2} - опциональное наличие до 2 цифр после запятой или точки
 */
val formattedNumberRegex: Regex = "^[0-9\\s]{1,12}[.,]?[0-9]{0,2}$".toRegex()


/**
 * Возвращает Int значение для данной строки. Если значение в строке полностью числовое и валидное,
 * но выходит за границы Int или значение больше чем [maxValue], то возвращаем [maxValue]. Если
 * значение неверное, то возвращаем 0.
 */
fun String.toIntWithinBounds(minValue: Int, maxValue: Int): Int =
    if (this.matches(DIGITS_ONLY_REGEX.toRegex())) {
        this.toIntOrNull() ?: Int.MAX_VALUE
    } else {
        0
    }.coerceAtMost(maxValue)
        .coerceAtLeast(minValue)

/**
 * Возвращает Double значение для данной строки. Если значение в строке полностью числовое и валидное,
 * но выходит за границы Double или значение больше чем [maxValue], то возвращаем [maxValue]. Если
 * значение неверное, то возвращаем 0.
 */
fun String.toDoubleWithinBounds(minValue: Double, maxValue: Double): Double =
    if (this.matches(formattedNumberRegex)) {
        this.toDoubleOrNull() ?: Double.MAX_VALUE
    } else {
        0.0
    }.coerceAtMost(maxValue)
        .coerceAtLeast(minValue)