package GUI;

import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.Set;

public class GraphController {
    @FXML private PieChart pieSimulation;
    @FXML private PieChart pieTheory;
    @FXML private HBox chartsBox;
    @FXML private Label lblSimTitle;
    @FXML private Label lblTheoryTitle; // used now to avoid unused warning

    private static final String SUCCESS_COLOR = "#2ecc71";
    private static final String FAIL_COLOR = "#e74c3c";
    private static final double BASE_WIDTH = 600.0;
    private static final double BASE_HEIGHT = 400.0;
    private static final double MIN_SCALE = 0.6;
    private static final double SCALE_PAD = 0.95;
    private static final double TITLE_FONT_SIZE = 12.0;

    @FXML
    public void initialize() {
        if (pieSimulation != null) pieSimulation.getData().clear();
        if (pieTheory != null) pieTheory.getData().clear();

        bindChartsScaling();
    }

    // Small orchestrator that populates both pies (simulation + theory)
    public void nastavData(double nameranaSancaPercent, String nazovStrategie) {
        double successSim = clampPercent(nameranaSancaPercent);

        if (lblSimTitle != null) {
            lblSimTitle.setText("Simulácia: " + nazovStrategie);
            lblSimTitle.setFont(Font.font("System", FontWeight.BOLD, TITLE_FONT_SIZE));
        }
        if (lblTheoryTitle != null) {
            lblTheoryTitle.setText("Teoretická hodnota");
            lblTheoryTitle.setFont(Font.font("System", FontWeight.BOLD, TITLE_FONT_SIZE));
        }

        // Populate simulation pie
        populatePie(pieSimulation, successSim);

        // Simple heuristic for theory (keeps previous behavior)
        double theoryPercent = (nazovStrategie != null && nazovStrategie.toLowerCase().contains("cykl")) ? 31.18 : 0.0;
        populatePie(pieTheory, theoryPercent);
    }

    // --- Refactored helpers ---
    private void bindChartsScaling() {
        if (chartsBox == null) return;

        Runnable setup = () -> {
            if (chartsBox.getScene() == null) return;
            DoubleBinding scaleBinding = new DoubleBinding() {
                { super.bind(chartsBox.getScene().widthProperty(), chartsBox.getScene().heightProperty()); }
                @Override
                protected double computeValue() {
                    double w = chartsBox.getScene().getWidth();
                    double h = chartsBox.getScene().getHeight();
                    double sx = w / BASE_WIDTH;
                    double sy = h / BASE_HEIGHT;
                    return Math.max(MIN_SCALE, Math.min(sx, sy) * SCALE_PAD);
                }
            };
            chartsBox.scaleXProperty().bind(scaleBinding);
            chartsBox.scaleYProperty().bind(scaleBinding);
        };

        if (chartsBox.getScene() != null) setup.run();
        else chartsBox.sceneProperty().addListener((obs, oldScene, newScene) -> { if (newScene != null) setup.run(); });
    }

    private void populatePie(PieChart chart, double successPercent) {
        if (chart == null) return;

        double success = clampPercent(successPercent);
        double fail = Math.max(0.0, 100.0 - success);

        chart.getData().clear();
        PieChart.Data successSlice = new PieChart.Data(String.format("Úspech: %.2f%%", success), success);
        PieChart.Data failSlice = new PieChart.Data(String.format("Neúspech: %.2f%%", fail), fail);
        chart.getData().addAll(successSlice, failSlice);

        // apply visuals
        setupSlice(successSlice, SUCCESS_COLOR, String.format("%.2f%% úspech", success));
        setupSlice(failSlice, FAIL_COLOR, String.format("%.2f%% neúspech", fail));

        Platform.runLater(() -> styleLegend(chart));
    }

    // Sets pie slice color and installs tooltip; safe if node isn't created yet.
    private void setupSlice(PieChart.Data slice, String colorHex, String tooltipText) {
        if (slice == null) return;
        Runnable apply = () -> {
            Node node = slice.getNode();
            if (node != null) {
                node.setStyle(String.format("-fx-pie-color: %s;", colorHex));
                Tooltip.install(node, new Tooltip(tooltipText));
            }
        };

        if (slice.getNode() != null) apply.run();
        else slice.nodeProperty().addListener((obs, oldNode, newNode) -> { if (newNode != null) apply.run(); });
    }

    // Deterministically color legend symbols by index: first = success, others = failure
    private void styleLegend(PieChart chart) {
        if (chart == null) return;
        Platform.runLater(() -> {
            Node legendNode = chart.lookup(".chart-legend");
            if (legendNode instanceof Parent) {
                int idx = 0;
                for (Node item : ((Parent) legendNode).getChildrenUnmodifiable()) {
                    try {
                        Node symbol = item.lookup(".chart-legend-item-symbol");
                        if (symbol != null && idx < chart.getData().size()) {
                            symbol.setStyle(idx == 0
                                    ? ("-fx-background-color: " + SUCCESS_COLOR + " !important; -fx-background-radius: 5px;")
                                    : ("-fx-background-color: " + FAIL_COLOR + " !important; -fx-background-radius: 5px;"));
                        }
                    } catch (Exception ignored) { }
                    idx++;
                }
                return;
            }

            // fallback
            Set<Node> symbols = chart.lookupAll(".chart-legend-item-symbol");
            int i = 0;
            for (Node symbol : symbols) {
                if (i < chart.getData().size()) {
                    symbol.setStyle(i == 0
                            ? ("-fx-background-color: " + SUCCESS_COLOR + " !important; -fx-background-radius: 5px;")
                            : ("-fx-background-color: " + FAIL_COLOR + " !important; -fx-background-radius: 5px;"));
                }
                i++;
            }
        });
    }


    private double clampPercent(double v) {
        if (Double.isNaN(v) || v < 0) return 0.0;
        return Math.min(v, 100.0);
    }

    @FXML
    public void onClose(javafx.event.ActionEvent event) {
        ((javafx.stage.Stage)((javafx.scene.Node)event.getSource()).getScene().getWindow()).close();
    }
}