module com.dniprotech.financialaccounting {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;


    opens com.dniprotech.financialaccounting to javafx.fxml;
    exports com.dniprotech.financialaccounting;
}