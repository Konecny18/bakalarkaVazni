module vazni.simulacia {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.logging;

    opens GUI to javafx.fxml;
    exports GUI;
}