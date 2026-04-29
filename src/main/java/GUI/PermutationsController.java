package GUI;

import javafx.concurrent.Task;
import javafx.application.Platform;
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

/**
 * Controller pre dialóg permutácií / simulácie.
 * Zodpovedá za validáciu vstupov, spustenie simulácií na pozadí (Task)
 * a prezentáciu textového reportu a otvorenie grafického zobrazenia.
 */
public class PermutationsController {

    @FXML private TextField tfPocetVaznov, tfPocetPermutacii, tfVylucit;
    @FXML private Label lblVylucitErr;
    @FXML private ComboBox<String> cbStrategie;
    @FXML private ProgressBar progressBar;
    @FXML private Button btnSpustit, btnCancel;
    @FXML private VBox vboxVysledky;
    @FXML private javafx.scene.control.TextArea txtReport;
    @FXML private ScrollPane rootScroll;
    @FXML private Button btnExportReport;

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

    /**
     * Handler pre tlačidlo "Spusti". Overí vstupy, nakonfiguruje vybranú stratégiu
     * (napr. počet vylúčených) a spustí simuláciu na pozadí a aktualizuje UI.
     */
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
            if (txtReport != null) {
                txtReport.clear();
                txtReport.setPrefHeight(300); // larger default while running
            }

            // Ensure we unbind any previous binding before setting progress
            if (progressBar.progressProperty().isBound()) {
                progressBar.progressProperty().unbind();
            }
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

            // Ensure cancellation also cleans up the UI
            currentTask.setOnCancelled(e -> finishUI());

            progressBar.progressProperty().bind(currentTask.progressProperty());

            currentTask.setOnSucceeded(e -> {
                poslednyResult = currentTask.getValue();
                poslednaStrategiaNazov = selected.nazovStrategie();
                posledneRawData = new ArrayList<>(runResults);
                if (txtReport != null) txtReport.setText(generujReport(poslednyResult, poslednaStrategiaNazov, params));
                // Adjust TextArea height relative to current window so user sees more of the report by default
                if (txtReport != null) {
                    Platform.runLater(() -> {
                        try {
                            if (btnSpustit != null && btnSpustit.getScene() != null) {
                                double winH = btnSpustit.getScene().getWindow().getHeight();
                                double pref = Math.max(450, winH * 0.65); // 65% of window height or at least 450px
                                txtReport.setPrefHeight(pref);
                            }
                        } catch (Exception ignored) {}
                    });
                }
                // Scroll main scroll pane to bottom so results are visible to the user
                if (rootScroll != null) {
                    Platform.runLater(() -> rootScroll.setVvalue(1.0));
                }
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

    /**
     * Otvorí grafické okno s výsledkami pre poslednú simuláciu.
     */
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
        // Use posledneRawData (populated just before calling this) as the source for detailed stats
        List<Integer> data = (posledneRawData == null) ? Collections.emptyList() : new ArrayList<>(posledneRawData);

        int totalRuns = r != null ? r.totalRuns : data.size();
        double mean = r != null ? r.priemer : (data.stream().mapToInt(i -> i).average().orElse(0.0));
        double successChance = r != null ? r.sancaPrezitia : 0.0;

        // Basic min/max/median/stddev
        int min = data.stream().mapToInt(i -> i).min().orElse(0);
        int max = data.stream().mapToInt(i -> i).max().orElse(0);
        double median;
        if (data.isEmpty()) median = 0.0;
        else {
            List<Integer> sorted = new ArrayList<>(data);
            Collections.sort(sorted);
            int n = sorted.size();
            if (n % 2 == 1) median = sorted.get(n/2);
            else median = (sorted.get(n/2 - 1) + sorted.get(n/2)) / 2.0;
        }

        double stddev = 0.0;
        if (!data.isEmpty()) {
            double sum = 0.0;
            for (int v : data) sum += (v - mean) * (v - mean);
            stddev = Math.sqrt(sum / data.size());
        }

        // Count of full successes (value == pocetVaznov) and zero successes
        long fullSuccesses = data.stream().filter(v -> v == p.pocetVaznov).count();
        long zeroSuccesses = data.stream().filter(v -> v == 0).count();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📊 Stratégia: %s\n", meno));
        sb.append(String.format("⚙️ Parametre: väzňov=%d, simulácií=%d\n", p.pocetVaznov, p.permutations));
        sb.append(String.format("✨ Úspešnosť (percentuálne): %.2f%% (%d/%d)\n", successChance, Math.round(successChance/100.0*totalRuns), totalRuns));
        sb.append(String.format("👥 Priemer úspešných: %.2f\n", mean));
        sb.append(String.format("📈 Medián: %.2f    σ (stddev): %.2f\n", median, stddev));
        sb.append(String.format("🔻 Min: %d    🔺 Max: %d\n", min, max));
        sb.append(String.format("✅ Plne úspešných simulácií: %d (%.2f%%)\n", fullSuccesses, totalRuns>0 ? (fullSuccesses*100.0/totalRuns) : 0.0));
        sb.append(String.format("❌ Žiadny úspech: %d (%.2f%%)\n", zeroSuccesses, totalRuns>0 ? (zeroSuccesses*100.0/totalRuns) : 0.0));

        sb.append("\nStručná frekvenčná tabuľka (hodnota -> počet, top 20 najčastejších):\n");
        Map<Integer, Long> freq = new HashMap<>();
        for (int v : data) freq.put(v, freq.getOrDefault(v, 0L) + 1L);

        // Sort by count desc, then value asc
        List<Map.Entry<Integer, Long>> entries = new ArrayList<>(freq.entrySet());
        entries.sort((a,b) -> { int c = Long.compare(b.getValue(), a.getValue()); return c!=0?c:Integer.compare(a.getKey(), b.getKey()); });

        int limit = Math.min(20, entries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<Integer, Long> e = entries.get(i);
            sb.append(String.format("  %3d -> %d\n", e.getKey(), e.getValue()));
        }
        if (entries.size() > limit) sb.append(String.format("  ... +%d ďalších hodnôt\n", entries.size() - limit));

        sb.append("\nPre detailnejšie vizualizácie kliknite na 'Zobraziť grafy'.");
        return sb.toString();
    }

    private void finishUI() {
        // Unbind progress property before modifying/hiding it to avoid bound-set exception
        if (progressBar.progressProperty().isBound()) {
            progressBar.progressProperty().unbind();
        }
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

    @FXML
    public void onExportReportClick() {
        if (txtReport == null) return;
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.setTitle("Ulož report ako...");
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Text Files", "*.txt"));
        java.io.File f = chooser.showSaveDialog(btnExportReport.getScene().getWindow());
        if (f == null) return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(f, java.nio.charset.StandardCharsets.UTF_8)) {
            pw.print(txtReport.getText());
            new Alert(Alert.AlertType.INFORMATION, "Report uložený: " + f.getAbsolutePath()).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Chyba pri ukladaní reportu: " + ex.getMessage()).showAndWait();
        }
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
