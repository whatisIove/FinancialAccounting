module com.dniprotech.financialaccounting {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.dniprotech.financialaccounting to javafx.fxml;
    exports com.dniprotech.financialaccounting;
}