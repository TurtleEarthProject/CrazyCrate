package com.badbones69.crazycrates.tasks.crates;

import ch.jalu.configme.SettingsManager;
import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.ChestManager;
import com.badbones69.crazycrates.api.builders.CrateBuilder;
import com.badbones69.crazycrates.api.builders.ItemBuilder;
import com.badbones69.crazycrates.api.enums.Messages;
import com.badbones69.crazycrates.api.enums.misc.Files;
import com.badbones69.crazycrates.api.enums.misc.Keys;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import com.badbones69.crazycrates.api.objects.Tier;
import com.badbones69.crazycrates.api.objects.crates.BrokeLocation;
import com.badbones69.crazycrates.api.objects.crates.CrateHologram;
import com.badbones69.crazycrates.api.objects.crates.CrateLocation;
import com.badbones69.crazycrates.api.objects.crates.quadcrates.CrateSchematic;
import com.badbones69.crazycrates.common.config.ConfigManager;
import com.badbones69.crazycrates.common.config.impl.ConfigKeys;
import com.badbones69.crazycrates.managers.InventoryManager;
import com.badbones69.crazycrates.managers.events.enums.EventType;
import com.badbones69.crazycrates.support.holograms.HologramManager;
import com.badbones69.crazycrates.support.holograms.types.DecentHologramsSupport;
import com.badbones69.crazycrates.support.holograms.types.FancyHologramsSupport;
import com.badbones69.crazycrates.tasks.crates.types.CasinoCrate;
import com.badbones69.crazycrates.tasks.crates.types.CosmicCrate;
import com.badbones69.crazycrates.tasks.crates.types.CrateOnTheGo;
import com.badbones69.crazycrates.tasks.crates.types.CsgoCrate;
import com.badbones69.crazycrates.tasks.crates.types.FireCrackerCrate;
import com.badbones69.crazycrates.tasks.crates.types.QuadCrate;
import com.badbones69.crazycrates.tasks.crates.types.QuickCrate;
import com.badbones69.crazycrates.tasks.crates.types.RouletteCrate;
import com.badbones69.crazycrates.tasks.crates.types.WarCrate;
import com.badbones69.crazycrates.tasks.crates.types.WheelCrate;
import com.badbones69.crazycrates.tasks.crates.types.WonderCrate;
import com.badbones69.crazycrates.tasks.menus.CrateMainMenu;
import com.badbones69.crazycrates.utils.MiscUtils;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import libs.com.ryderbelserion.vital.common.utils.FileUtil;
import libs.com.ryderbelserion.vital.paper.api.enums.Support;
import libs.com.ryderbelserion.vital.paper.api.files.CustomFile;
import libs.com.ryderbelserion.vital.paper.api.files.FileManager;
import com.badbones69.crazycrates.utils.ColorUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import us.crazycrew.crazycrates.api.enums.types.CrateType;
import us.crazycrew.crazycrates.api.enums.types.KeyType;

public class CrateManager {
    private final CrazyCrates plugin = CrazyCrates.getPlugin();
    private final InventoryManager inventoryManager;
    private final FileManager fileManager;
    private final List<CrateLocation> crateLocations;
    private final List<CrateSchematic> crateSchematics;
    private final List<BrokeLocation> brokeLocations;
    private final Map<UUID, Location> cratesInUse;
    private final List<String> brokeCrates;
    private final List<Crate> crates;
    private final Map<UUID, Map<Integer, Tier>> tiers;
    private HologramManager holograms;
    private boolean giveNewPlayersKeys;
    private final SettingsManager config;
    private final Map<UUID, Crate> playerOpeningCrates;
    private final Map<UUID, KeyType> playerKeys;
    private final Map<UUID, io.papermc.paper.threadedregions.scheduler.ScheduledTask> currentTasks;
    private final Map<UUID, TimerTask> timerTasks;
    private final Map<UUID, List<io.papermc.paper.threadedregions.scheduler.ScheduledTask>> currentQuadTasks;
    private final Map<UUID, Boolean> canPick;
    private final Map<UUID, Boolean> canClose;
    private final Map<UUID, Boolean> checkHands;
    private final List<Entity> allRewards;
    private final Map<UUID, Entity> rewards;
    private final Map<UUID, ArrayList<Integer>> slots;

    public CrateManager() {
        this.inventoryManager = this.plugin.getInventoryManager();
        this.fileManager = this.plugin.getVital().getFileManager();
        this.crateLocations = new ArrayList();
        this.crateSchematics = new ArrayList();
        this.brokeLocations = new ArrayList();
        this.cratesInUse = new HashMap();
        this.brokeCrates = new ArrayList();
        this.crates = new ArrayList();
        this.tiers = new WeakHashMap();
        this.config = ConfigManager.getConfig();
        this.playerOpeningCrates = new HashMap();
        this.playerKeys = new HashMap();
        this.currentTasks = new HashMap<>();
        this.timerTasks = new HashMap();
        this.currentQuadTasks = new HashMap();
        this.canPick = new HashMap();
        this.canClose = new HashMap();
        this.checkHands = new HashMap();
        this.allRewards = new ArrayList();
        this.rewards = new HashMap();
        this.slots = new HashMap();
    }

    public void addTier(Player player, final int slot, final Tier tier) {
        if (this.tiers.containsKey(player.getUniqueId())) {
            ((Map) this.tiers.get(player.getUniqueId())).put(slot, tier);
        } else {
            this.tiers.put(player.getUniqueId(), new WeakHashMap<Integer, Tier>() {
                {
                    this.put(slot, tier);
                }
            });
        }
    }

