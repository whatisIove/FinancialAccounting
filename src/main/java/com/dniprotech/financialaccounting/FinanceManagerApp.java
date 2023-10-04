package com.dniprotech.financialaccounting;

import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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

        HBox userInfoBox = new HBox(10);
        Label userInfoLabel = new Label("Користувач: " + currentUser);
        Label balanceLabel = new Label("Баланс: ");
        Label balanceValueLabel = new Label();
        balance = new SimpleDoubleProperty(0.0);
        balanceValueLabel.textProperty().bind(balance.asString());

        userInfoBox.getChildren().addAll(userInfoLabel, balanceLabel, balanceValueLabel);
        root.setTop(userInfoBox);

        VBox transactionInputBox = new VBox(10);
        Label transactionLabel = new Label("Додавання операції");
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
        categoryComboBox.setItems(FXCollections.observableArrayList(categories));

        ComboBox<Subcategory> subcategoryComboBox = new ComboBox<>();
        subcategoryComboBox.setPromptText("Виберіть підкатегорію");

        categoryComboBox.setOnAction(e -> {
            Category selectedCategory = categoryComboBox.getValue();
            if (selectedCategory != null) {
                List<Subcategory> subcategories = categorySubcategoriesMap.get(selectedCategory);
                subcategoryComboBox.setItems(FXCollections.observableArrayList(subcategories));
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
            Subcategory subcategory = subcategoryComboBox.getValue();
            addTransaction(amount, category, subcategory);
        });

        addRecordButton.setStyle("-fx-background-color: green; -fx-text-fill: white;");

        transactionInputBox.getChildren().addAll(transactionLabel, amountField, categoryComboBox, subcategoryComboBox, addRecordButton);
        transactionInputBox.setAlignment(Pos.CENTER);
        root.setBottom(transactionInputBox);

        transactionHistoryTableView = createTransactionHistoryTableView();
        root.setCenter(transactionHistoryTableView);

        pieChart = new PieChart();
        pieChart.setLegendVisible(false);
        pieChart.setPrefSize(300, 300);

        VBox rightVBox = new VBox(10);
        rightVBox.getChildren().add(pieChart);
        root.setRight(rightVBox);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
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

    private void addTransaction(double amount, Category category, Subcategory subcategory) {
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
                String transactionInfo = dateTime + " - Категорія: " + category.getName() + " - Підкатегорія: " + subcategory.getName() + ", Сума: " + amount + ", Опис: " + description;
                Transaction transaction = new Transaction(dateTime, category.getName(), subcategory.getName(), amount, description);
                transactionHistoryTableView.getItems().add(transaction);
                if ("Витрата".equals(operationType)) {
                    balance.set(balance.get() - amount);
                } else {
                    balance.set(balance.get() + amount);
                }
                updatePieChart();
                saveTransactionToFile(currentUser, transactionInfo);
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
        Map<String, Double> categoryAmountMap = new HashMap<>();
        for (Transaction transaction : transactionHistoryTableView.getItems()) {
            String category = transaction.getCategory();
            Double amount = transaction.getAmount();
            if (categoryAmountMap.containsKey(category)) {
                categoryAmountMap.put(category, categoryAmountMap.get(category) + amount);
            } else {
                categoryAmountMap.put(category, amount);
            }
        }

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : categoryAmountMap.entrySet()) {
            String category = entry.getKey();
            double amount = entry.getValue();
            pieChartData.add(new PieChart.Data(category, Math.abs(amount)));
        }

        pieChart.setData(pieChartData);
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

        @Override
        public String toString() {
            return name;
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

        @Override
        public String toString() {
            return name;
        }
    }
}
