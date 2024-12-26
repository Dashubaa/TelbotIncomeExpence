package com.example.telbot.service;

import com.example.telbot.dao.CategoryRepository;
import com.example.telbot.models.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public void saveCategory(Category category) {
        categoryRepository.save(category);
    }

    public List<Category> getCategoriesByChatIdAndType(Long chatId, String type) {
        return categoryRepository.findByChatIdAndType(chatId, type);
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
    public Category createCategory(Long chatId, String name, String type) {
        if (!categoryExists(chatId, name, type)) {
            Category category = new Category();
            category.setName(name);
            category.setType(type);
            category.setUser(chatId);
            return category;
        } else {
            return null;
        }
    }
    private boolean categoryExists(Long chatId, String name, String type) {
        List<Category> categories = categoryRepository.findByChatIdAndType(chatId, type);
        for (Category category : categories) {
            if (category.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
    public List getAllCategories(Long chatId, String type) {
        List<Category> categories = categoryRepository.findByChatIdAndType(chatId, type);
        return categories;
    }
    public String allCategoriesFormat(List<Category> categories) {
        StringBuilder sb = new StringBuilder("Доступные категории:\n");
        for (Category category : categories) {
            sb.append("- ").append(category.getName()).append("\n");
        }
        return sb.toString();
    }

}
