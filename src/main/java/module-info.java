module com.example.vaznisimulacia {
    requires javafx.controls;
    requires javafx.fxml;


    // Toto povolí JavaFX prístup k tvojmu novému GUI balíčku
    opens GUI to javafx.fxml, javafx.graphics;

    // Toto sprístupní balíčky pre zvyšok aplikácie
    exports GUI;
    exports logic;
}