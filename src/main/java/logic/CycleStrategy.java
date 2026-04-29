package logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementácia cyklickej stratégie pre problém väzňov a krabíc.
 * Využíva rozklad permutácie na disjunktné cykly a vyhodnocuje úspech podľa dĺžok cyklov.
 */
public class CycleStrategy implements Strategy {

    /** Dĺžka najdlhšieho cyklu z poslednej simulácie. */
    private int poslednyMaxCyklus = 0;
    /** Historický rekord úspešných väzňov v neúplných behách. */
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
     * @param pocetKrabic počet krabíc (väzňov)
     * @return zoznam, kde index = krabica a hodnota = číslo na lístku
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
     * Rozklad permutácie na cykly a spočítanie úspešných väzňov podľa dĺžok cyklov.
     * @param pocetVaznov počet väzňov / krabíc
     * @param limitPokusov maximálny počet otvorení krabíc pre jedného väzňa
     * @return počet väzňov, ktorí našli svoje číslo
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

            // V cyklickej stratégii: ak je dĺžka cyklu <= limitPokusov,
            // všetci väzni v tomto cykle nájdu svoje číslo.
            if (dlzka <= limitPokusov) {
                uspesniVazni += dlzka;
            }
        }

        this.poslednyMaxCyklus = maxDlzkaCyklu;

        // Sledovanie rekordu úspešnosti v neúspešných behách
        if (uspesniVazni < pocetVaznov) {
            if (uspesniVazni > maxUspesnychVHistorii) {
                maxUspesnychVHistorii = uspesniVazni;
            }
        }

        return uspesniVazni;
    }

    /**
     * Pomocná metóda na rozklad permutácie na zoznam cyklov.
     * @param krabice permutácia reprezentovaná ako zoznam
     * @return zoznam cyklov, každý cyklus je zoznam indexov
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
     * Pomocná metóda pre prepojenie s grafom (GraphController).
     * Vracia iba dĺžky jednotlivých cyklov.
     * @param krabice permutácia
     * @return zoznam dĺžok cyklov
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