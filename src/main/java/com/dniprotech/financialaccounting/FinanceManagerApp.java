package com.dniprotech.financialaccounting;

import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.*;
import java.text.SimpleDateFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
    private TableView<Transaction> incomeCategoryTableView;
    private TableView<Transaction> expenseCategoryTableView;

    private LineChart<String, Number> lineChart;
    private BarChart<String, Number> barChart;
    private ScatterChart<String, Number> scatterChart;
    private TextField timeField; // Добавляем TextField для ввода времени
    private Label incomeTotalLabel = new Label();
    private Label expenseTotalLabel = new Label();


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Управління фінансами");
        String userName = getUserInfo();
        currentUser = userName;

        // Определяем имя файла данных пользователя на основе введенного имени
        String userDataFileName = userName + "_transactions.txt";
        System.out.println("Имя файла данных пользователя: " + userDataFileName);

        // Загружаем данные пользователя, если файл существует
        File userDataFile = new File(userDataFileName);
        if (userDataFile.exists()) {
            try {
                loadDataFromFile(userDataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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

        Button openChartsButton = new Button("Аналітичні інструменти");
        openChartsButton.setOnAction(e -> openChartsWindow());

        HBox buttonsBox = new HBox(10); // 10 - это отступ между кнопками
        buttonsBox.setAlignment(Pos.CENTER); // Выравнивание по центру
        buttonsBox.getChildren().addAll(addRecordButton, openChartsButton);

        transactionInputBox.getChildren().addAll(transactionLabel, amountField, datePicker, timeField,
                categoryComboBox, subcategoryComboBox, buttonsBox); // Добавляем HBox с кнопками
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


        primaryStage.setOnCloseRequest(e -> {
            try {
                File userFile = new File("userdata", currentUser + "_transactions.txt");
                saveDataToFile(userFile);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        Scene scene = new Scene(root, 1200, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void createAndConfigurePieChart() {
        pieChart = new PieChart();
        pieChart.setTitle("Розподіл за категоріями");
        updatePieChart();
    }


    private void createAndConfigureLineChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Графік балансу");

        // Configure the x-axis and y-axis as needed
        xAxis.setLabel("Дата");
        yAxis.setLabel("Баланс");

        // You can also initialize the lineChart data or configure it as needed
        updateLineChart(); // You can configure the lineChart data in the updateLineChart method
    }

    private void createAndConfigureBarChart() {
        barChart = createBarChart();

        // Настройте barChart по желанию, установив заголовок и метки осей
        barChart.setTitle("Графiк балансy");
        barChart.getXAxis().setLabel("Дата");
        barChart.getYAxis().setLabel("Баланс");

        updateBarChart(barChart);
    }

    private void createAndConfigureScatterChart() {
        scatterChart = createScatterChart();

        scatterChart.setTitle("Точкова діаграма");
        scatterChart.getXAxis().setLabel("Підкатегорії");
        scatterChart.getYAxis().setLabel("Прибуток та витрати");
        scatterChart.setLegendVisible(false);
        updateScatterChart(scatterChart);
    }

    private void createAndConfigureIncomeCategoryTableView() {
        incomeCategoryTableView = createIncomeCategoryTableView();
        updateIncomeCategoryTable(); // Метод для обновления данных в таблице прибыли
    }

    private void createAndConfigureExpenseCategoryTableView() {
        expenseCategoryTableView = createExpenseCategoryTableView();
        updateExpenseCategoryTable(); // Метод для обновления данных в таблице расходов
    }

    private void openChartsWindow() {
        // Create a new Stage for the analytical tools window
        Stage chartsStage = new Stage();
        chartsStage.setTitle("Аналітичні інструменти");

        // Create a BorderPane as the main layout
        BorderPane mainLayout = new BorderPane();

        // Create VBox for the charts
        VBox chartBox = new VBox(20);
        chartBox.setAlignment(Pos.CENTER);

        // Create a ComboBox for chart and table selection
        ComboBox<String> chartSelector = new ComboBox<>();
        chartSelector.setPromptText("Оберіть тип");
        ObservableList<String> chartOptions = FXCollections.observableArrayList(
                "Pie Chart", "Line Chart", "Bar Chart", "Scatter Chart", "Income Table", "Expense Table");
        chartSelector.setItems(chartOptions);

        // Add an event handler for the ComboBox selection
        chartSelector.setOnAction(event -> {
            String selectedOption = chartSelector.getValue();
            chartBox.getChildren().clear(); // Clear existing chart or table

            if ("Pie Chart".equals(selectedOption)) {
                createAndConfigurePieChart();
                chartBox.getChildren().add(pieChart);
            } else if ("Line Chart".equals(selectedOption)) {
                createAndConfigureLineChart();
                chartBox.getChildren().add(lineChart);
            } else if ("Bar Chart".equals(selectedOption)) {
                createAndConfigureBarChart();
                chartBox.getChildren().add(barChart);
            } else if ("Scatter Chart".equals(selectedOption)) {
                createAndConfigureScatterChart();
                chartBox.getChildren().add(scatterChart);
            } else if ("Income Table".equals(selectedOption)) {
                createAndConfigureIncomeCategoryTableView();
                Label incomeLabel = new Label("Таблиця прибутку"); // Создаем лейбл для таблицы прибыли
                chartBox.getChildren().add(incomeLabel); // Добавляем лейбл в chartBox
                incomeLabel.setStyle("-fx-font-size: 18;");
                chartBox.getChildren().add(incomeCategoryTableView); // Добавляем таблицу в chartBox
                incomeTotalLabel.setStyle("-fx-font-size: 16;");
                chartBox.getChildren().add(incomeTotalLabel); // Добавляем лейбл для прибыли
            } else if ("Expense Table".equals(selectedOption)) {
                createAndConfigureExpenseCategoryTableView();
                Label expenseLabel = new Label("Таблиця витрат"); // Создаем лейбл для таблицы расходов
                expenseLabel.setStyle("-fx-font-size: 18;");
                chartBox.getChildren().add(expenseLabel); // Добавляем лейбл в chartBox
                chartBox.getChildren().add(expenseCategoryTableView); // Добавляем таблицу в chartBox
                expenseTotalLabel.setStyle("-fx-font-size: 16;");
                chartBox.getChildren().add(expenseTotalLabel); // Добавляем лейбл для расходов

        }
        });

        // Add chart containers to the main layout
        mainLayout.setTop(chartSelector);
        mainLayout.setCenter(chartBox);

        // Create a scene for the analytical tools window
        Scene chartsScene = new Scene(mainLayout, 800, 600);

        // Set the scene for the window
        chartsStage.setScene(chartsScene);

        // Open the window
        chartsStage.show();
    }

    private Transaction createTransactionFromRecord(TransactionRecord transactionRecord) {
        LocalDate date = transactionRecord.getDate();
        LocalTime time = transactionRecord.getTime();
        LocalDateTime dateTime = LocalDateTime.of(date, time);
        String category = transactionRecord.getCategory();
        String subcategory = transactionRecord.getSubcategory();
        double amount = transactionRecord.getAmount();
        String description = transactionRecord.getDescription();
        return new Transaction(dateTime, category, subcategory, amount, description);
    }

    private TableView<Transaction> createIncomeCategoryTableView() {
        TableView<Transaction> incomeCategoryTableView = new TableView<>();
        incomeCategoryTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Transaction, LocalDateTime> incomeDateColumn = new TableColumn<>("Дата та час");
        incomeDateColumn.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        incomeDateColumn.setCellFactory(column -> new TableCell<>() {
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

        TableColumn<Transaction, String> incomeCategoryColumn = new TableColumn<>("Категорія");
        incomeCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Transaction, String> incomeSubcategoryColumn = new TableColumn<>("Підкатегорія");
        incomeSubcategoryColumn.setCellValueFactory(new PropertyValueFactory<>("subcategory"));

        TableColumn<Transaction, Double> incomeAmountColumn = new TableColumn<>("Сума");
        incomeAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        incomeAmountColumn.setCellFactory(tc -> new TableCell<>() {
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

        incomeCategoryTableView.getColumns().addAll(incomeDateColumn, incomeCategoryColumn, incomeSubcategoryColumn, incomeAmountColumn);

        return incomeCategoryTableView;
    }

    private TableView<Transaction> createExpenseCategoryTableView() {
        TableView<Transaction> expenseCategoryTableView = new TableView<>();
        expenseCategoryTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Transaction, LocalDateTime> expenseDateColumn = new TableColumn<>("Дата та час");
        expenseDateColumn.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        expenseDateColumn.setCellFactory(column -> new TableCell<>() {
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

        TableColumn<Transaction, String> expenseCategoryColumn = new TableColumn<>("Категорія");
        expenseCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Transaction, String> expenseSubcategoryColumn = new TableColumn<>("Підкатегорія");
        expenseSubcategoryColumn.setCellValueFactory(new PropertyValueFactory<>("subcategory"));

        TableColumn<Transaction, Double> expenseAmountColumn = new TableColumn<>("Сума");
        expenseAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        expenseAmountColumn.setCellFactory(tc -> new TableCell<>() {
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

        expenseCategoryTableView.getColumns().addAll(expenseDateColumn, expenseCategoryColumn, expenseSubcategoryColumn, expenseAmountColumn);

        return expenseCategoryTableView;
    }

    private void updateIncomeCategoryTable() {
        List<Transaction> transactions = transactionHistoryTableView.getItems();
        List<Transaction> incomeTransactions = transactions.stream()
                .filter(transaction -> "Прибуток".equals(transaction.getCategory()))
                .collect(Collectors.toList());

        incomeCategoryTableView.getItems().clear();
        incomeCategoryTableView.getItems().addAll(incomeTransactions);

        double totalIncome = incomeTransactions.stream().mapToDouble(Transaction::getAmount).sum();
        updateTotalLabel(incomeTotalLabel, totalIncome);
    }

    private void updateExpenseCategoryTable() {
        List<Transaction> transactions = transactionHistoryTableView.getItems();
        List<Transaction> expenseTransactions = transactions.stream()
                .filter(transaction -> "Витрата".equals(transaction.getCategory()))
                .collect(Collectors.toList());

        expenseCategoryTableView.getItems().clear();
        expenseCategoryTableView.getItems().addAll(expenseTransactions);

        double totalExpense = expenseTransactions.stream().mapToDouble(Transaction::getAmount).sum();
        updateTotalLabel(expenseTotalLabel, totalExpense);
    }


    private void updateTotalLabel(Label totalLabel, double totalAmount) {
        totalLabel.setText(String.format("Загальна сума: %.2f", totalAmount));
    }

    public ScatterChart<String, Number> createScatterChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        ScatterChart<String, Number> scatterChart = new ScatterChart<>(xAxis, yAxis);
        scatterChart.setTitle("Scatter Chart");

        updateScatterChart(scatterChart);

        return scatterChart;
    }

    public void updateScatterChart(ScatterChart<String, Number> scatterChart) {
        scatterChart.getData().clear();

        List<Transaction> transactions = transactionHistoryTableView.getItems();

        // Создайте карту для отображения категорий/подкатегорий и соответствующих цветов
        Map<String, Paint> categoryColors = new HashMap<>();

        // Создайте карту для суммирования данных по подкатегориям
        Map<String, Double> subcategorySums = new HashMap<>();

        for (Transaction transaction : transactions) {
            String category = transaction.getCategory();
            String subcategory = transaction.getSubcategory();
            double totalAmount = transaction.getIncome() - transaction.getExpense();

            // Определите цвет для категории или подкатегории, если его еще нет
            if (!categoryColors.containsKey(subcategory)) {
                categoryColors.put(subcategory, getRandomColor());
            }

            // Суммируйте данные по подкатегории
            if (subcategorySums.containsKey(subcategory)) {
                subcategorySums.put(subcategory, subcategorySums.get(subcategory) + totalAmount);
            } else {
                subcategorySums.put(subcategory, totalAmount);
            }
        }

        // Создайте серии данных на основе сумм подкатегорий
        for (Map.Entry<String, Double> entry : subcategorySums.entrySet()) {
            String subcategory = entry.getKey();
            double totalAmount = entry.getValue();

            // Создайте серию данных для каждой подкатегории
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(subcategory);

            // Создайте точку с соответствующим цветом для подкатегории
            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(subcategory, totalAmount);
            dataPoint.setNode(createDataPointNode(categoryColors.get(subcategory)));

            series.getData().add(dataPoint);

            scatterChart.getData().add(series);
        }
    }


    private Node createDataPointNode(Paint color) {
        Circle circle = new Circle(5); // Размер точки
        circle.setFill(color); // Установка цвета точки
        return circle;
    }

    private Paint getRandomColor() {
        Random rand = new Random();
        return Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
    }


    private PieChart createPieChart() {
        Map<String, Double> categoryBalances = new HashMap<>();
        double totalBalance = 0.0;

        for (Transaction transaction : transactionHistoryTableView.getItems()) {
            String category = transaction.getCategory();
            Double amount = transaction.getAmount();
            categoryBalances.merge(category, amount, Double::sum);
            totalBalance += amount;
        }

        PieChart chart = new PieChart();

        for (Map.Entry<String, Double> categoryEntry : categoryBalances.entrySet()) {
            String category = categoryEntry.getKey();
            double balance = categoryEntry.getValue();
            double percentage = (balance / totalBalance) * 100;
            String categoryLabel = category + " (" + String.format("%.2f%%", percentage) + ")";
            PieChart.Data categoryData = new PieChart.Data(categoryLabel, Math.abs(percentage)); // Изменено в этой строке
            chart.getData().add(categoryData);
        }
        return chart;
    }

    private BarChart<String, Number> createBarChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Суммарні витрати та прибутки");

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();

        expenseSeries.setName("Витрати");
        incomeSeries.setName("Прибуток");

        // Добавьте данные в expenseSeries и incomeSeries на основе вашей транзакционной истории
        for (Transaction transaction : transactionHistoryTableView.getItems()) {
            String date = transaction.getDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            double amount = transaction.getAmount();
            String category = transaction.getCategory();

            if ("Витрата".equals(category)) {
                expenseSeries.getData().add(new XYChart.Data<>(date, amount));
            } else if ("Прибуток".equals(category)) {
                incomeSeries.getData().add(new XYChart.Data<>(date, amount));
            }
        }

        chart.getData().addAll(expenseSeries, incomeSeries);

        return chart;
    }

    private void updateBarChart(BarChart<String, Number> barChart) {
        barChart.getData().clear(); // Очищаем существующие данные

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();

        expenseSeries.setName("Витрати");
        incomeSeries.setName("Прибуток");

        // Добавьте данные в expenseSeries и incomeSeries на основе вашей транзакционной истории
        for (Transaction transaction : transactionHistoryTableView.getItems()) {
            String date = transaction.getDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            double amount = transaction.getAmount();
            String category = transaction.getCategory();

            if ("Витрата".equals(category)) {
                expenseSeries.getData().add(new XYChart.Data<>(date, amount));
            } else if ("Прибуток".equals(category)) {
                incomeSeries.getData().add(new XYChart.Data<>(date, amount));
            }
        }

        barChart.getData().addAll(expenseSeries, incomeSeries); // Добавляем новые данные
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
                currentUser = result;

                File userFile = new File("userdata", currentUser + "_transactions.txt");
                if (userFile.exists()) {
                    try {
                        loadDataFromFile(userFile); // Load the data into tables
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Файл не найден для пользователя: " + currentUser);
                }
            } else {
                System.exit(0);
            }
        }
        return result;
    }

    private void loadDataFromFile(File userFile) throws IOException {
        if (userFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(userFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    TransactionRecord transactionRecord = TransactionRecord.fromString(line);
                    if (transactionRecord != null) {
                        Transaction transaction = createTransactionFromRecord(transactionRecord);
                        transactionHistoryTableView.getItems().add(transaction);
                    }
                }
            }
        } else {
            System.out.println("Файл не найден для пользователя: " + currentUser);
        }
    }

    private LineChart<String, Number> createLineChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Дата");
        yAxis.setLabel("Баланс");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Зміна балансу з часом");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Баланс");

        // Add data points to the series based on your transaction history
        for (Transaction transaction : transactionHistoryTableView.getItems()) {
            String date = transaction.getDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            double balanceChange = transaction.getAmount();
            series.getData().add(new XYChart.Data<>(date, balanceChange));
        }

        chart.getData().add(series);

        return chart;
    }

    private void updateLineChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Баланс");

        double cumulativeBalance = 0.0;

        for (Transaction transaction : transactionHistoryTableView.getItems()) {
            String date = transaction.getDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            double balanceChange = transaction.getAmount();
            String category = transaction.getCategory();

            // Adjust the cumulative balance based on the transaction category
            if ("Витрата".equals(category)) {
                cumulativeBalance -= balanceChange;
            } else if ("Прибуток".equals(category)) {
                cumulativeBalance += balanceChange;
            }

            series.getData().add(new XYChart.Data<>(date, cumulativeBalance));
        }

        lineChart.getData().setAll(series);  // Set the new series
        lineChart.layout();  // Force a layout update
    }


    private void saveDataToFile(File userFile) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(userFile, true))) {
            for (Transaction transaction : transactionHistoryTableView.getItems()) {
                writer.write(transaction.toRecordString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addTransaction(double amount, Category category, String subcategory, LocalDateTime dateTime) {
        if (amount != 0 && category != null) {
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

                if ("Витрата".equals(operationType) && balance != null) {
                    balance.set(balance.get() - amount);
                } else if ("Прибуток".equals(operationType) && balance != null) {
                    balance.set(balance.get() + amount);
                }

                updatePieChart();

                updateLineChart();
                updateBarChart(barChart); // Обновляем столбчатую диаграмму
                updateBalanceTable();
                updateScatterChart(scatterChart);
                saveTransactionToFile(currentUser, transactionInfo);
                // Дополнительные обновления для таблиц категорий
                if ("Прибуток".equals(operationType)) {
                    updateIncomeCategoryTable();
                } else if ("Витрата".equals(operationType)) {
                    updateExpenseCategoryTable();
                }
            }
        }
    }

    private void updatePieChart() {
        Map<String, Double> categoryBalances = new HashMap<>();
        double totalBalance = 0.0;

        for (Transaction transaction : transactionHistoryTableView.getItems()) {
            String category = transaction.getCategory();
            Double amount = transaction.getAmount();
            categoryBalances.merge(category, amount, Double::sum);
            totalBalance += amount;
        }

        pieChart.getData().clear();

        for (Map.Entry<String, Double> categoryEntry : categoryBalances.entrySet()) {
            String category = categoryEntry.getKey();
            double balance = categoryEntry.getValue();
            double percentage = (balance / totalBalance) * 100;
            String categoryLabel = category + " (" + String.format("%.2f%%", percentage) + ")";
            PieChart.Data categoryData = new PieChart.Data(categoryLabel, Math.abs(percentage));
            pieChart.getData().add(categoryData);
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

        public double getIncome() {
            return amount > 0 ? amount : 0;
        }

        public double getExpense() {
            return amount < 0 ? -amount : 0;
        }


        public String toRecordString() {
            String formattedDateTime = dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
            return formattedDateTime + " - Категорія: " + category + " - Підкатегорія: " + subcategory + ", Сума: "
                    + amount + ", Опис: " + description;
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

    public class CategoryData {
        private String category;
        private List<SubcategoryData> subcategories;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public List<SubcategoryData> getSubcategories() {
            return subcategories;
        }

        public void setSubcategories(List<SubcategoryData> subcategories) {
            this.subcategories = subcategories;
        }
    }

    public class SubcategoryData {
        private String subcategory;
        private List<Double> incomes;
        private List<Double> expenses;

        public String getSubcategory() {
            return subcategory;
        }

        public void setSubcategory(String subcategory) {
            this.subcategory = subcategory;
        }

        public List<Double> getIncomes() {
            return incomes;
        }

        public void setIncomes(List<Double> incomes) {
            this.incomes = incomes;
        }

        public List<Double> getExpenses() {
            return expenses;
        }

        public void setExpenses(List<Double> expenses) {
            this.expenses = expenses;
        }
    }


}