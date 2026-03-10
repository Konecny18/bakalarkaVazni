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
                "Stratégia nepárnych čísel",
                "Spoločné vylúčenie (prvých 10)"
        );
        cbStrategieExplain.getSelectionModel().select(0);
        onStrategiaChanged();
    }

    @FXML
    public void onStrategiaChanged() {
        String vybrana = cbStrategieExplain.getSelectionModel().getSelectedItem();

        if (vybrana.contains("Cyklická")) {
            txtVysvetlenie.setText("CYKLICKÁ STRATÉGIA:\nVäzni využívajú matematiku permutácií. " +
                    "Každý začne krabicou so svojím číslom. Lístok vo vnútri ho pošle na ďalšiu adresu.\n\n" +
                    "VÝSLEDOK: Skupina prežije, ak v miestnosti nie je cyklus dlhší ako 50.");
        } else if (vybrana.contains("Náhodná")) {
            txtVysvetlenie.setText("NÁHODNÁ STRATÉGIA:\nKaždý väzeň háda naslepo. Voľby sú nezávislé.\n\n" +
                    "VÝSLEDOK: Šanca na prežitie je (1/2)^100, čo je prakticky nemožné.");
        } else if (vybrana.contains("vylúčenie")) {
            txtVysvetlenie.setText("SPOLOČNÉ VYLÚČENIE:\nVäzni ignorujú tmavošedé krabice (0-9).\n\n" +
                    "KRITICKÁ CHYBA: Ak je lístok väzňa č. 5 v krabici č. 5 (ktorú nikto neotvorí), všetci zomrú. " +
                    "Šanca, že v 10 krabiciach nebude ani jedno dôležité číslo, je takmer nulová.");
        } else {
            txtVysvetlenie.setText("DETERMINISTICKÉ VOĽBY (Párne/Nepárne):\nVäzni si delia priestor.\n\n" +
                    "CHYBA: Ak sa lístok nachádza v 'zakázanej' polovici, väzeň ho nikdy nenájde.");
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

        // Pomocná logika pre zobrazenie úspechu v GUI
        boolean zlyhanie = false;

        if (vybrana.contains("Cyklická")) {
            List<List<Integer>> cykly = cycleLogic.najdiVsetkyCykly(krabice);
            Random rnd = new Random();
            for (List<Integer> cyklus : cykly) {
                Color farba = Color.hsb(rnd.nextInt(360), 0.5, 0.9);
                cycleColorMap.put(cyklus, farba);
                if (cyklus.size() > 50) zlyhanie = true;
                for (Integer i : cyklus) boxToCycleMap.put(i, cyklus);
                txtAreaCykly.appendText("Dĺžka " + cyklus.size() + ": " + cyklus + "\n");
            }
            lblStatus.setText(zlyhanie ? "SKUPINA ZOMRELA (Príliš dlhý cyklus)" : "SKUPINA PREŽILA");
            lblStatus.setTextFill(zlyhanie ? Color.RED : Color.GREEN);
        } else {
            lblStatus.setText("VÝSLEDOK: ZLYHANIE (Šanca ~0%)");
            lblStatus.setTextFill(Color.RED);
            if (vybrana.contains("vylúčenie")) txtAreaCykly.setText("Tmavé krabice sú ignorované.");
        }

        for (int i = 0; i < 100; i++) {
            paneKrabice.add(vytvorKrabicu(i, krabice.get(i), vybrana), i % 10, i / 10);
        }
    }

    private StackPane vytvorKrabicu(int index, int obsah, String strategia) {
        StackPane stack = new StackPane();

        // Dynamická veľkosť krabice podľa okna
        DoubleBinding sizeBinding = Bindings.createDoubleBinding(() -> {
            return Math.min(45.0, Math.min(paneKrabice.getWidth()/11, paneKrabice.getHeight()/11));
        }, paneKrabice.widthProperty(), paneKrabice.heightProperty());

        Rectangle rect = new Rectangle();
        rect.widthProperty().bind(sizeBinding);
        rect.heightProperty().bind(sizeBinding);
        rect.setArcWidth(8); rect.setArcHeight(8);
        rect.setStroke(Color.web("#bdc3c7"));

        // FARBENIE PODĽA VYBRANEJ STRATÉGIE
        if (strategia.contains("Cyklická")) {
            List<Integer> cyklus = boxToCycleMap.get(index);
            rect.setFill(cycleColorMap.getOrDefault(cyklus, Color.LIGHTGRAY));
        } else if (strategia.contains("vylúčenie")) {
            rect.setFill(index < 10 ? Color.web("#34495e") : Color.web("#3498db"));
        } else if (strategia.contains("párnych")) {
            rect.setFill(index % 2 == 0 ? Color.web("#2ecc71") : Color.web("#ecf0f1"));
        } else if (strategia.contains("nepárnych")) {
            rect.setFill(index % 2 != 0 ? Color.web("#e67e22") : Color.web("#ecf0f1"));
        } else {
            rect.setFill(Color.web("#ecf0f1"));
        }

        Text t = new Text(String.valueOf(index));
        t.setFill(index < 10 && strategia.contains("vylúčenie") ? Color.WHITE : Color.BLACK);

        stack.getChildren().addAll(rect, t);
        stack.setOnMouseClicked(e -> lblInfo.setText("Krabica č. " + index + " obsahuje lístok väzňa č. " + obsah));

        return stack;
    }

    @FXML public void onClose(javafx.event.ActionEvent event) {
        ((javafx.stage.Stage)((javafx.scene.Node)event.getSource()).getScene().getWindow()).close();
    }
}