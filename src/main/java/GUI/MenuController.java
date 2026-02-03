package GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class MenuController {
    @FXML
    private Button btnSimulacia; // Toto musí mať rovnaké fx:id ako v FXML

    @FXML
    private Button btnVysvetlenie; // A toto tiež

    @FXML
    public void onSimulaciaButtonClick() {
        System.out.println("Otvorím okno simulácie...");
        try {
            FXMLLoader loader = new FXMLLoader(MenuController.class.getResource("/permutations-view.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nastavenie simulácie");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onVysvetlenieButtonClick() {
        System.out.println("Otvorím okno vysvetlenia...");
        // Tu neskôr zavoláš show() metódu pre ExplainView
    }
}