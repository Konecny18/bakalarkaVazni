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
import javafx.scene.layout.VBox;
import logic.*;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.util.*;

public class PermutationsController {

    @FXML private TextField tfPocetVaznov, tfPocetPermutacii, tfVylucit;
    @FXML private Label lblVylucitErr;
    @FXML private ComboBox<String> cbStrategie;
    @FXML private ProgressBar progressBar;
    @FXML private Button btnSpustit, btnCancel;
    @FXML private VBox vboxVysledky;
    @FXML private Label lblReport;

    private final List<Strategy> availableStrategies = new ArrayList<>();
    private SimulationResult poslednyResult;
    private String poslednaStrategiaNazov;
    private List<Integer> posledneRawData = new ArrayList<>();
    private Task<SimulationResult> currentTask;

    @FXML
    public void initialize() {
        tfPocetVaznov.setText("100");
        tfPocetPermutacii.setText("10000");
        tfVylucit.setText("0");
        // Setup inline validation for exclusion input
        tfVylucit.textProperty().addListener((obs, oldV, newV) -> validateVylucit());
        vboxVysledky.setVisible(false);
        vboxVysledky.managedProperty().bind(vboxVysledky.visibleProperty());
        progressBar.setVisible(false);

        availableStrategies.addAll(List.of(
                new CycleStrategy(), new RandomStrategy(), new HybridStrategy(),
                new EvenStrategy(), new OddStrategy(), new ExclusionStrategy()
        ));
        for (Strategy s : availableStrategies) cbStrategie.getItems().add(s.nazovStrategie());

        if (!cbStrategie.getItems().isEmpty()) cbStrategie.getSelectionModel().select(0);

        // Logika pre vypínanie poľa "vylúčiť"
        tfVylucit.disableProperty().bind(cbStrategie.getSelectionModel().selectedItemProperty().map(s ->
                s == null || !s.toLowerCase().contains("vylúčenie")));

        // Validate initial state
        validateVylucit();
    }

    // Validates tfVylucit; shows inline message and red border if invalid
    private void validateVylucit() {
        if (lblVylucitErr == null || tfVylucit == null || tfPocetVaznov == null) return;
        String text = tfVylucit.getText();
        int max = 1000; // fallback
        try { max = Integer.parseInt(tfPocetVaznov.getText()); } catch (Exception ignored) {}

        if (text == null || text.isBlank()) {
            lblVylucitErr.setText("");
            tfVylucit.setStyle("");
            return;
        }

        try {
            int val = Integer.parseInt(text);
            if (val < 0 || val >= max) {
                lblVylucitErr.setText("Zadajte celé číslo medzi 0 a " + (max-1));
                tfVylucit.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
            } else {
                lblVylucitErr.setText("");
                tfVylucit.setStyle("");
            }
        } catch (NumberFormatException ex) {
            lblVylucitErr.setText("Zadajte platné celé číslo");
            tfVylucit.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2;");
        }
    }

