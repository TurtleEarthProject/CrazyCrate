package com.badbones69.crazycrates;

import com.badbones69.crazycrates.api.mmo.CRMMOApi;
import com.badbones69.crazycrates.api.PrizeManager;
import com.badbones69.crazycrates.commands.CommandManager;
import com.badbones69.crazycrates.common.Server;
import com.badbones69.crazycrates.listeners.BrokeLocationsListener;
import com.badbones69.crazycrates.listeners.CrateControlListener;
import com.badbones69.crazycrates.listeners.MiscListener;
import com.badbones69.crazycrates.listeners.crates.CrateOpenListener;
import com.badbones69.crazycrates.listeners.crates.types.CosmicCrateListener;
import com.badbones69.crazycrates.listeners.crates.types.MobileCrateListener;
import com.badbones69.crazycrates.listeners.crates.types.QuadCrateListener;
import com.badbones69.crazycrates.listeners.crates.types.WarCrateListener;
import com.badbones69.crazycrates.listeners.other.EntityDamageListener;
import com.badbones69.crazycrates.managers.BukkitUserManager;
import com.badbones69.crazycrates.managers.InventoryManager;
import com.badbones69.crazycrates.support.MetricsWrapper;
import com.badbones69.crazycrates.support.holograms.HologramManager;
import com.badbones69.crazycrates.support.placeholders.PlaceholderAPISupport;
import com.badbones69.crazycrates.tasks.crates.CrateManager;
import com.badbones69.crazycrates.listeners.CommandBlockListener;
import com.badbones69.crazycrates.utils.MiscUtils;

import java.util.Arrays;
import java.util.Timer;
import libs.com.ryderbelserion.vital.paper.Vital;
import libs.com.ryderbelserion.vital.paper.api.enums.Support;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class CrazyCrates extends JavaPlugin {
    private final Vital vital = new Vital(this);
    private final Timer timer = new Timer();
    private final long startTime = System.nanoTime();
    private InventoryManager inventoryManager;
    private BukkitUserManager userManager;
    private CrateManager crateManager;
    private HeadDatabaseAPI api;
    private Server instance;
    @Nullable
    private CRMMOApi mmoApi;

    @Internal
    public static CrazyCrates getPlugin() {
        return (CrazyCrates) JavaPlugin.getPlugin(CrazyCrates.class);
    }

    public void onEnable() {
        this.instance = new Server(this.getDataFolder());
        this.instance.apply();
        this.vital.getFileManager().addFile("locations.yml").addFile("data.yml").addFile("respin-gui.yml", "guis")
                .addFile("crates.log", "logs").addFile("keys.log", "logs").addFolder("crates").addFolder("schematics")
                .init();
        MiscUtils.janitor();
        MiscUtils.save();
        MiscUtils.registerPermissions();
        if (Support.head_database.isEnabled()) {
            this.api = new HeadDatabaseAPI();
        }

        if (getServer().getPluginManager().isPluginEnabled("MMOItems")) {
            mmoApi = new CRMMOApi();
        }

        this.inventoryManager = new InventoryManager();
        this.crateManager = new CrateManager();
        this.userManager = new BukkitUserManager();
        this.instance.setUserManager(this.userManager);
        this.crateManager.loadHolograms();
        this.inventoryManager.loadButtons();
        this.crateManager.loadCrates();
        CommandManager.load();
        (new MetricsWrapper(this, 4514)).start();
        Arrays.asList(new BrokeLocationsListener(), new CrateControlListener(), new EntityDamageListener(),
                new MobileCrateListener(), new CosmicCrateListener(), new QuadCrateListener(), new CrateOpenListener(),
                new WarCrateListener(), new MiscListener(), new CommandBlockListener())
                .forEach((listener) -> this.getServer().getPluginManager().registerEvents(listener, this));
        if (Support.placeholder_api.isEnabled()) {
            if (MiscUtils.isLogging()) {
                this.getLogger().info("PlaceholderAPI support is enabled!");
            }

            (new PlaceholderAPISupport()).register();
        }

        /*
         * if (MiscUtils.isLogging()) {
         * for(Support value : Support.values()) {
         * if (value.isEnabled()) {
         * this.getLogger().info(AdvUtil.parse("<bold><gold>" + value.getName() +
         * " <green>FOUND"));
         * } else {
         * this.getLogger().info(AdvUtil.parse("<bold><gold>" + value.getName() +
         * " <red>NOT FOUND"));
         * }
         * }
         * 
         * this.getLogger().info("Done ({})!", String.format(Locale.ROOT, "%.3fs",
         * (double)(System.nanoTime() - this.startTime) / (double)1.0E9F));
         * }
         */

    }

    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        this.timer.cancel();
        if (this.crateManager != null) {
            this.crateManager.purgeRewards();
            HologramManager holograms = this.crateManager.getHolograms();
            if (holograms != null) {
                holograms.purge(true);
            }
        }

        if (this.instance != null) {
            this.instance.disable();
        }

        PrizeManager.flushDataSave();
        MiscUtils.janitor();
    }

    @Internal
    public final InventoryManager getInventoryManager() {
        return this.inventoryManager;
    }

    @Internal
    public final BukkitUserManager getUserManager() {
        return this.userManager;
    }

    @Internal
    public final CrateManager getCrateManager() {
        return this.crateManager;
    }

    @Internal
    public final @Nullable HeadDatabaseAPI getApi() {
        return this.api == null ? null : this.api;
    }

    @Internal
    public final Server getInstance() {
        return this.instance;
    }

    @Internal
    public final @Nullable CRMMOApi getMmoApi() {
        return this.mmoApi;
    }

    @Internal
    public final Vital getVital() {
        return this.vital;
    }

    @Internal
    public final Timer getTimer() {
        return this.timer;
    }
}
