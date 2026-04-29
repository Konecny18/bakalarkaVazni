package logic;

/**
 * Rozhranie Strategy definuje kontrakt pre rôzne stratégie hľadania pre väzňov.
 * Implementácie poskytujú názov pre zobrazenie a metódu simulácie, ktorá
 * vráti počet úspešných väzňov pre jednu permutáciu.
 */
public interface Strategy {
    /**
     * Vráti názov stratégie pre zobrazenie (napr. "Cyklická stratégia").
     * @return lokalizovaný názov stratégie
     */
    String nazovStrategie();

    /**
     * Spustí stratégiu pre jednu permutáciu a vráti, koľko väzňov
     * našlo svoje číslo v rámci {@code maxPokusov} pokusov.
     * @param pocetVaznov počet väzňov / krabíc
     * @param maxPokusov maximálny počet otvorení krabíc pre jedného väzňa
     * @return počet úspešných väzňov
     */
    int pocitaj(int pocetVaznov, int maxPokusov);

    /**
     * Voliteľné: vráti dĺžku najdlhšieho cyklu z poslednej simulácie
     * (má význam len pri stratégiách založených na cykloch).
     * Predvolená implementácia vráti 0.
     * @return dĺžka najdlhšieho cyklu z poslednej simulácie
     */
    default int getNajdlhsiCyklusPoslednejSimulacie() { return 0; }

    /**
     * Voliteľné: vráti maximálny počet úspešných väzňov pozorovaný v histórii
     * pre danú stratégiu. Predvolená implementácia vráti 0.
     * @return maximálny pozorovaný počet úspechov
     */
    default int getMaxUspesnychVHistorii() { return 0; }

    /**
     * Resetnúť vnútorné štatistiky, ak ich implementácia ukladá.
     * Predvolená implementácia nič nerobí.
     */
    default void resetStats() {}
}
