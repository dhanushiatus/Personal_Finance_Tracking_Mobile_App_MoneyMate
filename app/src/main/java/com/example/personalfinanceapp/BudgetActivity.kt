package com.example.personalfinanceapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.personalfinanceapp.model.Transaction
import com.example.personalfinanceapp.navigation.BottomNavbarNavigation
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BudgetActivity : AppCompatActivity() {

    private lateinit var inputBudget: EditText
    private lateinit var btnSave: Button
    private lateinit var btnReset: Button
    private lateinit var tvCurrentBudget: TextView
    private lateinit var tvTotalSpent: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvWarning: TextView

    private var budget: Float = 0f
    private var totalSpent: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        // Setup bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        BottomNavbarNavigation.setup(this, bottomNav, R.id.nav_budget)

        // Initialize views
        inputBudget = findViewById(R.id.inputBudget)
        btnSave = findViewById(R.id.btnSaveBudget)
        btnReset = findViewById(R.id.btnResetBudget)
        tvCurrentBudget = findViewById(R.id.tvCurrentBudget)
        tvTotalSpent = findViewById(R.id.tvTotalSpent)
        progressBar = findViewById(R.id.progressBar)
        tvWarning = findViewById(R.id.tvWarning)

        // Remove spinner if it's visible
        progressBar.visibility = ProgressBar.GONE

        // Load existing budget and calculate total spent
        loadBudget()
        calculateTotalSpent()
        updateUI()

        // Back button click listener
        val btnBack: ImageButton = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            // Redirect to TransactionListActivity
            val intent = Intent(this, TransactionListActivity::class.java)
            startActivity(intent)
            finish() // Optionally, close the current activity
        }

        // Save button click listener
        btnSave.setOnClickListener {
            val input = inputBudget.text.toString().toFloatOrNull()
            if (input == null || input <= 0) {
                Toast.makeText(this, "Enter a valid budget", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Save the budget to shared preferences
            budget = input
            getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
                .edit().putFloat("budget", budget).apply()

            updateUI()
            Toast.makeText(this, "Budget Saved ✅", Toast.LENGTH_SHORT).show()
        }

        // Reset button click listener
        btnReset.setOnClickListener {
            // Reset the budget
            val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
            prefs.edit().remove("budget").apply()
            budget = 0f
            updateUI()
            Toast.makeText(this, "Budget has been reset", Toast.LENGTH_SHORT).show()
        }
    }

    // Load the budget from shared preferences
    private fun loadBudget() {
        val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        budget = prefs.getFloat("budget", 0f)
    }

    // Calculate the total spent from transactions stored in shared preferences
    private fun calculateTotalSpent() {
        val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("transactions", null)
        val type = object : TypeToken<List<Transaction>>() {}.type
        val transactions: List<Transaction> = if (json != null) Gson().fromJson(json, type) else emptyList()
        totalSpent = transactions.sumOf { it.amount }.toFloat()
    }

    // Update the UI with current budget, total spent, progress bar, and warnings
    private fun updateUI() {
        // Update current budget and total spent text
        tvCurrentBudget.text = if (budget == 0f) "No budget set" else "Current Budget: Rs.${"%.2f".format(budget)}"
        tvTotalSpent.text = "Total Spent: Rs.${"%.2f".format(totalSpent)}"

        // Calculate the percentage of budget used
        val percentageUsed = if (budget > 0) (totalSpent / budget) * 100 else 0f
        progressBar.progress = percentageUsed.toInt()

        // Update warning text based on the budget usage
        tvWarning.text = when {
            budget == 0f -> "⚠️ Budget not set!"
            percentageUsed > 100 -> "❌ You have exceeded your budget!"
            percentageUsed > 80 -> "⚠️ You're close to exceeding your budget!"
            else -> ""
        }
    }
}
