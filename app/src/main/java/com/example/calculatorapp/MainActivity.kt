package com.example.calculatorapp

import android.os.Bundle
import android.widget.HorizontalScrollView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.calculatorapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    // переменная, сохраняющая текущее выражение
    private var expression = ""

    // счётчик, хранящий открытые
    private var openBrackets = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        setupButtons()
        binding.textViewResult.text = "0"
    }

    private fun setupButtons() {
        // определяет список кнопок и соответствующие им значения
        with(binding) {
            val buttons = listOf(
                btnNumZero to getString(R.string.zero),
                btnNumOne to getString(R.string.one),
                btnNumTwo to getString(R.string.two),
                btnNumThree to getString(R.string.three),
                btnNumFour to getString(R.string.four),
                btnNumFive to getString(R.string.five),
                btnNumSix to getString(R.string.six),
                btnNumSeven to getString(R.string.seven),
                btnNumEight to getString(R.string.eight),
                btnNumNine to getString(R.string.nine),
                btnNumDot to getString(R.string.dot),
                btnPlus to getString(R.string.plus),
                btnMinus to getString(R.string.minus),
                btnMultiply to getString(R.string.multiplication),
                btnDivide to getString(R.string.division),
                btnExponent to getString(R.string.exponent),
                btnPercent to getString(R.string.percentage),
                btnPiNumber to getString(R.string.pi_number),
                btnSquareRoot to getString(R.string.square_root),
                btnFactorial to getString(R.string.factorial),
                btnBrackets to getString(R.string.brackets),
                btnNumDelete to getString(R.string.delete),
                btnClear to getString(R.string.all_clear),
                btnEquality to getString(R.string.equality)
            )
            buttons.forEach { (button, value) ->
                button.setOnClickListener { handleInput(value) }
            }
        }
    }

    //метод обрабатывает ввод в зависимости от нажатой кнопки
    private fun handleInput(value: String) {
        val basicInputValues = listOf(
            getString(R.string.dot),
            getString(R.string.plus),
            getString(R.string.minus),
            getString(R.string.multiplication),
            getString(R.string.division),
            getString(R.string.exponent),
            getString(R.string.percentage),
            getString(R.string.factorial),
            getString(R.string.pi_number)
        )
        when (value) {
            in getString(R.string.zero)..getString(R.string.nine), in basicInputValues -> {
                //проверяется, можно ли добавить точку метод(canAddDot), чтобы избежать двух точек в одном числе
                if (value == getString(R.string.dot) && !canAddDot()) return //возвращает в начало функции и не даёт второй раз нажать кнопку
                //если перед Pi стоит цифра или ), добавляется умножение , чтобы избежать неявных ошибок.
                if (value == getString(R.string.pi_number) && expression.isNotEmpty() && expression.last()
                        .isDigitOrClosingBracket()
                ) {
                    expression += R.string.multiplication
                }
                expression += value
            }

            //если перед √ стоит цифра или ), добавляется умножение.
            getString(R.string.square_root) -> {
                if (expression.isNotEmpty() && expression.last().isDigitOrClosingBracket())
                    expression += "×"
                expression += "√("
                //увеличивает openBrackets
                openBrackets++
            }

            getString(R.string.brackets) -> {
                //если shouldAddOpeningBracket возвращает true
                if (shouldAddOpeningBracket()) {
                    //добавляет (
                    expression += "("
                    //увеличивает openBrackets
                    openBrackets++
                } else if (openBrackets > 0) {
                    //добавляет )
                    expression += ")"
                    //уменьшает openBrackets
                    openBrackets--
                }
            }

            getString(R.string.delete) -> {
                //если удаляется √(
                if (expression.endsWith("√(")) {
                    // удаляются оба символа
                    expression = expression.dropLast(2)
                    //openBrackets уменьшается
                    openBrackets--
                } else if (expression.isNotEmpty()) {
                    val lastChar = expression.last()
                    // если удаляется ( или )
                    expression = expression.dropLast(1)
                    //обновляется openBrackets
                    if (lastChar == '(') openBrackets--
                    if (lastChar == ')') openBrackets++
                }
            }

            getString(R.string.all_clear) -> {
                //cбрасывает expression и openBrackets, устанавливает textViewResult в "0".
                expression = ""
                openBrackets = 0
                binding.textViewResult.text = "0"
                return
            }

            getString(R.string.equality) -> {
                try {
                    //добавляет закрывающие скобки для всех ).
                    val finalExpr = expression + ")".repeat(openBrackets)
                    //вызывает Calculator.calculateInfixWithTwoStacks для вычисления результата
                    val result = Calculator.calculateInfixWithTwoStacks(finalExpr)
                    expression =
                            //если результат — целое число, преобразует его в строку без дробной части.
                        if (result % 1 == 0.0) result.toLong().toString() else result.toString()
                            .trimEnd('0').trimEnd('.')
                    binding.textViewResult.text = expression
                    openBrackets = 0
                } catch (e: Exception) {
                    //если возникает ошибка, отображается текст ошибки.
                    binding.textViewResult.text = e.message
                    expression = ""
                    openBrackets = 0
                }
                return
            }
        }
        //после обработки ввода обновляется textViewResult, и scrollView
        binding.textViewResult.text = expression.ifEmpty { "0" }
        binding.scrollView.post {
            binding.scrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
        }
    }

    //проверяет, можно ли добавить точку.
    private fun canAddDot(): Boolean {
        val lastChar = expression.takeLastWhile { it.isDigit() || it == '.' }
        return '.' !in lastChar
    }

    //определяет, нужно ли добавить (
    private fun shouldAddOpeningBracket(): Boolean {
        return expression.isEmpty()
                || expression.last() in "+-${getString(R.string.multiplication)}${getString(R.string.division)}^("
                || expression.endsWith("√(")
    }

    // проверяет, является ли символ цифрой или закрывающей скобкой.
    private fun Char.isDigitOrClosingBracket(): Boolean {
        return isDigit()
    }
}
