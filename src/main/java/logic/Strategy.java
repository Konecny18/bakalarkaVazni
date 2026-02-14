package logic;

public interface Strategy {
    // Vráti názov pre zobrazenie v menu (napr. "Cyklická")
    String nazovStrategie();

    // Hlavná simulácia, vráti počet úspešných väzňov
    int pocitaj(int pocetVaznov, int maxPokusov);
}
