package de.gamematex.pvpheads;

import de.gamematex.pvpheads.gui.HeadGuiManager;
import de.gamematex.pvpheads.listener.HeadBreakListener;
import de.gamematex.pvpheads.listener.HeadInteractListener;
import de.gamematex.pvpheads.listener.HeadSignListener;
import de.gamematex.pvpheads.listener.PlayerDeathListener;
import org.bukkit.plugin.java.JavaPlugin;

public class PvPHeadsPlugin extends JavaPlugin {

    private HeadGuiManager guiManager;

    @Override
    public void onEnable() {
        this.guiManager = new HeadGuiManager(this);

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new HeadInteractListener(this, guiManager), this);
        getServer().getPluginManager().registerEvents(new HeadBreakListener(this, guiManager), this);
        getServer().getPluginManager().registerEvents(new HeadSignListener(this), this);

        getLogger().info("PvPHeads wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("PvPHeads wurde deaktiviert.");
    }

    public HeadGuiManager getGuiManager() {
        return guiManager;
    }
}
