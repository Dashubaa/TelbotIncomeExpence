package com.example.telbot.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StateService {
    private final Map<Long, String> userStates = new HashMap<>();

    public void setState(Long chatId, String state) {
        userStates.put(chatId, state);
    }

    public String getState(Long chatId) {
        return userStates.getOrDefault(chatId, "");
    }

    public void clearState(Long chatId) {
        userStates.remove(chatId);
    }
}

