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
        vboxVysledky.setVisible(false);
        vboxVysledky.managedProperty().bind(vboxVysledky.visibleProperty());
        progressBar.setVisible(false);

        availableStrategies.addAll(List.of(
                new CycleStrategy(), new RandomStrategy(), new HybridStrategy(),
                new EvenStrategy(), new OddStrategy(), new ExclusionStrategy()
        ));
        for (Strategy s : availableStrategies) cbStrategie.getItems().add(s.nazovStrategie());

        tfVylucit.disableProperty().bind(cbStrategie.getSelectionModel().selectedItemProperty().map(s -> s == null || !s.contains("vylúčenie")));
        if (!cbStrategie.getItems().isEmpty()) cbStrategie.getSelectionModel().select(0);
    }

    /**
     * Start simulation on background Task. Validates inputs and keeps UI responsive.
     */
    @FXML
    public void onSpustiButtonClick() {
        SimulationParams params = validujVstupy();
        if (params == null) return;

        Strategy strategy = najdiStrategiu(cbStrategie.getValue());
        if (strategy == null) {
            new Alert(Alert.AlertType.ERROR, "Vyberte stratégie.").showAndWait();
            return;
        }

        // parse exclusion count once (if needed) to avoid double-parsing
        int vylucene = 0;
        if (strategy instanceof ExclusionStrategy) {
            try {
                vylucene = Integer.parseInt(tfVylucit.getText());
                ((ExclusionStrategy) strategy).setPocetVylucenych(vylucene);
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.ERROR, "Zadajte platné číslo pre vylúčené krabice.").showAndWait();
                return;
            }
        }

        // Use a fresh strategy instance for the run to avoid accidental shared state
        Strategy strategyForRun = najdiStrategiu(cbStrategie.getValue());
        if (strategyForRun instanceof ExclusionStrategy) {
            ((ExclusionStrategy) strategyForRun).setPocetVylucenych(vylucene);
        }

        // Prepare UI state
        btnSpustit.setDisable(true);
        btnCancel.setVisible(true);
        vboxVysledky.setVisible(false);
        progressBar.setProgress(0);
        progressBar.setVisible(true);

        List<Integer> runResults = Collections.synchronizedList(new ArrayList<>());

        currentTask = new Task<>() {
            @Override
            protected SimulationResult call() {
                // Reset strategy state on background thread if needed
                strategyForRun.resetStats();

                long vsetciUspeli = 0L;
                int limit = params.pocetVaznov / 2;

                for (int i = 0; i < params.permutations; i++) {
                    if (isCancelled()) break;

                    int uspesni = strategyForRun.pocitaj(params.pocetVaznov, limit);
                    runResults.add(uspesni);

                    if (uspesni == params.pocetVaznov) vsetciUspeli++;

                    if (i % Math.max(1, params.permutations / 200) == 0) updateProgress(i, params.permutations);
                }

                // ensure progress reports completion
                updateProgress(params.permutations, params.permutations);

                int actualRuns = runResults.size();
                return new SimulationResult(vsetciUspeli, new ArrayList<>(runResults), actualRuns);
            }
        };

        progressBar.progressProperty().bind(currentTask.progressProperty());

        currentTask.setOnSucceeded(e -> {
            poslednyResult = currentTask.getValue();
            poslednaStrategiaNazov = strategyForRun.nazovStrategie();
            posledneRawData = new ArrayList<>(runResults);
            lblReport.setText(generujReport(poslednyResult, poslednaStrategiaNazov, params));
            finishUI();
        });

        currentTask.setOnCancelled(e -> {
            lblReport.setText("Výpočet bol zrušený používateľom.");
            finishUI();
        });

        currentTask.setOnFailed(e -> {
            Throwable ex = currentTask.getException();
            String msg = (ex == null) ? "Neznáma chyba" : ex.getMessage();
            new Alert(Alert.AlertType.ERROR, "Chyba počas simulácie: " + msg).showAndWait();
            finishUI();
        });

        Thread t = new Thread(currentTask, "permutation-sim");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Build a human-friendly report from results.
     */
    private String generujReport(SimulationResult r, String meno, SimulationParams p) {
        Locale sk = Locale.forLanguageTag("sk-SK");
        double teoria = meno.toLowerCase().contains("cykl") ? 31.18 : 0.0;
        double odchylka = r.sancaPrezitia - teoria;

        StringBuilder sb = new StringBuilder();
        sb.append("📊 FINÁLNY REPORT SIMULÁCIE\n");
        sb.append("==================================================\n");
        sb.append(String.format(sk, "📍 Stratégia:      %s\n", meno));
        sb.append(String.format(sk, "🔄 Konfigurácia:   %d väzňov | %,d simulácií\n", p.pocetVaznov, r.totalRuns));
        sb.append("--------------------------------------------------\n\n");

        sb.append("🏆 DOSIAHNUTÉ VÝSLEDKY\n");
        sb.append(String.format(sk, "✨ Šanca na prežitie:   %.2f %%\n", r.sancaPrezitia));
        if (teoria > 0) {
            sb.append(String.format(sk, "📉 Teoretická hodnota:  %.2f %% (Odchýlka: %+.2f %%)\n", teoria, odchylka));
        }

        sb.append("\n📈 DISTRIBUČNÉ CHARAKTERISTIKY\n");
        sb.append("--------------------------------------------------\n");
        sb.append(String.format(sk, "🔸 Priemerný počet úspešných väzňov:  %.2f z %d\n", r.priemer, p.pocetVaznov));
        sb.append(String.format(sk, "🔸 Medián úspechov:                   %.1f\n", r.median));
        sb.append(String.format(sk, "🔸 Smerodajná odchýlka (σ):          %.2f\n", r.stdDev));
        sb.append(String.format(sk, "🔸 Maximálny počet úspechov:         %d\n", r.maxVJednej));

        sb.append("\n💡 INTERPRETÁCIA\n");
        sb.append("--------------------------------------------------\n");
        if (r.stdDev > 20) {
            sb.append("👉 Vysoká smerodajná odchýlka potvrdzuje bimodálne\n   rozdelenie (úspech skupiny je viazaný na cykly).");
        } else {
            sb.append("👉 Nízka odchýlka naznačuje nezávislé pokusy\n   a konvergenciu k normálnemu rozdeleniu.");
        }
        sb.append("\n==================================================");

        return sb.toString();
    }

    private void finishUI() {
        try { progressBar.progressProperty().unbind(); } catch (Exception ignored) {}
        progressBar.setVisible(false);
        btnCancel.setVisible(false);
        btnSpustit.setDisable(false);
        vboxVysledky.setVisible(true);
    }

    @FXML private void onCancelButtonClick() { if (currentTask != null) currentTask.cancel(); }

    @FXML
    private void onZobrazGrafClick() {
        if (posledneRawData == null || posledneRawData.isEmpty() || poslednyResult == null) {
            new Alert(Alert.AlertType.INFORMATION, "Žiadne výsledky na zobrazenie. Spustite simuláciu.").showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/graph-view.fxml"));
            Parent root = loader.load();
            GraphController ctrl = loader.getController();
            ctrl.nastavData(posledneRawData, poslednyResult.sancaPrezitia, poslednaStrategiaNazov);
            Stage s = new Stage(); s.initModality(Modality.APPLICATION_MODAL);
            s.setScene(new Scene(root)); s.show();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Nepodarilo sa otvoriť okno grafu: " + e.getMessage()).showAndWait();
        }
    }

    private Strategy najdiStrategiu(String n) { return availableStrategies.stream().filter(s -> s.nazovStrategie().equals(n)).findFirst().orElse(null); }

    private SimulationParams validujVstupy() {
        try {
            int pocet = Integer.parseInt(tfPocetVaznov.getText());
            int permutations = Integer.parseInt(tfPocetPermutacii.getText());
            if (pocet <= 0 || permutations <= 0) throw new NumberFormatException();
            return new SimulationParams(pocet, permutations);
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Zadajte platné kladné čísla pre počet väzňov a počet simulácií.").showAndWait();
            return null;
        }
    }

    @FXML public void onCloseButtonClick(ActionEvent e) { ((Stage)((Node)e.getSource()).getScene().getWindow()).close(); }

    private static class SimulationParams {
        int pocetVaznov, permutations;
        SimulationParams(int p, int perm) { this.pocetVaznov = p; this.permutations = perm; }
    }

    private static class SimulationResult {
        double sancaPrezitia, priemer, median, stdDev;
        int maxVJednej;
        int totalRuns;

        SimulationResult(long uspesni, List<Integer> data, int actualRuns) {
            this.totalRuns = Math.max(0, actualRuns);
            this.sancaPrezitia = (this.totalRuns > 0) ? ((double) uspesni / this.totalRuns) * 100.0 : 0.0;

            if (data == null || data.isEmpty()) {
                this.priemer = 0.0; this.median = 0.0; this.stdDev = 0.0; this.maxVJednej = 0; return;
            }

            IntSummaryStatistics stats = data.stream().mapToInt(i -> i).summaryStatistics();
            this.priemer = stats.getAverage();
            this.maxVJednej = stats.getMax();

            List<Integer> s = data.stream().sorted().toList();
            int n = s.size();
            this.median = (n % 2 == 1) ? s.get(n/2) : (s.get(n/2 - 1) + s.get(n/2)) / 2.0;

            double var = data.stream().mapToDouble(i -> Math.pow(i - this.priemer, 2)).sum() / n;
            this.stdDev = Math.sqrt(var);
        }
    }
}
