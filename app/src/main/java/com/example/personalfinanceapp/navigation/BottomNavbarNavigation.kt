package com.example.personalfinanceapp.navigation

import android.content.Context
import android.content.Intent
import com.example.personalfinanceapp.*
import com.google.android.material.bottomnavigation.BottomNavigationView

object BottomNavbarNavigation {

    fun setup(context: Context, bottomNavigationView: BottomNavigationView, currentItemId: Int) {
        bottomNavigationView.selectedItemId = currentItemId

        bottomNavigationView.setOnItemSelectedListener { item ->
            if (item.itemId == currentItemId) return@setOnItemSelectedListener true

            when (item.itemId) {
                R.id.nav_home -> {
                    context.startActivity(Intent(context, MainActivity::class.java))
                    true
                }
                R.id.nav_transactions -> {
                    context.startActivity(Intent(context, TransactionListActivity::class.java))
                    true
                }
                R.id.nav_budget -> {
                    context.startActivity(Intent(context, BudgetActivity::class.java))
                    true
                }
                R.id.nav_chart -> {
                    context.startActivity(Intent(context, CategoryChartActivity::class.java))
                    true
                }
                R.id.nav_reminder -> {
                    context.startActivity(Intent(context, ReminderSettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
