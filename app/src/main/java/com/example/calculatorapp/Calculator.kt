package com.example.calculatorcomposeapp.domain

import java.util.*
import kotlin.math.*

object Calculator {
    //https://github.com/btoktogaziev/CalculatorApp/blob/error-fixing/app/src/main/java/com/example/calculatorapp/Calculator.kt
    //задаёт приоритеты операций
    private val operatorPriority = mapOf(
        "+" to 1,
        "-" to 1,
        "*" to 2,
        "/" to 2,
        "^" to 3,
        "" to 4,
        "-u" to 4,
        "!" to 5,
        "%" to 5,
        "(" to 0 //( приоритет 0, чтобы операторы внутри скобок обрабатывались первыми
    )

    //унарные операторы, которые работают только с одним операндом
    private val unaryOperators = setOf("!", "sqrt", "%", "-u")

    //основной метод, принимающий обычное инфиксное выражение и возвращает результат в Double
    fun calculateInfixWithTwoStacks(input: String): Double {
        //обработка выражений на замену спец. символов на обычные
        val expression = input
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", PI.toString())

        //строка разбивается на лист символов(операнды, операторы, скобки) с помощью метода toSymbol(выражение)
        val symbols = toSymbol(expression)
        //обработка символов в стэках
        val values = Stack<Double>()//стэк чисел
        val operators = Stack<String>()//стэк операторов
        //обрабатывает каждый символ в листе символов
        for (symbol in symbols) {
            when {
                //если число, добавление в стэк чисел
                symbol.toDoubleOrNull() != null -> {
                    val num = symbol.toDouble()
                    if (operators.isNotEmpty() && operators.peek() == "-u") {
                        operators.pop()
                        values.push(-num)
                    } else {
                        values.push(num)
                    }
                }
                //если унарный минус(минус перед отрицательным числом), то добавление в стэк операторов
                symbol == "-u" -> operators.push(symbol)
                //если (, то добавление в стэк операторов
                symbol == "(" -> operators.push(symbol)
                //если ), то ...
                symbol == ")" -> {
                    //если на вершине стэка не (, то выполняются все операции
                    while (operators.isNotEmpty() && operators.peek() != "(") {
                        //выполняет операцию с операндом, взятых из стэка
                        applyOperations(operators.pop(), values)
                    }
                    //если на вершине стэка (, то удаляется последний оператор
                    if (operators.isNotEmpty() && operators.peek() == "(") {
                        operators.pop()
                    }
                    //если на вершине стэка операторов кв.корень, то он применяется
                    if (operators.isNotEmpty() && operators.peek() == "sqrt") {
                        applyOperations(operators.pop(), values)
                    }
                }
                //если унарные операторы из списка, то добавление в стэк операторов
                symbol in unaryOperators -> operators.push(symbol)

                // если остальные оператора(бинарные), то
                else -> {
                    // пока на вершине есть оператор с приоритетом больше или равным текущему, выполняются операции из стэка
                    while (operators.isNotEmpty() && operatorPriority.containsKey(operators.peek()) && operatorPriority[operators.peek()]!! >= operatorPriority[symbol]!!) {
                        applyOperations(operators.pop(), values)
                    }
                    // после текущий оператор добавляется в стэк
                    operators.push(symbol)
                }
            }
        }
        // после обработки всех символов, выполняются оставшиеся операции из стэка, пока он не станет пустым
        while (operators.isNotEmpty()) {
            applyOperations(operators.pop(), values)
        }
        //возвращает результат вычислений, последний из стэка чисел
        return values.pop()
    }

