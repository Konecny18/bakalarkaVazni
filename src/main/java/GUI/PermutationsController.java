package GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import logic.Strategy;
import logic.CycleStrategy;
import logic.RandomStrategy; // pridaná stratégia

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PermutationsController {

    @FXML private TextField tfPocetVaznov;
    @FXML private ComboBox<String> cbStrategie;
    @FXML private TextField tfPocetPermutacii;

    private final List<Strategy> availableStrategies = new ArrayList<>();

    @FXML
    public void initialize() {
        tfPocetVaznov.setText("100");
        tfPocetPermutacii.setText("1000");

        // Pridáme obe stratégie pre porovnanie
        availableStrategies.add(new CycleStrategy());
        availableStrategies.add(new RandomStrategy());

        for (Strategy s : availableStrategies) {
            cbStrategie.getItems().add(s.nazovStrategie());
        }
        if (!cbStrategie.getItems().isEmpty()) {
            cbStrategie.getSelectionModel().select(0);
        }
    }

    @FXML
    public void onSpustiButtonClick(ActionEvent event) {
        // 1. Validácia vstupov (tvoj pôvodný kód je správny)
        int pocet;
        int permutations;
        try {
            pocet = Integer.parseInt(tfPocetVaznov.getText());
            permutations = Integer.parseInt(tfPocetPermutacii.getText());
            if (pocet <= 0 || permutations <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Zadajte platné kladné čísla.").showAndWait();
            return;
        }

        // 2. Výber stratégie
        String vybrana = cbStrategie.getSelectionModel().getSelectedItem();
        Strategy chosen = availableStrategies.stream()
                .filter(s -> s.nazovStrategie().equals(vybrana))
                .findFirst().orElse(null);

        if (chosen == null) return;

        // 3. JADRO SIMULÁCIE - Tu počítame prežitie skupiny
        long vsetciUspeliCount = 0L;
        long sumaIndividualnychUspechov = 0L;

        for (int i = 0; i < permutations; i++) {
            int uspesniVTejtoSimulacii = chosen.pocitaj(pocet, pocet);

            // Skupina prežije LEN ak uspeli VŠETCI (každý mal len 50 pokusov)
            if (uspesniVTejtoSimulacii == pocet) {
                vsetciUspeliCount++;
            }
            sumaIndividualnychUspechov += uspesniVTejtoSimulacii;
        }

        // 4. Výpočet štatistík
        double sancaPrežitiaSkupiny = ((double) vsetciUspeliCount / permutations) * 100.0;
        double priemernyPocetVaznov = (double) sumaIndividualnychUspechov / permutations;

        // 5. Zobrazenie výsledkov
        String message = String.format(
                "Stratégia: %s\n" +
                        "Počet väzňov: %d (max %d pokusov)\n" +
                        "Počet simulácií: %d\n\n" +
                        "--- VÝSLEDKY ---\n" +
                        "Skupina prežila: %d krát\n" +
                        "Šanca na prežitie skupiny: %.2f%%\n" +
                        "Priemerne úspešných väzňov: %.2f",
                vybrana, pocet, pocet / 2, permutations, vsetciUspeliCount, sancaPrežitiaSkupiny, priemernyPocetVaznov
        );

        zobrazVysledky(message, sancaPrežitiaSkupiny, vybrana);
    }

    @FXML
    public void onCloseButtonClick(ActionEvent event) {
        ((Stage)((Node)event.getSource()).getScene().getWindow()).close();
    }

    // Toto voláš po skončení výpočtov v metóde spustiSimulaciu()
    private void zobrazVysledky(String detailnaSprava, double sanca, String nazovStrategie) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Štatistický report simulácie");
        alert.setHeaderText("Simulácia úspešne dokončená");

        // Tu vložíme tvoj podrobný text
        alert.setContentText(detailnaSprava);

        // Pridáme vlastné tlačidlá
        ButtonType btnGraf = new ButtonType("Zobraziť graf");
        ButtonType btnZavriet = new ButtonType("Zavrieť", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnGraf, btnZavriet);

        // Aby sa text v Alerte dal dobre čítať (ak je dlhý), môžeme ho trochu zväčšiť
        alert.getDialogPane().setMinWidth(400);

        alert.showAndWait().ifPresent(response -> {
            if (response == btnGraf) {
                otvorGrafickeOkno(sanca, nazovStrategie);
            }
        });
    }

    private void otvorGrafickeOkno(double sanca, String strategia) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/graph-view.fxml"));
            Parent root = loader.load();

            // Získame controller prislúchajúci k novému oknu
            GraphController graphCtrl = loader.getController();

            // Odovzdáme dáta priamo cez setter (žiadne globálne premenné)
            graphCtrl.nastavData(sanca, strategia);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Porovnávací graf");
            stage.setScene(new Scene(root));
            // Nastavíme ownera aby sa okno modalne správalo nad aktuálnym oknom
            if (tfPocetVaznov.getScene() != null) {
                stage.initOwner(tfPocetVaznov.getScene().getWindow());
            }
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}