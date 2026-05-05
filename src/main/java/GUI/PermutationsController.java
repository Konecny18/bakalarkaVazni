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
import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.NumberFormat;

/**
 * Controller pre dialóg permutácií / simulácie.
 * Zodpovedá za validáciu vstupov, spustenie simulácií na pozadí a prezentáciu reportu.
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

        tfVylucit.disableProperty().bind(cbStrategie.getSelectionModel().selectedItemProperty().map(s ->
                s == null || !s.toLowerCase().contains("vylúčenie")));

        validateVylucit();
    }

    private void validateVylucit() {
        if (lblVylucitErr == null || tfVylucit == null || tfPocetVaznov == null) return;
        String text = tfVylucit.getText();
        int max = 1000;
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

            if (selected instanceof logic.ExclusionStrategy es) {
                try {
                    int toExclude = Integer.parseInt(tfVylucit.getText());
                    es.setPocetVylucenych(toExclude);
                } catch (NumberFormatException nfe) {
                    new Alert(Alert.AlertType.ERROR, "Neplatný počet vylúčených").showAndWait();
                    return;
                }
            }

            btnSpustit.setDisable(true);
            btnCancel.setVisible(true);
            vboxVysledky.setVisible(false);
            if (txtReport != null) txtReport.clear();

            if (progressBar.progressProperty().isBound()) progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            progressBar.setVisible(true);

            List<Integer> runResults = Collections.synchronizedList(new ArrayList<>());

            currentTask = new Task<>() {
                @Override
                protected SimulationResult call() {
                    selected.resetStats();
                    long vsetciUspeli = 0L;
                    int limit = params.pocetVaznov / 2;
                    Map<Integer, Integer> akumulovaneCykly = new HashMap<>();

                    for (int i = 0; i < params.permutations; i++) {
                        if (isCancelled()) break;

                        if (selected instanceof CycleStrategy cs) {
                            List<Integer> krabice = cs.generujKrabice(params.pocetVaznov);
                            List<Integer> dlzky = cs.ziskajDlzkyCyklov(krabice);
                            for (int d : dlzky) akumulovaneCykly.put(d, akumulovaneCykly.getOrDefault(d, 0) + 1);

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

            currentTask.setOnSucceeded(e -> {
                poslednyResult = currentTask.getValue();
                poslednaStrategiaNazov = selected.nazovStrategie();
                posledneRawData = new ArrayList<>(runResults);
                if (txtReport != null) txtReport.setText(generujReport(poslednyResult, poslednaStrategiaNazov, params));

                if (rootScroll != null) Platform.runLater(() -> rootScroll.setVvalue(1.0));
                finishUI();
            });

            currentTask.setOnFailed(e -> { finishUI(); new Alert(Alert.AlertType.ERROR, "Chyba simulácie.").showAndWait(); });
            currentTask.setOnCancelled(e -> finishUI());

            progressBar.progressProperty().bind(currentTask.progressProperty());
            new Thread(currentTask).start();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Chyba: " + ex.getMessage()).showAndWait();
            finishUI();
        }
    }

    private String generujReport(SimulationResult r, String meno, SimulationParams p) {
        List<Integer> data = (posledneRawData == null) ? Collections.emptyList() : new ArrayList<>(posledneRawData);
        double mean = r != null ? r.priemer : (data.stream().mapToInt(i -> i).average().orElse(0.0));
        double successChance = r != null ? r.sancaPrezitia : 0.0;
        int max = data.stream().mapToInt(i -> i).max().orElse(0);

        double median;
        if (data.isEmpty()) median = 0.0;
        else {
            List<Integer> sorted = new ArrayList<>(data);
            Collections.sort(sorted);
            int n = sorted.size();
            median = (n % 2 == 1) ? sorted.get(n/2) : (sorted.get(n/2 - 1) + sorted.get(n/2)) / 2.0;
        }

        double stddev = 0.0;
        if (!data.isEmpty()) {
            double sum = 0.0;
            for (int v : data) sum += (v - mean) * (v - mean);
            stddev = Math.sqrt(sum / data.size());
        }

        BigDecimal theoreticBD = null;
        if (meno != null && meno.toLowerCase().contains("cyklick")) {
            try { theoreticBD = vypocitajTeoretickuPercent(p.pocetVaznov); } catch (Exception ignored) {}
        }

        Locale sk = Locale.forLanguageTag("sk-SK");
        NumberFormat nf = NumberFormat.getNumberInstance(sk);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        NumberFormat intFmt = NumberFormat.getIntegerInstance(sk);

        StringBuilder sb = new StringBuilder();
        sb.append("📊 FINÁLNY REPORT SIMULÁCIE\n");
        sb.append("===============================================\n");
        sb.append(String.format("🔑 Stratégia:      %s\n", meno));
        sb.append(String.format("♻️ Konfigurácia:   %s väzňov | %s simulácií\n", intFmt.format(p.pocetVaznov), intFmt.format(p.permutations)));
        sb.append("===============================================\n\n");

        sb.append("🏆 DOSIAHNUTÉ VÝSLEDKY\n");
        sb.append("-----------------------------------------------\n");
        sb.append(String.format("✨ Šanca na prežitie:   %s %%\n", nf.format(successChance)));

        if (theoreticBD != null) {
            double dev = successChance - theoreticBD.doubleValue();
            String sign = dev >= 0 ? "+" : "-";
            sb.append(String.format("📉 Teoretická hodnota:  %s %% (Odchýlka: %s%s %%)\n",
                    nf.format(theoreticBD.doubleValue()), sign, nf.format(Math.abs(dev))));
        } else {
            sb.append("📉 Teoretická hodnota:  ---\n");
        }
        sb.append("\n");

        sb.append("📈 DISTRIBUČNÉ CHARAKTERISTIKY\n");
        sb.append("-----------------------------------------------\n");
        sb.append(String.format("❖ Priemerný počet úspešných väzňov:  %s z %s\n", nf.format(mean), intFmt.format(p.pocetVaznov)));
        sb.append(String.format("❖ Medián úspechov:                   %s\n", nf.format(median)));
        sb.append(String.format("❖ Smerodajná odchýlka (σ):           %s\n", nf.format(stddev)));
        sb.append(String.format("❖ Maximálny počet úspechov:          %s\n", intFmt.format(max)));
        sb.append("\n");

        sb.append("💡 INTERPRETÁCIA\n");
        sb.append("-----------------------------------------------\n");
        if (stddev > (p.pocetVaznov / 4.0)) {
            sb.append("👉 Vysoká smerodajná odchýlka potvrdzuje bimodálne\n   rozdelenie (úspech skupiny je viazaný na cykly).\n");
        } else {
            sb.append("👉 Nízka smerodajná odchýlka naznačuje rovnomerné\n   rozdelenie výsledkov okolo priemeru.\n");
        }
        sb.append("===============================================");

        return sb.toString();
    }

    private BigDecimal vypocitajTeoretickuPercent(int n) {
        if (n <= 0) return BigDecimal.ZERO;
        int m = n / 2;
        BigInteger[] fact = new BigInteger[n + 1];
        fact[0] = BigInteger.ONE;
        for (int i = 1; i <= n; i++) fact[i] = fact[i-1].multiply(BigInteger.valueOf(i));

        BigInteger[] p = new BigInteger[n + 1];
        p[0] = BigInteger.ONE;
        for (int i = 1; i <= n; i++) {
            BigInteger sum = BigInteger.ZERO;
            for (int k = 1; k <= Math.min(i, m); k++) {
                sum = sum.add(fact[i-1].divide(fact[i-k]).multiply(p[i-k]));
            }
            p[i] = sum;
        }
        return new BigDecimal(p[n]).divide(new BigDecimal(fact[n]), new MathContext(10, RoundingMode.HALF_UP)).multiply(BigDecimal.valueOf(100));
    }

    private void finishUI() {
        if (progressBar.progressProperty().isBound()) progressBar.progressProperty().unbind();
        progressBar.setVisible(false);
        btnCancel.setVisible(false);
        btnSpustit.setDisable(false);
        vboxVysledky.setVisible(true);
    }

    @FXML public void onCancelButtonClick() { if (currentTask != null) currentTask.cancel(); }

    @FXML
    private void onZobrazGrafClick() {
        if (poslednyResult == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/graph-view.fxml"));
            Parent root = loader.load();
            GraphController ctrl = loader.getController();
            ctrl.nastavData(posledneRawData, poslednyResult.sancaPrezitia, poslednaStrategiaNazov, poslednyResult.celkovaDistribuciaCyklov);
            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Grafické výsledky");
            s.setScene(new Scene(root));
            s.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void onExportReportClick() {
        if (txtReport == null) return;
        javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
        chooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Text Files", "*.txt"));
        java.io.File f = chooser.showSaveDialog(btnExportReport.getScene().getWindow());
        if (f == null) return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(f, java.nio.charset.StandardCharsets.UTF_8)) {
            pw.print(txtReport.getText());
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @FXML public void onCloseButtonClick(ActionEvent e) { ((Stage)((Node)e.getSource()).getScene().getWindow()).close(); }

    private Strategy najdiStrategiu(String n) { return availableStrategies.stream().filter(s -> s.nazovStrategie().equals(n)).findFirst().orElse(null); }

    private SimulationParams validujVstupy() {
        try { return new SimulationParams(Integer.parseInt(tfPocetVaznov.getText()), Integer.parseInt(tfPocetPermutacii.getText())); }
        catch (Exception e) { return null; }
    }

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