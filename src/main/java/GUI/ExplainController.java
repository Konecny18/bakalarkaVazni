package GUI;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import logic.CycleStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;

public class ExplainController {
    @FXML private GridPane paneKrabice; // V FXML musí byť GridPane
    @FXML private Label lblInfo;
    @FXML private Label lblStatus;
    @FXML private TextArea txtAreaCykly;

    private final CycleStrategy strategy = new CycleStrategy();
    private final Map<Integer, List<Integer>> boxToCycleMap = new HashMap<>();
    private final Map<List<Integer>, Color> cycleColorMap = new HashMap<>();

    @FXML
    public void initialize() {
        generujNovuMapu();
    }

    @FXML
    public void generujNovuMapu() {
        // 1. Vyčistenie starých dát
        paneKrabice.getChildren().clear();
        boxToCycleMap.clear();
        cycleColorMap.clear();
        txtAreaCykly.clear();

        // 2. Generovanie dát (Krabice sú fixné pre všetkých väzňov v tomto kole)
        List<Integer> krabice = strategy.generujKrabice(100);
        List<List<Integer>> cykly = strategy.najdiVsetkyCykly(krabice);

        // 3. Spracovanie cyklov a textový výpis
        StringBuilder sb = new StringBuilder("--- DETAILNÝ ZOZNAM CYKLOV ---\n");
        boolean skupinaPrezila = true;
        Random rnd = new Random();

        for (int i = 0; i < cykly.size(); i++) {
            List<Integer> cyklus = cykly.get(i);

            // Každý cyklus dostane unikátnu farbu
            Color farba = Color.hsb(rnd.nextInt(360), 0.5, 0.9);
            cycleColorMap.put(cyklus, farba);

            if (cyklus.size() > 50) {
                skupinaPrezila = false;
            }

            for (Integer boxIdx : cyklus) {
                boxToCycleMap.put(boxIdx, cyklus);
            }

            sb.append(String.format("Cyklus %d (Dĺžka: %d): ", i + 1, cyklus.size()));
            sb.append(formatujCyklus(cyklus));
            sb.append("\n\n");
        }
        txtAreaCykly.setText(sb.toString());

        // 4. Vykreslenie 100 krabíc do mriežky 10x10
        for (int i = 0; i < 100; i++) {
            int stlpec = i % 10;
            int riadok = i / 10;

            StackPane vizualnaKrabica = vytvorVizuálnuKrabicu(i, krabice.get(i));

            // Pridanie do GridPane na konkrétne súradnice
            paneKrabice.add(vizualnaKrabica, stlpec, riadok);
        }

        // 5. Status bar
        if (skupinaPrezila) {
            lblStatus.setText("STATUS: SKUPINA PREŽILA! (Všetky cykly ≤ 50)");
            lblStatus.setTextFill(Color.GREEN);
        } else {
            lblStatus.setText("STATUS: SKUPINA ZOMRELA! (Existuje cyklus > 50)");
            lblStatus.setTextFill(Color.RED);
        }
    }

    private String formatujCyklus(List<Integer> cyklus) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cyklus.size(); i++) {
            sb.append(cyklus.get(i));
            if (i < cyklus.size() - 1) sb.append(" → ");
        }
        sb.append("]");
        return sb.toString();
    }

    private StackPane vytvorVizuálnuKrabicu(int index, int obsah) {
        StackPane stack = new StackPane();
        List<Integer> cyklus = boxToCycleMap.get(index);
        Color farbaCyklu = cycleColorMap.get(cyklus);

        Rectangle rect = new Rectangle();

        // Dynamické prispôsobenie veľkosti mriežke
        // Použijeme spoločné viazanie aby boli štvorce skutočne štvorcové a rástli trochu menej pri fullscreen
        DoubleBinding sizeBinding = Bindings.createDoubleBinding(
                () -> Math.min(paneKrabice.getWidth() / 11.5, paneKrabice.getHeight() / 11.5),
                paneKrabice.widthProperty(), paneKrabice.heightProperty()
        );
        rect.widthProperty().bind(sizeBinding);
        rect.heightProperty().bind(sizeBinding);

        rect.setFill(farbaCyklu);
        rect.setStroke(Color.BLACK);
        rect.setArcWidth(10);
        rect.setArcHeight(10);

        Text txt = new Text(String.valueOf(index));
        txt.setStyle("-fx-font-weight: bold;");

        stack.getChildren().addAll(rect, txt);

        stack.setOnMouseClicked(e -> {
            lblInfo.setText(String.format("Krabica č. %d obsahuje lístok %d. Cyklus má dĺžku %d.",
                    index, obsah, cyklus.size()));
        });

        return stack;
    }

    @FXML
    public void onClose(javafx.event.ActionEvent event) {
        ((javafx.stage.Stage)((javafx.scene.Node)event.getSource()).getScene().getWindow()).close();
    }
}