package GUI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import java.util.*;
import java.util.stream.Collectors;

public class GraphController {
    @FXML private PieChart pieSimulation, pieTheory;
    @FXML private BarChart<String, Number> histogram;
    @FXML private Label lblSimTitle, lblMean, lblStdDev, lblMedian, lblMax, lblTotalRuns;

    private static final String SUCCESS_COLOR = "#2ecc71";
    private static final String FAIL_COLOR = "#e74c3c";

    @FXML
    public void initialize() {
        // Resetujeme grafy pri štarte aplikácie
        if (pieSimulation != null) pieSimulation.getData().clear();
        if (pieTheory != null) pieTheory.getData().clear();
        if (histogram != null) {
            histogram.getData().clear();
            histogram.setAnimated(false); // Dôležité pre stabilitu pri zmene veľkosti okna
        }
    }

    /**
     * Hlavná metóda na naplnenie grafov dátami zo simulácie.
     */
    public void nastavData(List<Integer> runResults, double nameranaSancaPercent, String nazovStrategie) {
        if (lblSimTitle != null) lblSimTitle.setText("Simulácia: " + nazovStrategie);

        // 1. Koláčové grafy (Simulovaná vs. Teoretická hodnota)
        populatePie(pieSimulation, nameranaSancaPercent);

        // Dynamické nastavenie teórie podľa stratégie
        double theoryPercent = (nazovStrategie != null && nazovStrategie.toLowerCase().contains("cykl")) ? 31.18 : 0.0;
        populatePie(pieTheory, theoryPercent);

        // 2. Histogram a Štatistiky
        if (runResults != null && !runResults.isEmpty()) {
            populateHistogram(runResults);
            computeAndShowStats(runResults);
        }
    }

