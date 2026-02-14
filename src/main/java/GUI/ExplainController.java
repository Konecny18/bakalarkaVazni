package GUI;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import logic.CycleStrategy;

import java.util.*;

public class ExplainController {
    @FXML private GridPane paneKrabice;
    @FXML private ComboBox<String> cbStrategieExplain;
    @FXML private TextArea txtVysvetlenie;
    @FXML private TextArea txtAreaCykly;
    @FXML private Label lblStatus;
    @FXML private Label lblInfo;

    private final CycleStrategy cycleLogic = new CycleStrategy();
    private final Map<Integer, List<Integer>> boxToCycleMap = new HashMap<>();
    private final Map<List<Integer>, Color> cycleColorMap = new HashMap<>();

    @FXML
    public void initialize() {
        cbStrategieExplain.getItems().addAll("Cyklická stratégia", "Náhodná stratégia");
        cbStrategieExplain.getSelectionModel().select(0);
        onStrategiaChanged();
    }

    @FXML
    public void onStrategiaChanged() {
        String vybrana = cbStrategieExplain.getSelectionModel().getSelectedItem();

        if ("Cyklická stratégia".equals(vybrana)) {
            txtVysvetlenie.setText("""
                    CYKLICKÁ STRATÉGIA:

                    Každý väzeň začne otvorením krabice so svojím číslom. \
                    Ak v nej nenájde svoj lístok, ide ku krabici s číslom, ktoré práve našiel.

                    Matematika: Skupina uspeje, ak neexistuje cyklus dlhší ako 50. \
                    Šanca na prežitie je ~31.18%.""");
        } else {
            txtVysvetlenie.setText("""
                    NÁHODNÁ STRATÉGIA:

                    Každý väzeň si vyberie 50 krabíc úplne náhodne. \
                    Voľby väzňov sú nezávislé, nevzniká žiadna matematická väzba.

                    Matematika: Pravdepodobnosť úspechu je (1/2)^100. \
                    To je cca 0.0000000000000000000000000000008, teda prakticky nula.""");
        }
        generujNovuMapu();
    }

    @FXML
    public void generujNovuMapu() {
        paneKrabice.getChildren().clear();
        boxToCycleMap.clear();
        cycleColorMap.clear();
        txtAreaCykly.clear();

        String vybrana = cbStrategieExplain.getSelectionModel().getSelectedItem();
        List<Integer> krabice = cycleLogic.generujKrabice(100);
        boolean zlyhanie = false;

        if ("Cyklická stratégia".equals(vybrana)) {
            // Výpočet cyklov len pre cyklickú stratégiu
            List<List<Integer>> cykly = cycleLogic.najdiVsetkyCykly(krabice);
            StringBuilder sb = new StringBuilder("LOGIKA CYKLOV:\n");
            Random rnd = new Random();

            for (List<Integer> cyklus : cykly) {
                Color farba = Color.hsb(rnd.nextInt(360), 0.6, 0.9);
                cycleColorMap.put(cyklus, farba);
                if (cyklus.size() > 50) zlyhanie = true;

                for (Integer i : cyklus) boxToCycleMap.put(i, cyklus);
                sb.append("Dĺžka ").append(cyklus.size()).append(": ").append(cyklus).append("\n");
            }
            txtAreaCykly.setText(sb.toString());

            if (zlyhanie) {
                lblStatus.setText("STATUS: SKUPINA BY ZOMRELA (Cyklus > 50)");
                lblStatus.setTextFill(Color.RED);
            } else {
                lblStatus.setText("STATUS: SKUPINA BY PREŽILA (Všetky cykly ≤ 50)");
                lblStatus.setTextFill(Color.GREEN);
            }
        } else {
            // Náhodná stratégia - žiadne cykly, fixné zlyhanie
            txtAreaCykly.setText("V náhodnej stratégii väzni nesledujú cykly.\nKaždý pokus je izolovaný.");
            lblStatus.setText("STATUS: SKUPINA BY ZOMRELA (Pravdepodobnosť ~0%)");
            lblStatus.setTextFill(Color.RED);
        }

        // Vykreslenie mriežky
        for (int i = 0; i < 100; i++) {
            paneKrabice.add(vytvorKrabicu(i, krabice.get(i), vybrana), i % 10, i / 10);
        }
    }

    private StackPane vytvorKrabicu(int index, int obsah, String strategia) {
        StackPane stack = new StackPane();
        List<Integer> cyklus = boxToCycleMap.get(index);

        // Tvoj responzívny binding veľkosti
        DoubleBinding sizeBinding = Bindings.createDoubleBinding(() -> {
            double cellW = paneKrabice.getWidth() / 10.0;
            double cellH = paneKrabice.getHeight() / 10.0;
            double s = Math.min(cellW, cellH) * 0.75;
            return Math.max(24.0, Math.min(80.0, s));
        }, paneKrabice.widthProperty(), paneKrabice.heightProperty());

        Rectangle rect = new Rectangle();
        rect.widthProperty().bind(sizeBinding);
        rect.heightProperty().bind(sizeBinding);

        // Farbenie podľa stratégie
        if ("Cyklická stratégia".equals(strategia) && cyklus != null) {
            rect.setFill(cycleColorMap.get(cyklus));
        } else {
            rect.setFill(Color.LIGHTSTEELBLUE); // Neutrálna farba pre náhodnú stratégiu
        }

        rect.setStroke(Color.BLACK);
        rect.setArcWidth(10);
        rect.setArcHeight(10);

        Text t = new Text(String.valueOf(index));
        t.styleProperty().bind(Bindings.createStringBinding(() -> {
            int fontPx = Math.max(10, (int) (sizeBinding.get() / 2.8));
            return "-fx-font-size: " + fontPx + "px; -fx-font-weight: bold;";
        }, sizeBinding));

        stack.getChildren().addAll(rect, t);

        stack.setOnMouseClicked(e -> {
            if ("Cyklická stratégia".equals(strategia) && cyklus != null) {
                lblInfo.setText("Krabica " + index + " obsahuje lístok " + obsah + ". Dĺžka cyklu: " + cyklus.size());
            } else {
                lblInfo.setText("Krabica " + index + " obsahuje lístok " + obsah + ". (Náhodný pokus)");
            }
        });

        return stack;
    }

    @FXML public void onClose(javafx.event.ActionEvent event) {
        ((javafx.stage.Stage)((javafx.scene.Node)event.getSource()).getScene().getWindow()).close();
    }
}