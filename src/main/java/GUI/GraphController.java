package GUI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.awt.Desktop;
import javax.imageio.ImageIO;

/**
 * Kontrolér, ktorý sa stará o vykresľovanie štatistických vizualizácií (koláčové grafy a histogramy)
 * pre výsledky simulácie. Dostáva agregované dáta z `PermutationsController` a plní JavaFX grafy.
 * Poskytuje aj export do PNG.
 */
public class GraphController {

    @FXML private Pane mainContainer;
    @FXML private PieChart pieSimulation, pieTheory;
    @FXML private BarChart<String, Number> histogram;

    // --- NOVÝ GRAF PRE CYKLY ---
    @FXML private BarChart<String, Number> cycleHistogram;

    @FXML private Label lblSimTitle, lblMean, lblStdDev, lblMedian, lblMax, lblTotalRuns;
    @FXML private javafx.scene.layout.VBox cycleSection;

    private static final String SUCCESS_COLOR = "#2ecc71"; // Zelená
    private static final String FAIL_COLOR = "#e74c3c";    // Červená
    private static final String BIN_COLOR = "#e67e22";     // Oranžová

    /**
     * Inicializácia kontroléra: vyčistí grafy a nastaví počiatočné stavy.
     * Volané automaticky JavaFX pri načítaní FXML.
     */
    @FXML
    public void initialize() {
        if (pieSimulation != null) pieSimulation.getData().clear();
        if (pieTheory != null) pieTheory.getData().clear();
        if (histogram != null) {
            histogram.getData().clear();
            histogram.setAnimated(false);
        }
        if (cycleHistogram != null) {
            cycleHistogram.getData().clear();
            cycleHistogram.setAnimated(false);
        }

        // Skryť sekciu pre cykly štandardne; controller ju zobrazí len pre cyklické stratégie
        if (cycleSection != null) { cycleSection.setVisible(false); cycleSection.setManaged(false); }

    }

    /**
     * Nastaví a vykreslí všetky potrebné grafy.
     * @param runResults zoznam výsledkov (úspešných väzňov) pre jednotlivé behy
     * @param nameranaSancaPercent meraná úspešnosť v percentách (0..100)
     * @param nazovStrategie názov stratégie (slúži na určenie, či ide o cyklickú stratégiu)
     * @param akumulovaneCykly voliteľná agregovaná distribúcia dĺžok cyklov (dĺžka -> počet výskytov)
     */
    public void nastavData(List<Integer> runResults, double nameranaSancaPercent, String nazovStrategie, Map<Integer, Integer> akumulovaneCykly) {
        if (lblSimTitle != null) lblSimTitle.setText("Simulácia: " + nazovStrategie);

        boolean isCyclic = false;
        if (nazovStrategie != null) {
            String n = nazovStrategie.toLowerCase();
            isCyclic = n.contains("cykl") || n.contains("cycle");
        }
        if (cycleSection != null) { cycleSection.setVisible(isCyclic); cycleSection.setManaged(isCyclic); }

        populatePie(pieSimulation, nameranaSancaPercent);
        double theoryPercent = (nazovStrategie != null && nazovStrategie.toLowerCase().contains("cykl")) ? 31.18 : 0.0;
        populatePie(pieTheory, theoryPercent);

        if (runResults != null && !runResults.isEmpty()) {
            populateHistogram(runResults);
            computeAndShowStats(runResults);
        }

        // --- Vykreslenie AKUMULOVANÉHO histogramu ---
        if (isCyclic && akumulovaneCykly != null && !akumulovaneCykly.isEmpty()) {
             cycleHistogram.getData().clear();
             XYChart.Series<String, Number> series = new XYChart.Series<>();

             // Utriedime podľa dĺžky (klúča)
             List<Integer> sortedKeys = new ArrayList<>(akumulovaneCykly.keySet());
             Collections.sort(sortedKeys);

             for (int dlzka : sortedKeys) {
                 int pocetVyskytov = akumulovaneCykly.get(dlzka);
                 // Pridáme do grafu len ak je to podstatné (napr. každú dĺžku)
                 series.getData().add(new XYChart.Data<>(String.valueOf(dlzka), pocetVyskytov));
             }

             cycleHistogram.getData().add(series);

             // Stylovanie (Zelená <= 50, Červená > 50)
             Platform.runLater(() -> {
                 for (XYChart.Data<String, Number> data : series.getData()) {
                     Node node = data.getNode();
                     if (node != null) {
                         int dlzka = Integer.parseInt(data.getXValue());
                         node.setStyle("-fx-bar-fill: " + (dlzka > 50 ? "#e74c3c" : "#2ecc71") + ";");

                         // Tooltip pre stĺpce cyklov (pridal som sem, predtým bol len v nepoužívanej metóde)
                         Tooltip t = new Tooltip("Dĺžka cyklu: " + dlzka + "\nPočet v permutácii: " + data.getYValue());
                         Tooltip.install(node, t);
                     }
                 }
             });
        } else {
            // If not cyclic or no data, ensure cycle histogram is cleared to avoid stale visuals
            if (cycleHistogram != null) cycleHistogram.getData().clear();
        }
    }


