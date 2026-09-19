package com.badbones69.crazycrates.api.objects;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.PrizeManager;
import com.badbones69.crazycrates.api.builders.ItemBuilder;
import com.badbones69.crazycrates.api.enums.Messages;
import com.badbones69.crazycrates.api.enums.misc.Keys;
import com.badbones69.crazycrates.api.mmo.CRMMOApi;
import com.badbones69.crazycrates.api.nexo.CRNexoApi;
import com.badbones69.crazycrates.common.config.ConfigManager;
import com.badbones69.crazycrates.common.config.impl.messages.CrateKeys;
import com.badbones69.crazycrates.utils.ItemUtils;
import com.badbones69.crazycrates.utils.ColorUtils;
import com.badbones69.crazycrates.utils.MiscUtils;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import libs.com.ryderbelserion.vital.common.utils.StringUtil;
import libs.com.ryderbelserion.vital.common.utils.math.MathUtil;
import libs.com.ryderbelserion.vital.paper.api.enums.Support;
import libs.com.ryderbelserion.vital.paper.util.ItemUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Prize {
    private final CrazyCrates plugin = CrazyCrates.getPlugin();
    private final ConfigurationSection section;
    private final List<ItemBuilder> builders;
    private final List<String> commands;
    private final List<String> messages;
    private final String sectionName;
    private final String prizeName;
    private List<String> permissions = new ArrayList<>();
    private ItemBuilder displayItem = new ItemBuilder();
    private String nexoDisplayId;
    private boolean nexoDisplayResolved;
    private boolean firework = false;
    private String crateName = "";
    private double weight = -1.0;
    private int maxPulls;
    private List<Tier> tiers = new ArrayList<>();
    private Prize alternativePrize;
    private boolean broadcastToggle = false;
    private List<String> broadcastMessages = new ArrayList<>();
    private String broadcastPermission = "";
    private List<ItemStack> editorItems = new ArrayList<>();

    public Prize(@NotNull ConfigurationSection section, List<ItemStack> editorItems, @NotNull List<Tier> tierPrizes,
            @NotNull String crateName, @Nullable Prize alternativePrize) {
        this.section = section;
        this.sectionName = section.getName();
        this.crateName = crateName;
        this.builders = ItemUtils.convertStringList(this.section.getStringList("Items"), this.sectionName);
        this.maxPulls = section.getInt("Settings.Max-Pulls", -1);
        this.tiers = tierPrizes;
        this.alternativePrize = alternativePrize;
        this.prizeName = ColorUtils.color(section.getString("DisplayName", ""));
        this.weight = section.getDouble("Weight", -1.0);
        this.firework = section.getBoolean("Firework", false);
        this.messages = section.getStringList("Messages");
        this.commands = section.getStringList("Commands");
        this.permissions = section.getStringList("BlackListed-Permissions");
        if (!this.permissions.isEmpty()) {
            this.permissions.replaceAll(String::toLowerCase);
        }
        this.broadcastToggle = section.getBoolean("Settings.Broadcast.Toggle", false);
        this.broadcastMessages = section.getStringList("Settings.Broadcast.Messages");
        this.broadcastPermission = section.getString("Settings.Broadcast.Permission", "");
        if (this.broadcastToggle && !this.broadcastPermission.isEmpty()) {
            MiscUtils.registerPermission(this.broadcastPermission,
                    "Hides the broadcast message for prize: " + this.prizeName + " if a player has this permission",
                    false);
        } else if (!this.broadcastToggle && !this.broadcastPermission.isEmpty()) {
            MiscUtils.unregisterPermission(this.broadcastPermission);
        }
        this.displayItem = this.display();
        this.editorItems = editorItems;
    }

    public Prize(@NotNull String prizeName, @NotNull String sectionName, @NotNull ConfigurationSection section) {
        this.prizeName = ColorUtils.color(prizeName);
        this.messages = section.getStringList("Messages");
        this.commands = section.getStringList("Commands");
        this.sectionName = sectionName;
        this.section = section;
        this.builders = ItemUtils.convertStringList(this.section.getStringList("Items"), this.sectionName);
    }

    @NotNull
    public final String getPrizeName() {
        return this.prizeName.isEmpty() ? "<lang:" + this.displayItem.getType().getItemTranslationKey() + ">"
                : this.prizeName;
    }

    @NotNull
    public final String getStrippedName() {
        return ColorUtils.plainText(this.getPrizeName());
    }

    @NotNull
    public final String getSectionName() {
        return this.sectionName;
    }

    @NotNull
    public final ItemStack getDisplayItem(Crate crate) {
        return this.getDisplayItem(null, crate);
    }

    @NotNull
    public final ItemStack getDisplayItem(@Nullable Player player, Crate crate) {
        // Nexo loads item definitions asynchronously. Retry the configured
        // Nexo display item when the first crate load happened too early.
        if (this.nexoDisplayId != null && !this.nexoDisplayResolved) {
            CRNexoApi nexoApi = this.plugin.getNexoApi();
            if (nexoApi != null && nexoApi.getItemStack(this.nexoDisplayId) != null) {
                this.displayItem = this.display();
            }
        }
        int pulls = PrizeManager.getCurrentPulls(this, crate);
        int maxPulls = this.getMaxPulls();
        String amount = String.valueOf(pulls);
        ArrayList<String> lore = new ArrayList<>();
        boolean isPapiEnabled = Support.placeholder_api.isEnabled();
        String displayName = this.displayItem.getDisplayName();
        this.displayItem.setDisplayName(ColorUtils.color(
                player != null && isPapiEnabled ? PlaceholderAPI.setPlaceholders(player, displayName) : displayName));
        if (this.section.contains("DisplayLore") && !this.section.contains("Lore")) {
            this.section.getStringList("DisplayLore").forEach(line -> lore
                    .add(ColorUtils.color(player != null && isPapiEnabled
                            ? PlaceholderAPI.setPlaceholders(player, line) : line)));
        }
        if (this.section.contains("Lore")) {
            if (MiscUtils.isLogging()) {
                Arrays.asList(
                        "Deprecated usage of Lore in your Prize " + this.sectionName + " in " + this.crateName
                                + ".yml, please change Lore to DisplayLore",
                        "Lore will be removed in the next major version of Minecraft in favor of DisplayLore",
                        "You can turn my nagging off in config.yml, verbose_logging: true -> false")
                        .forEach(msg -> this.plugin.getLogger().warning(msg));
            }
            this.section.getStringList("Lore").forEach(line -> lore
                    .add(ColorUtils.color(player != null && isPapiEnabled
                            ? PlaceholderAPI.setPlaceholders(player, line) : line)));
        }
        if (maxPulls != 0 && pulls != 0 && pulls >= maxPulls) {
            if (player != null) {
                String line = Messages.crate_prize_max_pulls.getMessage(player);
                if (!line.isEmpty()) {
                    String variable = line.replaceAll("\\{maxpulls}", String.valueOf(maxPulls)).replaceAll("\\{pulls}",
                            amount);
                    lore.add(ColorUtils.color(isPapiEnabled
                            ? PlaceholderAPI.setPlaceholders(player, variable) : variable));
                }
            } else {
                String line = ConfigManager.getMessages().getProperty(CrateKeys.crate_prize_max_pulls);
                if (!line.isEmpty()) {
                    lore.add(ColorUtils.color(line.replaceAll("\\{maxpulls}", String.valueOf(maxPulls))
                            .replaceAll("\\{pulls}", amount)));
                }
            }
        }
        this.displayItem.setDisplayLore(lore);
        if (player != null) {
            this.displayItem.setPlayer(player);
        }
        String weight = MathUtil.format(crate.getChance(this.getWeight()));
        ((ItemBuilder) ((ItemBuilder) this.displayItem.addLorePlaceholder("%chance%", weight))
                .addLorePlaceholder("%maxpulls%", String.valueOf(maxPulls))).addLorePlaceholder("%pulls%", amount);
        ((ItemBuilder) ((ItemBuilder) this.displayItem.addNamePlaceholder("%chance%", weight))
                .addNamePlaceholder("%maxpulls%", String.valueOf(maxPulls))).addNamePlaceholder("%pulls%", amount);
        return ((ItemBuilder) this.displayItem.setPersistentString(Keys.crate_prize.getNamespacedKey(),
                this.sectionName)).asItemStack();
    }

    public final RoundingMode mode() {
        return RoundingMode.HALF_EVEN;
    }

    @NotNull
    public final List<Tier> getTiers() {
        return this.tiers;
    }

    @NotNull
    public final List<String> getMessages() {
        return this.messages;
    }

    @NotNull
    public final List<String> getCommands() {
        return this.commands;
    }

    @NotNull
    public final List<ItemBuilder> getItemBuilders() {
        return this.builders;
    }

    @NotNull
    public final String getCrateName() {
        return this.crateName;
    }

    public final double getWeight() {
        return this.weight;
    }

    public final boolean useFireworks() {
        return this.firework;
    }

    @NotNull
    public final Prize getAlternativePrize() {
        return this.alternativePrize;
    }

    public final boolean hasAlternativePrize() {
        return this.alternativePrize == null;
    }

    public final boolean hasPermission(@NotNull Player player) {
        if (player.isOp()) {
            return false;
        }
        for (String permission : this.permissions) {
            if (!player.hasPermission(permission))
                continue;
            return true;
        }
        return false;
    }

    public void broadcast(Player target, Crate crate) {
        if (this.broadcastToggle) {
            String permission = this.broadcastPermission;
            Server server = this.plugin.getServer();
            String messages = StringUtil.chomp(StringUtil.convertList(this.broadcastMessages));
            if (permission.isEmpty()) {
                server.broadcast(ColorUtils.toComponent(this.getMessage(target, messages, crate)));
                return;
            }
            server.broadcast(ColorUtils.toComponent(this.getMessage(target, messages, crate)), permission);
        } else if (crate.isBroadcastToggle()) {
            String permission = crate.getBroadcastPermission();
            Server server = this.plugin.getServer();
            String messages = StringUtil.chomp(StringUtil.convertList(crate.getBroadcastMessages()));
            if (permission.isEmpty()) {
                server.broadcast(ColorUtils.toComponent(this.getMessage(target, messages, crate)));
                return;
            }
            server.broadcast(ColorUtils.toComponent(this.getMessage(target, messages, crate)), permission);
        }
    }

    @NotNull
    private String getMessage(final Player target, String message, final Crate crate) {
        final String maxPulls = String.valueOf(this.getMaxPulls());
        final String pulls = String.valueOf(PrizeManager.getCurrentPulls(this, crate));
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%reward%",
                Prize.this.getPrizeName().replaceAll("%maxpulls%", maxPulls).replaceAll("%pulls%", pulls));
        placeholders.put("%reward_stripped%", Prize.this.getStrippedName());
        placeholders.put("%crate%", crate.getCrateName());
        placeholders.put("%player%", target.getName());
        placeholders.put("%maxpulls%", maxPulls);
        placeholders.put("%pulls%", pulls);

        String parsed = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            parsed = parsed.replace(entry.getKey(), entry.getValue());
        }
        return parsed;
    }

    @NotNull
    private ItemBuilder display() {
        ItemBuilder builder = new ItemBuilder();
        try {
            Object type;

            String configuredNexoId = this.section.getString("Nexo.Id",
                    this.section.getString("Nexo.Item", ""));
            if (configuredNexoId != null && !configuredNexoId.isBlank()) {
                this.nexoDisplayId = configuredNexoId.trim();
                CRNexoApi nexoApi = this.plugin.getNexoApi();
                if (nexoApi != null) {
                    ItemStack nexoItem = nexoApi.getItemStack(this.nexoDisplayId);
                    if (nexoItem != null) {
                        builder = new ItemBuilder(nexoItem);
                        this.nexoDisplayResolved = true;
                    }
                }
            }

            // If configured, use an MMOItems template as the base display item.
            if (!this.nexoDisplayResolved && this.section.contains("MMOItems.Type")
                    && this.section.contains("MMOItems.Id")) {
                CRMMOApi api = this.plugin.getMmoApi();
                if (api != null) {
                    String mmoType = this.section.getString("MMOItems.Type", "");
                    String mmoId = this.section.getString("MMOItems.Id", "");
                    if (mmoType != null && !mmoType.isEmpty() && mmoId != null && !mmoId.isEmpty()) {
                        ItemStack mmoItem = api.getItemStack(mmoType, mmoId);
                        if (mmoItem != null) {
                            builder = new ItemBuilder(mmoItem);
                        }
                    }
                }
            }

            if (this.section.contains("DisplayData")) {
                builder = builder.fromBase64(this.section.getString("DisplayData", ""));
            }
            if (this.section.contains("DisplayName")) {
                builder.setDisplayName(this.prizeName);
            }
            if (this.section.contains("DisplayItem")) {
                builder.withType(this.section.getString("DisplayItem", "red_terracotta").toLowerCase());
            }
            if (this.section.contains("DisplayAmount")) {
                builder.setAmount(this.section.getInt("DisplayAmount", 1));
            }
            if (this.section.contains("DisplayLore") && !this.section.contains("Lore")) {
                builder.setDisplayLore(ColorUtils.color(this.section.getStringList("DisplayLore")));
            }
            if (this.section.contains("Lore")) {
                if (MiscUtils.isLogging()) {
                    Arrays.asList(
                            "Deprecated usage of Lore in your Prize " + this.sectionName + " in " + this.crateName
                                    + ".yml, please change Lore to DisplayLore",
                            "Lore will be removed in the next major version of Minecraft in favor of DisplayLore",
                            "You can turn my nagging off in config.yml, verbose_logging: true -> false")
                            .forEach(msg -> this.plugin.getLogger().warning(msg));
                }
                builder.setDisplayLore(ColorUtils.color(this.section.getStringList("Lore")));
            }
            builder.setGlowing(this.section.contains("Glowing") ? this.section.getBoolean("Glowing") : null);
            builder.setDamage(this.section.getInt("DisplayDamage", 0));
            if (this.section.contains("Patterns")) {
                if (MiscUtils.isLogging()) {
                    Arrays.asList(
                            "Deprecated usage of Patterns in your Prize " + this.sectionName + " in " + this.crateName
                                    + ".yml, please change Patterns to DisplayPatterns",
                            "Patterns will be removed in the next major version of Minecraft in favor of DisplayPatterns",
                            "You can turn my nagging off in config.yml, verbose_logging: true -> false")
                            .forEach(msg -> this.plugin.getLogger().warning(msg));
                }
                for (String pattern : this.section.getStringList("Patterns")) {
                    builder.addPattern(pattern.toLowerCase());
                }
            }
            if (this.section.contains("DisplayPatterns")) {
                for (String pattern : this.section.getStringList("DisplayPatterns")) {
                    builder.addPattern(pattern.toLowerCase());
                }
            }
            builder.setItemFlags(this.section.getStringList("Flags"));
            builder.setHidingItemFlags(this.section.getBoolean("HideItemFlags", false));
            builder.setUnbreakable(this.section.getBoolean("Unbreakable", false));
            builder.setCustomModelData(this.section.getInt("Settings.Custom-Model-Data", -1));
            if (this.section.contains("Settings.Mob-Type")
                    && (type = ItemUtil.getEntity(this.section.getString("Settings.Mob-Type", "cow"))) != null) {
                builder.setEntityType((EntityType) type);
            }
            if (this.section.contains("Skull") && this.plugin.getApi() != null) {
                builder.setSkull(this.section.getString("Skull", ""), this.plugin.getApi());
            }
            if (this.section.contains("Player")) {
                builder.setPlayer(this.section.getString("Player", ""));
            }
            if (this.section.contains("DisplayTrim.Pattern") && builder.isArmor()) {
                builder.applyTrimPattern(this.section.getString("DisplayTrim.Pattern", "sentry"));
            }
            if (this.section.contains("DisplayTrim.Material") && builder.isArmor()) {
                builder.applyTrimMaterial(this.section.getString("DisplayTrim.Material", "quartz"));
            }
            if (this.section.contains("DisplayEnchantments")) {
                for (String ench : this.section.getStringList("DisplayEnchantments")) {
                    String[] value = ench.split(":");
                    builder.addEnchantment(value[0], Integer.parseInt(value[1]), true);
                }
            }
            return builder;
        } catch (Exception exception) {
            List<String> lore = new ArrayList<>();
            lore.add("&cThere was an error with one of your prizes!");
            lore.add("&cThe reward in question is labeled: &e" + Prize.this.section.getName() + " &cin crate: &e"
                    + Prize.this.crateName);
            lore.add("&cName of the reward is " + Prize.this.section.getString("DisplayName"));
            lore.add("&cIf you are confused, Stop by our discord for support!");

            ItemBuilder error = new ItemBuilder(Material.RED_TERRACOTTA).setDisplayName("&l&cERROR");
            error.setDisplayLore(lore);
            return error;
        }
    }

    public final List<ItemStack> getEditorItems() {
        return this.editorItems;
    }

    public final int getMaxPulls() {
        return this.maxPulls == -1 ? 0 : this.maxPulls;
    }
}
