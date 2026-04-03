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
                sb.append("🧩 CYKLICKÁ STRATÉGIA: Skrytá štruktúra problému\n");
                sb.append("--------------------------------------------------\n");

                sb.append("O ČO IDE:\n");
                sb.append("Každý väzeň začne krabicou so svojím číslom a sleduje čísla ako smerníky.\n\n");

                sb.append("🧠 TEORETICKÉ POZADIE:\n");
                sb.append("Rozloženie lístkov v krabiciach nie je len 'náhodný chaos'.\n");
                sb.append("Matematicky ide o tzv. permutáciu – každé číslo sa nachádza práve raz.\n\n");

                sb.append("👉 Kľúčová vlastnosť permutácie:\n");
                sb.append("Každá permutácia sa dá rozložiť na tzv. cykly (uzavreté slučky).\n\n");

                sb.append("Príklad (zjednodušený):\n");
                sb.append("1 → 5 → 3 → 1  (jeden cyklus)\n");
                sb.append("2 → 4 → 2      (druhý cyklus)\n\n");

                sb.append("👉 Čo to znamená:\n");
                sb.append("Ak začneš na čísle 1 a sleduješ ukazovatele, nikdy nevyjdeš z tohto cyklu.\n\n");

                sb.append("🖼️ PREPOJENIE NA OBRÁZOK:\n");
                sb.append("Každý takýto cyklus vidíš na obrázku ako samostatnú farbu.\n");
                sb.append("Farba = jedna uzavretá cesta.\n\n");

                sb.append("👉 Kľúčový insight:\n");
                sb.append("Väzeň sa vždy nachádza vo svojom cykle.\n");
                sb.append("Nikdy nemôže 'zablúdiť' do iného.\n\n");

                sb.append("📏 ROZHODUJÚCI FAKTOR:\n");
                sb.append("Dĺžka cyklu.\n");
                sb.append("Ak má cyklus viac ako 50 prvkov, väzeň ho nestihne celý prejsť.\n\n");

                sb.append("📊 PRAVDEPODOBNOSŤ:\n");
                sb.append("Zaujímavý výsledok matematiky:\n");
                sb.append("Pravdepodobnosť, že existuje cyklus dlhší ako 50, je asi 69 %.\n");
                sb.append("To znamená, že šanca na úspech je ~31 %.\n\n");

                sb.append("💡 HLAVNÁ MYŠLIENKA:\n");
                sb.append("Nehrá sa o šťastie jednotlivca, ale o globálnu štruktúru celej permutácie.");
                break;

            case "Náhodná stratégia":
                sb.append("🎲 NÁHODNÁ STRATÉGIA: Nezávislé pravdepodobnosti\n");
                sb.append("--------------------------------------------------\n");

                sb.append("🧠 TEORETICKÉ POZADIE:\n");
                sb.append("Každý výber krabice je nezávislý náhodný pokus.\n");
                sb.append("To znamená, že úspech jedného väzňa nijako nepomáha ostatným.\n\n");

                sb.append("👉 Matematicky:\n");
                sb.append("Každý väzeň má pravdepodobnosť 0.5 nájsť svoje číslo.\n\n");

                sb.append("👉 Ale skupina potrebuje:\n");
                sb.append("Všetci musia uspieť naraz.\n\n");

                sb.append("📊 Výpočet:\n");
                sb.append("0.5^100 = extrémne malé číslo (~10^-30)\n\n");

                sb.append("🖼️ PREPOJENIE NA OBRÁZOK:\n");
                sb.append("Aj keď obrázok obsahuje cykly (štruktúru), táto stratégia ich ignoruje.\n\n");

                sb.append("💡 HLAVNÁ MYŠLIENKA:\n");
                sb.append("Bez využitia štruktúry sa problém mení na čistú náhodu – a tá zlyháva.");
                break;

            case "Hybridná stratégia":
                sb.append("⚖️ HYBRIDNÁ STRATÉGIA: Rozbitie závislosti\n");
                sb.append("--------------------------------------------------\n");

                sb.append("🧠 TEORETICKÉ POZADIE:\n");
                sb.append("Problém vyžaduje spoločný úspech (logická AND podmienka).\n\n");

                sb.append("👉 Dôležitý princíp:\n");
                sb.append("Ak sú udalosti nezávislé:\n");
                sb.append("P(A ∩ B) = P(A) × P(B)\n\n");

                sb.append("👉 Čo sa tu deje:\n");
                sb.append("Časť väzňov využíva cykly (závislá štruktúra),\n");
                sb.append("časť ide náhodne (nezávislé pokusy).\n\n");

                sb.append("📊 Dôsledok:\n");
                sb.append("Stačí malá skupina náhodných → pravdepodobnosť padá exponenciálne.\n\n");

                sb.append("🖼️ PREPOJENIE NA OBRÁZOK:\n");
                sb.append("Vidíš cykly (poriadok), ale časť väzňov sa nimi neriadi.\n\n");

                sb.append("💡 HLAVNÁ MYŠLIENKA:\n");
                sb.append("Zmiešaním stratégií sa stratí výhoda korelácie.");
                break;

            case "Spoločné vylúčenie (prvých 10)":
                sb.append("🚫 SPOLOČNÉ VYLÚČENIE: Chybná redukcia priestoru\n");
                sb.append("--------------------------------------------------\n");

                sb.append("🧠 TEORETICKÉ POZADIE:\n");
                sb.append("Lístky sú rozmiestnené rovnomerne (uniformne náhodne).\n\n");

                sb.append("👉 To znamená:\n");
                sb.append("Každá krabica má rovnakú šancu obsahovať akékoľvek číslo.\n\n");

                sb.append("📊 Dôsledok:\n");
                sb.append("Šanca, že konkrétne číslo je v zakázanej oblasti = 10 %.\n\n");

                sb.append("👉 Pre 100 väzňov:\n");
                sb.append("Takmer isté, že niekto prehrá.\n\n");

                sb.append("🖼️ PREPOJENIE NA OBRÁZOK:\n");
                sb.append("Cykly často prechádzajú cez zakázané krabice → nedajú sa dokončiť.\n\n");

                sb.append("💡 HLAVNÁ MYŠLIENKA:\n");
                sb.append("Ignorovanie časti priestoru bez informácie je fatálna chyba.");
                break;

            default:
                sb.append("🔢 PARITNÁ STRATÉGIA: Nesúlad so štruktúrou\n");
                sb.append("--------------------------------------------------\n");

                sb.append("🧠 TEORETICKÉ POZADIE:\n");
                sb.append("Rozdelenie (párne/nepárne) nemá žiadny vzťah k permutácii.\n\n");

                sb.append("👉 Cykly:\n");
                sb.append("prechádzajú medzi všetkými číslami bez ohľadu na paritu.\n\n");

                sb.append("📊 Dôsledok:\n");
                sb.append("Polovica väzňov nikdy nemôže nájsť svoje číslo.\n\n");

                sb.append("🖼️ PREPOJENIE NA OBRÁZOK:\n");
                sb.append("Farby (cykly) ignorujú tvoje rozdelenie – preto stratégia zlyhá.\n\n");

                sb.append("💡 HLAVNÁ MYŠLIENKA:\n");
                sb.append("Stratégia musí rešpektovať skutočnú štruktúru problému.");
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
