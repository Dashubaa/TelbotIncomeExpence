package com.example.telbot;

import com.example.telbot.models.Category;
import com.example.telbot.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class ExpenseBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private StateService stateService;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Long chatId = update.getMessage() != null ? update.getMessage().getChatId() : update.getCallbackQuery().getMessage().getChatId();
        String messageText = update.getMessage() != null ? update.getMessage().getText() : update.getCallbackQuery().getData();

        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            if (callbackData.equals("/menu")) {
                sendMessage(chatId, "Выберите действие:", getInlineKeyboard());
                stateService.clearState(chatId);
            } else if (callbackData.equals("/expense")) {
                List<Category> categories = categoryService.getAllCategories(chatId, "expense");
                String categoriesMessage = categoryService.allCategoriesFormat(categories);
                sendMessage(chatId, categoriesMessage, null);
                sendMessage(chatId, "Введите категорию и сумму (через пробел):", null);
                stateService.setState(chatId, "ADDING_EXPENSE");
            } else if (callbackData.equals("/income")) {
                List<Category> categories = categoryService.getAllCategories(chatId, "income");
                String categoriesMessage = categoryService.allCategoriesFormat(categories);
                sendMessage(chatId, categoriesMessage, null);
                sendMessage(chatId, "Введите категорию и сумму (через пробел):", null);
                stateService.setState(chatId, "ADDING_INCOME");
            } else if (callbackData.equals("/addcategory income")) {
                sendMessage(chatId, "Введите название категории доходов:", null);
                stateService.setState(chatId, "ADDING_INCOME_CATEGORY");
            } else if (callbackData.equals("/addcategory expense")) {
                sendMessage(chatId, "Введите название категории расходов:", null);
                stateService.setState(chatId, "ADDING_EXPENSE_CATEGORY");
            } else if (callbackData.equals("/start")) {
                sendMessage(chatId, "Привет, мой финансово грамотный друг! \uD83D\uDCB0 Я здесь, чтобы помочь тебе следить за твоими денюжками. \uD83D\uDCB8 О твоих копейках буду знать только я и ты, потому что я надежный чат-бот! Налоговая не придет за тобой после использования меня \uD83E\uDD11, так что не переживай и смело приступай к подсчету своих фантиков, чтобы поскорее воспользоваться всем моим функционалом! \uD83D\uDCB2", getInlineKeyboard());
                stateService.clearState(chatId);
            }
            System.out.println(chatId+": "+ messageText);
        } else {
            System.out.println(stateService.getState(chatId));
            switch (stateService.getState(chatId)) {
                case "ADDING_INCOME_CATEGORY":
                    System.out.println(stateService.getState(chatId));
                    Category categoryI = categoryService.createCategory(chatId, messageText, "income");
                    if (categoryI == null) {
                        sendMessage(chatId, "Категория с таким именем уже существует, введите другое название.", null);
                    } else {
                        categoryService.saveCategory(categoryI);
                        sendMessage(chatId, "Категория доходов успешно добавлена.",getInlineKeyboard());
                    }
                    break;
                case "ADDING_EXPENSE_CATEGORY":
                    Category categoryE = categoryService.createCategory(chatId, messageText, "expense");
                    if (categoryE == null) {
                        sendMessage(chatId, "Категория с таким именем уже существует, введите другое название:", null);
                    } else {
                        categoryService.saveCategory(categoryE);
                        sendMessage(chatId, "Категория расход успешно добавлена.",getInlineKeyboard());
                    }
                    break;
                case "ADDING_EXPENSE":
                    if (isValidInput(messageText)) {
                        Boolean resultE = transactionService.addExpense(chatId, messageText);
                        if(resultE){
                            sendMessage(chatId, "Транзакция расход успешно добавлена.",getInlineKeyboard());
                        }else{
                            sendMessage(chatId, "Категория с таким именем не найдена, введите другое название:", null);
                        }
                    } else {
                        sendMessage(chatId, "Неверный формат ввода. Пожалуйста, введите категорию и сумму, разделенные пробелом.", null);
                    }
                    break;
                case "ADDING_INCOME":
                    if (isValidInput(messageText)) {
                        Boolean resultI = transactionService.addIncome(chatId, messageText);
                        if(resultI){
                            sendMessage(chatId, "Транзакция доход успешно добавлена.",getInlineKeyboard());
                        }else{
                            sendMessage(chatId, "Категория с таким именем не найдена, введите другое название:", null);
                        }
                    } else {
                        sendMessage(chatId, "Неверный формат ввода. Пожалуйста, введите категорию и сумму, разделенные пробелом.", null);
                    }
                    break;

                default:
                    handleDefaultCommands(chatId, messageText);
                    break;
            }
        }
        System.out.println(chatId+": "+ messageText);
    }

    private void handleDefaultCommands(Long chatId, String messageText) {
        if (messageText.startsWith("/menu")) {
            sendMessage(chatId, "Выберите действие:", getInlineKeyboard());
            stateService.clearState(chatId);
        } else if (messageText.startsWith("/expense")) {
            sendMessage(chatId, "Введите категорию и сумму (через пробел):", null);
            stateService.setState(chatId, "ADDING_EXPENSE");
        } else if (messageText.startsWith("/income")) {
            sendMessage(chatId, "Введите категорию и сумму (через пробел):", null);
            stateService.setState(chatId, "ADDING_INCOME");
        } else if (messageText.startsWith("/addcategory income")) {
            sendMessage(chatId, "Введите название категории доходов:", null);
            stateService.setState(chatId, "ADDING_INCOME_CATEGORY");
        } else if (messageText.startsWith("/addcategory expense")) {
            sendMessage(chatId, "Введите название категории расходов:", null);
            stateService.setState(chatId, "ADDING_EXPENSE_CATEGORY");
        }else if (messageText.startsWith("/start")) {
            sendMessage(chatId, "Привет, мой финансово грамотный друг! \uD83D\uDCB0 Я здесь, чтобы помочь тебе следить за твоими денюжками. \uD83D\uDCB8 О твоих копейках буду знать только я и ты, потому что я надежный чат-бот! Налоговая не придет за тобой после использования меня \uD83E\uDD11, так что не переживай и смело приступай к подсчету своих фантиков, чтобы поскорее воспользоваться всем моим функционалом! \uD83D\uDCB2", getInlineKeyboard());
            stateService.clearState(chatId);
        } else {
            sendMessage(chatId, "Неверная команда. Пожалуйста, используйте кнопки ниже.", getInlineKeyboard());
        }
        stateService.clearState(chatId);
    }
    private boolean isValidInput(String input) {
        String[] parts = input.split(" ");
        if (parts.length != 2) {
            return false; // Ожидаем два значения
        }
        try {
            Double.parseDouble(parts[1]); // Проверяем, является ли второе значение числом
        } catch (NumberFormatException e) {
            return false; // Если не число, возвращаем false
        }
        return true; // Входные данные валидны
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public InlineKeyboardMarkup getInlineKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        rowInline1.add(InlineKeyboardButton.builder().text("Добавить доход").callbackData("/income").build());
        rowInline1.add(InlineKeyboardButton.builder().text("Добавить расход").callbackData("/expense").build());

        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        rowInline2.add(InlineKeyboardButton.builder().text("Добавить категорию доходов").callbackData("/addcategory income").build());


        List<InlineKeyboardButton> rowInline3 = new ArrayList<>();
        rowInline3.add(InlineKeyboardButton.builder().text("Добавить категорию расходов").callbackData("/addcategory expense").build());


        List<InlineKeyboardButton> rowInline4 = new ArrayList<>();
        rowInline4.add(InlineKeyboardButton.builder().text("Удалить категорию доходов").callbackData("/delcategory income").build());
        rowInline4.add(InlineKeyboardButton.builder().text("Удалить категорию расходов").callbackData("/delcategory expense").build());

        List<InlineKeyboardButton> rowInline5 = new ArrayList<>();
        rowInline5.add(InlineKeyboardButton.builder().text("Обновить транзакцию").callbackData("/updatetransaction").build());
        rowInline5.add(InlineKeyboardButton.builder().text("Удалить транзакцию").callbackData("/deletetransaction").build());

        List<InlineKeyboardButton> rowInline6 = new ArrayList<>();
        rowInline6.add(InlineKeyboardButton.builder().text("Меню").callbackData("/menu").build());
        rowInline6.add(InlineKeyboardButton.builder().text("Общая статистика").callbackData("/summary").build());

        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);
        rowsInline.add(rowInline3);
        rowsInline.add(rowInline4);
        rowsInline.add(rowInline5);
        rowsInline.add(rowInline6);

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        return inlineKeyboardMarkup;
    }
    private void sendMessage(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        if (keyboard != null) {
            message.setReplyMarkup(keyboard);
        }
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
