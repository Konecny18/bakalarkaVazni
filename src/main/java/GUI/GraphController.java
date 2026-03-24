package GUI;

import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent; // ✅ TENTO IMPORT CHÝBAL
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
    @FXML private Label lblTheoryTitle;

    private static final double BASE_WIDTH = 600.0;
    private static final double BASE_HEIGHT = 400.0;

    @FXML
    public void initialize() {
        if (pieSimulation != null) pieSimulation.getData().clear();
        if (pieTheory != null) pieTheory.getData().clear();

        if (chartsBox != null) {
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
                        return Math.max(0.6, Math.min(sx, sy) * 0.95);
                    }
                };
                chartsBox.scaleXProperty().bind(scaleBinding);
                chartsBox.scaleYProperty().bind(scaleBinding);
            };

            if (chartsBox.getScene() != null) setup.run();
            else chartsBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) setup.run();
            });
        }
    }

    public void nastavData(double nameranaSancaPercent, String nazovStrategie) {
        double successSim = clampPercent(nameranaSancaPercent);
        double failSim = 100.0 - successSim;

        if (lblSimTitle != null) {
            lblSimTitle.setText("Simulácia: " + nazovStrategie);
            lblSimTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        }

        pieSimulation.getData().clear();
        PieChart.Data successSlice = new PieChart.Data(String.format("Úspech: %.2f%%", successSim), successSim);
        PieChart.Data failSlice = new PieChart.Data(String.format("Neúspech: %.2f%%", failSim), failSim);
        pieSimulation.getData().addAll(successSlice, failSlice);

        double theoryPercent = (nazovStrategie != null && nazovStrategie.toLowerCase().contains("cykl")) ? 31.18 : 0.0;
        double theoryFail = 100.0 - theoryPercent;

        pieTheory.getData().clear();
        PieChart.Data tSuccess = new PieChart.Data(String.format("Úspech: %.2f%%", theoryPercent), theoryPercent);
        PieChart.Data tFail = new PieChart.Data(String.format("Neúspech: %.2f%%", theoryFail), theoryFail);
        pieTheory.getData().addAll(tSuccess, tFail);

        setSliceColorWhenReady(successSlice, "#2ecc71");
        setSliceColorWhenReady(failSlice, "#e74c3c");
        setSliceColorWhenReady(tSuccess, "#2ecc71");
        setSliceColorWhenReady(tFail, "#e74c3c");

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
        Platform.runLater(() -> {
            // Prefer getting legend items in visual order from the legend container so
            // we can map colors deterministically by index (0 = success, 1 = failure).
            Node legendNode = chart.lookup(".chart-legend");
            if (legendNode instanceof Parent) {
                Parent legendParent = (Parent) legendNode;
                int idx = 0;
                for (Node item : legendParent.getChildrenUnmodifiable()) {
                    try {
                        Node symbol = item.lookup(".chart-legend-item-symbol");
                        if (symbol != null) {
                            if (idx < chart.getData().size()) {
                                // Index 0 = success (green), others = failure (red)
                                if (idx == 0) {
                                    symbol.setStyle("-fx-background-color: #2ecc71 !important; -fx-background-radius: 5px;");
                                } else {
                                    symbol.setStyle("-fx-background-color: #e74c3c !important; -fx-background-radius: 5px;");
                                }
                            }
                        }
                    } catch (Exception ignored) {
                        // If any lookup fails for a particular item, continue to next.
                    }
                    idx++;
                }
                return;
            }

            // Fallback: if we couldn't access legend container, use lookupAll but rely on
            // ordering to set first as success and others as failure.
            Set<Node> symbols = chart.lookupAll(".chart-legend-item-symbol");
            int i = 0;
            for (Node symbol : symbols) {
                if (i < chart.getData().size()) {
                    if (i == 0) {
                        symbol.setStyle("-fx-background-color: #2ecc71 !important; -fx-background-radius: 5px;");
                    } else {
                        symbol.setStyle("-fx-background-color: #e74c3c !important; -fx-background-radius: 5px;");
                    }
                }
                i++;
            }
        });
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
        return Math.min(v, 100.0);
    }

    @FXML
    public void onClose(javafx.event.ActionEvent event) {
        ((javafx.stage.Stage)((javafx.scene.Node)event.getSource()).getScene().getWindow()).close();
    }
}