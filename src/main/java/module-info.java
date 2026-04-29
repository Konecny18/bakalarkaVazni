module vazniSimulacia {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens GUI to javafx.fxml;
    exports GUI;
}