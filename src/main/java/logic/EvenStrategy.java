package logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stratégia párnych čísel: väzeň otvára len krabice s párnym indexom.
 * Edukačný príklad, očakáva sa nízka úspešnosť.
 */
public class EvenStrategy implements Strategy {
    @Override
    public String nazovStrategie() {
        return "Stratégia párnych čísel";
    }

    /**
     * Simulácia, kde sa kontrolujú iba párne indexy (0,2,4...).
     * @param pocetVaznov počet väzňov/krabíc
     * @param maxPokusov maximálny počet otvorení krabíc na väzňa
     * @return počet väzňov, ktorí našli svoje číslo
     */
    @Override
    public int pocitaj(int pocetVaznov, int maxPokusov) {
        // Generujeme štandardne premiešané krabice
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