    @FXML
    public void onSpustiButtonClick() {
        try {
            SimulationParams params = validujVstupy();
            if (params == null) return;

            Strategy selected = najdiStrategiu(cbStrategie.getValue());
            if (selected == null) return;

            // If ExclusionStrategy is selected, parse and set the exclusion count from tfVylucit
            if (selected instanceof logic.ExclusionStrategy es) {
                try {
                    int toExclude = Integer.parseInt(tfVylucit.getText());
                    if (toExclude < 0 || toExclude >= params.pocetVaznov) {
                        new Alert(Alert.AlertType.ERROR, "Počet vylúčených musí byť medzi 0 a počtom väzňov-1 (" + (params.pocetVaznov-1) + ")").showAndWait();
                        return;
                    }
                    es.setPocetVylucenych(toExclude);
                } catch (NumberFormatException nfe) {
                    new Alert(Alert.AlertType.ERROR, "Neplatný počet vylúčených (zadajte celé číslo)").showAndWait();
                    return;
                }
            }

            // Príprava UI
            btnSpustit.setDisable(true);
            btnCancel.setVisible(true);
            vboxVysledky.setVisible(false);
            progressBar.setProgress(0);
            progressBar.setVisible(true);

            List<Integer> runResults = Collections.synchronizedList(new ArrayList<>());

            currentTask = new Task<>() {
                @Override
                protected SimulationResult call() {
                    selected.resetStats();
                    long vsetciUspeli = 0L;
                    int limit = params.pocetVaznov / 2;

                    // Mapa pre štatistiku cyklov (Dĺžka -> Počet výskytov)
                    Map<Integer, Integer> akumulovaneCykly = new HashMap<>();

                    for (int i = 0; i < params.permutations; i++) {
                        if (isCancelled()) break;

                        // Ak je to CycleStrategy, zbierame podrobné dáta o cykloch
                        if (selected instanceof CycleStrategy cs) {
                            List<Integer> krabice = cs.generujKrabice(params.pocetVaznov);
                            List<Integer> dlzky = cs.ziskajDlzkyCyklov(krabice);

                            for (int d : dlzky) {
                                akumulovaneCykly.put(d, akumulovaneCykly.getOrDefault(d, 0) + 1);
                            }

                            int maxD = dlzky.stream().max(Integer::compare).orElse(0);
                            if (maxD <= limit) {
                                vsetciUspeli++;
                                runResults.add(params.pocetVaznov);
                            } else {
                                int uspesni = dlzky.stream().filter(d -> d <= limit).mapToInt(Integer::intValue).sum();
                                runResults.add(uspesni);
                            }
                        } else {
                            runResults.add(selected.pocitaj(params.pocetVaznov, limit));
                        }

                        if (i % 500 == 0) updateProgress(i, params.permutations);
                    }

                    return new SimulationResult(vsetciUspeli, new ArrayList<>(runResults), runResults.size(), akumulovaneCykly);
                }
            };

            progressBar.progressProperty().bind(currentTask.progressProperty());

            currentTask.setOnSucceeded(e -> {
                poslednyResult = currentTask.getValue();
                poslednaStrategiaNazov = selected.nazovStrategie();
                posledneRawData = new ArrayList<>(runResults);
                lblReport.setText(generujReport(poslednyResult, poslednaStrategiaNazov, params));
                finishUI();
            });

            currentTask.setOnFailed(e -> {
                finishUI();
                new Alert(Alert.AlertType.ERROR, "Chyba simulácie.").showAndWait();
            });

            new Thread(currentTask).start();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Chyba pri spúšťaní simulácie: " + ex.getMessage()).showAndWait();
            finishUI();
        }
    }

    // TÁTO METÓDA CHÝBALA/MALA ZLÝ NÁZOV (podľa tvojho erroru)
    @FXML
    public void onCancelButtonClick() {
        if (currentTask != null) currentTask.cancel();
    }

    @FXML
    private void onZobrazGrafClick() {
        if (poslednyResult == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/graph-view.fxml"));
            Parent root = loader.load();
            GraphController ctrl = loader.getController();

            // Posielame akumulované dáta o cykloch zo všetkých behov
            ctrl.nastavData(
                    posledneRawData,
                    poslednyResult.sancaPrezitia,
                    poslednaStrategiaNazov,
                    poslednyResult.celkovaDistribuciaCyklov
            );

            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Grafické výsledky");
            s.setScene(new Scene(root));
            s.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generujReport(SimulationResult r, String meno, SimulationParams p) {
        return String.format("📊 Stratégia: %s\n✨ Úspešnosť: %.2f%%\n👥 Priemer úspešných: %.2f",
                meno, r.sancaPrezitia, r.priemer);
    }

    private void finishUI() {
        progressBar.setVisible(false);
        btnCancel.setVisible(false);
        btnSpustit.setDisable(false);
        vboxVysledky.setVisible(true);
    }

    private Strategy najdiStrategiu(String n) {
        return availableStrategies.stream().filter(s -> s.nazovStrategie().equals(n)).findFirst().orElse(null);
    }

    private SimulationParams validujVstupy() {
        try {
            return new SimulationParams(Integer.parseInt(tfPocetVaznov.getText()), Integer.parseInt(tfPocetPermutacii.getText()));
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Neplatné vstupy").showAndWait();
            return null;
        }
    }

    @FXML
    public void onCloseButtonClick(ActionEvent e) {
        ((Stage)((Node)e.getSource()).getScene().getWindow()).close();
    }

    // --- Vnútorné triedy pre dáta ---

    private static class SimulationParams {
        int pocetVaznov, permutations;
        SimulationParams(int p, int perm) { this.pocetVaznov = p; this.permutations = perm; }
    }

    private static class SimulationResult {
        double sancaPrezitia, priemer;
        int totalRuns;
        Map<Integer, Integer> celkovaDistribuciaCyklov;

        SimulationResult(long uspesni, List<Integer> data, int actualRuns, Map<Integer, Integer> cyklyStats) {
            this.totalRuns = actualRuns;
            this.celkovaDistribuciaCyklov = cyklyStats;
            this.sancaPrezitia = (actualRuns > 0) ? ((double) uspesni / actualRuns) * 100.0 : 0.0;
            this.priemer = data.stream().mapToInt(i -> i).average().orElse(0.0);
        }
    }
}
