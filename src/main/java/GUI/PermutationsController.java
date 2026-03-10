package GUI;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.layout.VBox;
import logic.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PermutationsController {

    @FXML private TextField tfPocetVaznov;
    @FXML private ComboBox<String> cbStrategie;
    @FXML private TextField tfPocetPermutacii;
    @FXML private TextField tfVylucit; // Nové pole pre ExclusionStrategy
    @FXML private ProgressBar progressBar;

    @FXML private VBox vboxVysledky;
    @FXML private Label lblReport;

    private final List<Strategy> availableStrategies = new ArrayList<>();
    private double poslednaSanca;
    private String poslednaStrategia;

    @FXML
    public void initialize() {
        tfPocetVaznov.setText("100");
        tfPocetPermutacii.setText("10000");
        tfVylucit.setText("0");

        vboxVysledky.setVisible(false);
        vboxVysledky.managedProperty().bind(vboxVysledky.visibleProperty());
        progressBar.setVisible(false);

        // Inicializácia stratégií
        availableStrategies.clear();
        cbStrategie.getItems().clear();

        availableStrategies.add(new CycleStrategy());
        availableStrategies.add(new RandomStrategy());
        availableStrategies.add(new EvenStrategy());      // PRIDANÉ SPÄŤ
        availableStrategies.add(new OddStrategy());
        availableStrategies.add(new ExclusionStrategy()); // Nezabudni vytvoriť túto triedu

        for (Strategy s : availableStrategies) {
            cbStrategie.getItems().add(s.nazovStrategie());
        }

        // Dynamické vypínanie tfVylucit, ak nie je vybraná ExclusionStrategy
        tfVylucit.disableProperty().bind(
                cbStrategie.getSelectionModel().selectedItemProperty().map(s -> s == null || !s.contains("vylúčenie"))
        );

        if (!cbStrategie.getItems().isEmpty()) {
            cbStrategie.getSelectionModel().select(0);
        }
    }

    @FXML
    public void onSpustiButtonClick(ActionEvent event) {
        SimulationParams params = validujVstupy();
        if (params == null) return;

        Strategy chosenStrategy = najdiStrategiu(cbStrategie.getSelectionModel().getSelectedItem());
        if (chosenStrategy == null) return;

        // Nastavenie parametrov pre ExclusionStrategy, ak je vybraná
        if (chosenStrategy instanceof ExclusionStrategy) {
            try {
                int v = Integer.parseInt(tfVylucit.getText());
                ((ExclusionStrategy) chosenStrategy).setPocetVylucenych(v);
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.ERROR, "Zadajte platné číslo pre vylúčené krabice.").showAndWait();
                return;
            }
        }

        chosenStrategy.resetStats();

        // UI Príprava
        Button spustiBtn = (Button) event.getSource();
        spustiBtn.setDisable(true);
        vboxVysledky.setVisible(false);
        progressBar.setVisible(true);
        progressBar.setProgress(0);

        Task<SimulationResult> simulationTask = new Task<>() {
            @Override
            protected SimulationResult call() {
                long vsetciUspeliCount = 0L;
                long sumaIndividualnychUspechov = 0L;
                long sumaMaxCyklus = 0;
                int absolutneNajdlhsiCyklus = 0;
                int limit = params.pocetVaznov / 2;

                for (int i = 0; i < params.permutations; i++) {
                    int uspesni = chosenStrategy.pocitaj(params.pocetVaznov, limit);

                    if (uspesni == params.pocetVaznov) vsetciUspeliCount++;
                    sumaIndividualnychUspechov += uspesni;

                    int aktualnyMax = chosenStrategy.getNajdlhsiCyklusPoslednejSimulacie();
                    sumaMaxCyklus += aktualnyMax;
                    if (aktualnyMax > absolutneNajdlhsiCyklus) absolutneNajdlhsiCyklus = aktualnyMax;

                    if (i % 100 == 0) updateProgress(i, params.permutations);
                }
                updateProgress(params.permutations, params.permutations);

                return new SimulationResult(
                        vsetciUspeliCount, sumaIndividualnychUspechov,
                        sumaMaxCyklus, absolutneNajdlhsiCyklus, params.permutations
                );
            }
        };

        progressBar.progressProperty().bind(simulationTask.progressProperty());

        simulationTask.setOnSucceeded(e -> {
            SimulationResult result = simulationTask.getValue();
            poslednaSanca = result.sancaPrezitia;
            poslednaStrategia = chosenStrategy.nazovStrategie();

            lblReport.setText(generujReport(result, chosenStrategy, params));

            progressBar.setVisible(false);
            progressBar.progressProperty().unbind();
            vboxVysledky.setVisible(true);
            spustiBtn.setDisable(false);
        });

        new Thread(simulationTask).start();
    }

    private String generujReport(SimulationResult r, Strategy s, SimulationParams p) {
        Locale localeSK = new Locale("sk", "SK");
        StringBuilder sb = new StringBuilder();
        int limit = p.pocetVaznov / 2;
        long celkovyPocetPokusov = (long) p.pocetVaznov * p.permutations;

        sb.append(String.format(localeSK, "STRATÉGIA: %s\n", s.nazovStrategie()));
        sb.append(String.format(localeSK, "Väzňov: %d | Limit: %d pokusov\n", p.pocetVaznov, limit));
        sb.append(String.format(localeSK, "Počet simulácií: %,d\n", p.permutations));
        sb.append("--------------------------------------------------\n");
        sb.append(String.format(localeSK, "Celkovo úspešných hľadaní: %,d z %,d\n", r.sumaIndividualnychUspechov, celkovyPocetPokusov));
        sb.append(String.format(localeSK, "Úspešné prežitia skupiny: %,d\n", r.vsetciUspeliCount));
        sb.append(String.format(localeSK, "Šanca na prežitie: %.2f %%\n", r.sancaPrezitia));

        // Zobrazenie rekordu úspešných jednotlivcov v neúspešnom pokuse
        int rekord = s.getMaxUspesnychVHistorii();
        if (rekord > 0 && r.vsetciUspeliCount < p.permutations) {
            sb.append(String.format(localeSK, "Najviac úspešných v neúspešnej simulácii: %d z %d\n", rekord, p.pocetVaznov));
        }

        sb.append(String.format(localeSK, "Priemerne úspešných väzňov: %.2f z %d\n", r.priemernyPocetUspesnych, p.pocetVaznov));

        if (s.nazovStrategie().contains("Cyklická")) {
            sb.append("\n--- MATEMATICKÁ ANALÝZA CYKLOV ---\n");
            sb.append(String.format(localeSK, "Priemerný najdlhší cyklus: %.1f\n", r.priemernaDlzkaMaxCyklu));
            sb.append(String.format(localeSK, "Najdlhší cyklus v histórii: %d\n", r.absolutneNajdlhsiCyklus));
        }
        return sb.toString();
    }

    private Strategy najdiStrategiu(String nazov) {
        return availableStrategies.stream()
                .filter(s -> s.nazovStrategie().startsWith(nazov.split(" ")[0])) // Flexibilnejšie vyhľadávanie
                .findFirst().orElse(null);
    }

    private SimulationParams validujVstupy() {
        try {
            int pocet = Integer.parseInt(tfPocetVaznov.getText());
            int permutations = Integer.parseInt(tfPocetPermutacii.getText());
            if (pocet <= 0 || permutations <= 0) throw new NumberFormatException();
            return new SimulationParams(pocet, permutations);
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Zadajte platné čísla.").showAndWait();
            return null;
        }
    }

    @FXML private void onZobrazGrafClick(ActionEvent event) { otvorGrafickeOkno(poslednaSanca, poslednaStrategia); }

    private void otvorGrafickeOkno(double sanca, String strategia) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/graph-view.fxml"));
            Parent root = loader.load();
            GraphController graphCtrl = loader.getController();
            graphCtrl.nastavData(sanca, strategia);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML public void onCloseButtonClick(ActionEvent event) {
        ((Stage)((Node)event.getSource()).getScene().getWindow()).close();
    }

    // Pomocné triedy
    private static class SimulationParams {
        int pocetVaznov, permutations;
        SimulationParams(int p, int perm) { this.pocetVaznov = p; this.permutations = perm; }
    }

    private static class SimulationResult {
        long vsetciUspeliCount, sumaIndividualnychUspechov, sumaMaxCyklus;
        double sancaPrezitia, priemernyPocetUspesnych, priemernaDlzkaMaxCyklu;
        int absolutneNajdlhsiCyklus;

        SimulationResult(long uspesni, long sumaIndiv, long sumaCykl, int maxCykl, int perm) {
            this.vsetciUspeliCount = uspesni;
            this.sumaIndividualnychUspechov = sumaIndiv;
            this.sancaPrezitia = ((double) uspesni / perm) * 100.0;
            this.priemernyPocetUspesnych = (double) sumaIndiv / perm;
            this.priemernaDlzkaMaxCyklu = (double) sumaCykl / perm;
            this.absolutneNajdlhsiCyklus = maxCykl;
        }
    }
}