    public void removeTier(Player player) {
        this.tiers.remove(player.getUniqueId());
    }

    public final Tier getTier(Player player, int slot) {
        return (Tier) ((Map) this.tiers.get(player.getUniqueId())).get(slot);
    }

    public Map<UUID, Map<Integer, Tier>> getTiers() {
        return Collections.unmodifiableMap(this.tiers);
    }

    public void reloadCrate(@Nullable Crate crate) {
        try {
            if (crate == null) {
                return;
            }

            FileConfiguration file = crate.getFile();
            this.inventoryManager.closePreview();
            crate.purge();
            List<Prize> prizes = new ArrayList<>();
            ConfigurationSection prizesSection = file.getConfigurationSection("Crate.Prizes");
            if (prizesSection != null) {
                for (String prize : prizesSection.getKeys(false)) {
                    ConfigurationSection prizeSection = prizesSection.getConfigurationSection(prize);
                    List<Tier> tierPrizes = new ArrayList<>();
                    if (prizeSection != null) {
                        List<ItemStack> editorItems = new ArrayList<>();
                        if (prizeSection.contains("Editor-Items")) {
                            List<?> list = prizeSection.getList("Editor-Items");
                            if (list != null) {
                                for (Object key : list) {
                                    editorItems.add((ItemStack) key);
                                }
                            }
                        }

                        for (String tier : prizeSection.getStringList("Tiers")) {
                            for (Tier key : crate.getTiers()) {
                                if (key.getName().equalsIgnoreCase(tier)) {
                                    tierPrizes.add(key);
                                }
                            }
                        }

                        Prize alternativePrize = null;
                        ConfigurationSection alternativeSection = prizeSection
                                .getConfigurationSection("Alternative-Prize");
                        if (alternativeSection != null) {
                            boolean isEnabled = alternativeSection.getBoolean("Toggle");
                            if (isEnabled) {
                                alternativePrize = new Prize(
                                        ColorUtils.color(
                                                prizeSection.getString("DisplayName", "")),
                                        prizeSection.getName(), alternativeSection);
                            }
                        }

                        prizes.add(new Prize(prizeSection, editorItems, tierPrizes, crate.getFileName(),
                                alternativePrize));
                    }
                }
            }

            crate.setPrize(prizes);
            this.inventoryManager.openPreview(crate);
        } catch (Exception exception) {
            String fileName = crate.getFileName();
            this.brokeCrates.add(fileName);
            if (MiscUtils.isLogging()) {
                this.plugin.getLogger().warning("There was an error while loading the {}.yml file.");
            }
        }

    }

    public void loadHolograms() {
        String pluginName = (String) this.config.getProperty(ConfigKeys.hologram_plugin);
        if (this.holograms != null && !pluginName.isEmpty()) {
            this.holograms.purge(false);
        }

        switch (pluginName) {
            case "DecentHolograms":
                if (!Support.decent_holograms.isEnabled()) {
                    return;
                }

                this.holograms = new DecentHologramsSupport();
                break;
            case "FancyHolograms":
                if (!Support.fancy_holograms.isEnabled()) {
                    return;
                }

                this.holograms = new FancyHologramsSupport();
                break;
            /*
             * case "CMI":
             * if (!Support.cmi.isEnabled() && !CMIModule.holograms.isEnabled()) {
             * return;
             * }
             * 
             * this.holograms = new CMIHologramsSupport();
             * break;
             */
            default:
                if (Support.decent_holograms.isEnabled()) {
                    this.holograms = new DecentHologramsSupport();
                } else if (Support.fancy_holograms.isEnabled()) {
                    this.holograms = new FancyHologramsSupport();
                } /*
                   * else if (Support.cmi.isEnabled() && CMIModule.holograms.isEnabled()) {
                   * this.holograms = new CMIHologramsSupport();
                   * }
                   */
        }

        if (this.holograms == null) {
            if (MiscUtils.isLogging()) {
                List<String> var10000 = Arrays.asList(
                        "There was no hologram plugin found on the server. If you are using CMI",
                        "Please make sure you enabled the hologram module in modules.yml",
                        "You can run /crazycrates reload if using CMI otherwise restart your server.");
                Logger var10001 = this.plugin.getLogger();
                Objects.requireNonNull(var10001);
                var10000.forEach(var10001::warning);
            }

        } else {
            if (MiscUtils.isLogging()) {
                this.plugin.getLogger().info("{} support has been enabled.");
            }

        }
    }

    public List<String> getCrateNames() {
        return this.plugin.getInstance().getCrateFiles();
    }

