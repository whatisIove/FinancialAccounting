package com.dniprotech.financialaccounting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Pair;
import javafx.util.StringConverter;

import java.io.*;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FinanceManagerApp extends Application {
    private Stage primaryStage;
    private String currentUser;
    private VBox root;
    private File usersFile;
    private final Label userName = new Label();

    private final DatePicker datePicker = new DatePicker();


    private SimpleDoubleProperty balance;
    private ObservableList<Category> categories;
    private Map<Category, List<Subcategory>> categorySubcategoriesMap;
    private TableView<Transaction> transactionHistoryTableView;
    private TableView<BalanceCategory> balanceTableView;
    private PieChart pieChart;
    private List<Transaction> transactions = new ArrayList<>();

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Управленіння особистими фінансами");
        root = new VBox(10);
        root.setFillWidth(true); // Заполняем всю доступную вертикальную область
        usersFile = new File("users.txt");

        if (!usersFile.exists()) {
            try {
                usersFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        showLoginOrMainApp();
    }


    private void showLoginOrMainApp() {
        root.getChildren().clear(); // Очищаем все элементы из root

        if (currentUser == null) {
            Dialog<ButtonType> loginOrRegisterDialog = new Dialog<>();
            loginOrRegisterDialog.setTitle("Вибір дії");
            ButtonType loginButtonType = new ButtonType("Увійти", ButtonBar.ButtonData.OK_DONE);
            ButtonType registerButtonType = new ButtonType("Зареєструватися", ButtonBar.ButtonData.APPLY);
            loginOrRegisterDialog.getDialogPane().getButtonTypes().addAll(loginButtonType, registerButtonType);

            // Устанавливаем текст в диалоге
            loginOrRegisterDialog.setContentText("Оберіть, що ви хочете зробити:");

            // Отображаем диалоговое окно и обрабатываем результат
            Optional<ButtonType> result = loginOrRegisterDialog.showAndWait();

            if (result.isPresent() && result.get() == loginButtonType) {
                showLoginDialog(); // Открываем диалог входа
            } else if (result.isPresent() && result.get() == registerButtonType) {
                showRegistrationDialog(); // Открываем диалог регистрации
            }
        } else {
            // Очищаем root от кнопок "Увійти" и "Зареєструватися"
            root.getChildren().removeIf(node -> (node instanceof Button));

            pieChart = createPieChart(); // Создаем диаграмму
            root.getChildren().add(pieChart); // Добавляем диаграмму на форму

            balanceTableView = createBalanceTableView(); // Создаем таблицу балансов
            root.getChildren().add(balanceTableView); // Добавляем таблицу балансов на форму

            openMainApp();

        }

        Scene scene = new Scene(root, 1200, 768);
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    private void showLoginDialog() {
        Dialog<Pair<String, String>> loginDialog = new Dialog<>();
        loginDialog.setTitle("Вхід");
        ButtonType loginButtonType = new ButtonType("Увійти", ButtonBar.ButtonData.OK_DONE);
        loginDialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Создаем текстовые поля для имени пользователя и пароля
        TextField usernameField = new TextField();
        usernameField.setPromptText("Ім'я користувача");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Пароль");

        // Добавляем текстовые поля в диалоговое окно
        loginDialog.getDialogPane().setContent(new VBox(10, usernameField, passwordField));

        // Валидация при входе
        loginDialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(usernameField.getText(), passwordField.getText());
            }
            return null;
        });

        // Отображаем диалоговое окно и обрабатываем результат
        Optional<Pair<String, String>> result = loginDialog.showAndWait();
        result.ifPresent(credentials -> {
            String username = credentials.getKey();
            String password = credentials.getValue();
            if (loginUser(username, password)) {
                currentUser = username;
                root.getChildren().removeIf(node -> (node instanceof Button));
                userName.setText("Користувач: " + currentUser + ", дата: " + LocalDate.now());
                Font font = Font.font("Times New Roman", FontWeight.BOLD, FontPosture.ITALIC, 18);
                userName.setFont(font);

                root.getChildren().add(userName); // Добавляем usernameLabel
                openMainApp();
            } else {
                showAlert("Помилка входу", "Неправильне ім'я користувача або пароль.");
            }
        });
    }

        private void showRegistrationDialog() {
            Dialog<Pair<String, String>> registrationDialog = new Dialog<>();
            registrationDialog.setTitle("Регістрація");
            ButtonType registerButtonType = new ButtonType("Зареєструвати", ButtonBar.ButtonData.OK_DONE);
            registrationDialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

            // Создаем текстовые поля для имени пользователя и пароля
            TextField usernameField = new TextField();
            usernameField.setPromptText("Ім'я користувача");
            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Пароль");

            // Добавляем текстовые поля в диалоговое окно
            registrationDialog.getDialogPane().setContent(new VBox(10, usernameField, passwordField));

            // Валидация при регистрации
            registrationDialog.setResultConverter(dialogButton -> {
                if (dialogButton == registerButtonType) {
                    return new Pair<>(usernameField.getText(), passwordField.getText());
                }
                return null;
            });

            // Отображаем диалоговое окно и обрабатываем результат
            Optional<Pair<String, String>> result = registrationDialog.showAndWait();
            result.ifPresent(credentials -> {
                String username = credentials.getKey();
                String password = credentials.getValue();
                if (registerUser(username, password)) {
                    currentUser = username;
                    root.getChildren().removeIf(node -> (node instanceof Button)); // Удаляем кнопки "Войти" и "Зарегистрироваться"
                    openMainApp();
                } else {
                    showAlert("Помилка регістраціґ", "Користувач із таким ім'ям вже існує.");
                }
            });
        }


            private boolean loginUser(String username, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    String storedUsername = parts[0];
                    String storedPassword = parts[1];
                    if (username.equals(storedUsername) && hashPassword(password).equals(storedPassword)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean registerUser(String username, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length > 0) {
                    String storedUsername = parts[0];
                    if (username.equals(storedUsername)) {
                        return false;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt", true))) {
            writer.write(username + " " + hashPassword(password));
            writer.newLine();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    private void openMainApp() {
        primaryStage.setTitle("Управління особистими фінансами");
        transactions = loadTransactionsFromFile(currentUser);
        Button logoutButton = new Button("Вийти");
        logoutButton.setOnAction(e -> {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Підтвердження");
            confirmation.setHeaderText(null);
            confirmation.setContentText("Ви впевнені, що хочете вийти?");

            ButtonType confirmButtonType = new ButtonType("Так", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButtonType = new ButtonType("Скасувати", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmation.getButtonTypes().setAll(confirmButtonType, cancelButtonType);

            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == confirmButtonType) {
                // Выход из приложения
                primaryStage.close();
            }
        });

        categories = FXCollections.observableArrayList(
                new Category("Витрата", Arrays.asList(
                        new Subcategory("Комунальні послуги"),
                        new Subcategory("Їжа"),
                        new Subcategory("Розваги"),
                        new Subcategory("Одяг"),
                        new Subcategory("Кредит"),
                        new Subcategory("Депозит")
                )),
                new Category("Прибуток", Arrays.asList(
                        new Subcategory("Заробітна плата"),
                        new Subcategory("Дивіденди"),
                        new Subcategory("Подарунок")
                ))
        );

        categorySubcategoriesMap = new HashMap<>();
        for (Category category : categories) {
            categorySubcategoriesMap.put(category, category.getSubcategories());
        }

        ComboBox<Category> categoryComboBox = new ComboBox<>();
        categoryComboBox.setPromptText("Виберіть категорію");
        categoryComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Category category) {
                return category.getName();
            }

            @Override
            public Category fromString(String string) {
                return null;
            }
        });

        categoryComboBox.setItems(FXCollections.observableArrayList(categories));

        ComboBox<String> subcategoryComboBox = new ComboBox<>();
        subcategoryComboBox.setPromptText("Виберіть підкатегорію");
        subcategoryComboBox.setDisable(true);

        categoryComboBox.setOnAction(e -> {
            Category selectedCategory = categoryComboBox.getValue();
            if (selectedCategory != null) {
                List<Subcategory> subcategories = categorySubcategoriesMap.get(selectedCategory);
                List<String> subcategoryNames = new ArrayList<>();
                for (Subcategory subcategory : subcategories) {
                    subcategoryNames.add(subcategory.getName());
                }
                subcategoryComboBox.setItems(FXCollections.observableArrayList(subcategoryNames));
                subcategoryComboBox.setDisable(false);
                subcategoryComboBox.getSelectionModel().clearSelection();
            } else {
                subcategoryComboBox.getItems().clear();
                subcategoryComboBox.setPromptText("Підкатегорія відсутня");
                subcategoryComboBox.setDisable(true);
            }
        });

        TextField amountField = new TextField();
        amountField.setPromptText("Сума");
        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Опис");

        datePicker.setPromptText("Оберіть дату");

        Button addButton = new Button("Додати запис");
        addButton.setOnAction(e -> {
            LocalDate selectedDate = datePicker.getValue();
            Category category = categoryComboBox.getValue();
            String subcategory = subcategoryComboBox.getValue();
            String amountStr = amountField.getText();

            if (category != null && subcategory != null && !amountStr.isEmpty()) {
                try {
                    double amount = Double.parseDouble(amountStr);
                    String description = descriptionField.getText();
                    addTransaction(category.getName(), subcategory, amount, description, selectedDate);
                    categoryComboBox.getSelectionModel().clearSelection();
                    subcategoryComboBox.getSelectionModel().clearSelection();
                    amountField.clear();
                    descriptionField.clear();
                    datePicker.getEditor().clear();
                } catch (NumberFormatException ex) {
                    showAlert("Помилка", "Сума має бути числом.");
                }
            } else {
                showAlert("Помилка", "Будь ласка, заповніть всі поля.");
            }
        });


        root.getChildren().addAll(logoutButton, categoryComboBox, subcategoryComboBox, amountField,
                descriptionField, datePicker, addButton, transactionHistoryTableView, balanceTableView, pieChart);
        updateBalanceTable();
        updateTransactionTable();
    }


    private void updateTransactionTable() {
        ObservableList<Transaction> transactionData = FXCollections.observableArrayList(transactions);
        transactionHistoryTableView.setItems(transactionData);
    }

    public List<Transaction> loadTransactionsFromFile(String user) {
        List<Transaction> transactions = new ArrayList<>();
        String fileName = user + "_transactions.json";
        File file = new File(fileName);

        if (file.exists()) {
            try (FileReader reader = new FileReader(fileName);
                 BufferedReader bufferedReader = new BufferedReader(reader)) {
                Type listType = new TypeToken<List<Transaction>>() {}.getType();
                transactions = gson.fromJson(bufferedReader, listType);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return transactions;
    }

    private void addTransaction(String operationType, String subcategory, double amount, String description, LocalDate date) {
        if (amount != 0) {
            TextInputDialog descriptionDialog = new TextInputDialog();
            descriptionDialog.setTitle("Додавання запису");
            descriptionDialog.setHeaderText("Будь ласка, введіть опис операції (" + operationType + "):");
            descriptionDialog.setContentText("Опис:");

            Optional<String> descriptionResult = descriptionDialog.showAndWait();
            if (descriptionResult.isPresent()) {
                String userDescription = descriptionResult.get(); // Переименовали переменную в userDescription

                String transactionInfo = date.toString() + " - Категорія: " + operationType + " - " +
                        "Підкатегорія: " + subcategory + ", Сума: " + amount + ", Опис: " + userDescription;
                Transaction transaction = new Transaction(date, operationType, subcategory, amount, userDescription);
                transactionHistoryTableView.getItems().add(transaction);
                if ("Витрата".equals(operationType)) {
                    balance.set(balance.get() - amount);
                } else if ("Прибуток".equals(operationType)) {
                    balance.set(balance.get() + amount);
                }
                updatePieChart();
                saveTransactionToFile(currentUser, transactions);

                // Добавьте эту строку для обновления таблицы балансов
                updateBalanceTable();
            }
        }
    }


    private void saveTransactionToFile(String user, List<Transaction> transactions) {
        String fileName = user + "_transactions.json";
        try (FileWriter writer = new FileWriter(fileName)) {
            gson.toJson(transactions, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // В методе addTransaction вызываем saveTransactionToFile следующим образом:


    private PieChart createPieChart() {
        Map<String, Map<String, Double>> categorySubcategoryBalances = new HashMap<>();
        double totalBalance = 0.0;

        for (Transaction transaction : transactionHistoryTableView.getItems()) {
            String category = transaction.getCategory();
            String subcategory = transaction.getSubcategory();
            Double amount = transaction.getAmount();

            categorySubcategoryBalances
                    .computeIfAbsent(category, k -> new HashMap<>())
                    .merge(subcategory, amount, Double::sum);

            totalBalance += amount;
        }

        PieChart chart = new PieChart();
        for (Map.Entry<String, Map<String, Double>> categoryEntry : categorySubcategoryBalances.entrySet()) {
            String category = categoryEntry.getKey();
            Map<String, Double> subcategoryBalances = categoryEntry.getValue();
            double categoryTotal = subcategoryBalances.values().stream().mapToDouble(Double::doubleValue).sum();

            for (Map.Entry<String, Double> subcategoryEntry : subcategoryBalances.entrySet()) {
                String subcategory = subcategoryEntry.getKey();
                double balance = subcategoryEntry.getValue();

                // Рассчитываем процент для подкатегории
                double percentage = (balance / categoryTotal) * 100;
                String subcategoryLabel = subcategory + " (" + String.format("%.2f%%", percentage) + ")";

                // Создаем объект PieChart.Data и добавляем его в диаграмму
                PieChart.Data subcategoryData = new PieChart.Data(subcategoryLabel, Math.abs(balance));
                chart.getData().add(subcategoryData);
            }
        }
        return chart;
    }

    private void updateBalanceTable() {
        BalanceCategory totalBalanceCategory = new BalanceCategory("Баланс");
        double totalIncome = calculateTotalIncome();
        double totalExpense = calculateTotalExpense();
        double totalBalance = totalIncome - totalExpense;

        totalBalanceCategory.setTotalIncome(totalIncome);
        totalBalanceCategory.setTotalExpense(totalExpense);
        totalBalanceCategory.setTotalBalance(totalBalance);

        ObservableList<BalanceCategory> balanceData = FXCollections.observableArrayList(totalBalanceCategory);
        balanceTableView.setItems(balanceData);
    }


//    private void updateCategoryBalances(String category, double amount) {
//        for (Category cat : categories) {
//            if (cat.getName().equals(category)) {
//                cat.updateBalance(amount);
//            }
//        }
//        updateBalanceTable();
//    }

    private void updatePieChart() {
        Map<String, Map<String, Double>> categorySubcategoryBalances = new HashMap<>();
        double totalBalance = 0.0;

        for (Transaction transaction : transactionHistoryTableView.getItems()) {
            String category = transaction.getCategory();
            String subcategory = transaction.getSubcategory();
            Double amount = transaction.getAmount();

            categorySubcategoryBalances
                    .computeIfAbsent(category, k -> new HashMap<>())
                    .merge(subcategory, amount, Double::sum);

            totalBalance += amount;
        }

        pieChart.getData().clear();

        for (Map.Entry<String, Map<String, Double>> categoryEntry : categorySubcategoryBalances.entrySet()) {
            String category = categoryEntry.getKey();
            Map<String, Double> subcategoryBalances = categoryEntry.getValue();
            double categoryTotal = subcategoryBalances.values().stream().mapToDouble(Double::doubleValue).sum();

            for (Map.Entry<String, Double> subcategoryEntry : subcategoryBalances.entrySet()) {
                String subcategory = subcategoryEntry.getKey();
                double balance = subcategoryEntry.getValue();

                // Рассчитываем процент для подкатегории
                double percentage = (balance / categoryTotal) * 100;
                String subcategoryLabel = subcategory + " (" + String.format("%.2f%%", percentage) + ")";

                // Создаем объект PieChart.Data и добавляем его в диаграмму
                PieChart.Data subcategoryData = new PieChart.Data(subcategoryLabel, Math.abs(balance));
                pieChart.getData().add(subcategoryData);
            }
        }
    }

    private double calculateTotalIncome() {
        double totalIncome = 0.0;
        for (Transaction transaction : transactionHistoryTableView.getItems()) {
            if ("Прибуток".equals(transaction.getCategory())) {
                totalIncome += transaction.getAmount();
            }
        }
        return totalIncome;
    }

    private double calculateTotalExpense() {
        double totalExpense = 0.0;
        for (Transaction transaction : transactionHistoryTableView.getItems()) {
            if ("Витрата".equals(transaction.getCategory())) {
                totalExpense += transaction.getAmount();
            }
        }
        return totalExpense;
    }

    private TableView<Transaction> createTransactionHistoryTableView() {
        TableView<Transaction> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Transaction, LocalDate> dateColumn = new TableColumn<>("Дата та час");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        dateColumn.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });

        TableColumn<Transaction, String> categoryColumn = new TableColumn<>("Категорія");
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Transaction, String> subcategoryColumn = new TableColumn<>("Підкатегорія");
        subcategoryColumn.setCellValueFactory(new PropertyValueFactory<>("subcategory"));

        TableColumn<Transaction, Double> amountColumn = new TableColumn<>("Сума");
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", amount));
                }
            }
        });

        TableColumn<Transaction, String> descriptionColumn = new TableColumn<>("Опис");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dateColumn.setPrefWidth(125); // Устанавливаем ширину столбца dateColumn
        categoryColumn.setPrefWidth(80); // Устанавливаем ширину столбца categoryColumn
        subcategoryColumn.setPrefWidth(125); // Устанавливаем ширину столбца subcategoryColumn
        amountColumn.setPrefWidth(70); // Устанавливаем ширину столбца amountColumn
        descriptionColumn.setPrefWidth(180); // Устанавливаем ширину столбца descriptionColumn


        tableView.getColumns().addAll(dateColumn, categoryColumn, subcategoryColumn, amountColumn, descriptionColumn);
        return tableView;
    }

    private TableView<BalanceCategory> createBalanceTableView() {
        TableView<BalanceCategory> tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<BalanceCategory, String> categoryColumn = new TableColumn<>("Категорія");
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<BalanceCategory, Double> incomeColumn = new TableColumn<>("Прибуток");
        incomeColumn.setCellValueFactory(new PropertyValueFactory<>("totalIncome"));
        incomeColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double income, boolean empty) {
                super.updateItem(income, empty);
                if (empty || income == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", income));
                }
            }
        });

        TableColumn<BalanceCategory, Double> expenseColumn = new TableColumn<>("Витрати");
        expenseColumn.setCellValueFactory(new PropertyValueFactory<>("totalExpense"));
        expenseColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double expense, boolean empty) {
                super.updateItem(expense, empty);
                if (empty || expense == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", expense));
                }
            }
        });

        TableColumn<BalanceCategory, Double> balanceColumn = new TableColumn<>("Баланс");
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("totalBalance"));
        balanceColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double balance, boolean empty) {
                super.updateItem(balance, empty);
                if (empty || balance == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", balance));
                }
            }
        });

        tableView.getColumns().addAll(categoryColumn, incomeColumn, expenseColumn, balanceColumn);
        return tableView;
    }

    public class Transaction implements Serializable {

        @SerializedName("date")
        private final LocalDate date; // Изменили тип данных на LocalDate

            private final String category;
            private final String subcategory;
            private final double amount;
            private final String description;

            public Transaction(LocalDate date, String category, String subcategory, double amount, String description) {
                this.date = date;
                this.category = category;
                this.subcategory = subcategory;
                this.amount = amount;
                this.description = description;
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


    public static class Category {
        private final String name;
        private final List<Subcategory> subcategories;

        public Category(String name, List<Subcategory> subcategories) {
            this.name = name;
            this.subcategories = subcategories;
        }

        public String getName() {
            return name;
        }

        public List<Subcategory> getSubcategories() {
            return subcategories;
        }
    }

    public static class Subcategory {
        private final String name;

        public Subcategory(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class BalanceCategory {
        private String category;
        private double totalIncome;
        private double totalExpense;
        private double totalBalance;

        public BalanceCategory(String category) {
            this.category = category;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public double getTotalIncome() {
            return totalIncome;
        }

        public void setTotalIncome(double totalIncome) {
            this.totalIncome = totalIncome;
        }

        public double getTotalExpense() {
            return totalExpense;
        }

        public void setTotalExpense(double totalExpense) {
            this.totalExpense = totalExpense;
        }

        public double getTotalBalance() {
            return totalBalance;
        }

        public void setTotalBalance(double totalBalance) {
            this.totalBalance = totalBalance;
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return password;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}