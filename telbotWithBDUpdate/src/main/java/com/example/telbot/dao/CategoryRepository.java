package com.example.telbot.dao;

import com.example.telbot.models.Category;
import com.example.telbot.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByChatIdAndType(Long chatId, String type);
}