package logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EvenStrategy implements Strategy {
    @Override
    public String nazovStrategie() {
        return "Stratégia párnych čísel";
    }

    @Override
    public int pocitaj(int pocetVaznov, int maxPokusov) {
        // Generujeme štandardne premiešané krabice (ako v realite)
        List<Integer> krabice = new ArrayList<>();
        for (int i = 0; i < pocetVaznov; i++) krabice.add(i);
        Collections.shuffle(krabice);

        int uspesniVazni = 0;

        for (int vazonId = 0; vazonId < pocetVaznov; vazonId++) {
            int otovreneKrabice = 0;

            // Väzeň prechádza krabice a otvára len tie s párnym indexom
            for (int i = 0; i < pocetVaznov; i++) {
                if (i % 2 == 0) { // Ak je index krabice párny
                    if (krabice.get(i) == vazonId) {
                        uspesniVazni++;
                        break;
                    }
                    otovreneKrabice++;
                }

                // Ak už vyčerpal svoj limit pokusov, končí
                if (otovreneKrabice >= maxPokusov) {
                    break;
                }
            }
        }
        return uspesniVazni;
    }
}