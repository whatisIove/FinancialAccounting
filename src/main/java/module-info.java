module com.dniprotech.financialaccounting {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;
    requires com.google.gson;

    opens com.dniprotech.financialaccounting to javafx.fxml, com.google.gson;
    exports com.dniprotech.financialaccounting;
}

