package com.dniprotech.financialaccounting;

import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FinanceManagerApp extends Application {
    private Stage primaryStage;
    private BorderPane root;
    private String currentUser;
    private SimpleDoubleProperty balance;
    private ObservableList<Category> categories;
    private Map<Category, List<Subcategory>> categorySubcategoriesMap;
    private TableView<Transaction> transactionHistoryTableView;
    private TableView<BalanceCategory> balanceTableView;
    private PieChart pieChart;
    private TextField timeField; // Добавляем TextField для ввода времени

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Управління фінансами");

        currentUser = getUserInfo();

        root = new BorderPane();

        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("Файл");
        MenuItem saveItem = new MenuItem("Зберегти");
        MenuItem exitItem = new MenuItem("Вийти");
        exitItem.setOnAction(e -> primaryStage.close());
        fileMenu.getItems().addAll(saveItem, new SeparatorMenuItem(), exitItem);
        menuBar.getMenus().add(fileMenu);
        root.setTop(menuBar);

        VBox userInfoBox = new VBox(10);
        Label userInfoLabel = new Label("Користувач: " + currentUser);
        userInfoLabel.setStyle("-fx-font-size: 18px;");
        balance = new SimpleDoubleProperty(0.0);

        userInfoBox.getChildren().addAll(userInfoLabel);
        root.setTop(userInfoBox);

        VBox transactionInputBox = new VBox(10);
        transactionInputBox.setPadding(new Insets(20));
        Label transactionLabel = new Label("Додавання операції");
        transactionLabel.setStyle("-fx-font-size: 18px;");
        TextField amountField = new TextField();
        amountField.setPromptText("Сума");

        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Дата");
        datePicker.setValue(LocalDate.now());

        // Добавляем TextField для ввода времени
        timeField = new TextField();
        timeField.setPromptText("ЧЧ:ММ:СС");

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

        categoryComboBox.setOnAction(e -> {
            Category selectedCategory = categoryComboBox.getValue();
            if (selectedCategory != null) {
                List<Subcategory> subcategories = selectedCategory.getSubcategories();
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

        Button addRecordButton = new Button("Додати запис");
        addRecordButton.setOnAction(e -> {
            double amount = Double.parseDouble(amountField.getText());
            Category category = categoryComboBox.getValue();
            String subcategory = subcategoryComboBox.getValue();
            LocalDate selectedDate = datePicker.getValue();
            String selectedTime = timeField.getText(); // Получаем значение времени из TextField
            LocalDateTime dateTime = selectedDate.atTime(LocalTime.parse(selectedTime));
            addTransaction(amount, category, subcategory, dateTime);
        });

        addRecordButton.setStyle("-fx-background-color: green; -fx-text-fill: white;");
        addRecordButton.setMaxWidth(Double.MAX_VALUE);



        transactionInputBox.getChildren().addAll(transactionLabel, amountField, datePicker, timeField, categoryComboBox, subcategoryComboBox, addRecordButton);
        transactionInputBox.setAlignment(Pos.CENTER);
        root.setLeft(transactionInputBox);

        balanceTableView = createBalanceTableView();
        balanceTableView.setMaxHeight(Double.MAX_VALUE);
        balanceTableView.setPlaceholder(new Label("Таблиця балансів відсутня"));

        VBox balanceTableBox = new VBox(10);
        balanceTableBox.setPadding(new Insets(20));
        Label balanceTableLabel = new Label("Таблиця балансів");
        balanceTableLabel.setStyle("-fx-font-size: 18px;");
        balanceTableBox.getChildren().addAll(balanceTableLabel, balanceTableView);
        root.setCenter(balanceTableBox);

        transactionHistoryTableView = createTransactionHistoryTableView();
        transactionHistoryTableView.setMaxHeight(Double.MAX_VALUE);
        transactionHistoryTableView.setPlaceholder(new Label("Історія транзакцій відсутня"));

        VBox transactionHistoryBox = new VBox(10);
        transactionHistoryBox.setPadding(new Insets(20));
        Label transactionHistoryLabel = new Label("Історія транзакцій");
        transactionHistoryLabel.setStyle("-fx-font-size: 18px;");
        transactionHistoryBox.getChildren().addAll(transactionHistoryLabel, transactionHistoryTableView);
        root.setRight(transactionHistoryBox);

        pieChart = new PieChart();
        pieChart.setTitle("Розподіл витрат за категоріями");
        root.setBottom(pieChart);

        primaryStage.setOnCloseRequest(e -> {
            try {
                saveDataToFile();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        Scene scene = new Scene(root, 1200, 768);
        primaryStage.setScene(scene);
        primaryStage.show();

        try {
            loadDataFromFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadDataFromFile() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(currentUser + "_transactions.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Разбиваем строку на части, чтобы получить информацию о транзакции
                String[] parts = line.split(" - ");
                if (parts.length == 5) {
                    String dateTimeStr = parts[0];
                    String category = parts[1].split(": ")[1];
                    String subcategory = parts[2].split(": ")[1];
                    String amountStr = parts[3].split(": ")[1];
                    String description = parts[4].split(": ")[1];

                    // Преобразуем строку с датой и временем в LocalDateTime
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
                    LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, formatter);

                    // Преобразуем строку с суммой в double
                    double amount = Double.parseDouble(amountStr);

                    // Создаем объект транзакции и добавляем его в таблицу и обновляем баланс
                    Transaction transaction = new Transaction(dateTime, category, subcategory, amount, description);
                    transactionHistoryTableView.getItems().add(transaction);
                    if ("Витрата".equals(category)) {
                        balance.set(balance.get() - amount);
                    } else if ("Прибуток".equals(category)) {
                        balance.set(balance.get() + amount);
                    }
                    updatePieChart();
                    updateBalanceTable();
                }
            }
        }
    }

    private void saveDataToFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentUser + "_transactions.txt"))) {
            for (Transaction transaction : transactionHistoryTableView.getItems()) {
                String dateTimeStr = transaction.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                String category = transaction.getCategory();
                String subcategory = transaction.getSubcategory();
                String amountStr = String.valueOf(transaction.getAmount());
                String description = transaction.getDescription();

                String transactionInfo = dateTimeStr + " - Категорія: " + category + " - Підкатегорія: " + subcategory + ", Сума: " + amountStr + ", Опис: " + description;
                writer.write(transactionInfo);
                writer.newLine();
            }
        }
    }

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

    private String getUserInfo() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Авторизація");
        dialog.setHeaderText("Будь ласка, введіть ваше ім'я та прізвище:");
        dialog.setContentText("Ім'я та прізвище:");

        String result = "";
        while (result.isEmpty()) {
            Optional<String> userInput = dialog.showAndWait();
            if (userInput.isPresent()) {
                result = userInput.get();
            } else {
                System.exit(0);
            }
        }

        return result;
    }

    private void addTransaction(double amount, Category category, String subcategory, LocalDateTime dateTime) {
        if (amount != 0) {
            String operationType = category.getName();
            TextInputDialog descriptionDialog = new TextInputDialog();
            descriptionDialog.setTitle("Додавання запису");
            descriptionDialog.setHeaderText("Будь ласка, введіть опис операції (" + operationType + "):");
            descriptionDialog.setContentText("Опис:");

            Optional<String> descriptionResult = descriptionDialog.showAndWait();
            if (descriptionResult.isPresent()) {
                String description = descriptionResult.get();

                String transactionInfo = dateTime.toString() + " - Категорія: " + category.getName() + " - Підкатегорія: " + subcategory + ", Сума: " + amount + ", Опис: " + description;
                Transaction transaction = new Transaction(dateTime, category.getName(), subcategory, amount, description);
                transactionHistoryTableView.getItems().add(transaction);
                if ("Витрата".equals(operationType)) {
                    balance.set(balance.get() - amount);
                } else if ("Прибуток".equals(operationType)) {
                    balance.set(balance.get() + amount);
                }
                updatePieChart();
                saveTransactionToFile(currentUser, transactionInfo);

                // Добавьте эту строку для обновления таблицы балансов
                updateBalanceTable();
            }
        }
    }



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

    private String getFormattedDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return sdf.format(new Date());
    }

    private void saveTransactionToFile(String user, String transactionInfo) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(user + "_transactions.txt", true))) {
            writer.write(transactionInfo);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
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

        TableColumn<Transaction, LocalDateTime> dateColumn = new TableColumn<>("Дата та час");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        dateColumn.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
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

    public static class Transaction {
        private final LocalDateTime dateTime;
        private final String category;
        private final String subcategory;
        private final double amount;
        private final String description;

        public Transaction(LocalDateTime dateTime, String category, String subcategory, double amount, String description) {
            this.dateTime = dateTime;
            this.category = category;
            this.subcategory = subcategory;
            this.amount = amount;
            this.description = description;
        }

        public LocalDateTime getDateTime() {
            return dateTime;
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
}