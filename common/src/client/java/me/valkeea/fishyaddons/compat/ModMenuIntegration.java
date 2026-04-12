package me.valkeea.fishyaddons.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;

public class ModMenuIntegration implements ModMenuApi {
    
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> ScreenManager.getOrCreateConfigScreen();
    }
}