    //метод,разбивающий выражение на лист символов(операнды, операторы, скобки)
    private fun toSymbol(expr: String): List<String> {
        //переменная хранит изменяемый лист стрингов
        val chars = mutableListOf<String>()
        //индекс цикла
        var i = 0
        //цикл проходит пока значение индекс будет меньше длины выражения
        while (i < expr.length) {
            // проходит по каждому элементу строки по индексу
            when (val c = expr[i]) {
                //если символ цифра или точка
                in '0'..'9', '.' -> {
                    // переменная, хранит начало числа
                    val start = i
                    // идёт цикл, где сохраняется количество символов в числе
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                    //добавляется в лист как число от нач до конца
                    chars.add(expr.substring(start, i))
                }
                //если символ Пи,то добавляется значение PI.
                'π' -> {
                    chars.add(PI.toString())
                    i++
                }
                // если символ кв.корень, то в лист добавляется как оператор sqrt
                '√' -> {
                    chars.add("sqrt")
                    i++
                    // проверка на ( или число
                    if (i >= expr.length || (expr[i] != '(' && !expr[i].isDigit())) {
                        throw IllegalArgumentException("Invalid expression after sqrt")
                    }
                }
                // если символ один из этих операторов, то
                '+', '-', '*', '/', '^', '!', '%', '(', ')' -> {
                    val listForCheck = listOf(
                        "(",
                        "+",
                        "-",
                        "*",
                        "/",
                        "^"
                    )
                    // проверяется не является ли минус унарным, в начале выражения или другого оператора
                    if (c == '-' && (chars.isEmpty() || chars.last() in listForCheck)
                    ) {
                        // добавляется символ унарного оператора
                        chars.add("-u")
                        i++
                    } else if (c == '+' && (chars.isEmpty() || chars.last() in listForCheck)) {
                        i++
                    } else {
                        // добавляется сам оператор
                        chars.add(c.toString())
                        i++
                    }
                }
                // неизвестные символы
                else -> throw IllegalArgumentException("Unknown symbol: $c")
            }
        }

        return chars
    }

    //метод выполняет операцию, взятую из стэка операции, над числами из стэка чисел
    private fun applyOperations(op: String, values: Stack<Double>) {
        //когда оператор
        when (op) {
            // если унарный минус,
            "-u" -> {
                if (values.isEmpty()) throw IllegalArgumentException("Invalid expression: no operand for unary minus")
                // то знак верхнего числа меняется
                values.push(-values.pop())
            }
            //если факториал
            "!" -> {
                // то создаёт переменную, хранящую последнее число из стэка чисел
                val n = values.pop()
                // провекра на целое и неотрицательное число
                if (n < 0 || n % 1 != 0.0) throw IllegalArgumentException("Invalid factorial")
                // если всё ок, то вычисляет факториал и возвращает в стэк чисел
                values.push((1..n.toInt()).fold(1L) { acc, i -> acc * i }.toDouble())
            }
            // если кв. корень
            "sqrt" -> {
                if (values.isEmpty()) throw IllegalArgumentException("Invalid expression: no operand for sqrt")
                // то создаёт переменную, хранящую последнее число из стэка чисел
                val value = values.pop()
                // проверяет на неотрицательное число
                if (value < 0) throw IllegalArgumentException("Square root of negative number")
                // если всё ок, то вычисляет кв. корень значение и возвращает в стэк значений
                values.push(sqrt(value))
            }
            // если процент, то просто берёт последнее значение, делит его на 100 и возвращает в стэк
            "%" -> values.push(values.pop() / 100)
            // если плюс, минус, умножение, то метод-расширение popTwo просто берёт два последних значения из стэка значений и выполняет операцию в лямбде let
            "+" -> values.push(values.popTwo().let { it.first + it.second })
            "-" -> values.push(values.popTwo().let { it.first - it.second })
            "*" -> values.push(values.popTwo().let { it.first * it.second })
            // если деление, в целом тоже самое, но проверка второго значения на равенство нулю
            "/" -> values.popTwo().let {
                if (it.second == 0.0) throw ArithmeticException("Division by zero")
                values.push(it.first / it.second)
            }
            // если степень, тоже самое что и плюс, минус, и умножение
            "^" -> values.push(values.popTwo().let { it.first.pow(it.second) })
            else -> throw IllegalArgumentException("Unknown operator: $op")
        }
    }

    // метод расширение, извлекает верхние два значения из стэка и возвращает их как пару (а, б)
    private fun Stack<Double>.popTwo(): Pair<Double, Double> {
        val b = pop()
        val a = pop()
        return a to b
    }
}