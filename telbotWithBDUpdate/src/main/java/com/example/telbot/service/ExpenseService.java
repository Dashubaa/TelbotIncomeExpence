package com.example.telbot.service;

import com.example.telbot.models.Category;
import com.example.telbot.models.Transaction;
import com.example.telbot.models.User;
import com.example.telbot.dao.CategoryRepository;
import com.example.telbot.dao.TransactionRepository;
import com.example.telbot.dao.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExpenseService {


    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;


    public List<Transaction> getTransactions(Long chatId) {
        return transactionRepository.findByChatId(chatId);
    }

    public void addTransaction(Long chatId, String categoryName, double amount) {

        Category category = categoryRepository.findByChatIdAndType(chatId, "expense")
                .stream()
                .filter(cat -> cat.getName().equalsIgnoreCase(categoryName))
                .findFirst()
                .orElse((null));

        Transaction transaction = new Transaction();
        transaction.setUser(chatId);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setTimestamp(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

}
