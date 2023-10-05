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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FinanceManagerApp extends Application {
    private Stage primaryStage;
    private BorderPane root;
    private String currentUser;
    private SimpleDoubleProperty balance;
    private ObservableList<Category> categories;
    private Map<Category, List<Subcategory>> categorySubcategoriesMap;
    private PieChart pieChart;
    private TableView<Transaction> transactionHistoryTableView;
    private TableView<BalanceCategory> balanceTableView;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Управление финансами");

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
            addTransaction(amount, category, subcategory);
        });

        addRecordButton.setStyle("-fx-background-color: green; -fx-text-fill: white;");
        addRecordButton.setMaxWidth(Double.MAX_VALUE);

        transactionInputBox.getChildren().addAll(transactionLabel, amountField, categoryComboBox, subcategoryComboBox, addRecordButton);
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
        transactionHistoryTableView.setPlaceholder(new Label("Історія операцій відсутня"));

        VBox transactionHistoryBox = new VBox(10);
        transactionHistoryBox.setPadding(new Insets(20));
        Label transactionHistoryLabel = new Label("Історія операцій");
        transactionHistoryLabel.setStyle("-fx-font-size: 18px;");
        transactionHistoryBox.getChildren().addAll(transactionHistoryLabel, transactionHistoryTableView);
        root.setBottom(transactionHistoryBox);

        pieChart = new PieChart();
        pieChart.setLegendVisible(false);
        pieChart.setPrefSize(400, 400);
        pieChart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        VBox pieChartBox = new VBox(10);
        pieChartBox.setPadding(new Insets(20));
        Label pieChartLabel = new Label("Діаграма розподілу");
        pieChartLabel.setStyle("-fx-font-size: 18px;");
        pieChartBox.getChildren().addAll(pieChartLabel, pieChart);
        root.setRight(pieChartBox);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
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

    private void addTransaction(double amount, Category category, String subcategory) {
        if (amount != 0) {
            String operationType = category.getName();
            TextInputDialog descriptionDialog = new TextInputDialog();
            descriptionDialog.setTitle("Додавання запису");
            descriptionDialog.setHeaderText("Будь ласка, введіть опис операції (" + operationType + "):");
            descriptionDialog.setContentText("Опис:");

            Optional<String> descriptionResult = descriptionDialog.showAndWait();
            if (descriptionResult.isPresent()) {
                String description = descriptionResult.get();
                String dateTime = getFormattedDateTime();
                String transactionInfo = dateTime + " - Категорія: " + category.getName() + " - Підкатегорія: " + subcategory + ", Сума: " + amount + ", Опис: " + description;
                Transaction transaction = new Transaction(dateTime, category.getName(), subcategory, amount, description);
                transactionHistoryTableView.getItems().add(transaction);
                if ("Витрата".equals(operationType)) {
                    balance.set(balance.get() - amount);
                } else if ("Прибуток".equals(operationType)) {
                    balance.set(balance.get() + amount);
                }
                updatePieChart();
                saveTransactionToFile(currentUser, transactionInfo);
                updateBalanceTable();
            }
        }
    }

    private String getFormattedDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return sdf.format(new Date());
    }

    private void saveTransactionToFile(String user, String transactionInfo) {
        try (FileWriter writer = new FileWriter(user + "_transactions.txt", true);
             BufferedWriter bw = new BufferedWriter(writer)) {

            bw.write(transactionInfo);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TableView<Transaction> createTransactionHistoryTableView() {
        TableView<Transaction> tableView = new TableView<>();

        TableColumn<Transaction, String> dateTimeColumn = new TableColumn<>("Дата/Час");
        dateTimeColumn.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        dateTimeColumn.setMinWidth(150);

        TableColumn<Transaction, String> categoryColumn = new TableColumn<>("Категорія");
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryColumn.setMinWidth(100);

        TableColumn<Transaction, String> subcategoryColumn = new TableColumn<>("Підкатегорія");
        subcategoryColumn.setCellValueFactory(new PropertyValueFactory<>("subcategory"));
        subcategoryColumn.setMinWidth(140);

        TableColumn<Transaction, Double> amountColumn = new TableColumn<>("Сума");
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountColumn.setMinWidth(20);

        TableColumn<Transaction, String> descriptionColumn = new TableColumn<>("Опис");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setMinWidth(200);

        tableView.getColumns().addAll(dateTimeColumn, categoryColumn, subcategoryColumn, amountColumn, descriptionColumn);

        return tableView;
    }

    private void updatePieChart() {
        Map<String, BalanceCategory> balanceCategoryMap = new HashMap<>();
        double totalBalance = 0.0;

        for (Transaction transaction : transactionHistoryTableView.getItems()) {
            String category = transaction.getCategory();
            String subcategory = transaction.getSubcategory();
            Double amount = transaction.getAmount();

            balanceCategoryMap.computeIfAbsent(category, BalanceCategory::new)
                    .getSubcategoryBalances()
                    .merge(subcategory, amount, Double::sum);

            totalBalance += amount;
        }

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (BalanceCategory balanceCategory : balanceCategoryMap.values()) {
            String category = balanceCategory.getCategory();
            Map<String, Double> subcategoryBalances = balanceCategory.getSubcategoryBalances();

            for (Map.Entry<String, Double> subcategoryEntry : subcategoryBalances.entrySet()) {
                String subcategory = subcategoryEntry.getKey();
                double balance = subcategoryEntry.getValue();

                pieChartData.add(new PieChart.Data(subcategory, Math.abs(balance)));
            }
        }

        pieChart.setData(pieChartData);
    }

    private TableView<BalanceCategory> createBalanceTableView() {
        TableView<BalanceCategory> tableView = new TableView<>();

        TableColumn<BalanceCategory, String> categoryColumn = new TableColumn<>("Категорія");
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryColumn.setMinWidth(150);

        TableColumn<BalanceCategory, Double> incomeColumn = new TableColumn<>("Общий доход");
        incomeColumn.setCellValueFactory(cellData -> {
            double totalIncome = calculateTotalIncome();
            return new SimpleDoubleProperty(totalIncome).asObject();
        });
        incomeColumn.setMinWidth(100);

        TableColumn<BalanceCategory, Double> expenseColumn = new TableColumn<>("Общие расходы");
        expenseColumn.setCellValueFactory(cellData -> {
            double totalExpense = calculateTotalExpense();
            return new SimpleDoubleProperty(totalExpense).asObject();
        });
        expenseColumn.setMinWidth(100);

        TableColumn<BalanceCategory, Double> balanceColumn = new TableColumn<>("Баланс");
        balanceColumn.setCellValueFactory(cellData -> {
            double totalBalance = calculateTotalBalance();
            return new SimpleDoubleProperty(totalBalance).asObject();
        });
        balanceColumn.setMinWidth(100);

        tableView.getColumns().addAll(categoryColumn, incomeColumn, expenseColumn, balanceColumn);

        return tableView;
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

    private double calculateTotalBalance() {
        return calculateTotalIncome() - calculateTotalExpense();
    }

    public static class Transaction {
        private final String dateTime;
        private final String category;
        private final String subcategory;
        private final Double amount;
        private final String description;

        public Transaction(String dateTime, String category, String subcategory, Double amount, String description) {
            this.dateTime = dateTime;
            this.category = category;
            this.subcategory = subcategory;
            this.amount = amount;
            this.description = description;
        }

        public String getDateTime() {
            return dateTime;
        }

        public String getCategory() {
            return category;
        }

        public String getSubcategory() {
            return subcategory;
        }

        public Double getAmount() {
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
        private final String category;
        private final Map<String, Double> subcategoryBalances;
        private double totalIncome;
        private double totalExpense;
        private double totalBalance;

        public BalanceCategory(String category) {
            this.category = category;
            this.subcategoryBalances = new HashMap<>();
        }

        public String getCategory() {
            return category;
        }

        public Map<String, Double> getSubcategoryBalances() {
            return subcategoryBalances;
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