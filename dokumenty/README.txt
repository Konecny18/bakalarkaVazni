Simulácia problému 100 väzňov
Autor: Damián Konečný

🚀 Inštrukcie na spustenie
Možnosť 1: Spustenie jedným klikom (Najpohodlnejšie)
V hlavnom priečinku projektu sa nachádza súbor SPUSTI_SIMULACIU.bat.

Stačí naň dvakrát kliknúť v Prieskumníkovi Windows.

Skript automaticky použije pribalený Maven Wrapper (mvnw), skontroluje závislosti a spustí aplikáciu.

Poznámka: Vyžaduje v systéme nastavenú premennú JAVA_HOME smerujúcu na JDK 19+.

Možnosť 2: Cez Maven v IntelliJ IDEA
Ak máte projekt otvorený vo vývojovom prostredí:

V pravom paneli otvorte kartu Maven.

Rozkliknite: vazniSimulacia -> Plugins -> javafx -> javafx:run.

Možnosť 3: Priame spustenie hlavnej triedy

V strome súborov navigujte do: src/main/java/GUI/AppLauncher.java.

Kliknite pravým tlačidlom na triedu a zvoľte Run 'AppLauncher.main()'.

📋 Popis projektu
Táto aplikácia simuluje známy matematický problém 100 väzňov.
Umožňuje používateľovi vizualizovať rôzne stratégie (hľadanie cyklov, náhodný výber, párne/nepárne čísla) a porovnávať ich úspešnosť pomocou generovaných grafov a štatistík.

🛠 Technické požiadavky
Java JDK: 19 alebo novšia (projekt bol vyvíjaný v JDK 19.0.1).

Závislosti: JavaFX (riešené automaticky cez Maven).

Modulárny systém: Projekt využíva Java Module System (module-info.java).