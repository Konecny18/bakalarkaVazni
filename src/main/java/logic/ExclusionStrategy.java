package logic;

import java.util.*;

public class ExclusionStrategy implements Strategy {
    private int pocetVylucenych = 0;
    private int maxUspesnychVHistorii = 0;

    @Override
    public String nazovStrategie() {
        return "Spoločné vylúčenie (Ignoruje " + pocetVylucenych + " krabíc)";
    }

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