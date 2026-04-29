package logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stratégia nepárnych čísel: väzeň otvára len krabice s nepárnym indexom.
 * Edukačný príklad, očakáva sa nízka úspešnosť.
 */
public class OddStrategy implements Strategy {
    @Override
    public String nazovStrategie() {
        return "Stratégia nepárnych čísel";
    }

    /**
     * Simulácia, kde sa kontrolujú iba nepárne indexy (1,3,5...).
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