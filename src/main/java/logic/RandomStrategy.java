package logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Náhodná stratégia: každý väzeň otvára náhodné krabice bez zohľadnenia cyklov.
 * Táto implementácia udržiava jednoduchý historický rekord maximálneho počtu úspechov.
 */
public class RandomStrategy implements Strategy {

    /** Záznam o maxime úspešných väzňov pozorovanom v histórii (pre neúplné behy). */
    private int maxUspesnychVHistorii = 0; // Tu si budeme pamätať rekord

    @Override
    public String nazovStrategie() {
        return "Náhodná stratégia";
    }

    /**
     * Spustí jednu simuláciu, kde každý väzeň náhodne vyberá krabice až do {@code maxPokusov}.
     * @param pocetVaznov počet väzňov/krabíc
     * @param maxPokusov maximálny počet otvorení krabíc na väzňa
     * @return počet väzňov, ktorí našli svoje číslo
     */
    @Override
    public int pocitaj(int pocetVaznov, int maxPokusov) {
        List<Integer> krabice = new ArrayList<>();
        for (int i = 0; i < pocetVaznov; i++) krabice.add(i);
        Collections.shuffle(krabice);

        int uspesniVazni = 0;
        Random rnd = new Random();

        for (int vazonId = 0; vazonId < pocetVaznov; vazonId++) {
            if (simulujNahodnyVyber(krabice, vazonId, maxPokusov, rnd)) {
                uspesniVazni++;
            }
        }

        // Sledovanie rekordu v prípade, že skupina nebola plne úspešná
        if (uspesniVazni < pocetVaznov) {
            if (uspesniVazni > maxUspesnychVHistorii) {
                maxUspesnychVHistorii = uspesniVazni;
            }
        }

        return uspesniVazni;
    }

    /**
     * Pomocná metóda: simuluje náhodné otváranie bez opakovania indexov.
     */
    private boolean simulujNahodnyVyber(List<Integer> krabice, int hladaneCislo, int maxPokusov, Random rnd) {
        int n = krabice.size();
        List<Integer> indexy = new ArrayList<>();
        for (int i = 0; i < n; i++) indexy.add(i);
        Collections.shuffle(indexy, rnd);

        for (int i = 0; i < maxPokusov; i++) {
            if (krabice.get(indexy.get(i)) == hladaneCislo) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vráti historický rekord úspešných väzňov pozorovaných v minulosti.
     * @return maximálny počet úspechov v histórii
     */
    public int getMaxUspesnychVHistorii() {
        return maxUspesnychVHistorii;
    }


    /**
     * Resetnúť interné štatistiky.
     */
    public void resetStats() {
        this.maxUspesnychVHistorii = 0;
    }
}