    public void loadCrates() {
        if ((Boolean) ConfigManager.getConfig().getProperty(ConfigKeys.update_examples_folder)) {
            Path path = this.plugin.getDataFolder().toPath();
            Class<? extends CrazyCrates> classObject = this.plugin.getClass();
            Arrays.asList("config.yml", "data.yml", "locations.yml", "messages.yml")
                    .forEach((filex) -> FileUtil.extract(filex, "examples", true));
            FileUtil.extracts(classObject, "/guis/", path.resolve("examples").resolve("guis"), true);
            FileUtil.extracts(classObject, "/logs/", path.resolve("examples").resolve("logs"), true);
            FileUtil.extracts(classObject, "/crates/", path.resolve("examples").resolve("crates"), true);
            FileUtil.extracts(classObject, "/schematics/", path.resolve("examples").resolve("schematics"), true);
        }

        this.giveNewPlayersKeys = false;
        this.purge();
        if (this.holograms != null) {
            this.holograms.purge(false);
        }

        if (MiscUtils.isLogging()) {
            this.plugin.getLogger().info("Loading all crate information...");
        }

        for (String crateName : this.getCrateNames()) {
            try {
                CustomFile customFile = this.fileManager.getFile(crateName, true);
                if (customFile != null) {
                    YamlConfiguration file = customFile.getConfiguration();
                    if (file != null) {
                        // Bundled Crate reads these fields directly and sends them to MiniMessage consumers.
                        for (String path : List.of("Crate.CrateName", "Crate.Name", "Crate.Preview.Glass.Name",
                                "Crate.tier-preview.glass.name")) {
                            if (file.contains(path)) {
                                file.set(path, ColorUtils.color(file.getString(path, "")));
                            }
                        }
                        CrateType crateType = CrateType.getFromName(
                                ColorUtils.color(file.getString("Crate.CrateType", "CSGO")));
                        ArrayList<Prize> prizes = new ArrayList();
                        List<Tier> tiers = new ArrayList();
                        String previewName;
                        if (file.contains("Crate.Preview-Name")) {
                            previewName = ColorUtils.color(
                                    file.getString("Crate.Preview-Name", " "));
                        } else if (file.contains("Crate.Preview.Name")) {
                            previewName = ColorUtils.color(
                                    file.getString("Crate.Preview.Name", " "));
                        } else if (file.contains("Crate.CrateName")) {
                            previewName = ColorUtils.color(
                                    file.getString("Crate.CrateName", " "));
                        } else {
                            previewName = ColorUtils.color(
                                    file.getString("Crate.Name", " "));
                        }

                        int maxMassOpen = file.getInt("Crate.Max-Mass-Open", 10);
                        int requiredKeys = file.getInt("Crate.RequiredKeys", 0);
                        ConfigurationSection section = file.getConfigurationSection("Crate.Tiers");
                        if (file.contains("Crate.Tiers") && section != null) {
                            for (String tier : section.getKeys(false)) {
                                String path = "Crate.Tiers." + tier;
                                ConfigurationSection tierSection = file.getConfigurationSection(path);
                                if (tierSection != null) {
                                    tiers.add(new Tier(tier, tierSection));
                                }
                            }
                        }

                        boolean isTiersEmpty = crateType == CrateType.cosmic || crateType == CrateType.casino;
                        if (isTiersEmpty && tiers.isEmpty()) {
                            this.brokeCrates.add(crateName);
                            if (MiscUtils.isLogging()) {
                                this.plugin.getLogger().warning("No tiers were found for {}.yml file.");
                            }
                        } else {
                            ConfigurationSection prizesSection = file.getConfigurationSection("Crate.Prizes");
                            if (prizesSection != null) {
                                for (String prize : prizesSection.getKeys(false)) {
                                    ConfigurationSection prizeSection = prizesSection.getConfigurationSection(prize);
                                    List<Tier> tierPrizes = new ArrayList();
                                    Prize alternativePrize = null;
                                    if (prizeSection != null) {
                                        List<ItemStack> editorItems = new ArrayList();
                                        if (prizeSection.contains("Editor-Items")) {
                                            List<?> keys = prizeSection.getList("Editor-Items");
                                            if (keys != null) {
                                                for (Object key : keys) {
                                                    editorItems.add((ItemStack) key);
                                                }
                                            }
                                        }

                                        for (String tier : prizeSection.getStringList("Tiers")) {
                                            for (Tier key : tiers) {
                                                if (key.getName().equalsIgnoreCase(tier)) {
                                                    tierPrizes.add(key);
                                                }
                                            }
                                        }

                                        ConfigurationSection alternativeSection = prizeSection
                                                .getConfigurationSection("Alternative-Prize");
                                        if (alternativeSection != null) {
                                            boolean isEnabled = alternativeSection.getBoolean("Toggle");
                                            if (isEnabled) {
                                                alternativePrize = new Prize(
                                                        ColorUtils.color(
                                                                prizeSection.getString("DisplayName",
                                                                        "<lang:item.minecraft." + prizeSection
                                                                                .getString("DisplayItem", "stone")
                                                                                .toLowerCase() + ">")),
                                                        prizeSection.getName(), alternativeSection);
                                            }
                                        }

                                        prizes.add(new Prize(prizeSection, editorItems, tierPrizes, crateName,
                                                alternativePrize));
                                    }
                                }
                            }

                            int newPlayersKeys = file.getInt("Crate.StartingKeys", 0);
                            if (!this.giveNewPlayersKeys && newPlayersKeys > 0) {
                                this.giveNewPlayersKeys = true;
                            }

                            List<String> prizeMessage = file.contains("Crate.Prize-Message")
                                    ? file.getStringList("Crate.Prize-Message")
                                    : Collections.emptyList();
                            List<String> prizeCommands = file.contains("Crate.Prize-Commands")
                                    ? file.getStringList("Crate.Prize-Commands")
                                    : Collections.emptyList();
                            CrateHologram holo = new CrateHologram(file.getBoolean("Crate.Hologram.Toggle"),
                                    file.getDouble("Crate.Hologram.Height", (double) 0.0F),
                                    file.getInt("Crate.Hologram.Range", 8),
                                    ColorUtils.color(
                                            file.getString("Crate.Hologram.Color", "transparent")),
                                    file.getInt("Crate.Hologram.Update-Interval", -1),
                                    ColorUtils.color(file.getStringList("Crate.Hologram.Message")));
                            this.addCrate(new Crate(crateName, previewName, crateType, this.getKey(file),
                                    ColorUtils.color(file.getString("Crate.PhysicalKey.Name",
                                            "Crate.PhysicalKey.Name is missing from " + crateName + ".yml")),
                                    prizes, file, newPlayersKeys, tiers, maxMassOpen, requiredKeys, prizeMessage,
                                    prizeCommands, holo));
                            PluginManager server = this.plugin.getServer().getPluginManager();
                            boolean isNewSystemEnabled = (Boolean) this.config
                                    .getProperty(ConfigKeys.use_new_permission_system);
                            String node = isNewSystemEnabled ? "crazycrates.deny.open." + crateName
                                    : "crazycrates.open." + crateName;
                            String description = isNewSystemEnabled ? "Prevents you from opening " + crateName
                                    : "Allows you to open " + crateName;
                            PermissionDefault permissionDefault = isNewSystemEnabled ? PermissionDefault.FALSE
                                    : PermissionDefault.TRUE;
                            if (server.getPermission(node) == null) {
                                Permission permission = new Permission(node, description, permissionDefault);
                                server.addPermission(permission);
                            }
                        }
                    }
                }
            } catch (Exception exception) {
                this.brokeCrates.add(crateName);
                if (MiscUtils.isLogging()) {
                    this.plugin.getLogger().warning("There was an error while loading the {}.yml file.");
                }
            }
        }

        this.addCrate(new Crate("Menu"));
        if (MiscUtils.isLogging()) {
            Arrays.asList("All crate information has been loaded.", "Loading all the physical crate locations.")
                    .forEach((line) -> this.plugin.getLogger().info(line));
        }

        YamlConfiguration locations = Files.locations.getConfiguration();
        int loadedAmount = 0;
        int brokeAmount = 0;
        ConfigurationSection section = locations.getConfigurationSection("Locations");
        if (section != null) {
            for (String locationName : section.getKeys(false)) {
                try {
                    String worldName = locations.getString("Locations." + locationName + ".World");
                    if (worldName == null) {
                        return;
                    }

                    if (worldName.isEmpty() || worldName.trim().isEmpty()) {
                        return;
                    }

                    World world = this.plugin.getServer().getWorld(worldName);
                    int x = locations.getInt("Locations." + locationName + ".X");
                    int y = locations.getInt("Locations." + locationName + ".Y");
                    int z = locations.getInt("Locations." + locationName + ".Z");
                    Location location = new Location(world, (double) x, (double) y, (double) z);
                    Crate crate = this.getCrateFromName(locations.getString("Locations." + locationName + ".Crate"));
                    if (world != null && crate != null) {
                        this.crateLocations.add(new CrateLocation(locationName, crate, location));
                        if (this.holograms != null) {
                            this.holograms.createHologram(location, crate, locationName);
                        }

                        ++loadedAmount;
                    } else {
                        this.brokeLocations.add(new BrokeLocation(locationName, crate, x, y, z, worldName));
                        ++brokeAmount;
                    }
                } catch (Exception var24) {
                }
            }
        }

        if (MiscUtils.isLogging()) {
            Logger logger = this.plugin.getLogger();
            if (loadedAmount > 0 || brokeAmount > 0) {
                if (brokeAmount <= 0) {
                    logger.info("All physical crate locations have been loaded.");
                } else {
                    logger.info("Loaded {} physical crate locations.");
                    logger.info("Failed to load {} physical crate locations.");
                }
            }

            logger.info("Searching for schematics to load.");
        }

        String[] schems = (new File(String.valueOf(this.plugin.getDataFolder()) + "/schematics/")).list();
        if (schems != null) {
            for (String schematicName : schems) {
                if (schematicName.endsWith(".nbt")) {
                    List var10000 = this.crateSchematics;
                    String var10006 = String.valueOf(this.plugin.getDataFolder());
                    var10000.add(
                            new CrateSchematic(schematicName, new File(var10006 + "/schematics/" + schematicName)));
                    if (MiscUtils.isLogging()) {
                        this.plugin.getLogger().info("{} was successfully found and loaded.");
                    }
                }
            }
        }

        if (MiscUtils.isLogging()) {
            this.plugin.getLogger().info("All schematics were found and loaded.");
        }

        this.cleanDataFile();
        this.inventoryManager.loadButtons();
    }

