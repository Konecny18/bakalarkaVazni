package logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CycleStrategy implements Strategy {
    @Override
    public String nazovStrategie() {
        return "Cyklická stratégia";
    }

    public List<Integer> generujKrabice(int pocetKrabic) {
        List<Integer> krabice = new ArrayList<>();
        for (int i = 0; i < pocetKrabic; i++) {
            krabice.add(i);
        }
        Collections.shuffle(krabice);
        return krabice;
    }

    @Override
    public int pocitaj(int pocetVaznov, int maxPokusov) {
        // Počet krabíc musí byť VŽDY rovnaký ako počet väzňov
        List<Integer> krabice = generujKrabice(pocetVaznov);
        return simulujSExistujucimiKrabicami(pocetVaznov, krabice, maxPokusov);
    }

    public int simulujSExistujucimiKrabicami(int pocetVaznov, List<Integer> krabice, int maxPokusov) {
        int uspesniVazni = 0;

        for (int vazonId = 0; vazonId < pocetVaznov; vazonId++) {
            int aktualnaKrabica = vazonId;
            boolean nasielSvojeCislo = false;

            for (int pokus = 0; pokus < maxPokusov; pokus++) {
                int cisloVKrabici = krabice.get(aktualnaKrabica);
                if (cisloVKrabici == vazonId) {
                    nasielSvojeCislo = true;
                    break;
                }
                aktualnaKrabica = cisloVKrabici;
            }
            if (nasielSvojeCislo) uspesniVazni++;
        }
        return uspesniVazni;
    }

    public List<List<Integer>> najdiVsetkyCykly(List<Integer> krabice) {
        int pocetKrabic = krabice.size();
        List<List<Integer>> vsetkyCykly = new ArrayList<>();
        boolean[] navstivene = new boolean[pocetKrabic];

        for (int i = 0; i < pocetKrabic; i++) {
            if (!navstivene[i]) {
                List<Integer> aktualnyCyklus = new ArrayList<>();
                int k = i;
                while (!navstivene[k]) {
                    navstivene[k] = true;
                    aktualnyCyklus.add(k);
                    k = krabice.get(k);
                }
                vsetkyCykly.add(aktualnyCyklus);
            }
        }
        return vsetkyCykly;
    }
}