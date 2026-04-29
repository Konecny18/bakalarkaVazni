package logic;

import java.util.*;

/**
 * Stratégia vylúčenia: simulácia, kde prvých N krabíc je kolektívne vylúčených
 * (žiadny väzeň ich neotvorí). Vhodné na testovanie vplyvu odstránenia časti priestoru.
 */
public class ExclusionStrategy implements Strategy {
    private int pocetVylucenych = 0;
    private int maxUspesnychVHistorii = 0;

    @Override
    public String nazovStrategie() {
        return "Spoločné vylúčenie (Ignoruje " + pocetVylucenych + " krabíc)";
    }

    /**
     * Nastaví počet počiatočných krabíc, ktoré budú vylúčené (indexy 0..n-1).
     * @param n počet vylúčených krabíc
     */
    public void setPocetVylucenych(int n) {
        this.pocetVylucenych = n;
    }

    @Override
    public void resetStats() {
        this.maxUspesnychVHistorii = 0;
    }

    @Override
    public int getMaxUspesnychVHistorii() {
        return maxUspesnychVHistorii;
    }

    /**
     * Simulácia, v ktorej sú prvé {@code pocetVylucenych} krabíc vylúčené zo všetkých hľadaní.
     * @param pocetVaznov celkový počet väzňov/krabíc
     * @param limitPokusov maximálny počet otvorení krabíc na väzňa
     * @return počet väzňov, ktorí našli svoje číslo
     */
    @Override
    public int pocitaj(int pocetVaznov, int limitPokusov) {
        List<Integer> krabice = new ArrayList<>();
        for (int i = 0; i < pocetVaznov; i++) krabice.add(i);
        Collections.shuffle(krabice);

        int uspesniVazni = 0;
        Random rnd = new Random();

        for (int vezenId = 0; vezenId < pocetVaznov; vezenId++) {
            // Väzni sa dohodli, že krabice 0 až (pocetVylucenych-1) neotvoria
            List<Integer> dostupneIndexy = new ArrayList<>();
            for (int i = pocetVylucenych; i < pocetVaznov; i++) {
                dostupneIndexy.add(i);
            }

            // Ak je limit väčší ako počet dostupných krabíc, otvorí všetky zvyšné
            int realnyLimit = Math.min(limitPokusov, dostupneIndexy.size());
            Collections.shuffle(dostupneIndexy, rnd);

            boolean nasiel = false;
            for (int i = 0; i < realnyLimit; i++) {
                if (krabice.get(dostupneIndexy.get(i)) == vezenId) {
                    nasiel = true;
                    break;
                }
            }
            if (nasiel) uspesniVazni++;
        }

        if (uspesniVazni < pocetVaznov && uspesniVazni > maxUspesnychVHistorii) {
            maxUspesnychVHistorii = uspesniVazni;
        }

        return uspesniVazni;
    }
}