package com.example.telbot.dao;

import com.example.telbot.models.Transaction;
import com.example.telbot.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByChatId(Long chatId);
}