    /**
     * Vykreslí histogram. Používame diskrétne hodnoty (bez binningu) pre maximálnu presnosť
     * pri interpretácii bimodálneho rozdelenia (0-50 vs 100).
     */
    private void populateHistogram(List<Integer> runs) {
        histogram.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        // Zistíme počet väzňov (buď z max výsledku alebo predpokladáme 100/1000)
        int nVaznov = runs.stream().mapToInt(i -> i).max().orElse(100);

        // 1. Logika binningu (zoskupovanie)
        if (nVaznov > 50) {
            int pocetBinov = 50;
            int velkostBinu = Math.max(1, nVaznov / pocetBinov);
            Map<Integer, Long> bins = new TreeMap<>();
            long uspesneSimulacieVsetci = 0;

            for (Integer vysledok : runs) {
                if (vysledok == nVaznov) { uspesneSimulacieVsetci++; continue; }
                int binStart = (vysledok / velkostBinu) * velkostBinu;
                bins.put(binStart, bins.getOrDefault(binStart, 0L) + 1);
            }

            for (Map.Entry<Integer, Long> entry : bins.entrySet()) {
                int start = entry.getKey();
                int end = Math.min(start + velkostBinu - 1, nVaznov - 1);
                String label = (start == end) ? String.valueOf(start) : start + "-" + end;
                series.getData().add(new XYChart.Data<>(label, entry.getValue()));
            }
            if (uspesneSimulacieVsetci > 0) {
                series.getData().add(new XYChart.Data<>(String.valueOf(nVaznov), uspesneSimulacieVsetci));
            }
        } else {
            Map<Integer, Long> freq = runs.stream().collect(Collectors.groupingBy(i -> i, Collectors.counting()));
            for (int v = 0; v <= nVaznov; v++) {
                if (freq.containsKey(v)) series.getData().add(new XYChart.Data<>(String.valueOf(v), freq.get(v)));
            }
        }

        histogram.getData().add(series);

        // OBRANA PROTI NULL: Musíme počkať, kým JavaFX vytvorí grafické uzly stĺpcov
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    // Určenie farby
                    String barColor = data.getXValue().equals(String.valueOf(nVaznov)) ? SUCCESS_COLOR : "#e67e22";
                    node.setStyle("-fx-bar-fill: " + barColor + ";");

                    // Tooltip
                    Tooltip t = new Tooltip("Počet úspešných: " + data.getXValue() + "\nFrekvencia: " + data.getYValue());
                    // Odstránenie oneskorenia tooltipu (voliteľné)
                    t.setShowDelay(javafx.util.Duration.millis(100));
                    Tooltip.install(node, t);

                    // Hover efekty cez MouseEvents
                    node.setOnMouseEntered(e -> node.setStyle("-fx-bar-fill: " + barColor + "; -fx-brightness: 1.2; -fx-cursor: hand;"));
                    node.setOnMouseExited(e -> node.setStyle("-fx-bar-fill: " + barColor + "; -fx-brightness: 1.0;"));
                }
            }
        });

        histogram.getXAxis().setTickLabelRotation(nVaznov > 200 ? 45 : 0);
    }

    /**
     * Vypočíta a zobrazí štatistické deskriptory.
     */
    private void computeAndShowStats(List<Integer> runs) {
        int n = runs.size();
        IntSummaryStatistics stats = runs.stream().mapToInt(i -> i).summaryStatistics();
        double mean = stats.getAverage();

        // Výpočet smerodajnej odchýlky (sigma)
        double variance = runs.stream().mapToDouble(i -> Math.pow(i - mean, 2)).sum() / n;
        double stddev = Math.sqrt(variance);

        // Výpočet mediánu
        List<Integer> sorted = runs.stream().sorted().toList();
        double median = (n % 2 == 1) ? sorted.get(n / 2) : ((sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0);

        // Aktualizácia Labelov v UI
        lblTotalRuns.setText(String.format("Simulácií: %,d", n));
        lblMean.setText(String.format("Priemer: %.2f", mean));
        lblStdDev.setText(String.format("Smerodajná odchýlka: %.2f", stddev));
        lblMedian.setText(String.format("Medián: %.1f", median));
        lblMax.setText(String.format("Max úspechov: %d", stats.getMax()));
    }

    /**
     * Nastaví PieChart s jednotným farebným štýlom.
     */
    private void populatePie(PieChart chart, double success) {
        chart.getData().clear();
        PieChart.Data sSlice = new PieChart.Data(String.format("Úspech (%.1f%%)", success), success);
        PieChart.Data fSlice = new PieChart.Data(String.format("Neúspech (%.1f%%)", 100 - success), 100 - success);
        chart.getData().addAll(sSlice, fSlice);

        // Aplikácia farieb priamo na segmenty
        applySliceStyle(sSlice, SUCCESS_COLOR);
        applySliceStyle(fSlice, FAIL_COLOR);

        // Aplikácia farieb do legendy (riešené cez Platform.runLater, aby bol graf už vykreslený)
        Platform.runLater(() -> {
            Node legend = chart.lookup(".chart-legend");
            if (legend instanceof Parent p) {
                int i = 0;
                for (Node item : p.getChildrenUnmodifiable()) {
                    Node symbol = item.lookup(".chart-legend-item-symbol");
                    if (symbol != null) {
                        symbol.setStyle("-fx-background-color: " + (i == 0 ? SUCCESS_COLOR : FAIL_COLOR) + " !important;");
                    }
                    i++;
                }
            }
        });
    }

    private void applySliceStyle(PieChart.Data slice, String color) {
        if (slice.getNode() != null) {
            slice.getNode().setStyle("-fx-pie-color: " + color + ";");
        } else {
            slice.nodeProperty().addListener((obs, old, newNode) -> {
                if (newNode != null) newNode.setStyle("-fx-pie-color: " + color + ";");
            });
        }
    }

    @FXML
    public void onClose(javafx.event.ActionEvent event) {
        ((javafx.stage.Stage)((javafx.scene.Node)event.getSource()).getScene().getWindow()).close();
    }
}