    /**
     * Vytvorí a naplní histogram (bar chart) agregovanými výsledkami.
     * Zabezpečí aj tooltipy pre jednotlivé stĺpce.
     */
    private void populateHistogram(List<Integer> runs) {
        histogram.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        int nVaznov = runs.stream().mapToInt(i -> i).max().orElse(100);

        // Agregácia dát do binov pre lepšiu prehľadnosť (pôvodná logika)
        if (nVaznov > 50) {
            int velkostBinu = 2; // Nastaviteľné
            Map<Integer, Long> bins = new TreeMap<>();
            long uspesneSimulacieVsetci = 0;

            for (Integer vysledok : runs) {
                if (vysledok == nVaznov) { uspesneSimulacieVsetci++; continue; }
                int binStart = (vysledok / velkostBinu) * velkostBinu;
                bins.put(binStart, bins.getOrDefault(binStart, 0L) + 1);
            }

            for (Map.Entry<Integer, Long> entry : bins.entrySet()) {
                int start = entry.getKey();
                int end = start + velkostBinu - 1;
                series.getData().add(new XYChart.Data<>(start + "-" + end, entry.getValue()));
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

        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    String barColor = data.getXValue().contains(String.valueOf(nVaznov)) ? SUCCESS_COLOR : BIN_COLOR;
                    node.setStyle("-fx-bar-fill: " + barColor + ";");

                    // Show tooltip with the count when hovering over a bar
                    Tooltip t = new Tooltip("Počet: " + data.getYValue());
                    Tooltip.install(node, t);
                }
            }
        });
    }

    /**
     * Vypočíta štatistiky (priemer, medián, smerodajná odchýlka) a vypíše ich do štítkov.
     */
    private void computeAndShowStats(List<Integer> runs) {
        int n = runs.size();
        IntSummaryStatistics stats = runs.stream().mapToInt(i -> i).summaryStatistics();
        double mean = stats.getAverage();
        double variance = runs.stream().mapToDouble(i -> Math.pow(i - mean, 2)).sum() / n;
        double stddev = Math.sqrt(variance);

        List<Integer> sorted = runs.stream().sorted().toList();
        double median = (n % 2 == 1) ? sorted.get(n / 2) : ((sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0);

        lblTotalRuns.setText(String.format("🔄 Počet simulácií:   %,d", n));
        lblMean.setText(String.format("📊 Priemer úspechov:  %.2f", mean));
        lblStdDev.setText(String.format("📉 Smerodajná odch.:  %.2f (σ)", stddev));
        lblMedian.setText(String.format("📍 Medián:            %.1f", median));
        lblMax.setText(String.format("🏆 Max. v jednej:     %d", stats.getMax()));
    }

    /**
     * Exportuje aktuálny pohľad (mainContainer) do PNG súboru; otvorí dialóg na uloženie.
     */
    @FXML
    public void exportujDoPNG() {
        if (mainContainer == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Uložiť výsledky ako obrázok");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Obrázky", "*.png"));
        fc.setInitialFileName("vysledky_simulacie.png");

        File file = fc.showSaveDialog(mainContainer.getScene().getWindow());

        if (file != null) {
            try {
                SnapshotParameters sp = new SnapshotParameters();
                sp.setFill(Color.WHITE);
                WritableImage image = mainContainer.snapshot(sp, null);
                BufferedImage bimage = fromFXImage(image);
                ImageIO.write(bimage, "png", file);

                // --- Moderný Alert ---
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Export dokončený");
                a.setHeaderText(null);
                DialogPane dialogPane = a.getDialogPane();
                dialogPane.setStyle("-fx-background-color: white; -fx-font-family: 'Segoe UI', sans-serif;");

                Label icon = new Label("✅");
                icon.setStyle("-fx-font-size: 40px; -fx-text-fill: #2ecc71; -fx-padding: 0 10 0 0;");
                a.setGraphic(icon);

                VBox content = new VBox(10);
                Label titleLabel = new Label("Obrázok bol úspešne vygenerovaný!");
                Label pathLabel = new Label(file.getAbsolutePath());
                pathLabel.setWrapText(true);
                pathLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-background-color: #f0f0f0; -fx-padding: 8; -fx-background-radius: 5;");

                content.getChildren().addAll(titleLabel, pathLabel);
                a.getDialogPane().setContent(content);

                ButtonType openBtn = new ButtonType("📂 Otvoriť");
                ButtonType okBtn = new ButtonType("Hotovo", ButtonBar.ButtonData.OK_DONE);
                a.getButtonTypes().setAll(openBtn, okBtn);

                Optional<ButtonType> resp = a.showAndWait();
                if (resp.isPresent() && resp.get() == openBtn) {
                    Desktop.getDesktop().open(file.getParentFile());
                }
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Chyba: " + e.getMessage()).showAndWait();
            }
        }
    }

    /**
     * Pomocná metóda na vykreslenie koláčového grafu: naplní dáta a aplikuje farby.
     */
    private void populatePie(PieChart chart, double success) {
        chart.getData().clear();
        PieChart.Data sSlice = new PieChart.Data(String.format("Úspech (%.1f%%)", success), success);
        PieChart.Data fSlice = new PieChart.Data(String.format("Neúspech (%.1f%%)", 100 - success), 100 - success);
        chart.getData().addAll(sSlice, fSlice);
        applySliceStyle(sSlice, SUCCESS_COLOR);
        applySliceStyle(fSlice, FAIL_COLOR);
    }

    /**
     * Aplikuje požadované štýly na daný kúsoček koláčového grafu (pie slice).
     */
    private void applySliceStyle(PieChart.Data slice, String color) {
        if (slice.getNode() != null) slice.getNode().setStyle("-fx-pie-color: " + color + ";");
        else slice.nodeProperty().addListener((o, old, n) -> { if (n != null) n.setStyle("-fx-pie-color: " + color + ";"); });
    }

    @FXML
    public void onClose(javafx.event.ActionEvent event) {
        ((javafx.stage.Stage)((Node)event.getSource()).getScene().getWindow()).close();
    }

    /**
     * Konvertuje JavaFX WritableImage na AWT BufferedImage (pre ImageIO export).
     */
    private static BufferedImage fromFXImage(WritableImage img) {
        int width = (int) img.getWidth();
        int height = (int) img.getHeight();
        BufferedImage buf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        javafx.scene.image.PixelReader pr = img.getPixelReader();
        int[] buffer = new int[width];
        for (int y = 0; y < height; y++) {
            pr.getPixels(0, y, width, 1, javafx.scene.image.WritablePixelFormat.getIntArgbInstance(), buffer, 0, width);
            for (int x = 0; x < width; x++) buf.setRGB(x, y, buffer[x]);
        }
        return buf;
    }
}
