package logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RandomStrategy implements Strategy {
    @Override
    public String nazovStrategie() {
        return "Random strategia";
    }

    @Override
    public int pocitaj(int pocetVaznov, int pocetKrabic) {
        // Príprava krabíc - naplníme ich číslami a náhodne premiešame
        List<Integer> krabice = new ArrayList<>();
        for (int i = 0; i < pocetKrabic; i++) {
            krabice.add(i);
        }
        Collections.shuffle(krabice);

        int uspesniVazni = 0;

        // Každý väzeň bude otvárať náhodné (bez opakovania) krabice až do limitu
        int maxPokusov = Math.max(1, pocetKrabic / 2);

        for (int vazonId = 0; vazonId < pocetVaznov; vazonId++) {
            // Vytvoríme poradie náhodného otvárania krabíc (bez opakovania)
            List<Integer> poradie = new ArrayList<>();
            for (int i = 0; i < pocetKrabic; i++) poradie.add(i);
            Collections.shuffle(poradie);

            boolean nasielSvojeCislo = false;
            for (int pokus = 0; pokus < Math.min(maxPokusov, poradie.size()); pokus++) {
                int idx = poradie.get(pokus);
                int cisloVKrabici = krabice.get(idx);
                if (cisloVKrabici == vazonId) {
                    nasielSvojeCislo = true;
                    break;
                }
            }

            if (nasielSvojeCislo) uspesniVazni++;
        }

        return uspesniVazni;
    }
}
