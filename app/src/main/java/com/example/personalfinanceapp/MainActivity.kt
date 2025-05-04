package com.example.personalfinanceapp

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.personalfinanceapp.model.Transaction
import com.example.personalfinanceapp.navigation.BottomNavbarNavigation
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var tvWarningMain: TextView
    private lateinit var tvAppUserCount: TextView

    private val importJsonLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let { importJsonFromUri(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvAppUserCount = findViewById(R.id.tvAppUserCount)
        animateUserCount(0, 5000000)

        // Toolbar setup
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)


        // 🔥 Enable back button
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.back) // Optional: use your custom icon
        }


        toolbar.setNavigationOnClickListener {
            val intent = Intent(this, LaunchActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        val welcomeText = findViewById<TextView>(R.id.tvWelcome)
        tvWarningMain = findViewById(R.id.tvBudgetWarningMain)
        welcomeText.text = "Welcome to Personal Finance Tracker"

        // Floating action button
        findViewById<FloatingActionButton>(R.id.fabAddTransaction).setOnClickListener {
            startActivity(Intent(this, TransactionActivity::class.java))
        }

        // Bottom navigation
        findViewById<BottomNavigationView>(R.id.bottomNavigation).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionListActivity::class.java))
                    true
                }
                R.id.nav_budget -> {
                    startActivity(Intent(this, BudgetActivity::class.java))
                    true
                }
                R.id.nav_chart -> {
                    startActivity(Intent(this, CategoryChartActivity::class.java))
                    true
                }
                R.id.nav_reminder -> {
                    startActivity(Intent(this, ReminderSettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        createNotificationChannel()

        // Notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private fun animateUserCount(startValue: Int, endValue: Int) {
        val animator = ValueAnimator.ofInt(startValue, endValue)
        animator.duration = 300000 // Duration of the animation in milliseconds
        animator.addUpdateListener { animation ->
            val currentValue = animation.animatedValue as Int
            tvAppUserCount.text = "Users: $currentValue"
        }
        animator.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_export -> {
                exportTransactions()
                true
            }
            R.id.menu_import -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }
                importJsonLauncher.launch(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onResume() {
        super.onResume()
        showBudgetWarning(tvWarningMain)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showBudgetWarning(warningTextView: TextView) {
        val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        val budget = prefs.getFloat("budget", 0f)

        val json = prefs.getString("transactions", null)
        val type = object : TypeToken<List<Transaction>>() {}.type
        val transactions = json?.let { Gson().fromJson<List<Transaction>>(it, type) } ?: emptyList()

        val totalSpent = transactions.sumOf { it.amount }.toFloat()
        val percentageUsed = if (budget > 0) (totalSpent / budget) * 100 else 0f

        val warningText = when {
            budget == 0f -> "⚠️ Budget not set!"
            percentageUsed > 100 -> {
                sendBudgetNotification("❌ You've exceeded your budget!")
                "❌ You have exceeded your budget!"
            }
            percentageUsed > 80 -> {
                sendBudgetNotification("⚠️ You're close to exceeding your budget.")
                "⚠️ You're close to exceeding your budget!"
            }
            else -> ""
        }

        warningTextView.text = warningText
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun sendBudgetNotification(message: String) {
        val builder = NotificationCompat.Builder(this, "budget_alerts")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💰 Budget Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        NotificationManagerCompat.from(this).notify(1001, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "budget_alerts",
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for approaching or exceeded budgets"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun exportTransactions() {
        val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        val json = prefs.getString("transactions", null)

        if (json == null) {
            Toast.makeText(this, "No transactions to export", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "transactions_backup.json"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(json.toByteArray())
                outputStream.flush()
                Toast.makeText(this, "✅ Exported to Downloads/$fileName", Toast.LENGTH_LONG).show()
            }
        } ?: Toast.makeText(this, "❌ Failed to export", Toast.LENGTH_SHORT).show()
    }

    private fun importJsonFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val json = inputStream?.bufferedReader().use { it?.readText() }

            if (!json.isNullOrEmpty()) {
                getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
                    .edit().putString("transactions", json).apply()

                Toast.makeText(this, "✅ Transactions imported!", Toast.LENGTH_SHORT).show()
                showBudgetWarning(tvWarningMain)
            } else {
                Toast.makeText(this, "⚠️ File is empty", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Failed to import: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