    public void openCrate(@NotNull Player player, @NotNull Crate crate, @NotNull KeyType keyType,
            @NotNull Location location, boolean virtualCrate, boolean checkHand, EventType eventType) {
        this.openCrate(player, crate, keyType, location, virtualCrate, checkHand, false, eventType);
    }

    public void openCrate(@NotNull Player player, @NotNull Crate crate, @NotNull KeyType keyType,
            @NotNull Location location, boolean virtualCrate, boolean checkHand, boolean isSilent,
            EventType eventType) {
        SettingsManager config = ConfigManager.getConfig();
        if (crate.getCrateType() == CrateType.menu) {
            if ((Boolean) config.getProperty(ConfigKeys.enable_crate_menu)) {
                (new CrateMainMenu(player, (String) this.config.getProperty(ConfigKeys.inventory_name),
                        (Integer) this.config.getProperty(ConfigKeys.inventory_rows))).open();
            } else {
                Messages.feature_disabled.sendMessage(player);
            }
        } else {
            String fancyName = crate.getCrateName();
            CrateBuilder crateBuilder;
            switch (crate.getCrateType()) {
                case csgo:
                    crateBuilder = new CsgoCrate(crate, player, 27);
                    break;
                case casino:
                    crateBuilder = new CasinoCrate(crate, player, 27);
                    break;
                case wonder:
                    crateBuilder = new WonderCrate(crate, player, 45);
                    break;
                case wheel:
                    crateBuilder = new WheelCrate(crate, player, 54);
                    break;
                case roulette:
                    crateBuilder = new RouletteCrate(crate, player, 27);
                    break;
                case war:
                    crateBuilder = new WarCrate(crate, player, 9);
                    break;
                case cosmic:
                    crateBuilder = new CosmicCrate(crate, player, 27);
                    break;
                case quad_crate:
                    if (virtualCrate) {
                        Map<String, String> placeholders = new HashMap();
                        placeholders.put("{cratetype}", crate.getCrateType().getName());
                        placeholders.put("{crate}", fancyName);
                        Messages.cant_be_a_virtual_crate.sendMessage(player, placeholders);
                        this.removePlayerFromOpeningList(player);
                        return;
                    }

                    crateBuilder = new QuadCrate(crate, player, location);
                    break;
                case fire_cracker:
                    if (this.cratesInUse.containsValue(location)) {
                        Messages.crate_in_use.sendMessage(player, "{crate}", fancyName);
                        this.removePlayerFromOpeningList(player);
                        return;
                    }

                    if (virtualCrate) {
                        Map<String, String> placeholders = new HashMap();
                        placeholders.put("{cratetype}", crate.getCrateType().getName());
                        placeholders.put("{crate}", fancyName);
                        Messages.cant_be_a_virtual_crate.sendMessage(player, placeholders);
                        this.removePlayerFromOpeningList(player);
                        return;
                    }

                    crateBuilder = new FireCrackerCrate(crate, player, 45, location);
                    break;
                case crate_on_the_go:
                    if (virtualCrate) {
                        Map<String, String> placeholders = new HashMap();
                        placeholders.put("{cratetype}", crate.getCrateType().getName());
                        placeholders.put("{crate}", fancyName);
                        Messages.cant_be_a_virtual_crate.sendMessage(player, placeholders);
                        this.removePlayerFromOpeningList(player);
                        return;
                    }

                    crateBuilder = new CrateOnTheGo(crate, player);
                    break;
                case quick_crate:
                    if (this.cratesInUse.containsValue(location)) {
                        Messages.crate_in_use.sendMessage(player, "{crate}", fancyName);
                        this.removePlayerFromOpeningList(player);
                        return;
                    }

                    if (virtualCrate) {
                        Map<String, String> placeholders = new HashMap();
                        placeholders.put("{cratetype}", crate.getCrateType().getName());
                        placeholders.put("{crate}", fancyName);
                        Messages.cant_be_a_virtual_crate.sendMessage(player, placeholders);
                        this.removePlayerFromOpeningList(player);
                        return;
                    }

                    crateBuilder = new QuickCrate(crate, player, location);
                    break;
                default:
                    crateBuilder = new CsgoCrate(crate, player, 27);
                    if (MiscUtils.isLogging()) {
                        Logger logger = this.plugin.getLogger();
                        List<String> var10000 = Arrays.asList(
                                crate.getFileName() + " has an invalid crate type. Your Value: "
                                        + crate.getFile().getString("Crate.CrateType", "CSGO"),
                                "We will use " + CrateType.csgo.getName() + " until you change the crate type.",
                                "Valid Crate Types: CSGO/Casino/Cosmic/QuadCrate/QuickCrate/Roulette/CrateOnTheGo/FireCracker/Wonder/Wheel/War");
                        Objects.requireNonNull(logger);
                        var10000.forEach(logger::warning);
                    }
            }

            crateBuilder.open(keyType, checkHand, isSilent, eventType);
        }
    }

