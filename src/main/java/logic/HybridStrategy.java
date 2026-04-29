package logic;

import java.util.*;

/**
 * Hybridná stratégia: prvá polovica väzňov používa cyklickú stratégiu,
 * druhá polovica používa náhodný výber. Použiteľné na skúmanie miešaného správania.
 */
public class HybridStrategy implements Strategy {

    private int maxUspesnychVHistorii = 0;

    @Override
    public String nazovStrategie() {
        return "Hybridná (polovica cyklicky, polovica náhodne)";
    }

    @Override
    public void resetStats() {
        this.maxUspesnychVHistorii = 0;
    }

    @Override
    public int getMaxUspesnychVHistorii() {
        return maxUspesnychVHistorii;
    }

    private List<Integer> generujKrabice(int n) {
        List<Integer> krabice = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            krabice.add(i);
        }
        Collections.shuffle(krabice);
        return krabice;
    }

    /**
     * Spustí hybridnú simuláciu pre jednu permutáciu.
     * Prvá polovica väzňov sleduje cykly, druhá polovica náhodne otvára krabice.
     * @param pocetVaznov počet väzňov/krabíc
     * @param limitPokusov maximálny počet otvorení krabíc na väzňa
     * @return počet väzňov, ktorí našli svoje číslo
     */
    @Override
    public int pocitaj(int pocetVaznov, int limitPokusov) {
        List<Integer> krabice = generujKrabice(pocetVaznov);

        int uspesni = 0;

        for (int prisoner = 0; prisoner < pocetVaznov; prisoner++) {

            // PRVÁ POLOVICA → cyklická stratégia
            if (prisoner < pocetVaznov / 2) {
                int current = prisoner;
                boolean found = false;

                for (int i = 0; i < limitPokusov; i++) {
                    current = krabice.get(current);
                    if (current == prisoner) {
                        found = true;
                        break;
                    }
                }

                if (found) {
                    uspesni++;
                }

            } else {
                // DRUHÁ POLOVICA → náhodná stratégia
                List<Integer> nahodne = new ArrayList<>();
                for (int i = 0; i < pocetVaznov; i++) {
                    nahodne.add(i);
                }
                Collections.shuffle(nahodne);

                boolean found = false;

                for (int i = 0; i < limitPokusov; i++) {
                    int box = nahodne.get(i);
                    if (krabice.get(box) == prisoner) {
                        found = true;
                        break;
                    }
                }

                if (found) {
                    uspesni++;
                }
            }
        }

        // Sledovanie rekordu
        if (uspesni < pocetVaznov) {
            if (uspesni > maxUspesnychVHistorii) {
                maxUspesnychVHistorii = uspesni;
            }
        }

        return uspesni;
    }
}