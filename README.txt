Jasné, upravím to tak, aby to bolo pre oponenta čo najpohodlnejšie. Pridáme tam možnosť priameho spustenia hlavnej triedy, čo je v IntelliJ často najrýchlejšia cesta.

Tu je finálna verzia tvojho README.txt:

Simulácia problému 100 väzňov (100 Prisoners Problem)
Autor: Damián Konečný

Prostredie: JavaFX, Maven, JDK 26 (podporované JDK 19+)vytvo

Popis projektu
Táto aplikácia simuluje matematický problém 100 väzňov. Vizualizuje úspešnosť optimálnej stratégie založenej na hľadaní cyklov v porovnaní s náhodným výberom.

Požiadavky na spustenie
Java JDK 19 alebo novšia (odporúčaná verzia 26).

IntelliJ IDEA (alebo iné IDE s podporou Maven).

Inštrukcie na spustenie
Možnosť 1: Priame spustenie v IDE (Najrýchlejšie)
Otvorte projekt v IntelliJ IDEA.

V strome súborov (src/main/java/GUI/) vyhľadajte triedu Menu.

Kliknite na ňu pravým tlačidlom a zvoľte Run 'Menu.main()'.

Možnosť 2: Cez Maven plugin (Odporúčané pre čistý build)
V pravom paneli IntelliJ otvorte kartu Maven.

Rozkliknite: vazniSimulacia -> Plugins -> javafx -> javafx:run.
Alternatívne v termináli: mvn javafx:run

Možnosť 3: Spustenie cez pripravený artefakt
V priečinku out/artifacts/vazniSimulacia_jar/ sa nachádza zostavený archív. Spustenie (vyžaduje správne nastavené systémové premenné pre JavaFX):

Bash:
java -jar vazniSimulacia_jar.jar

Poznámky
Projekt je plne modulárny (JPMS). V prípade problémov s knižnicami v IDE odporúčam vykonať Maven -> Reload Project a následne Lifecycle -> clean.