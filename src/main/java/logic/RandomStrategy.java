package logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomStrategy implements Strategy {

    private int maxUspesnychVHistorii = 0; // Tu si budeme pamätať rekord

    @Override
    public String nazovStrategie() {
        return "Náhodná stratégia";
    }

    @Override
    public int pocitaj(int pocetVaznov, int maxPokusov) {
        List<Integer> krabice = new ArrayList<>();
        for (int i = 0; i < pocetVaznov; i++) krabice.add(i);
        Collections.shuffle(krabice);

        int uspesniVazni = 0;
        Random rnd = new Random();

        for (int vazonId = 0; vazonId < pocetVaznov; vazonId++) {
            if (simulujNahodnyVyber(krabice, vazonId, maxPokusov, rnd)) {
                uspesniVazni++;
            }
        }

        // --- NOVÁ LOGIKA PRE REKORD ---
        // Ak skupina ako celok NEUSPELA (uspesniVazni < pocetVaznov),
        // pozrieme sa, či je toto náš nový rekord v počte úspešných jednotlivcov.
        if (uspesniVazni < pocetVaznov) {
            if (uspesniVazni > maxUspesnychVHistorii) {
                maxUspesnychVHistorii = uspesniVazni;
            }
        }

        return uspesniVazni;
    }

    private boolean simulujNahodnyVyber(List<Integer> krabice, int hladaneCislo, int maxPokusov, Random rnd) {
        int n = krabice.size();
        List<Integer> indexy = new ArrayList<>();
        for (int i = 0; i < n; i++) indexy.add(i);
        Collections.shuffle(indexy, rnd);

        for (int i = 0; i < maxPokusov; i++) {
            if (krabice.get(indexy.get(i)) == hladaneCislo) {
                return true;
            }
        }
        return false;
    }

    // Túto metódu pridáme do Strategy interface, aby sme ju mohli volať v Controlleri
    public int getMaxUspesnychVHistorii() {
        return maxUspesnychVHistorii;
    }

    @Override
    public int getNajdlhsiCyklusPoslednejSimulacie() {
        return 0;
    }

    public void resetStats() {
        this.maxUspesnychVHistorii = 0;
    }
}