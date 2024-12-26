package com.example.telbot.service;

import com.example.telbot.dao.TransactionRepository;
import com.example.telbot.models.Category;
import com.example.telbot.models.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.telbot.dao.CategoryRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    private final Map<Long, List<Transaction>> transactions = new HashMap<>();


    public boolean addTransaction(Long chatId, String categoryName, double amount, String type) {

        Category category = categoryRepository.findByChatIdAndType(chatId, type)
                .stream()
                .filter(cat -> cat.getName().equalsIgnoreCase(categoryName))
                .findFirst()
                .orElse(null);
        if(category == null){
            return false;
        }

        Transaction transaction = new Transaction();
        transaction.setUser(chatId);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setTimestamp(LocalDateTime.now());
        transactionRepository.save(transaction);
        return true;
    }


    public boolean addExpense(Long chatId, String messageText) {
        String[] parts = messageText.split(" ");
        String category = parts[0];
        double amount = Double.parseDouble(parts[1]);
        return addTransaction(chatId, category, amount, "expense");
    }

    public boolean addIncome(Long chatId, String messageText) {
        String[] parts = messageText.split(" ");
        String category = parts[0];
        double amount = Double.parseDouble(parts[1]);
        return addTransaction(chatId, category, amount, "income");
    }

    public String getSummary(Long chatId) {
        List<Transaction> userTransactions = transactions.get(chatId);
        if (userTransactions == null || userTransactions.isEmpty()) {
            return "Нет транзакций.";
        }

        StringBuilder summary = new StringBuilder("Сводка транзакций:\n");
        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction transaction : userTransactions) {
            if (transaction.getAmount() > 0) {
                totalIncome += transaction.getAmount();
            } else {
                totalExpense += transaction.getAmount();
            }
        }

        summary.append("Доходы: ").append(totalIncome).append("\n");
        summary.append("Расходы: ").append(totalExpense).append("\n");
        summary.append("Итого: ").append(totalIncome + totalExpense);

        return summary.toString();
    }
}
