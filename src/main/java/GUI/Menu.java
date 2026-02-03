package GUI; // Pozor na správny názov balíčka

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Menu extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Načítanie FXML súboru
        FXMLLoader fxmlLoader = new FXMLLoader(Menu.class.getResource("/menu-view.fxml")); // Cesta k FXML súboru
        Scene scene = new Scene(fxmlLoader.load(), 400, 300);

        stage.setTitle("Bakalárska práca - Menu");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}