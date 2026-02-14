package GUI;

import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GraphController {
    @FXML private PieChart pieSimulation;
    @FXML private PieChart pieTheory;

    @FXML private HBox chartsBox;
    @FXML private Label lblSimTitle;
    @FXML private Label lblTheoryTitle;

    private static final double BASE_WIDTH = 600.0;
    private static final double BASE_HEIGHT = 400.0;

    @FXML
    public void initialize() {
        if (pieSimulation != null) pieSimulation.getData().clear();
        if (pieTheory != null) pieTheory.getData().clear();

        // bind scale so charts and labels grow/shrink with window size
        if (chartsBox != null) {
            // helper to set up binding once the scene is available
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
                        // scale a bit less aggressively (0.95 factor)
                        return Math.max(0.6, Math.min(sx, sy) * 0.95);
                    }
                };

                chartsBox.scaleXProperty().bind(scaleBinding);
                chartsBox.scaleYProperty().bind(scaleBinding);

                // Set headings to a smaller, regular font to avoid oversized titles
                if (lblSimTitle != null) lblSimTitle.setFont(Font.font("System", FontWeight.NORMAL, 10));
                if (lblTheoryTitle != null) lblTheoryTitle.setFont(Font.font("System", FontWeight.NORMAL, 10));
            };

            // If scene already present, set up immediately; otherwise wait for it
            if (chartsBox.getScene() != null) {
                setup.run();
            } else {
                chartsBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) setup.run();
                });
            }
        }
    }

    public void nastavData(double nameranaSancaPercent, String nazovStrategie) {
        double successSim = clampPercent(nameranaSancaPercent);
        double failSim = 100.0 - successSim;

        // Aktualizácia nadpisu, aby sme vedeli, čo porovnávame
        if (lblSimTitle != null) {
            lblSimTitle.setText("Simulácia: " + nazovStrategie);
            lblSimTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        }

        // 1. Nastavenie dát simulácie
        pieSimulation.getData().clear();
        PieChart.Data successSlice = new PieChart.Data(String.format("Úspech: %.2f%%", successSim), successSim);
        PieChart.Data failSlice = new PieChart.Data(String.format("Neúspech: %.2f%%", failSim), failSim);
        pieSimulation.getData().addAll(successSlice, failSlice);

        // 2. Dynamická Teoretická hodnota
        double theoryPercent = 0.0;
        // Len cyklická stratégia má teóriu ~31.18%, ostatné (náhodná, párna, nepárna) majú prakticky 0%
        if (nazovStrategie != null && nazovStrategie.toLowerCase().contains("cykl")) {
            theoryPercent = 31.18;
        }

        double theoryFail = 100.0 - theoryPercent;

        pieTheory.getData().clear();
        PieChart.Data tSuccess = new PieChart.Data(String.format("Úspech: %.2f%%", theoryPercent), theoryPercent);
        PieChart.Data tFail = new PieChart.Data(String.format("Neúspech: %.2f%%", theoryFail), theoryFail);
        pieTheory.getData().addAll(tSuccess, tFail);

        // 3. Farbenie - použijeme tvoje metódy
        setSliceColorWhenReady(successSlice, "#2ecc71");
        setSliceColorWhenReady(failSlice, "#e74c3c");
        setSliceColorWhenReady(tSuccess, "#2ecc71");
        setSliceColorWhenReady(tFail, "#e74c3c");

        // 4. Oprava legendy a tooltipy zostávajú rovnaké
        Platform.runLater(() -> {
            prefarbiLegendu(pieSimulation);
            prefarbiLegendu(pieTheory);
        });

        attachTooltipWhenReady(successSlice, String.format("%.2f%% úspech", successSim));
        attachTooltipWhenReady(failSlice, String.format("%.2f%% neúspech", failSim));
        attachTooltipWhenReady(tSuccess, String.format("%.2f%% teória", theoryPercent));
        attachTooltipWhenReady(tFail, String.format("%.2f%% teória zlyhania", theoryFail));
    }

    private void prefarbiLegendu(PieChart chart) {
        // Hľadáme všetky grafické prvky symbolov v legende
        for (Node node : chart.lookupAll(".chart-legend-item-symbol")) {
            // Každý symbol je vnútri Labelu, ktorý obsahuje text "Uspech" alebo "Neuspech"
            if (node.getParent() instanceof Label label) {
                String text = label.getText();
                if (text.contains("Uspech")) {
                    node.setStyle("-fx-background-color: #2ecc71;");
                } else if (text.contains("Neuspech")) {
                    node.setStyle("-fx-background-color: #e74c3c;");
                }
            }
        }
    }

    private void setSliceColorWhenReady(PieChart.Data slice, String colorHex) {
        if (slice.getNode() != null) {
            slice.getNode().setStyle(String.format("-fx-pie-color: %s;", colorHex));
        } else {
            slice.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) newNode.setStyle(String.format("-fx-pie-color: %s;", colorHex));
            });
        }
    }

    private void attachTooltipWhenReady(PieChart.Data slice, String text) {
        if (slice.getNode() != null) {
            Tooltip.install(slice.getNode(), new Tooltip(text));
        } else {
            slice.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) Tooltip.install(newNode, new Tooltip(text));
            });
        }
    }

    private double clampPercent(double v) {
        if (Double.isNaN(v) || v < 0) return 0.0;
        if (v > 100) return 100.0;
        return v;
    }

    @FXML
    public void onClose(javafx.event.ActionEvent event) {
        ((javafx.stage.Stage)((javafx.scene.Node)event.getSource()).getScene().getWindow()).close();
    }
}