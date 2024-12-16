package com.example.telbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Component
public class ExpenseBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    private Map<Long, String> userStates = new HashMap<>();
    private Map<Long, Map<String, Double>> userExpenses = new HashMap<>();
    private Map<Long, List<Transaction>> transactions = new HashMap<>();
    private Map<Long, List<String>> incomeCategories = new HashMap<>();
    private Map<Long, List<String>> expenseCategories = new HashMap<>();

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
            if (callbackData.startsWith("edit_")) {
                int index = Integer.parseInt(callbackData.split("_")[1]);
                sendMessage(chatId, "Введите новую сумму для транзакции " + index + ":", null);
                userStates.put(chatId, "UPDATING_TRANSACTION_" + index);
            } else if (callbackData.startsWith("delete_")) {
                int index = Integer.parseInt(callbackData.split("_")[1]);
                deleteTransaction(chatId, String.valueOf(index));
            } else if (callbackData.equals("/menu")) {
                sendMessage(chatId, "Выберите действие:", getInlineKeyboard());
                userStates.remove(chatId);
            } else if (callbackData.equals("/summary")) {
                executeSummaryCommand(chatId);
            } else if (callbackData.equals("/expense")) {
                sendMessage(chatId, "Введите категорию и сумму (через пробел):", null);
                userStates.put(chatId, "ADDING_EXPENSE");
            } else if (callbackData.equals("/income")) {
                sendMessage(chatId, "Введите категорию и сумму (через пробел):", null);
                userStates.put(chatId, "ADDING_INCOME");
            } else if (callbackData.equals("/addcategory income")) {
                sendMessage(chatId, "Введите название категории доходов:", null);
                userStates.put(chatId, "ADDING_INCOME_CATEGORY");
            } else if (callbackData.equals("/addcategory expense")) {
                sendMessage(chatId, "Введите название категории расходов:", null);
                userStates.put(chatId, "ADDING_EXPENSE_CATEGORY");
            } else if (callbackData.equals("/delcategory income")) {
                StringBuilder categories = new StringBuilder("Available income categories:\n");
                List<String> incomeCatList = incomeCategories.getOrDefault(chatId, new ArrayList<>());
                for (String cat : incomeCatList) {
                    categories.append(cat).append("\n");
                }
                sendMessage(chatId, categories.toString() + "Введите название категории доходов, которую хотите удалить:", null);
                userStates.put(chatId, "DELETING_INCOME_CATEGORY");
            } else if (callbackData.equals("/delcategory expense")) {
                StringBuilder categories = new StringBuilder("Available expense categories:\n");
                List<String> expenseCatList = expenseCategories.getOrDefault(chatId, new ArrayList<>());
                for (String cat : expenseCatList) {
                    categories.append(cat).append("\n");
                }
                sendMessage(chatId, categories.toString() + "Введите название категории расходов, которую хотите удалить:", null);
                userStates.put(chatId, "DELETING_EXPENSE_CATEGORY");
            } else if (callbackData.equals("/updatetransaction")) {
                listTransactions(chatId);
                sendMessage(chatId, "Введите индекс транзакции и новую сумму через пробел:", null);
                userStates.put(chatId, "UPDATING_TRANSACTION");
            } else if (callbackData.equals("/deletetransaction")) {
                listTransactions(chatId);
                sendMessage(chatId, "Введите индекс транзакции для удаления:", null);
                userStates.put(chatId, "DELETING_TRANSACTION");
            } else if (callbackData.equals("/report")) {
                sendMessage(chatId, "Выберите период для отчета:", getReportPeriodKeyboard());
                userStates.put(chatId, "SELECTING_REPORT_PERIOD");
            }
        } else {
            switch (userStates.getOrDefault(chatId, "")) {
                case "ADDING_INCOME_CATEGORY":
                    addIncomeCategory(chatId, messageText);
                    break;
                case "ADDING_EXPENSE_CATEGORY":
                    addExpenseCategory(chatId, messageText);
                    break;
                case "ADDING_EXPENSE":
                    addExpense(chatId, messageText);
                    break;
                case "ADDING_INCOME":
                    addIncome(chatId, messageText);
                    break;
                case "REQUESTING_REPORT":
                    generateReport(chatId, messageText);
                    break;
                case "SELECTING_REPORT_PERIOD":
                    handleReportPeriodSelection(chatId, messageText);
                    break;
                case "DELETING_INCOME_CATEGORY":
                    deleteIncomeCategory(chatId, messageText);
                    break;
                case "DELETING_EXPENSE_CATEGORY":
                    deleteExpenseCategory(chatId, messageText);
                    break;
                case "UPDATING_TRANSACTION":
                    updateTransaction(chatId, messageText);
                    break;
                case "DELETING_TRANSACTION":
                    deleteTransaction(chatId, messageText);
                    break;
                default:
                    handleDefaultCommands(chatId, messageText);
                    break;
            }
        }
    }


    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void addIncomeCategory(Long chatId, String category) {
        List<String> categories = incomeCategories.getOrDefault(chatId, new ArrayList<>());
        if (categories.contains(category)) {
            sendMessage(chatId, "Income category " + category + " already exists. Please enter a different name.", null);
        } else {
            categories.add(category);
            incomeCategories.put(chatId, categories);
            sendMessage(chatId, "Income category " + category + " added.", getInlineKeyboard());
            userStates.remove(chatId);
        }
    }

    private void addExpenseCategory(Long chatId, String category) {
        List<String> categories = expenseCategories.getOrDefault(chatId, new ArrayList<>());
        if (categories.contains(category)) {
            sendMessage(chatId, "Expense category " + category + " already exists. Please enter a different name.", null);
        } else {
            categories.add(category);
            expenseCategories.put(chatId, categories);
            sendMessage(chatId, "Expense category " + category + " added.", getInlineKeyboard());
            userStates.remove(chatId);
        }
    }

    private void deleteIncomeCategory(Long chatId, String category) {
        List<String> categories = incomeCategories.get(chatId);
        if (categories != null && categories.remove(category)) {
            sendMessage(chatId, "Income category " + category + " deleted.", getInlineKeyboard());
        } else {
            sendMessage(chatId, "Income category " + category + " not found.", getInlineKeyboard());
        }
        userStates.remove(chatId);
    }

    private void deleteExpenseCategory(Long chatId, String category) {
        List<String> categories = expenseCategories.get(chatId);
        if (categories != null && categories.remove(category)) {
            sendMessage(chatId, "Expense category " + category + " deleted.", getInlineKeyboard());
        } else {
            sendMessage(chatId, "Expense category " + category + " not found.", getInlineKeyboard());
        }
        userStates.remove(chatId);
    }

    private void addExpense(Long chatId, String messageText) {
        String[] parts = messageText.split(" ");
        if (parts.length != 2 || !isNumeric(parts[1])) {
            sendMessage(chatId, "Неверный формат. Пожалуйста, введите категорию и сумму (через пробел):", null);
            return;
        }
        String category = parts[0];
        double amount = Double.parseDouble(parts[1]);
        if (expenseCategories.getOrDefault(chatId, new ArrayList<>()).contains(category)) {
            transactions.putIfAbsent(chatId, new ArrayList<>());
            transactions.get(chatId).add(new Transaction(category, amount, LocalDateTime.now()));
            userExpenses.putIfAbsent(chatId, new HashMap<>());
            userExpenses.get(chatId).put(category, userExpenses.get(chatId).getOrDefault(category, 0.0) + amount);
            sendMessage(chatId, "Expense " + amount + " added to " + category, getInlineKeyboard());
        } else {
            sendMessage(chatId, "Category " + category + " not found in expense categories.", getInlineKeyboard());
        }
        userStates.remove(chatId);
    }


    private void addIncome(Long chatId, String messageText) {
        String[] parts = messageText.split(" ");
        if (parts.length != 2 || !isNumeric(parts[1])) {
            sendMessage(chatId, "Неверный формат. Пожалуйста, введите категорию и сумму (через пробел):", null);
            return;
        }
        String category = parts[0];
        double amount = Double.parseDouble(parts[1]);
        if (incomeCategories.getOrDefault(chatId, new ArrayList<>()).contains(category)) {
            transactions.putIfAbsent(chatId, new ArrayList<>());
            transactions.get(chatId).add(new Transaction(category, -amount, LocalDateTime.now()));
            userExpenses.putIfAbsent(chatId, new HashMap<>());
            userExpenses.get(chatId).put(category, userExpenses.get(chatId).getOrDefault(category, 0.0) - amount); // Сохраняем минус для учета
            sendMessage(chatId, "Income " + amount + " added to " + category, getInlineKeyboard());
        } else {
            sendMessage(chatId, "Category " + category + " not found in income categories.", getInlineKeyboard());
        }
        userStates.remove(chatId);
    }


    private void updateTransaction(Long chatId, String messageText) {
        String[] parts = messageText.split(" ");
        if (parts.length != 2 || !isNumeric(parts[1])) {
            sendMessage(chatId, "Неверный формат. Пожалуйста, введите индекс транзакции и новую сумму через пробел:", null);
            return;
        }
        int index = Integer.parseInt(parts[0]);
        double newAmount = Double.parseDouble(parts[1]);

        List<Transaction> userTransactions = transactions.get(chatId);
        if (userTransactions != null && index >= 0 && index < userTransactions.size()) {
            Transaction transaction = userTransactions.get(index);
            transaction.amount = newAmount;
            sendMessage(chatId, "Transaction updated to " + newAmount + " for " + transaction.category, getInlineKeyboard());
        } else {
            sendMessage(chatId, "Invalid transaction index.", getInlineKeyboard());
        }
        userStates.remove(chatId);
    }


    private void deleteTransaction(Long chatId, String messageText) {
        if (!isNumeric(messageText)) {
            sendMessage(chatId, "Неверный формат. Пожалуйста, введите индекс транзакции для удаления:", null);
            return;
        }
        int index = Integer.parseInt(messageText);
        List<Transaction> userTransactions = transactions.get(chatId);
        if (userTransactions != null && index >= 0 && index < userTransactions.size()) {
            Transaction transaction = userTransactions.remove(index);
            sendMessage(chatId, "Transaction deleted: " + transaction.category + " " + transaction.amount, getInlineKeyboard());
        } else {
            sendMessage(chatId, "Invalid transaction index.", getInlineKeyboard());
        }
        userStates.remove(chatId);
    }


    private void handleDefaultCommands(Long chatId, String messageText) {
        if (messageText.startsWith("/addcategory income")) {
            sendMessage(chatId, "Введите название категории доходов:", null);
            userStates.put(chatId, "ADDING_INCOME_CATEGORY");
        } else if (messageText.startsWith("/addcategory expense")) {
            sendMessage(chatId, "Введите название категории расходов:", null);
            userStates.put(chatId, "ADDING_EXPENSE_CATEGORY");
        } else if (messageText.startsWith("/delcategory income")) {
            StringBuilder categories = new StringBuilder("Available income categories:\n");
            List<String> incomeCatList = incomeCategories.getOrDefault(chatId, new ArrayList<>());
            for (String cat : incomeCatList) {
                categories.append(cat).append("\n");
            }
            sendMessage(chatId, categories.toString() + "Введите название категории доходов, которую хотите удалить:", null);
            userStates.put(chatId, "DELETING_INCOME_CATEGORY");
        } else if (messageText.startsWith("/delcategory expense")) {
            StringBuilder categories = new StringBuilder("Available expense categories:\n");
            List<String> expenseCatList = expenseCategories.getOrDefault(chatId, new ArrayList<>());
            for (String cat : expenseCatList) {
                categories.append(cat).append("\n");
            }
            sendMessage(chatId, categories.toString() + "Введите название категории расходов, которую хотите удалить:", null);
            userStates.put(chatId, "DELETING_EXPENSE_CATEGORY");
        } else if (messageText.startsWith("/expense")) {
            sendMessage(chatId, "Введите категорию и сумму (через пробел):", null);
            userStates.put(chatId, "ADDING_EXPENSE");
        } else if (messageText.startsWith("/income")) {
            sendMessage(chatId, "Введите категорию и сумму (через пробел):", null);
            userStates.put(chatId, "ADDING_INCOME");
        } else if (messageText.startsWith("/report")) {
            sendMessage(chatId, "Выберите период для отчета:", getReportPeriodKeyboard());
            userStates.put(chatId, "SELECTING_REPORT_PERIOD");
        } else if (messageText.startsWith("/summary")) {
            executeSummaryCommand(chatId);
        } else if (messageText.startsWith("/updatetransaction")) {
            listTransactions(chatId);
            sendMessage(chatId, "Введите индекс транзакции и новую сумму через пробел:", null);
            userStates.put(chatId, "UPDATING_TRANSACTION");
        } else if (messageText.startsWith("/deletetransaction")) {
            listTransactions(chatId);
            sendMessage(chatId, "Введите индекс транзакции для удаления:", null);
            userStates.put(chatId, "DELETING_TRANSACTION");
        } else {
            sendMessage(chatId, "Неверная команда. Пожалуйста, используйте кнопки ниже.", getInlineKeyboard());
        }
    }

    private void listTransactions(Long chatId) {
        List<Transaction> userTransactions = transactions.get(chatId);
        if (userTransactions != null && !userTransactions.isEmpty()) {
            StringBuilder transactionList = new StringBuilder("Transactions:\n");
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

            for (int i = 0; i < userTransactions.size(); i++) {
                Transaction transaction = userTransactions.get(i);
                transactionList.append(i).append(": ").append(transaction.category).append(" ").append(transaction.amount).append("\n");


            }
            inlineKeyboardMarkup.setKeyboard(rowsInline);
            sendMessage(chatId, transactionList.toString(), inlineKeyboardMarkup);
        } else {
            sendMessage(chatId, "No transactions available for editing or deletion.", getInlineKeyboard());
        }
    }

    private void generateReport(Long chatId, String period) {
        String[] dates = period.split(" ");
        LocalDateTime startDate = LocalDateTime.parse(dates[0] + "T00:00:00");
        LocalDateTime endDate = LocalDateTime.parse(dates[1] + "T23:59:59");

        StringBuilder report = new StringBuilder("Report from " + startDate.toLocalDate() + " to " + endDate.toLocalDate() + ":\n");
        double totalIncome = 0;
        double totalExpenses = 0;

        List<Transaction> userTransactions = transactions.getOrDefault(chatId, new ArrayList<>());
        for (Transaction transaction : userTransactions) {
            if (!transaction.date.isBefore(startDate) && !transaction.date.isAfter(endDate)) {
                if (transaction.amount < 0) {
                    report.append("Income ").append(transaction.category).append(": ").append(-transaction.amount).append("\n");
                    totalIncome -= transaction.amount;
                } else {
                    report.append("Expense ").append(transaction.category).append(": ").append(transaction.amount).append("\n");
                    totalExpenses += transaction.amount;
                }
            }
        }

        report.append("\nTotal Income: ").append(totalIncome).append("\n");
        report.append("Total Expenses: ").append(totalExpenses).append("\n");
        report.append("Net Total: ").append(totalIncome - totalExpenses).append("\n");

        sendMessage(chatId, report.toString(), getInlineKeyboard());
        userStates.remove(chatId);
    }
    private void handleReportPeriodSelection(Long chatId, String messageText) {
        LocalDateTime startDate;
        LocalDateTime endDate = LocalDateTime.now();

        switch (messageText) {
            case "current_month":
                startDate = endDate.withDayOfMonth(1);
                break;
            case "last_month":
                startDate = endDate.minusMonths(1).withDayOfMonth(1);
                endDate = startDate.plusMonths(1).minusDays(1);
                break;
            case "current_year":
                startDate = endDate.withDayOfYear(1);
                break;
            case "last_year":
                startDate = endDate.minusYears(1).withDayOfYear(1);
                endDate = startDate.plusYears(1).minusDays(1);
                break;
            default:
                sendMessage(chatId, "Неверный период. Пожалуйста, попробуйте еще раз.", getReportPeriodKeyboard());
                return;
        }

        generateReport(chatId, startDate, endDate);
        userStates.remove(chatId);
    }

    private void generateReport(Long chatId, LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder report = new StringBuilder("Report from " + startDate.toLocalDate() + " to " + endDate.toLocalDate() + ":\n");
        double totalIncome = 0;
        double totalExpenses = 0;

        List<Transaction> userTransactions = transactions.getOrDefault(chatId, new ArrayList<>());
        for (Transaction transaction : userTransactions) {
            if (!transaction.date.isBefore(startDate) && !transaction.date.isAfter(endDate)) {
                if (transaction.amount < 0) {
                    report.append("Income ").append(transaction.category).append(": ").append(-transaction.amount).append("\n");
                    totalIncome -= transaction.amount;
                } else {
                    report.append("Expense ").append(transaction.category).append(": ").append(transaction.amount).append("\n");
                    totalExpenses += transaction.amount;
                }
            }
        }

        report.append("\nTotal Income: ").append(totalIncome).append("\n");
        report.append("Total Expenses: ").append(totalExpenses).append("\n");
        report.append("Net Total: ").append(totalIncome - totalExpenses).append("\n");

        sendMessage(chatId, report.toString(), getInlineKeyboard());
    }

    private InlineKeyboardMarkup getReportPeriodKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        rowInline1.add(InlineKeyboardButton.builder().text("Текущий месяц").callbackData("current_month").build());
        rowInline1.add(InlineKeyboardButton.builder().text("Прошлый месяц").callbackData("last_month").build());

        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        rowInline2.add(InlineKeyboardButton.builder().text("Текущий год").callbackData("current_year").build());
        rowInline2.add(InlineKeyboardButton.builder().text("Прошлый год").callbackData("last_year").build());

        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        return inlineKeyboardMarkup;
    }


    private void executeSummaryCommand(Long chatId) {
        StringBuilder summary = new StringBuilder();
        double totalExpenses = 0;
        double totalIncome = 0;
        Map<String, Double> expenses = userExpenses.get(chatId);
        if (expenses != null) {
            for (Map.Entry<String, Double> entry : expenses.entrySet()) {
                double value = entry.getValue();
                if (value < 0) {
                    totalIncome += -value; // Учитываем доходы
                    summary.append(entry.getKey()).append(": ").append(-value).append("\n"); // Отображаем доходы без минуса
                } else {
                    totalExpenses += value; // Учитываем расходы
                    summary.append(entry.getKey()).append(": -").append(value).append("\n"); // Отображаем расходы с минусом
                }
            }
            summary.append("Total Income: ").append(totalIncome).append("\n");
            summary.append("Total Expenses: ").append(totalExpenses).append("\n");
            summary.append("Net Total: ").append(totalIncome - totalExpenses); // Чистая итоговая сумма
        } else {
            summary.append("No expenses recorded.");
        }
        sendMessage(chatId, summary.toString(), getInlineKeyboard());
    }


    public InlineKeyboardMarkup getInlineKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        rowInline1.add(InlineKeyboardButton.builder().text("Summary").callbackData("/summary").build());
        rowInline1.add(InlineKeyboardButton.builder().text("Expense").callbackData("/expense").build());

        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        rowInline2.add(InlineKeyboardButton.builder().text("Add Income Category").callbackData("/addcategory income").build());
        rowInline2.add(InlineKeyboardButton.builder().text("Add Expense Category").callbackData("/addcategory expense").build());

        List<InlineKeyboardButton> rowInline3 = new ArrayList<>();
        rowInline3.add(InlineKeyboardButton.builder().text("Income").callbackData("/income").build());
        rowInline3.add(InlineKeyboardButton.builder().text("Report").callbackData("/report").build());

        List<InlineKeyboardButton> rowInline4 = new ArrayList<>();
        rowInline4.add(InlineKeyboardButton.builder().text("Delete Income Category").callbackData("/delcategory income").build());
        rowInline4.add(InlineKeyboardButton.builder().text("Delete Expense Category").callbackData("/delcategory expense").build());

        List<InlineKeyboardButton> rowInline5 = new ArrayList<>();
        rowInline5.add(InlineKeyboardButton.builder().text("Update Transaction").callbackData("/updatetransaction").build());
        rowInline5.add(InlineKeyboardButton.builder().text("Delete Transaction").callbackData("/deletetransaction").build());

        List<InlineKeyboardButton> rowInline6 = new ArrayList<>();
        rowInline6.add(InlineKeyboardButton.builder().text("Меню").callbackData("/menu").build());

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
