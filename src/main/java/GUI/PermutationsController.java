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

    @FXML
    public void onSpustiButtonClick(ActionEvent event) {
        SimulationParams params = validujVstupy();
        Strategy strategy = najdiStrategiu(cbStrategie.getValue());
        if (params == null || strategy == null) return;

        if (strategy instanceof ExclusionStrategy) {
            ((ExclusionStrategy) strategy).setPocetVylucenych(Integer.parseInt(tfVylucit.getText()));
        }

        strategy.resetStats();
        btnSpustit.setDisable(true);
        btnCancel.setVisible(true);
        vboxVysledky.setVisible(false);
        progressBar.setVisible(true);

        List<Integer> runResults = new ArrayList<>();
        currentTask = new Task<>() {
            @Override
            protected SimulationResult call() {
                long vsetciUspeli = 0, sumaCykl = 0;
                int limit = params.pocetVaznov / 2;

                for (int i = 0; i < params.permutations; i++) {
                    if (isCancelled()) break;
                    int uspesni = strategy.pocitaj(params.pocetVaznov, limit);
                    runResults.add(uspesni);
                    if (uspesni == params.pocetVaznov) vsetciUspeli++;
                    sumaCykl += strategy.getNajdlhsiCyklusPoslednejSimulacie();
                    if (i % 500 == 0) updateProgress(i, params.permutations);
                }
                return new SimulationResult(vsetciUspeli, runResults, sumaCykl, params.permutations, params.pocetVaznov);
            }
        };

        progressBar.progressProperty().bind(currentTask.progressProperty());
        currentTask.setOnSucceeded(e -> {
            poslednyResult = currentTask.getValue();
            poslednaStrategiaNazov = strategy.nazovStrategie();
            posledneRawData = new ArrayList<>(runResults);
            lblReport.setText(generujReport(poslednyResult, poslednaStrategiaNazov, params));
            finishUI();
        });
        currentTask.setOnCancelled(e -> { lblReport.setText("ZRUŠENÉ."); finishUI(); });
        new Thread(currentTask).start();
    }

    private String generujReport(SimulationResult r, String meno, SimulationParams p) {
        Locale sk = Locale.forLanguageTag("sk-SK");
        double teoria = meno.toLowerCase().contains("cykl") ? 31.18 : 0.0;
        double odchylka = r.sancaPrezitia - teoria;

        StringBuilder sb = new StringBuilder();
        sb.append("📊 FINÁLNY REPORT SIMULÁCIE\n");
        sb.append("==================================================\n");
        sb.append(String.format(sk, "📍 Stratégia:      %s\n", meno));
        sb.append(String.format(sk, "🔄 Konfigurácia:   %d väzňov | %,d simulácií\n", p.pocetVaznov, p.permutations));
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
        progressBar.progressProperty().unbind();
        progressBar.setVisible(false);
        btnCancel.setVisible(false);
        btnSpustit.setDisable(false);
        vboxVysledky.setVisible(true);
    }

    @FXML private void onCancelButtonClick() { if (currentTask != null) currentTask.cancel(); }

    @FXML
    private void onZobrazGrafClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/graph-view.fxml"));
            Parent root = loader.load();
            GraphController ctrl = loader.getController();
            ctrl.nastavData(posledneRawData, poslednyResult.sancaPrezitia, poslednaStrategiaNazov);
            Stage s = new Stage(); s.initModality(Modality.APPLICATION_MODAL);
            s.setScene(new Scene(root)); s.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private Strategy najdiStrategiu(String n) { return availableStrategies.stream().filter(s -> s.nazovStrategie().equals(n)).findFirst().orElse(null); }
    private SimulationParams validujVstupy() {
        try { return new SimulationParams(Integer.parseInt(tfPocetVaznov.getText()), Integer.parseInt(tfPocetPermutacii.getText())); }
        catch (Exception e) { return null; }
    }

    @FXML public void onCloseButtonClick(ActionEvent e) { ((Stage)((Node)e.getSource()).getScene().getWindow()).close(); }

    private static class SimulationParams {
        int pocetVaznov, permutations;
        SimulationParams(int p, int perm) { this.pocetVaznov = p; this.permutations = perm; }
    }

    private static class SimulationResult {
        double sancaPrezitia, priemer, median, stdDev;
        int maxVJednej;
        SimulationResult(long uspesni, List<Integer> data, long sumaCykl, int perm, int vaznov) {
            this.sancaPrezitia = ((double) uspesni / perm) * 100.0;
            IntSummaryStatistics stats = data.stream().mapToInt(i -> i).summaryStatistics();
            this.priemer = stats.getAverage();
            this.maxVJednej = stats.getMax();
            List<Integer> s = data.stream().sorted().toList();
            this.median = (perm % 2 == 1) ? s.get(perm/2) : (s.get(perm/2-1) + s.get(perm/2)) / 2.0;
            double var = data.stream().mapToDouble(i -> Math.pow(i - priemer, 2)).sum() / perm;
            this.stdDev = Math.sqrt(var);
        }
    }
}