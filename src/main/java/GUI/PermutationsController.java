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
import javafx.scene.Cursor;
import logic.*;

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
        tfPocetPermutacii.setText("10000");

        availableStrategies.clear();
        cbStrategie.getItems().clear();

        // Pridanie všetkých stratégií
        availableStrategies.add(new CycleStrategy());
        availableStrategies.add(new RandomStrategy());
        availableStrategies.add(new EvenStrategy());
        availableStrategies.add(new OddStrategy());

        for (Strategy s : availableStrategies) {
            cbStrategie.getItems().add(s.nazovStrategie());
        }

        if (!cbStrategie.getItems().isEmpty()) {
            cbStrategie.getSelectionModel().select(0);
        }
    }

    @FXML
    public void onSpustiButtonClick(ActionEvent event) {
        Node sourceNode = (Node) event.getSource();
        Scene scene = sourceNode.getScene();

        // 1. Validácia vstupov
        int pocet;
        int permutations;
        try {
            pocet = Integer.parseInt(tfPocetVaznov.getText());
            permutations = Integer.parseInt(tfPocetPermutacii.getText());
            if (pocet <= 0 || permutations <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Zadajte platné kladné čísla (celé čísla > 0).").showAndWait();
            return;
        }

        // 2. Výber stratégie
        String vybranaNazov = cbStrategie.getSelectionModel().getSelectedItem();
        Strategy chosenStrategy = availableStrategies.stream()
                .filter(s -> s.nazovStrategie().equals(vybranaNazov))
                .findFirst().orElse(null);

        if (chosenStrategy == null) return;

        // 3. Spustenie simulácie (so zmenou kurzora na čakajúci)
        scene.setCursor(Cursor.WAIT);

        long vsetciUspeliCount = 0L;
        long sumaIndividualnychUspechov = 0L;

        // Jadro simulácie - každý väzeň má pocet/2 pokusov
        int limitPokusov = pocet / 2;

        for (int i = 0; i < permutations; i++) {
            int uspesniVTejtoSimulacii = chosenStrategy.pocitaj(pocet, limitPokusov);

            if (uspesniVTejtoSimulacii == pocet) {
                vsetciUspeliCount++;
            }
            sumaIndividualnychUspechov += uspesniVTejtoSimulacii;
        }

        scene.setCursor(Cursor.DEFAULT);

        // 4. Výpočet štatistík
        double sancaPrezititaSkupiny = ((double) vsetciUspeliCount / permutations) * 100.0;
        double priemernyPocetUspesnych = (double) sumaIndividualnychUspechov / permutations;

        // 5. Formátovanie správy
        String report = String.format(
                "Stratégia: %s\n" +
                        "Počet väzňov: %d\n" +
                        "Limit pokusov na osobu: %d\n" +
                        "Počet simulácií: %d\n\n" +
                        "--- ŠTATISTIKA ---\n" +
                        "Úspešné prežitia skupiny: %d\n" +
                        "Šanca na prežitie skupiny: %.2f%%\n" +
                        "Priemerný počet úspešných väzňov: %.2f z %d",
                vybranaNazov, pocet, limitPokusov, permutations,
                vsetciUspeliCount, sancaPrezititaSkupiny, priemernyPocetUspesnych, pocet
        );

        zobrazVysledky(report, sancaPrezititaSkupiny, vybranaNazov);
    }

    private void zobrazVysledky(String detailnaSprava, double sanca, String nazovStrategie) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Výsledok simulácie");
        alert.setHeaderText("Simulácia úspešne dobehla");
        alert.setContentText(detailnaSprava);

        ButtonType btnGraf = new ButtonType("Zobraziť grafy");
        ButtonType btnZavriet = new ButtonType("Zavrieť", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnGraf, btnZavriet);

        // Zväčšíme okno, aby sa správa nezalamovala
        alert.getDialogPane().setMinWidth(450);

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

            GraphController graphCtrl = loader.getController();
            graphCtrl.nastavData(sanca, strategia);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Vizuálna analýza výsledkov");
            stage.setScene(new Scene(root));

            if (tfPocetVaznov.getScene() != null) {
                stage.initOwner(tfPocetVaznov.getScene().getWindow());
            }

            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Chyba pri otváraní grafu: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    public void onCloseButtonClick(ActionEvent event) {
        ((Stage)((Node)event.getSource()).getScene().getWindow()).close();
    }
}