    public void addCrateInUse(@NotNull Player player, @NotNull Location location) {
        this.cratesInUse.put(player.getUniqueId(), location);
    }

    public Location getCrateInUseLocation(@NotNull Player player) {
        return (Location) this.cratesInUse.get(player.getUniqueId());
    }

    public boolean isCrateInUse(@NotNull Player player) {
        return this.cratesInUse.containsKey(player.getUniqueId());
    }

    public void removeCrateInUse(@NotNull Player player) {
        this.cratesInUse.remove(player.getUniqueId());
    }

    public Map<UUID, Location> getCratesInUse() {
        return Collections.unmodifiableMap(this.cratesInUse);
    }

    public void endCrate(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        if (this.currentTasks.containsKey(uuid)) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask task = this.currentTasks.get(uuid);
            if (task != null) {
                task.cancel();
            }
        }

    }

    public void endQuadCrate(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        if (this.currentQuadTasks.containsKey(uuid)) {
            for (io.papermc.paper.threadedregions.scheduler.ScheduledTask task : this.currentQuadTasks.get(uuid)) {
                task.cancel();
            }

            this.currentQuadTasks.remove(uuid);
        }

    }

    public void addQuadCrateTask(@NotNull Player player,
            @NotNull io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
        UUID uuid = player.getUniqueId();
        if (!this.currentQuadTasks.containsKey(uuid)) {
            this.currentQuadTasks.put(uuid, new ArrayList<>());
        }

        (this.currentQuadTasks.get(uuid)).add(task);
    }

    public boolean hasQuadCrateTask(@NotNull Player player) {
        return this.currentQuadTasks.containsKey(player.getUniqueId());
    }

    public void addCrateTask(@NotNull Player player,
            @NotNull io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
        this.currentTasks.put(player.getUniqueId(), task);
    }

    public void addRepeatingCrateTask(@NotNull Player player, @NotNull TimerTask task, long delay, long period) {
        this.timerTasks.put(player.getUniqueId(), task);
        this.plugin.getTimer().scheduleAtFixedRate(task, delay, period);
    }

    public void removeCrateTask(@NotNull Player player) {
        TimerTask task = (TimerTask) this.timerTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }

    }

    public void addCrateTask(Player player, TimerTask task, Long delay) {
        this.timerTasks.put(player.getUniqueId(), task);
        this.plugin.getTimer().schedule(task, delay);
    }

    public final io.papermc.paper.threadedregions.scheduler.ScheduledTask getCrateTask(@NotNull Player player) {
        return this.currentTasks.get(player.getUniqueId());
    }

    public final boolean hasCrateTask(@NotNull Player player) {
        return this.currentTasks.containsKey(player.getUniqueId());
    }

    public void addPlayerToOpeningList(@NotNull Player player, @NotNull Crate crate) {
        this.playerOpeningCrates.put(player.getUniqueId(), crate);
    }

    public void removePlayerFromOpeningList(@NotNull Player player) {
        this.playerOpeningCrates.remove(player.getUniqueId());
    }

    public boolean isInOpeningList(@NotNull Player player) {
        return this.playerOpeningCrates.containsKey(player.getUniqueId());
    }

    public final Crate getOpeningCrate(@NotNull Player player) {
        return (Crate) this.playerOpeningCrates.get(player.getUniqueId());
    }

    public void addPlayerKeyType(@NotNull Player player, @NotNull KeyType keyType) {
        this.playerKeys.put(player.getUniqueId(), keyType);
    }

    public void removePlayerKeyType(@NotNull Player player) {
        this.playerKeys.remove(player.getUniqueId());
    }

    public final boolean hasPlayerKeyType(@NotNull Player player) {
        return this.playerKeys.containsKey(player.getUniqueId());
    }

    public final @Nullable KeyType getPlayerKeyType(@NotNull Player player) {
        return (KeyType) this.playerKeys.get(player.getUniqueId());
    }

    public void purge() {
        this.crates.clear();
        this.brokeCrates.clear();
        this.crateLocations.clear();
        this.crateSchematics.clear();
    }

    public void setNewPlayerKeys(@NotNull Player player) {
        if (this.giveNewPlayersKeys) {
            String uuid = player.getUniqueId().toString();
            if (!player.hasPlayedBefore()) {
                this.getUsableCrates().stream().filter(Crate::doNewPlayersGetKeys).forEach((crate) -> {
                    Files.data.getConfiguration().set("Players." + uuid + "." + crate.getFileName(),
                            crate.getNewPlayerKeys());
                    Files.data.save();
                });
            }
        }

    }

    public void addCrate(@NotNull Crate crate) {
        this.crates.add(crate);
    }

    public void addLocation(@NotNull CrateLocation crateLocation) {
        this.crateLocations.add(crateLocation);
    }

    public void removeCrate(@NotNull Crate crate) {
        this.crates.remove(crate);
    }

    public boolean hasCrate(@NotNull Crate crate) {
        return this.crates.contains(crate);
    }

    public void addCrateLocation(@NotNull Location location, @NotNull Crate crate) {
        YamlConfiguration locations = Files.locations.getConfiguration();
        String id = "1";

        for (int i = 1; locations.contains("Locations." + i); ++i) {
            id = "" + (i + 1);
        }

        for (CrateLocation crateLocation : this.getCrateLocations()) {
            if (crateLocation.getLocation().equals(location)) {
                id = crateLocation.getID();
                break;
            }
        }

        locations.set("Locations." + id + ".Crate", crate.getFileName());
        locations.set("Locations." + id + ".World", location.getWorld().getName());
        locations.set("Locations." + id + ".X", location.getBlockX());
        locations.set("Locations." + id + ".Y", location.getBlockY());
        locations.set("Locations." + id + ".Z", location.getBlockZ());
        Files.locations.save();
        this.addLocation(new CrateLocation(id, crate, location));
        if (this.holograms != null) {
            this.holograms.createHologram(location, crate, id);
        }

    }

    public void removeCrateLocation(@NotNull String id) {
        Files.locations.getConfiguration().set("Locations." + id, (Object) null);
        Files.locations.save();
        CrateLocation location = null;

        for (CrateLocation crateLocation : this.getCrateLocations()) {
            if (crateLocation.getID().equalsIgnoreCase(id)) {
                location = crateLocation;
                break;
            }
        }

        if (location != null) {
            this.removeLocation(location);
            if (this.holograms != null && location.getCrate().getHologram().isEnabled()) {
                this.holograms.removeHologram(location.getID());
            }
        }

    }

    public final @NotNull List<Crate> getUsableCrates() {
        List<Crate> crateList = new ArrayList(this.crates);
        crateList.removeIf((crate) -> crate.getCrateType() == CrateType.menu);
        return crateList;
    }

    public final @NotNull List<Crate> getCrates() {
        return Collections.unmodifiableList(this.crates);
    }

    public final @Nullable Crate getCrateFromName(@Nullable String name) {
        if (name == null) {
            return null;
        } else if (name.isEmpty()) {
            return null;
        } else {
            Crate crate = null;

            for (Crate key : this.crates) {
                if (key.getFileName().equalsIgnoreCase(name)) {
                    crate = key;
                    break;
                }
            }

            return crate;
        }
    }

    public final boolean isCrateLocation(@NotNull Location location) {
        for (CrateLocation crateLocation : this.getCrateLocations()) {
            if (crateLocation.getLocation().equals(location)) {
                return true;
            }
        }

        return false;
    }

    public final boolean isKey(@NotNull ItemStack item) {
        return this.getCrateFromKey(item) != null;
    }

    public final @Nullable Crate getCrateFromKey(@NotNull ItemStack item) {
        if (item.getType() == Material.AIR) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String crateName = container.get(Keys.crate_key.getNamespacedKey(), PersistentDataType.STRING);
        return this.getCrateFromName(crateName);
    }

    public final @Nullable CrateLocation getCrateLocation(@NotNull Location location) {
        CrateLocation crateLocation = null;
        String asString = MiscUtils.location(location);

        for (CrateLocation key : this.crateLocations) {
            String locationAsString = MiscUtils.location(key.getLocation());
            if (locationAsString.equals(asString)) {
                crateLocation = key;
                break;
            }
        }

        return crateLocation;
    }

    public final @Nullable Crate getCrateFromLocation(@NotNull Location location) {
        Crate crate = null;
        String asString = MiscUtils.location(location);

        for (CrateLocation key : this.crateLocations) {
            String locationAsString = MiscUtils.location(key.getLocation());
            if (locationAsString.equals(asString)) {
                crate = key.getCrate();
                break;
            }
        }

        return crate;
    }

    public final @Nullable CrateSchematic getCrateSchematic(@NotNull String name) {
        if (name.isEmpty()) {
            return null;
        } else {
            for (CrateSchematic schematic : this.crateSchematics) {
                if (schematic.schematicName().equalsIgnoreCase(name)) {
                    return schematic;
                }
            }

            return null;
        }
    }

    public final boolean isDisplayReward(@NotNull Entity entity) {
        if (entity instanceof Item) {
            Item item = (Item) entity;
            ItemStack itemStack = item.getItemStack();
            if (itemStack.getType() == Material.AIR) {
                return false;
            }
            ItemMeta meta = itemStack.getItemMeta();
            if (meta == null) {
                return false;
            }
            return meta.getPersistentDataContainer().has(Keys.crate_prize.getNamespacedKey());
        } else {
            return false;
        }
    }

    public final boolean isKeyFromCrate(@Nullable ItemStack item, @Nullable Crate crate) {
        if (item != null && crate != null) {
            if (crate.getCrateType() == CrateType.menu) {
                return false;
            } else if (item.getType() == Material.AIR) {
                return false;
            } else {
                ItemMeta meta = item.getItemMeta();
                if (meta == null) {
                    return false;
                }
                PersistentDataContainer container = meta.getPersistentDataContainer();
                return !container.has(Keys.crate_key.getNamespacedKey()) ? false
                        : crate.getFileName()
                                .equals(container.get(Keys.crate_key.getNamespacedKey(), PersistentDataType.STRING));
            }
        } else {
            return false;
        }
    }

    public final @Nullable HologramManager getHolograms() {
        return this.holograms;
    }

    public final @NotNull List<CrateLocation> getCrateLocations() {
        return Collections.unmodifiableList(this.crateLocations);
    }

    public void removeLocation(@NotNull CrateLocation crateLocation) {
        this.crateLocations.remove(crateLocation);
    }

    public final @NotNull List<String> getBrokeCrates() {
        return Collections.unmodifiableList(this.brokeCrates);
    }

    public final @NotNull List<BrokeLocation> getBrokeLocations() {
        return Collections.unmodifiableList(this.brokeLocations);
    }

    public void removeBrokeLocation(@NotNull List<BrokeLocation> crateLocation) {
        this.brokeLocations.removeAll(crateLocation);
    }

    public final @NotNull List<CrateSchematic> getCrateSchematics() {
        return Collections.unmodifiableList(this.crateSchematics);
    }

    private ItemBuilder getKey(@NotNull FileConfiguration file) {
        String name = ColorUtils.color(file.getString("Crate.PhysicalKey.Name", ""));
        int customModelData = file.getInt("Crate.PhysicalKey.Custom-Model-Data", -1);
        List<String> lore = ColorUtils.color(file.getStringList("Crate.PhysicalKey.Lore"));
        boolean glowing = file.getBoolean("Crate.PhysicalKey.Glowing", true);
        boolean hideFlags = file.getBoolean("Crate.PhysicalKey.HideItemFlags", false);
        ItemBuilder itemBuilder = file.contains("Crate.PhysicalKey.Data")
                ? (new ItemBuilder()).fromBase64(file.getString("Crate.PhysicalKey.Data", ""))
                : (ItemBuilder) (new ItemBuilder())
                        .withType(file.getString("Crate.PhysicalKey.Item", "tripwire_hook").toLowerCase());
        return (ItemBuilder) ((ItemBuilder) ((ItemBuilder) ((ItemBuilder) ((ItemBuilder) itemBuilder
                .setDisplayName(name)).setDisplayLore(lore)).setGlowing(glowing)).setHidingItemFlags(hideFlags))
                .setCustomModelData(customModelData);
    }

    private void cleanDataFile() {
        YamlConfiguration data = Files.data.getConfiguration();
        if (data.contains("Players")) {
            if (MiscUtils.isLogging()) {
                this.plugin.getLogger().info("Cleaning up the data.yml file.");
            }

            List<String> removePlayers = new ArrayList();
            ConfigurationSection section = data.getConfigurationSection("Players");
            if (section != null) {
                for (String uuid : section.getKeys(false)) {
                    if (data.contains("Players." + uuid + ".tracking")) {
                        return;
                    }

                    boolean hasKeys = false;
                    List<String> noKeys = new ArrayList();

                    for (Crate crate : this.getUsableCrates()) {
                        String fileName = crate.getFileName();
                        if (data.getInt("Players." + uuid + "." + fileName) <= 0) {
                            noKeys.add(fileName);
                        } else {
                            hasKeys = true;
                        }
                    }

                    if (hasKeys) {
                        noKeys.forEach((cratex) -> data.set("Players." + uuid + "." + cratex, (Object) null));
                    } else {
                        removePlayers.add(uuid);
                    }
                }

                if (!removePlayers.isEmpty()) {
                    if (MiscUtils.isLogging()) {
                        this.plugin.getLogger().info("{} player's data has been marked to be removed.");
                    }

                    removePlayers.forEach((uuidx) -> data.set("Players." + uuidx, (Object) null));
                    if (MiscUtils.isLogging()) {
                        this.plugin.getLogger().info("All empty player data has been removed.");
                    }
                }

                if (MiscUtils.isLogging()) {
                    this.plugin.getLogger().info("The data.yml file has been cleaned.");
                }

                Files.data.save();
            }
        }
    }

    public void addPicker(@NotNull Player player, boolean value) {
        this.canPick.put(player.getUniqueId(), value);
    }

    public final boolean containsPicker(@NotNull Player player) {
        return this.canPick.containsKey(player.getUniqueId());
    }

    public boolean isPicker(@NotNull Player player) {
        return (Boolean) this.canPick.getOrDefault(player.getUniqueId(), false);
    }

    public void removePicker(@NotNull Player player) {
        this.canPick.remove(player.getUniqueId());
    }

    public void addCloser(@NotNull Player player, boolean value) {
        this.canClose.put(player.getUniqueId(), value);
    }

    public final boolean containsCloser(@NotNull Player player) {
        return this.canClose.containsKey(player.getUniqueId());
    }

    public void removeCloser(@NotNull Player player) {
        this.canClose.remove(player.getUniqueId());
    }

    public void addHands(@NotNull Player player, boolean checkHand) {
        this.checkHands.put(player.getUniqueId(), checkHand);
    }

    public void removeHands(@NotNull Player player) {
        this.checkHands.remove(player.getUniqueId());
    }

    public final boolean getHand(@NotNull Player player) {
        return (Boolean) this.checkHands.get(player.getUniqueId());
    }

    public void addReward(@NotNull Player player, @NotNull Entity entity) {
        this.allRewards.add(entity);
        this.rewards.put(player.getUniqueId(), entity);
    }

    public void endQuickCrate(@NotNull Player player, @NotNull Location location, @Nullable Crate crate,
            boolean useQuickCrateAgain) {
        if (this.hasCrateTask(player)) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask crateTask = this.getCrateTask(player);
            if (crateTask != null) {
                crateTask.cancel();
            }
            this.removeCrateTask(player);
        }

        UUID uuid = player.getUniqueId();
        if (this.rewards.get(uuid) != null) {
            this.allRewards.remove(this.rewards.get(uuid));
            ((Entity) this.rewards.get(uuid)).remove();
            this.rewards.remove(uuid);
        }

        ChestManager.closeChest(location.getBlock(), false);
        this.removeCrateInUse(player);
        this.removePlayerFromOpeningList(player);
        if (!useQuickCrateAgain && this.holograms != null && crate != null && crate.getHologram().isEnabled()) {
            CrateLocation crateLocation = this.getCrateLocation(location);
            if (crateLocation != null) {
                this.holograms.createHologram(location, crate, crateLocation.getID());
            }
        }

    }

    public void purgeRewards() {
        if (!this.allRewards.isEmpty()) {
            this.allRewards.stream().filter(Objects::nonNull).forEach(Entity::remove);
        }

    }

    public final Tier getTier(Crate crate, ItemStack item) {
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey key = Keys.crate_tier.getNamespacedKey();
        return !container.has(key) ? null : crate.getTier((String) container.get(key, PersistentDataType.STRING));
    }

    public void addSlot(Player player, final int rawSlot) {
        UUID uuid = player.getUniqueId();
        if (this.slots.containsKey(uuid)) {
            ArrayList<Integer> slots = this.slots.get(uuid);
            slots.add(rawSlot);
            this.slots.put(uuid, slots);
        } else {
            this.slots.put(uuid, new ArrayList<Integer>() {
                {
                    this.add(rawSlot);
                }
            });
        }
    }

    public final ArrayList<Integer> getSlots(Player player) {
        return (ArrayList) this.slots.get(player.getUniqueId());
    }

    public final boolean containsSlot(Player player) {
        return this.slots.containsKey(player.getUniqueId());
    }

    public void removeSlot(Player player) {
        this.slots.remove(player.getUniqueId());
    }
}
