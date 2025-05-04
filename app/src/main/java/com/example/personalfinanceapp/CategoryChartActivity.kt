package com.example.personalfinanceapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.personalfinanceapp.model.Transaction
import com.example.personalfinanceapp.navigation.BottomNavbarNavigation
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.graphics.Typeface


class CategoryChartActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var tvBudgetInChart: TextView
    private lateinit var tvTotalSpendInChart: TextView
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_chart)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        BottomNavbarNavigation.setup(this, bottomNav, R.id.nav_budget)

        pieChart = findViewById(R.id.pieChart)
        tvBudgetInChart = findViewById(R.id.tvBudgetInChart)
        tvTotalSpendInChart = findViewById(R.id.tvTotalSpendInChart)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            val intent = Intent(this, BudgetActivity::class.java)
            startActivity(intent)
            finish()
        }

        val transactions = loadTransactions()
        val categoryTotals = calculateTotals(transactions)

        loadAndShowBudget()
        setupPieChart(categoryTotals)
        showTotalSpend(transactions)
    }

    private fun showTotalSpend(transactions: List<Transaction>) {
        val total = transactions.sumOf { it.amount.toDouble() }
        tvTotalSpendInChart.text = "Total Spent: Rs.${"%.2f".format(total)}"
    }

    private fun loadAndShowBudget() {
        val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        val budget = prefs.getFloat("budget", 0f)

        tvBudgetInChart.text = if (budget == 0f) {
            "No Budget Set"
        } else {
            "Current Budget: Rs.${"%.2f".format(budget)}"
        }
    }

    private fun loadTransactions(): List<Transaction> {
        val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("transactions", null)
        val type = object : TypeToken<List<Transaction>>() {}.type
        return if (json != null) Gson().fromJson(json, type) else emptyList()
    }

    private fun calculateTotals(transactions: List<Transaction>): Map<String, Float> {
        val categoryMap = mutableMapOf<String, Float>()
        for (t in transactions) {
            val current = categoryMap[t.category] ?: 0f
            categoryMap[t.category] = current + t.amount.toFloat()
        }
        return categoryMap
    }

    private fun setupPieChart(categoryTotals: Map<String, Float>) {
        val entries = ArrayList<PieEntry>()
        for ((category, amount) in categoryTotals) {
            entries.add(PieEntry(amount, category))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors =
            ColorTemplate.MATERIAL_COLORS.toList() + ColorTemplate.JOYFUL_COLORS.toList()
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 16f
        dataSet.valueFormatter = PercentFormatter(pieChart)

        val boldTypeface = Typeface.defaultFromStyle(Typeface.BOLD)

        dataSet.valueTypeface = boldTypeface

        val data = PieData(dataSet)

        pieChart.data = data
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(16f)
        pieChart.setDrawEntryLabels(true)
        pieChart.setEntryLabelTypeface(boldTypeface)

        // Center Text
        pieChart.centerText = "Spending by Category"
        pieChart.setCenterTextColor(Color.WHITE)
        pieChart.setCenterTextSize(18f)
        pieChart.setCenterTextTypeface(boldTypeface)

        // Hole & Transparency
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.BLACK)
        pieChart.holeRadius = 58f
        pieChart.transparentCircleRadius = 62f
        pieChart.setTransparentCircleColor(Color.WHITE)
        pieChart.setTransparentCircleAlpha(110)

        // Background color (optional, if not already black from XML)
        pieChart.setBackgroundColor(Color.BLACK)

        // Legend styling for dark theme
        val legend = pieChart.legend
        legend.isEnabled = true
        legend.textColor = Color.WHITE
        legend.textSize = 14f
        legend.formSize = 12f
        legend.isWordWrapEnabled = true
        legend.typeface = boldTypeface

        // Animation
        pieChart.animateY(1400)

        pieChart.invalidate()
    }
}
