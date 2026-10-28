package com.sheltersleep;

import eu.midnightdust.lib.config.MidnightConfig;

/**
 * Configurazione modificabile da Mod Menu (schermata creata da MidnightLib).
 * Il file e' config/shelter_sleep.json. I testi della schermata sono in assets/shelter_sleep/lang.
 */
public class ShelterConfig extends MidnightConfig {

    // --- Regole ---
    @Entry public static boolean requireRoof = true;
    @Entry public static boolean requireDoor = true;
    @Entry(min = 1, max = 200, isSlider = true) public static int minRoomSize = 18;

    // --- Tolleranze ---
    @Entry(min = 0, max = 100, isSlider = true) public static int maxWindowPercent = 25;
    @Entry(min = 0, max = 100, isSlider = true) public static int maxRoofHolePercent = 0;

    // --- Varie ---
    @Entry public static boolean bypassInCreative = false;

    // --- Messaggi ---
    @Entry public static String msgNoRoof = "I need a roof";
    @Entry public static String msgNoSpace = "I need more space";
    @Entry public static String msgNoDoor = "I need a door";
    @Entry public static String msgNoWalls = "I need walls";

    /** Valori fissi (non modificabili dal menu) + valori scelti dal giocatore. */
    public static ShelterCheck.Settings settings() {
        return new ShelterCheck.Settings(
                Math.max(1, minRoomSize),
                1200,   // volume massimo prima di considerare l'area "aperta"
                20,     // raggio orizzontale massimo dal letto
                12,     // altezza massima dal letto
                10,     // altezza in cui si cerca il tetto
                Math.min(100, Math.max(0, maxWindowPercent)) / 100.0,
                Math.min(100, Math.max(0, maxRoofHolePercent)) / 100.0,
                requireRoof,
                requireDoor);
    }
}
