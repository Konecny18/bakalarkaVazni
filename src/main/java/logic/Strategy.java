package logic;

public interface Strategy {
    // Vráti názov pre zobrazenie v menu (napr. "Cyklická")
    String nazovStrategie();

    // Hlavná simulácia, vráti počet úspešných väzňov
    int pocitaj(int pocetVaznov, int maxPokusov);

    default int getNajdlhsiCyklusPoslednejSimulacie() { return 0; }

    default int getMaxUspesnychVHistorii() { return 0; }

    default void resetStats() {}
}
