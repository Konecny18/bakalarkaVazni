module com.example.vaznisimulacia {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.vaznisimulacia to javafx.fxml;
    exports com.example.vaznisimulacia;
}