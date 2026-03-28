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
        if (vybrana == null) return;

        StringBuilder sb = new StringBuilder();

        switch (vybrana) {
            case "Cyklická stratégia":
                sb.append("🧩 CYKLICKÁ STRATÉGIA: Matematický zázrak\n");
                sb.append("--------------------------------------------------\n");
                sb.append("PRINCÍP: Každý väzeň otvorí krabicu so svojím číslom. Ak tam nenájde svoj lístok, ide na číslo krabice, ktoré práve našiel (nasleduje ukazovateľ).\n\n");
                sb.append("MATEMATICKÝ ZÁKLAD: Problém 100 väzňov premeníme na analýzu náhodných permutácií. Každá permutácia (náhodné rozloženie) sa skladá z uzavretých cyklov.\n\n");
                sb.append("KĽÚČ K ÚSPECHU: Skupina prežije vtedy a len vtedy, ak NAJDLHŠÍ cyklus v tejto permutácii má dĺžku ≤ 50.\n\n");
                sb.append("VÝSLEDOK: Pravdepodobnosť, že náhodná permutácia neobsahuje cyklus dlhší ako 1/2 celkového počtu, je ln(2) ≈ 31,18 %.");
                break;

            case "Náhodná stratégia":
                sb.append("🎲 NÁHODNÁ STRATÉGIA: Pasca nezávislosti\n");
                sb.append("--------------------------------------------------\n");
                sb.append("PRINCÍP: Každý väzeň si vyberie 50 krabíc úplne náhodne, bez ohľadu na ostatných.\n\n");
                sb.append("LOGICKÁ CHYBA: Keďže sú pokusy väzňov štatisticky NEZÁVISLÉ, ich pravdepodobnosti sa násobia.\n\n");
                sb.append("VÝPPOČET: Pravdepodobnosť úspechu jedného je 1/2. Pre 100 väzňov je to (1/2)^100.\n\n");
                sb.append("VÝSLEDOK: 0,0000000000000000000000000000008. Šanca na prežitie je prakticky nulová. Vesmír skôr zanikne, než by touto cestou uspeli.");
                break;

            case "Hybridná stratégia":
                sb.append("⚖️ HYBRIDNÁ STRATÉGIA: Falošná nádej\n");
                sb.append("--------------------------------------------------\n");
                sb.append("KONCEPT: Snaha skombinovať cykly (pre štruktúru) a náhodu (pre istotu).\n\n");
                sb.append("ANALÝZA: V tomto probléme platí zákon 'reťaz je len taká silná, ako jej najslabší článok'. Aby prežili, MUSIA uspieť všetci do jedného.\n\n");
                sb.append("REALITA: Ak 50 väzňov zvolí náhodu, ich šanca na spoločný úspech je (1/2)^50. To je tak malé číslo, že znehodnotí akýkoľvek úspech cyklickej skupiny.\n\n");
                sb.append("ZÁVER: Miešanie stratégií v tomto prípade nefunguje. Matematika nepustí.");
                break;

            case "Spoločné vylúčenie (prvých 10)":
                sb.append("🚫 SPOLOČNÉ VYLÚČENIE: Zakázané zóny\n");
                sb.append("--------------------------------------------------\n");
                sb.append("MECHANIZMUS: Väzni sa dohodnú, že nikdy neotvoria krabice 0-9. Hľadajú len v zvyšných 90 krabiciach.\n\n");
                sb.append("KARDINÁLNA CHYBA: Ak sa lístok väzňa nachádza v jednej z týchto 10 zakázaných krabíc, daný väzeň nemá žiadnu šancu ho nájsť.\n\n");
                sb.append("ŠTATISTIKA: Pravdepodobnosť, že aspoň jeden lístok z chýbajúcich čísel skončí v zakázanej zóne, je extrémne vysoká.\n\n");
                sb.append("VÝSLEDOK: Šanca na prežitie klesá k nule.");
                break;

            default:
                sb.append("🔢 PARITNÁ STRATÉGIA\n");
                sb.append("--------------------------------------------------\n");
                sb.append("PRINCÍP: Väzni si rozdelia krabice podľa párnych a nepárnych čísel.\n\n");
                sb.append("PROBLÉM: Ak sa hľadané číslo nachádza v 'nesprávnej' skupine (napr. párny väzeň má lístok v nepárnej krabici), nikdy ho nenájde.\n\n");
                sb.append("ZÁVER: Podobne ako pri náhodnom výbere, osudy nie sú správne prepojené cez cykly, čo vedie k okamžitému zlyhaniu skupiny.");
                break;
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

        // normalize strategy string for robust checks
        String s = strategia == null ? "" : strategia.toLowerCase();

        if (s.contains("cyklick")) {
            List<Integer> cyklus = boxToCycleMap.get(index);
            rect.setFill(cycleColorMap.getOrDefault(cyklus, Color.LIGHTGRAY));
        } else if (s.contains("vyl") || s.contains("vyluc") || s.contains("vylú")) {
            // match both accented and unaccented variants of "vylúčenie"
            rect.setFill(index < 10 ? Color.web("#34495e") : Color.web("#3498db"));
        } else if (s.contains("nepár") || s.contains("nepar")) {
            // check for "nepárnych" first (odd strategy) so it doesn't get eaten by the 'pár' check
            rect.setFill(index % 2 != 0 ? Color.web("#e67e22") : Color.web("#ecf0f1"));
        } else if (s.contains("pár") || s.contains("par")) {
            rect.setFill(index % 2 == 0 ? Color.web("#2ecc71") : Color.web("#ecf0f1"));
        } else if (s.contains("hybrid")) { // ✅ NEW
            rect.setFill(index < 50 ? Color.web("#9b59b6") : Color.web("#f1c40f"));
        } else {
            rect.setFill(Color.web("#ecf0f1"));
        }

        Text t = new Text(String.valueOf(index));

        t.styleProperty().bind(Bindings.createStringBinding(() -> {
            double fontSize = sizeBinding.get() * 0.45;
            return "-fx-font-size: " + String.format(Locale.US, "%.1f", fontSize) + "px; -fx-font-weight: bold;";
        }, sizeBinding));

        // Set text color: white for excluded boxes when exclusion strategy is selected, otherwise default
        // reuse previously computed lowercase 's' variable
        if ((s.contains("vyl") || s.contains("vyluc") || s.contains("vylú")) && index < 10) {
            t.setFill(Color.WHITE);
        } else {
            t.setFill(Color.web("#2c3e50"));
        }

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
