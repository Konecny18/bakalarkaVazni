package logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OddStrategy implements Strategy {
    @Override
    public String nazovStrategie() {
        return "Stratégia nepárnych čísel";
    }

    @Override
    public int pocitaj(int pocetVaznov, int maxPokusov) {
        List<Integer> krabice = new ArrayList<>();
        for (int i = 0; i < pocetVaznov; i++) krabice.add(i);
        Collections.shuffle(krabice);

        int uspesniVazni = 0;

        for (int vazonId = 0; vazonId < pocetVaznov; vazonId++) {
            int otvoreneKrabice = 0;

            for (int i = 0; i < pocetVaznov; i++) {
                if (i % 2 != 0) { // Podmienka pre nepárne indexy (1, 3, 5...)
                    if (krabice.get(i) == vazonId) {
                        uspesniVazni++;
                        break;
                    }
                    otvoreneKrabice++;
                }
                if (otvoreneKrabice >= maxPokusov) break;
            }
        }
        return uspesniVazni;
    }
}