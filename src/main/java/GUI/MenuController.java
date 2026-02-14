package GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
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
    public void initialize() {
        // Efekt pre tlačidlo simulácie
        btnSimulacia.setOnMouseEntered(e -> btnSimulacia.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;"));
        btnSimulacia.setOnMouseExited(e -> btnSimulacia.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;"));

        // Efekt pre tlačidlo vysvetlenia
        btnVysvetlenie.setOnMouseEntered(e -> btnVysvetlenie.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;"));
        btnVysvetlenie.setOnMouseExited(e -> btnVysvetlenie.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;"));
    }

    @FXML
    public void onVysvetlenieButtonClick() {
        System.out.println("Otvorím okno vizualizácie cyklov...");
        try {
            // 1. Načítame FXML pre vizualizáciu
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/explain-view.fxml"));
            Parent root = loader.load();

            // 2. Vytvoríme novú scénu a okno (Stage)
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL); // Zablokuje menu, kým sa toto nezavrie
            stage.setTitle("Vizualizácia permutácií a cyklov");
            stage.setScene(new Scene(root));

            // 3. Zobrazíme okno
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Nepodarilo sa načítať okno vysvetlenia: " + e.getMessage());
            alert.showAndWait();
        }
    }
}