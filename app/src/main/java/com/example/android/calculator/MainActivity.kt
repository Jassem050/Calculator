package com.example.android.calculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.android.calculator.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import net.objecthunter.exp4j.ExpressionBuilder
import java.lang.IllegalArgumentException
import java.text.DecimalFormat
import java.util.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXPRESSION_KEY = "expression"
        const val OPERAND_KEY = "operand"
        const val NUMS_KEY = "nums"
        const val RESULT_KEY = "result"
    }

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var expressionBuffer: StringBuffer
    private lateinit var operandBuffer: StringBuffer
    private var calcResult = 0.0
    // list for adding valid operands
    private val numList = Stack<Double>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        val view = mainBinding.root
        setContentView(view)

        expressionBuffer = StringBuffer(1000)
        operandBuffer = StringBuffer(100)

        setUpClickListeners()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXPRESSION_KEY, expressionBuffer.toString())
        outState.putString(OPERAND_KEY, operandBuffer.toString())
        outState.putString(RESULT_KEY, mainBinding.resultEditor.text.toString())
        outState.putDoubleArray(NUMS_KEY, numList.toDoubleArray())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        expressionBuffer.append(savedInstanceState.getString(EXPRESSION_KEY))
        operandBuffer.append(savedInstanceState.getString(OPERAND_KEY))
        displayExpression()
        mainBinding.resultEditor.text = savedInstanceState.getString(RESULT_KEY)
        savedInstanceState.getDoubleArray(NUMS_KEY)?.map {
            numList.add(it)
        }
    }
    private fun setUpClickListeners() {
        mainBinding.let {
            button_one.setOnClickListener { input(getString(R.string.btn_one)) }
            button_two.setOnClickListener { input(getString(R.string.btn_two)) }
            button_three.setOnClickListener { input(getString(R.string.btn_three)) }
            button_four.setOnClickListener { input(getString(R.string.btn_four)) }
            button_five.setOnClickListener { input(getString(R.string.btn_five)) }
            button_six.setOnClickListener { input(getString(R.string.btn_six)) }
            button_seven.setOnClickListener { input(getString(R.string.btn_seven)) }
            button_eight.setOnClickListener { input(getString(R.string.btn_eight)) }
            button_nine.setOnClickListener { input(getString(R.string.btn_nine)) }
            button_zero.setOnClickListener { input(getString(R.string.btn_zero)) }
            button_zeros.setOnClickListener { input(getString(R.string.btn_zeros)) }
            button_dot.setOnClickListener { input(getString(R.string.btn_dot)) }
            button_divide.setOnClickListener { input(getString(R.string.btn_divide)) }
            button_multiply.setOnClickListener { input(getString(R.string.btn_multiply)) }
            button_minus.setOnClickListener { input(getString(R.string.btn_minus)) }
            button_plus.setOnClickListener { input(getString(R.string.btn_plus)) }
            button_equal.setOnClickListener { input(getString(R.string.btn_equal)) }
            button_delete.setOnClickListener { input(getString(R.string.btn_delete)) }
            button_c.setOnClickListener {
                reset()
                displayExpression()
            }
            button_ce.setOnClickListener {
                deleteLastEntry()
                displayExpression()
            }
        }
    }


    /**
    *  Performs the input based on the given conditions
    *
    * */
    private fun input(value: String) {
        val lastIndexValue = if (expressionBuffer.isNotEmpty())
            expressionBuffer.last().toString()
        else ""
        when {
            isOperand(value) -> { inputOperand(value) }
            isOperator(value) -> { inputOperator(value, lastIndexValue) }
            isDot(value) -> { inputDot(value) }
            isEqualOperator(value) -> { inputEqualOperator(lastIndexValue) }
            isDelete(value) -> { performDeletion() }
        }
        Log.d(TAG, "inputOperand: opBuff: $operandBuffer")
    }


    private fun performDeletion() {
        delete()
        try {
            if (expressionBuffer.isNotEmpty() && numList.size > 1) {
                val isOp = isOperator(expressionBuffer.last().toString())
                displayExpressionResult(
                    calculate(
                        expressionBuffer,
                        isLastCharOperator = isOperator(expressionBuffer.last().toString())
                    )
                )
            } else mainBinding.resultEditor.text = ""
        } catch (ex: IllegalArgumentException) {}
        displayExpression()
    }

    private fun inputEqualOperator(lastIndexValue: String) {
        if (expressionBuffer.isNotEmpty() && !isOperator(lastIndexValue)) {
            displayFinalResult(calculate(expressionBuffer, isLastCharOperator = false))
            numList.push(calcResult)
        }
    }

    private fun inputDot(value: String) {
        if (operandBuffer.isEmpty()) {
            appendExpression("0$value")
            displayExpression()
            appendOperand("0$value")
            numList.push(0.0)
        } else if (operandBuffer.isNotEmpty() && !operandBuffer.toString().contains('.')) {
            appendExpression(value)
            displayExpression()
            appendOperand(value)
            numList.pop()
            numList.push(operandBuffer.toString().toDouble())
        }
    }

    private fun inputOperator(value: String, lastIndexValue: String) {
        if (value == "-" && (expressionBuffer.isEmpty() || expressionBuffer.toString() == "0")) {
            appendExpression(value)
            appendOperand(value)
            displayExpression()
        } else if (value == "-" && lastIndexValue == getString(R.string.btn_divide)) {
            appendExpression(value)
            appendOperand(value)
            displayExpression()
        } else if (expressionBuffer.isNotEmpty() && !isOperator(lastIndexValue)) {
            appendExpression(value)
            displayExpression()
            operandBuffer.delete(0, operandBuffer.length)
        }
    }

    private fun inputOperand(value: String) {
        appendExpression(value)
        if (numList.isNotEmpty() && operandBuffer.isNotEmpty() && operandBuffer.toString() != "-") {
            numList.pop()
        }

        appendOperand(value)
        numList.push(operandBuffer.toString().toDouble())
        if (expressionBuffer.isNotEmpty() && numList.size > 1) {
            displayExpressionResult(calculate(expressionBuffer, isLastCharOperator = false))
        }
        displayExpression()
    }

    /**
     *  Evaluates the expression from the stringBuffer
     *  @param value - expression stringBuffer
     *  @param isLastCharOperator - (true - to remove last value if operator) or false
     *  @return - result Double value
     */
    private fun calculate(value: StringBuffer, isLastCharOperator: Boolean): Double {
        val expression = if (!isLastCharOperator) {
            value
                .replace(Regex.fromLiteral(getString(R.string.btn_divide)), "/")
                .replace(Regex.fromLiteral(getString(R.string.btn_multiply)), "*")
        } else {
            value
                .replace(Regex.fromLiteral(getString(R.string.btn_divide)), "/")
                .replace(Regex.fromLiteral(getString(R.string.btn_multiply)), "*")
                .removeSuffix(when(expressionBuffer.last().toString()) {
                    getString(R.string.btn_multiply) -> "*"
                    getString(R.string.btn_divide) -> "/"
                    else -> expressionBuffer.last().toString()
                })
        }

        calcResult = ExpressionBuilder(expression).build().evaluate()
        return calcResult
    }

    private fun appendExpression(value: String) {
        if (expressionBuffer.length == 1 && expressionBuffer.toString() == "0" && !isOperator(value)) {
            expressionBuffer.delete(0, expressionBuffer.length)
            if (value == "00") expressionBuffer.append("0")
            else
                expressionBuffer.append(value)
        } else if (expressionBuffer.isEmpty() && value =="00") {
            expressionBuffer.append("0")
        }  else expressionBuffer.append(value)
    }

    /**
     *  Adds valid operands to operandBuffer
     *  appends the last operand in an expression
     */
    private fun appendOperand(value: String) {
        if (operandBuffer.isEmpty() && value == "00") {
            operandBuffer.append("0")
        } else if (operandBuffer.toString() == "0") {
            operandBuffer.delete(0, operandBuffer.length)
            if (value == "00" || value == "0") operandBuffer.append("0") else operandBuffer.append(value)
        } else
            operandBuffer.append(value)
    }

    private fun displayExpression() {
        mainBinding.let {
            expression_editor.text =
                if (expressionBuffer.isNotEmpty()) expressionBuffer.toString() else ""
        }
    }

    /**
     *  method invoked when = button is clicked
     *  @param value - result after calculation
     *
     *  resets all values before applying the expression
     */
    private fun displayFinalResult(value: Double) {
        reset()
        appendExpression(formatNumber(value))
        appendOperand(formatNumber(value))
        displayExpression()
    }

    /**
    *  Displays the result while appending to expression stringBuffer
    *  @param value - should be the result after calculation
    * */
    private fun displayExpressionResult(value: Double) {
        mainBinding.resultEditor.text = formatNumber(value)
    }

    /**
     *  Deletes a char from expression stringBuffer
     *  when button_delete is clicked,
     *  Also, element is popped from numList stack
     *  when the operand is removed from operand stringBuffer
     */
    private fun delete() {
        if (expressionBuffer.isNotEmpty()) {
            expressionBuffer.deleteCharAt(expressionBuffer.lastIndex)

            if (expressionBuffer.isNotEmpty() && isOperator(expressionBuffer.last().toString())) {
                operandBuffer.delete(0, operandBuffer.length)

                if (!isOperator(expressionBuffer[expressionBuffer.length - 2].toString()))
                    numList.pop()
            } else if (operandBuffer.isNotEmpty()) {
                operandBuffer.deleteCharAt(operandBuffer.lastIndex)
                if (numList.isNotEmpty()) numList.pop()
                if (operandBuffer.isNotEmpty()) {
                    if (operandBuffer.toString() == ".") numList.push(0.0)
                    else
                        numList.push(operandBuffer.toString().toDouble())
                }
            } else if (operandBuffer.isEmpty() && numList.isNotEmpty()) {
                appendOperand(formatNumber(numList.last()))
            }

            if (expressionBuffer.isEmpty()) {
                reset()
            }

        }
    }

    /**
    *  Deletes the last operand in an expression
    *
    * */
    private fun deleteLastEntry() {
        if (expressionBuffer.isNotEmpty() && operandBuffer.isNotEmpty()) {
            val size = operandBuffer.length
            for (i in 1..size) {
                Log.d(TAG, "deleteLastEntry: expBuff: ${expressionBuffer.last()}")
                expressionBuffer.deleteCharAt(expressionBuffer.lastIndex)
            }
            operandBuffer.delete(0, operandBuffer.length)
            if (numList.isNotEmpty())
                numList.pop()
        }
        try {
            if (expressionBuffer.isNotEmpty() && numList.size > 1) {
                displayExpressionResult(
                    calculate(
                        expressionBuffer,
                        isLastCharOperator = isOperator(expressionBuffer.last().toString())
                    )
                )
            } else mainBinding.resultEditor.text = ""
        } catch (ex: IllegalArgumentException) {}
    }

    private fun reset() {
        expressionBuffer.delete(0, expressionBuffer.length)
        operandBuffer.delete(0, operandBuffer.length)
        mainBinding.resultEditor.text = ""
        numList.clear()
    }

    private fun formatNumber(value: Double): String {
        val decimalFormat = DecimalFormat("0.#########")
        return decimalFormat.format(value)
    }

    private fun isOperand(value: String): Boolean {
        return when(value) {
            getString(R.string.btn_one), getString(R.string.btn_two),
            getString(R.string.btn_three), getString(R.string.btn_four),
            getString(R.string.btn_five), getString(R.string.btn_six),
            getString(R.string.btn_seven), getString(R.string.btn_eight),
            getString(R.string.btn_nine), getString(R.string.btn_zero),
            getString(R.string.btn_zeros) -> true
            else -> false
        }
    }

    private fun isOperator(value: String): Boolean {
        return (value == getString(R.string.btn_plus) || value == getString(R.string.btn_minus) ||
                value == getString(R.string.btn_multiply) || value == getString(R.string.btn_divide))
    }

    private fun isEqualOperator(value: String): Boolean {
        return value == getString(R.string.btn_equal)
    }

    private fun isDot(value: String): Boolean {
        return value == getString(R.string.btn_dot)
    }

    private fun isDelete(value: String): Boolean {
        return value == getString(R.string.btn_delete)
    }

}