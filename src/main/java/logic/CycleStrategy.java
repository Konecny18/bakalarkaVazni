package logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementácia Cyklickej stratégie pre problém 100 väzňov.
 * Využíva matematický rozklad náhodnej permutácie na disjunktné cykly.
 */
public class CycleStrategy implements Strategy {

    private int poslednyMaxCyklus = 0;
    private int maxUspesnychVHistorii = 0;

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

    /**
     * Generuje náhodnú permutáciu lístkov v krabiciach.
     */
    public List<Integer> generujKrabice(int pocetKrabic) {
        List<Integer> krabice = new ArrayList<>();
        for (int i = 0; i < pocetKrabic; i++) {
            krabice.add(i);
        }
        Collections.shuffle(krabice);
        return krabice;
    }

    /**
     * Hlavná výpočtová metóda pre jednu simuláciu.
     */
    @Override
    public int pocitaj(int pocetVaznov, int limitPokusov) {
        List<Integer> krabice = generujKrabice(pocetVaznov);
        List<List<Integer>> cykly = najdiVsetkyCykly(krabice);

        int maxDlzkaCyklu = 0;
        int uspesniVazni = 0;

        for (List<Integer> cyklus : cykly) {
            int dlzka = cyklus.size();
            if (dlzka > maxDlzkaCyklu) maxDlzkaCyklu = dlzka;

            // V cyklickej stratégii platí: ak je dĺžka cyklu pod limitom (limitPokusov),
            // všetci väzni v tomto konkrétnom cykle nájdu svoje číslo.
            if (dlzka <= limitPokusov) {
                uspesniVazni += dlzka;
            }
        }

        this.poslednyMaxCyklus = maxDlzkaCyklu;

        // Sledovanie rekordu úspešnosti v neúspešných pokusoch (pre reporty)
        if (uspesniVazni < pocetVaznov) {
            if (uspesniVazni > maxUspesnychVHistorii) {
                maxUspesnychVHistorii = uspesniVazni;
            }
        }

        return uspesniVazni;
    }

    /**
     * Pomocná metóda na rozklad permutácie na zoznamy cyklov.
     * Používa sa na výpočet úspešnosti simulácie.
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

    /**
     * NOVÁ METÓDA pre prepojenie s grafom (GraphController).
     * Vracia iba dĺžky jednotlivých cyklov bez zoznamu prvkov.
     */
    public List<Integer> ziskajDlzkyCyklov(List<Integer> krabice) {
        List<Integer> dlzky = new ArrayList<>();
        List<List<Integer>> vsetkyCykly = najdiVsetkyCykly(krabice);

        for (List<Integer> cyklus : vsetkyCykly) {
            dlzky.add(cyklus.size());
        }
        return dlzky;
    }
}