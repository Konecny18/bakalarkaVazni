package logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CycleStrategy implements  Strategy{
    @Override
    public String nazovStrategie() {
        return "Cyklicka strategia";
    }

    @Override
    public int pocitaj(int pocetVaznov, int pocetKrabic) {
        // 1. Príprava krabíc - naplníme ich číslami a náhodne premiešame
        List<Integer> krabice = new ArrayList<>();
        for (int i = 0; i < pocetKrabic; i++) {
            krabice.add(i);
        }
        Collections.shuffle(krabice);

        int uspesniVazni = 0;

        // 2. Simulácia pre každého väzňa
        for (int vazonId = 0; vazonId < pocetVaznov; vazonId++) {
            int aktualnaKrabica = vazonId; // Každý začne krabicou so svojím číslom
            boolean nasielSvojeCislo = false;

            // Väzeň môže otvoriť max. polovicu krabíc (pravidlo 100 väzňov)
            for (int pokus = 0; pokus < pocetKrabic / 2; pokus++) {
                int cisloVKrabici = krabice.get(aktualnaKrabica);

                if (cisloVKrabici == vazonId) {
                    nasielSvojeCislo = true;
                    break;
                }
                // Ak nenašiel, ide na krabicu s číslom, ktoré práve uvidel
                aktualnaKrabica = cisloVKrabici;
            }

            if (nasielSvojeCislo) {
                uspesniVazni++;
            }
        }
        return uspesniVazni; // Vráti počet ľudí, ktorí uspeli
    }
}
