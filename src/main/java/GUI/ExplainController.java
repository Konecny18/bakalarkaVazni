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

    private List<Integer> poslednaMapaKrabic;

    @FXML
    public void initialize() {
        cbStrategieExplain.getItems().addAll(
                "Cyklická stratégia",
                "Náhodná stratégia",
                "Hybridná stratégia", // ✅ NEW
                "Stratégia párnych čísel",
                "Stratégia nepárnych čísel",
                "Spoločné vylúčenie (prvých 10)"
        );
        cbStrategieExplain.getSelectionModel().select(0);

        paneKrabice.widthProperty().addListener((obs, oldVal, newVal) -> prekresliMriezku());
        paneKrabice.heightProperty().addListener((obs, oldVal, newVal) -> prekresliMriezku());

        onStrategiaChanged();
    }

    @FXML
    public void onStrategiaChanged() {
        String vybrana = cbStrategieExplain.getSelectionModel().getSelectedItem();
        StringBuilder sb = new StringBuilder();

        if (vybrana.contains("Cyklická")) {
            sb.append("--- CYKLICKÁ STRATÉGIA (Optimálne riešenie) ---\n\n");
            sb.append("PRINCÍP: Väzeň sleduje cyklus podľa nájdených čísel.\n\n");
            sb.append("PREČO TO FUNGUJE: Využíva štruktúru permutácie.\n\n");
            sb.append("MATEMATIKA: ~31 % šanca na prežitie.");
        }
        else if (vybrana.contains("Náhodná")) {
            sb.append("--- NÁHODNÁ STRATÉGIA ---\n\n");
            sb.append("Každý vyberá krabice náhodne.\n");
            sb.append("Pravdepodobnosti sa násobia → výsledok ~0 %.");
        }
        else if (vybrana.contains("Hybridná")) {
            sb.append("--- HYBRIDNÁ STRATÉGIA ---\n\n");
            sb.append("PRINCÍP: Polovica väzňov používa cyklickú stratégiu,\n");
            sb.append("druhá polovica vyberá krabice náhodne.\n\n");

            sb.append("INTUÍCIA: Mohlo by sa zdať, že kombinácia pomôže.\n\n");

            sb.append("REALITA: Aby skupina prežila, musia uspieť všetci.\n");
            sb.append("Náhodná polovica takmer vždy zlyhá → celá skupina prehrá.\n\n");

            sb.append("ZÁVER: Šanca na prežitie je prakticky 0 %.\n");
            sb.append("Táto stratégia ukazuje, že miešanie stratégií môže škodiť.");
        }
        else if (vybrana.contains("vylúčenie")) {
            sb.append("--- SPOLOČNÉ VYLÚČENIE ---\n\n");
            sb.append("Niektoré krabice sú zakázané.\n");
            sb.append("Ak je číslo v zakázanej krabici → okamžitá prehra.");
        }
        else {
            sb.append("--- PARITNÁ STRATÉGIA ---\n\n");
            sb.append("Rozdelenie krabíc podľa parity.\n");
            sb.append("Ak číslo nie je v správnej polovici → väzeň zlyhá.\n");
            sb.append("Šanca ~0 %.");
        }

        txtVysvetlenie.setText(sb.toString());
        generujNovuMapu();
    }

    @FXML
    public void generujNovuMapu() {
        poslednaMapaKrabic = cycleLogic.generujKrabice(100);
        prekresliMriezku();
    }

    private void prekresliMriezku() {
        if (poslednaMapaKrabic == null) return;

        paneKrabice.getChildren().clear();
        boxToCycleMap.clear();
        cycleColorMap.clear();
        txtAreaCykly.clear();

        String vybrana = cbStrategieExplain.getSelectionModel().getSelectedItem();
        int maxDlzkaCyklu = 0;

        if (vybrana.contains("Cyklická")) {
            List<List<Integer>> cykly = cycleLogic.najdiVsetkyCykly(poslednaMapaKrabic);
            Random rnd = new Random();

            for (List<Integer> cyklus : cykly) {
                Color farba = Color.hsb(rnd.nextInt(360), 0.5, 0.9);
                cycleColorMap.put(cyklus, farba);

                if (cyklus.size() > maxDlzkaCyklu) maxDlzkaCyklu = cyklus.size();

                for (Integer i : cyklus) {
                    boxToCycleMap.put(i, cyklus);
                }

                txtAreaCykly.appendText("Dĺžka " + cyklus.size() + ": " + cyklus + "\n");
            }

            boolean prezili = maxDlzkaCyklu <= 50;
            lblStatus.setText(prezili ? "PREŽILI" : "ZOMRELI");
            lblStatus.setTextFill(prezili ? Color.GREEN : Color.RED);

        } else {
            // ✅ všetky ostatné (vrátane hybrid)
            lblStatus.setText("VÝSLEDOK: ZLYHANIE (≈ 0 %)");
            lblStatus.setTextFill(Color.RED);

            if (vybrana.contains("vylúčenie")) {
                txtAreaCykly.setText("Krabice 0-9 sú zakázané.");
            }
        }

        for (int i = 0; i < 100; i++) {
            paneKrabice.add(vytvorKrabicu(i, poslednaMapaKrabic.get(i), vybrana), i % 10, i / 10);
        }
    }

    private StackPane vytvorKrabicu(int index, int obsah, String strategia) {
        StackPane stack = new StackPane();

        DoubleBinding sizeBinding = Bindings.createDoubleBinding(() -> {
            double w = (paneKrabice.getWidth() - 60) / 10.5;
            double h = (paneKrabice.getHeight() - 60) / 10.5;
            return Math.max(15.0, Math.min(75.0, Math.min(w, h)));
        }, paneKrabice.widthProperty(), paneKrabice.heightProperty());

        Rectangle rect = new Rectangle();
        rect.widthProperty().bind(sizeBinding);
        rect.heightProperty().bind(sizeBinding);
        rect.setArcWidth(8);
        rect.setArcHeight(8);
        rect.setStroke(Color.web("#bdc3c7"));

        if (strategia.contains("Cyklická")) {
            List<Integer> cyklus = boxToCycleMap.get(index);
            rect.setFill(cycleColorMap.getOrDefault(cyklus, Color.LIGHTGRAY));
        } else if (strategia.contains("vylúčenie")) {
            rect.setFill(index < 10 ? Color.web("#34495e") : Color.web("#3498db"));
        } else if (strategia.contains("párnych")) {
            rect.setFill(index % 2 == 0 ? Color.web("#2ecc71") : Color.web("#ecf0f1"));
        } else if (strategia.contains("nepárnych")) {
            rect.setFill(index % 2 != 0 ? Color.web("#e67e22") : Color.web("#ecf0f1"));
        } else if (strategia.contains("Hybridná")) { // ✅ NEW
            rect.setFill(index < 50 ? Color.web("#9b59b6") : Color.web("#f1c40f"));
        } else {
            rect.setFill(Color.web("#ecf0f1"));
        }

        Text t = new Text(String.valueOf(index));

        t.styleProperty().bind(Bindings.createStringBinding(() -> {
            double fontSize = sizeBinding.get() * 0.45;
            return "-fx-font-size: " + String.format(Locale.US, "%.1f", fontSize) + "px; -fx-font-weight: bold;";
        }, sizeBinding));

        t.setFill(Color.web("#2c3e50"));

        stack.getChildren().addAll(rect, t);

        stack.setOnMouseClicked(e ->
                lblInfo.setText("Krabica č. " + index + " obsahuje lístok väzňa č. " + obsah)
        );

        stack.setCursor(javafx.scene.Cursor.HAND);

        return stack;
    }

    @FXML
    public void onClose(javafx.event.ActionEvent event) {
        ((javafx.stage.Stage)((javafx.scene.Node)event.getSource()).getScene().getWindow()).close();
    }
}