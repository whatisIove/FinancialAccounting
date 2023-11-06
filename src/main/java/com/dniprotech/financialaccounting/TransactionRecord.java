package com.dniprotech.financialaccounting;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TransactionRecord {
    private LocalDate date;
    private LocalTime time;
    private String category;
    private String subcategory;
    private double amount;
    private String description;

    public TransactionRecord(LocalDate date, LocalTime time, String category, String subcategory, double amount, String description) {
        this.date = date;
        this.time = time;
        this.category = category;
        this.subcategory = subcategory;
        this.amount = amount;
        this.description = description;
    }

    public static TransactionRecord fromString(String line) {
        String[] parts = line.split(" - ");
        if (parts.length == 5) {
            String dateTimeStr = parts[0];
            String category = parts[1].split(": ")[1];
            String subcategory = parts[2].split(": ")[1];
            String amountStr = parts[3].split(": ")[1];
            String description = parts[4].split(": ")[1];

            String[] dateTimeParts = dateTimeStr.split(" ");
            if (dateTimeParts.length == 2) {
                LocalDate date = LocalDate.parse(dateTimeParts[0], DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                LocalTime time = LocalTime.parse(dateTimeParts[1], DateTimeFormatter.ofPattern("HH:mm:ss"));
                double amount = Double.parseDouble(amountStr);
                return new TransactionRecord(date, time, category, subcategory, amount, description);
            }
        }
        return null; // В случае ошибки разбора строки
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public String getCategory() {
        return category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public double getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }
}
