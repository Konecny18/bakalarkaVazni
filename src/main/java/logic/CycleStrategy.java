package logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CycleStrategy implements Strategy {

    private int poslednyMaxCyklus = 0;
    private int maxUspesnychVHistorii = 0; // Premenná pre sledovanie rekordu v neúspešných pokusoch

    @Override
    public String nazovStrategie() {
        return "Cyklická stratégia";
    }

    @Override
    public void resetStats() {
        this.poslednyMaxCyklus = 0;
        this.maxUspesnychVHistorii = 0;
    }

    @Override
    public int getMaxUspesnychVHistorii() {
        return maxUspesnychVHistorii;
    }

    @Override
    public int getNajdlhsiCyklusPoslednejSimulacie() {
        return poslednyMaxCyklus;
    }

    public List<Integer> generujKrabice(int pocetKrabic) {
        List<Integer> krabice = new ArrayList<>();
        for (int i = 0; i < pocetKrabic; i++) {
            krabice.add(i);
        }
        Collections.shuffle(krabice);
        return krabice;
    }

    @Override
    public int pocitaj(int pocetVaznov, int limitPokusov) {
        List<Integer> krabice = generujKrabice(pocetVaznov);
        List<List<Integer>> cykly = najdiVsetkyCykly(krabice);

        int maxDlzkaCyklu = 0;
        int uspesniVazni = 0;

        for (List<Integer> cyklus : cykly) {
            int dlzka = cyklus.size();
            if (dlzka > maxDlzkaCyklu) maxDlzkaCyklu = dlzka;

            // V cyklickej stratégii platí: ak je dĺžka cyklu pod limitom,
            // všetci väzni v tomto cykle nájdu svoje číslo.
            if (dlzka <= limitPokusov) {
                uspesniVazni += dlzka;
            }
        }

        this.poslednyMaxCyklus = maxDlzkaCyklu;

        // Sledovanie rekordu: ak niekto zlyhal (skupina neprežila),
        // porovnáme počet úspešných s naším doterajším rekordom.
        if (uspesniVazni < pocetVaznov) {
            if (uspesniVazni > maxUspesnychVHistorii) {
                maxUspesnychVHistorii = uspesniVazni;
            }
        }

        return uspesniVazni;
    }

    /**
     * Pomocná metóda na rozklad permutácie (krabíc) na cykly.
     * Matematicky: Permutácia sa dá vždy jednoznačne rozložiť na disjunktné cykly.
     */
    public List<List<Integer>> najdiVsetkyCykly(List<Integer> krabice) {
        int pocetKrabic = krabice.size();
        List<List<Integer>> vsetkyCykly = new ArrayList<>();
        boolean[] navstivene = new boolean[pocetKrabic];

        for (int i = 0; i < pocetKrabic; i++) {
            if (!navstivene[i]) {
                List<Integer> aktualnyCyklus = new ArrayList<>();
                int k = i;
                while (!navstivene[k]) {
                    navstivene[k] = true;
                    aktualnyCyklus.add(k);
                    k = krabice.get(k);
                }
                vsetkyCykly.add(aktualnyCyklus);
            }
        }
        return vsetkyCykly;
    }
}