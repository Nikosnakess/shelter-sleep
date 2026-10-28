package com.sheltersleep;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Aggiunge il pulsante "Configura" della mod dentro Mod Menu. Viene caricata solo se Mod Menu e' installato. */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> ShelterConfig.getScreen(parent, ShelterSleep.MOD_ID);
    }
}
