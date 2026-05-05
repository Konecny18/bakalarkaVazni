package GUI;

/**
 * Vstupná trieda pre spustenie aplikácie z IDE/build systému.
 * Deleguje spustenie na triedu `Menu`, ktorá inicializuje JavaFX prostredie.
 */
public class AppLauncher {
    public static void main(String[] args) {
        Menu.main(args);
    }
}