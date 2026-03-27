package GUI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javax.imageio.ImageIO;

public class GraphController {

    // Toto fx:id musí byť priradené najvrchnejšiemu prvku v Scene Builderi (VBox/AnchorPane)
    @FXML private Pane mainContainer;

    @FXML private PieChart pieSimulation, pieTheory;
    @FXML private BarChart<String, Number> histogram;
    @FXML private Label lblSimTitle, lblMean, lblStdDev, lblMedian, lblMax, lblTotalRuns;

    private static final String SUCCESS_COLOR = "#2ecc71";
    private static final String FAIL_COLOR = "#e74c3c";
    private static final String BIN_COLOR = "#e67e22";

    @FXML
    public void initialize() {
        if (pieSimulation != null) pieSimulation.getData().clear();
        if (pieTheory != null) pieTheory.getData().clear();
        if (histogram != null) {
            histogram.getData().clear();
            histogram.setAnimated(false);
        }
    }

    public void nastavData(List<Integer> runResults, double nameranaSancaPercent, String nazovStrategie) {
        if (lblSimTitle != null) lblSimTitle.setText("Simulácia: " + nazovStrategie);

        populatePie(pieSimulation, nameranaSancaPercent);
        double theoryPercent = (nazovStrategie != null && nazovStrategie.toLowerCase().contains("cykl")) ? 31.18 : 0.0;
        populatePie(pieTheory, theoryPercent);

        if (runResults != null && !runResults.isEmpty()) {
            populateHistogram(runResults);
            computeAndShowStats(runResults);
        }
    }

    private void populateHistogram(List<Integer> runs) {
        histogram.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        int nVaznov = runs.stream().mapToInt(i -> i).max().orElse(100);

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

        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    String currentVal = data.getXValue();
                    String barColor = currentVal.equals(String.valueOf(nVaznov)) ? SUCCESS_COLOR : BIN_COLOR;
                    node.setStyle("-fx-bar-fill: " + barColor + ";");

                    Tooltip t = new Tooltip("Úspešných: " + currentVal + "\nFrekvencia: " + data.getYValue());
                    t.setShowDelay(javafx.util.Duration.millis(100));
                    Tooltip.install(node, t);

                    node.setOnMouseEntered(e -> node.setStyle("-fx-bar-fill: " + barColor + "; -fx-brightness: 1.2; -fx-cursor: hand;"));
                    node.setOnMouseExited(e -> node.setStyle("-fx-bar-fill: " + barColor + "; -fx-brightness: 1.0;"));
                }
            }
        });
        histogram.getXAxis().setTickLabelRotation(nVaznov > 200 ? 45 : 0);
    }

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

        if (stats.getMax() == (nVaznovRef(runs))) {
            lblMax.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }
    }

    private int nVaznovRef(List<Integer> runs) {
        return runs.stream().mapToInt(i -> i).max().orElse(100);
    }

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

                // --- Moderný Custom Alert ---
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Export dokončený");
                a.setHeaderText(null); // Odstránime predvolený header pre čistejší dizajn

                // Stylovanie samotného okna cez kód (ak nemáš externé CSS)
                DialogPane dialogPane = a.getDialogPane();
                dialogPane.setStyle("-fx-background-color: white; -fx-font-family: 'Segoe UI', sans-serif;");

                // Veľká zelená ikona úspechu
                Label icon = new Label("✅");
                icon.setStyle("-fx-font-size: 40px; -fx-text-fill: #2ecc71; -fx-padding: 0 10 0 0;");
                a.setGraphic(icon);

                // Formátovaný obsah s cestou k súboru
                VBox content = new VBox(10);
                Label titleLabel = new Label("Obrázok bol úspešne vygenerovaný!");
                titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                Label pathLabel = new Label(file.getAbsolutePath());
                pathLabel.setWrapText(true);
                pathLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-background-color: #f0f0f0; -fx-padding: 8; -fx-background-radius: 5; -fx-text-fill: #34495e;");
                pathLabel.setMaxWidth(400);

                content.getChildren().addAll(titleLabel, pathLabel);
                a.getDialogPane().setContent(content);

                // Vlastné tlačidlá
                ButtonType openBtn = new ButtonType("📂 Otvoriť priečinok");
                ButtonType copyBtn = new ButtonType("📋 Kopírovať cestu");
                ButtonType okBtn = new ButtonType("Hotovo", ButtonBar.ButtonData.OK_DONE);

                a.getButtonTypes().setAll(openBtn, copyBtn, okBtn);

                // Logika tlačidiel
                Optional<ButtonType> resp = a.showAndWait();
                if (resp.isPresent()) {
                    if (resp.get() == openBtn) {
                        if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file.getParentFile());
                    } else if (resp.get() == copyBtn) {
                        ClipboardContent cc = new ClipboardContent();
                        cc.putString(file.getAbsolutePath());
                        Clipboard.getSystemClipboard().setContent(cc);
                        // Malá spätná väzba po skopírovaní (voliteľné)
                    }
                }
            } catch (IOException e) {
                Alert error = new Alert(Alert.AlertType.ERROR, "Chyba: " + e.getMessage());
                error.showAndWait();
            }
        }
    }

    private void populatePie(PieChart chart, double success) {
        chart.getData().clear();
        PieChart.Data sSlice = new PieChart.Data(String.format("Úspech (%.1f%%)", success), success);
        PieChart.Data fSlice = new PieChart.Data(String.format("Neúspech (%.1f%%)", 100 - success), 100 - success);
        chart.getData().addAll(sSlice, fSlice);
        applySliceStyle(sSlice, SUCCESS_COLOR);
        applySliceStyle(fSlice, FAIL_COLOR);

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
        if (slice.getNode() != null) slice.getNode().setStyle("-fx-pie-color: " + color + ";");
        else slice.nodeProperty().addListener((o, old, n) -> { if (n != null) n.setStyle("-fx-pie-color: " + color + ";"); });
    }

    @FXML
    public void onClose(javafx.event.ActionEvent event) {
        ((javafx.stage.Stage)((Node)event.getSource()).getScene().getWindow()).close();
    }

    // Convert a JavaFX WritableImage to AWT BufferedImage without SwingFXUtils
    private static BufferedImage fromFXImage(WritableImage img) {
        int width = (int) img.getWidth();
        int height = (int) img.getHeight();
        BufferedImage buf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        javafx.scene.image.PixelReader pr = img.getPixelReader();
        int[] buffer = new int[width];
        for (int y = 0; y < height; y++) {
            pr.getPixels(0, y, width, 1, javafx.scene.image.WritablePixelFormat.getIntArgbInstance(), buffer, 0, width);
            for (int x = 0; x < width; x++) {
                buf.setRGB(x, y, buffer[x]);
            }
        }
        return buf;
    }
}
