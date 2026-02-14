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
        cbStrategieExplain.getItems().addAll(
                "Cyklická stratégia",
                "Náhodná stratégia",
                "Stratégia párnych čísel",
                "Stratégia nepárnych čísel"
        );
        cbStrategieExplain.getSelectionModel().select(0);
        onStrategiaChanged();
    }

    @FXML
    public void onStrategiaChanged() {
        String vybrana = cbStrategieExplain.getSelectionModel().getSelectedItem();

        if ("Cyklická stratégia".equals(vybrana)) {
            txtVysvetlenie.setText("CYKLICKÁ STRATÉGIA:\nVäzni sledujú čísla v krabiciach ako smerovníky (cykly).");
        } else if ("Náhodná stratégia".equals(vybrana)) {
            txtVysvetlenie.setText("NÁHODNÁ STRATÉGIA:\nKaždý si vyberá 50 náhodných krabíc bez akejkoľvek dohody.");
        } else if ("Stratégia párnych čísel".equals(vybrana)) {
            txtVysvetlenie.setText("STRATÉGIA PÁRNYCH ČÍSEL:\n" +
                    "Väzni sa dohodli, že budú otvárať len krabice na párnych pozíciách (0, 2, 4...).\n\n" +
                    "Dopad: Ak je lístok v nepárnej krabici, väzeň ho nikdy nenájde.");
        } else if ("Stratégia nepárnych čísel".equals(vybrana)) {
            txtVysvetlenie.setText("STRATÉGIA NEPÁRNYCH ČÍSEL:\n" +
                    "Väzni otvárajú len krabice na nepárnych indexoch (1, 3, 5...).\n\n" +
                    "Dopad: Ak je lístok v párnej krabici, väzeň ho nemá šancu nájsť.");
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
            lblStatus.setText(zlyhanie ? "SKUPINA ZOMRELA (Cyklus > 50)" : "SKUPINA PREŽILA");
            lblStatus.setTextFill(zlyhanie ? Color.RED : Color.GREEN);

        } else if ("Stratégia párnych čísel".equals(vybrana)) {
            txtAreaCykly.setText("Zvýraznené sú PÁRNE indexy (0, 2, 4...).\nNepárne sú ignorované.");
            lblStatus.setText("SKUPINA ZOMRELA (Šanca 0%)");
            lblStatus.setTextFill(Color.RED);
        } else if ("Stratégia nepárnych čísel".equals(vybrana)) {
            txtAreaCykly.setText("Zvýraznené sú NEPÁRNE indexy (1, 3, 5...).\nPárne sú ignorované.");
            lblStatus.setText("SKUPINA ZOMRELA (Šanca 0%)");
            lblStatus.setTextFill(Color.RED);
        } else {
            txtAreaCykly.setText("V náhodnej stratégii neexistuje vizuálna väzba.");
            lblStatus.setText("SKUPINA ZOMRELA (Náhodný výber)");
            lblStatus.setTextFill(Color.RED);
        }

        for (int i = 0; i < 100; i++) {
            paneKrabice.add(vytvorKrabicu(i, krabice.get(i), vybrana), i % 10, i / 10);
        }
    }

    private StackPane vytvorKrabicu(int index, int obsah, String strategia) {
        StackPane stack = new StackPane();

        DoubleBinding sizeBinding = Bindings.createDoubleBinding(() -> {
            double cellW = paneKrabice.getWidth() / 10.0;
            double cellH = paneKrabice.getHeight() / 10.0;
            return Math.max(24.0, Math.min(80.0, Math.min(cellW, cellH) * 0.75));
        }, paneKrabice.widthProperty(), paneKrabice.heightProperty());

        Rectangle rect = new Rectangle();
        rect.widthProperty().bind(sizeBinding);
        rect.heightProperty().bind(sizeBinding);
        rect.setArcWidth(10); rect.setArcHeight(10);
        rect.setStroke(Color.BLACK);

        // LOGIKA FARBENIA PODĽA STRATÉGIE
        if ("Cyklická stratégia".equals(strategia)) {
            List<Integer> cyklus = boxToCycleMap.get(index);
            rect.setFill(cyklus != null ? cycleColorMap.get(cyklus) : Color.LIGHTGRAY);
        } else if ("Stratégia párnych čísel".equals(strategia)) {
            if (index % 2 == 0) {
                rect.setFill(Color.web("#2ecc71")); // Zelená pre párne
            } else {
                rect.setFill(Color.web("#ecf0f1"));
                rect.setOpacity(0.5);
            }
        } else if ("Stratégia nepárnych čísel".equals(strategia)) {
            if (index % 2 != 0) {
                rect.setFill(Color.web("#e67e22")); // Oranžová pre nepárne
            } else {
                rect.setFill(Color.web("#ecf0f1"));
                rect.setOpacity(0.5);
            }
        } else {
            rect.setFill(Color.LIGHTSTEELBLUE);
        }

        Text t = new Text(String.valueOf(index));
        t.styleProperty().bind(Bindings.createStringBinding(() ->
                "-fx-font-size: " + Math.max(10, (int)(sizeBinding.get()/2.8)) + "px; -fx-font-weight: bold;", sizeBinding));

        stack.getChildren().addAll(rect, t);
        stack.setOnMouseClicked(e -> lblInfo.setText("Krabica " + index + " obsahuje lístok " + obsah));

        return stack;
    }

    @FXML public void onClose(javafx.event.ActionEvent event) {
        ((javafx.stage.Stage)((javafx.scene.Node)event.getSource()).getScene().getWindow()).close();
    }
}