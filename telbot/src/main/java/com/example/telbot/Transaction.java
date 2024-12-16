package com.example.telbot;

import java.time.LocalDateTime;

public class Transaction {
    String category;
    double amount;
    LocalDateTime date;

    public Transaction(String category, double amount, LocalDateTime date) {
        this.category = category;
        this.amount = amount;
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
