package com.example.personalfinanceapp.model

data class Transaction(
    val id: Int,
    var title: String,
    var amount: Double,
    var category: String,
    var date: String
)