package com.badbones69.crazycrates.listeners.crates.types;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.PrizeManager;
import com.badbones69.crazycrates.api.builders.ItemBuilder;
import com.badbones69.crazycrates.api.enums.Messages;
import com.badbones69.crazycrates.api.enums.misc.Keys;
import com.badbones69.crazycrates.api.events.PlayerPrizeEvent;
import com.badbones69.crazycrates.api.events.PlayerReceiveKeyEvent;
import com.badbones69.crazycrates.api.events.PlayerReceiveKeyEvent.KeyReceiveReason;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import com.badbones69.crazycrates.api.objects.Tier;
import com.badbones69.crazycrates.common.config.ConfigManager;
import com.badbones69.crazycrates.common.config.impl.ConfigKeys;
import com.badbones69.crazycrates.common.utils.Methods;
import com.badbones69.crazycrates.managers.BukkitUserManager;
import com.badbones69.crazycrates.managers.events.EventManager;
import com.badbones69.crazycrates.managers.events.enums.EventType;
import com.badbones69.crazycrates.tasks.crates.CrateManager;
import com.badbones69.crazycrates.tasks.crates.other.CosmicCrateManager;
import com.badbones69.crazycrates.tasks.menus.CratePrizeMenu;
import com.badbones69.crazycrates.utils.MiscUtils;
import com.badbones69.crazycrates.utils.SoundUtils;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import libs.com.ryderbelserion.vital.paper.api.enums.Support;
import me.clip.placeholderapi.PlaceholderAPI;
import com.badbones69.crazycrates.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import us.crazycrew.crazycrates.api.enums.types.CrateType;
import us.crazycrew.crazycrates.api.enums.types.KeyType;

public class CosmicCrateListener implements Listener {
    private final CrazyCrates plugin = CrazyCrates.getPlugin();
    private final CrateManager crateManager;
    private final BukkitUserManager userManager;

    public CosmicCrateListener() {
        this.crateManager = this.plugin.getCrateManager();
        this.userManager = this.plugin.getUserManager();
    }

    @EventHandler
    public void onPrizeReceive(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder var4 = inventory.getHolder();
        if (var4 instanceof CratePrizeMenu) {
            CratePrizeMenu holder = (CratePrizeMenu) var4;
            Player var13 = holder.getPlayer();
            event.setCancelled(true);
            Crate crate = this.crateManager.getOpeningCrate(var13);
            if (crate != null) {
                if (this.crateManager.isInOpeningList(var13) && crate.getCrateType() == CrateType.cosmic) {
                    if (holder.contains(" - Prizes")) {
                        int slot = event.getRawSlot();
                        if (this.crateManager.containsSlot(var13) && this.crateManager.getSlots(var13).contains(slot)) {
                            Messages.already_redeemed_prize.sendMessage(var13);
                        } else {
                            InventoryView view = event.getView();
                            Inventory topInventory = view.getTopInventory();
                            if (event.getClickedInventory() == topInventory) {
                                ItemStack itemStack = topInventory.getItem(slot);
                                if (itemStack != null && itemStack.getType() != Material.AIR) {
                                    Tier tier = this.crateManager.getTier(var13, slot);
                                    if (tier != null) {
                                        Prize prize = crate.pickPrize(var13, tier);

                                        for (int stop = 0; prize == null && stop <= 2000; ++stop) {
                                            prize = crate.pickPrize(var13, tier);
                                        }

                                        if (prize != null) {
                                            PrizeManager.givePrize(var13, prize, crate);
                                            this.plugin.getServer().getPluginManager()
                                                    .callEvent(new PlayerPrizeEvent(var13, crate, prize));
                                            event.setCurrentItem(prize.getDisplayItem(var13, crate));
                                            SoundUtils.playSound(crate, var13.getLocation(), "click-sound",
                                                    "ui.button.click");
                                            // holder.getCrate().playSound(var13, var13.getLocation(), "click-sound",
                                            // "ui.button.click", Source.PLAYER);
                                            if (prize.useFireworks()) {
                                                MiscUtils.spawnFirework(var13.getLocation().add((double) 0.0F,
                                                        (double) 1.0F, (double) 0.0F), (Color) null);
                                            }

                                            this.crateManager.addSlot(var13, slot);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMysteryBoxClick(final InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        InventoryHolder var4 = inventory.getHolder();
        if (var4 instanceof CratePrizeMenu) {
            CratePrizeMenu holder = (CratePrizeMenu) var4;
            final Player player = holder.getPlayer();
            final UUID uuid = player.getUniqueId();
            event.setCancelled(true);
            final Crate crate = this.crateManager.getOpeningCrate(player);
            if (crate != null) {
                if (this.crateManager.isInOpeningList(player) && crate.getCrateType() == CrateType.cosmic) {
                    if (holder.contains(" - Choose")) {
                        int slot = event.getRawSlot();
                        final InventoryView view = event.getView();
                        Inventory topInventory = view.getTopInventory();
                        if (event.getClickedInventory() == topInventory) {
                            ItemStack itemStack = topInventory.getItem(slot);
                            if (itemStack != null && itemStack.getType() != Material.AIR) {
                                final CosmicCrateManager cosmicCrateManager = (CosmicCrateManager) crate.getManager();
                                int totalPrizes = cosmicCrateManager.getTotalPrizes();
                                int pickedSlot = slot + 1;
                                ItemMeta itemMeta = itemStack.getItemMeta();
                                PersistentDataContainer container = itemMeta.getPersistentDataContainer();
                                if (container.has(Keys.cosmic_mystery_crate.getNamespacedKey())) {
                                    int size = cosmicCrateManager.getPrizes(player).size();
                                    if (size < totalPrizes) {
                                        Tier tier = this.crateManager.getTier(player, slot);
                                        if (tier == null) {
                                            return;
                                        }

                                        String tierName = tier.getName();
                                        ItemBuilder builder = (ItemBuilder) ((ItemBuilder) ((ItemBuilder) cosmicCrateManager
                                                .getPickedCrate().setPlayer(player))
                                                .addNamePlaceholder("%Slot%", String.valueOf(pickedSlot)))
                                                .addLorePlaceholder("%Slot%", String.valueOf(pickedSlot));
                                        builder.setAmount(pickedSlot);
                                        cosmicCrateManager.setTier(builder, tierName);
                                        event.setCurrentItem(builder.asItemStack());
                                        cosmicCrateManager.addPickedPrize(player, slot, tier);
                                        SoundUtils.playSound(crate, player.getLocation(), "click-sound",
                                                "ui.button.click");
                                        // crate.playSound(player, player.getLocation(), "click-sound",
                                        // "ui.button.click", Source.PLAYER);
                                    }
                                } else if (container.has(Keys.cosmic_picked_crate.getNamespacedKey())) {
                                    Tier tier = this.crateManager.getTier(player, slot);
                                    ItemBuilder builder = (ItemBuilder) ((ItemBuilder) ((ItemBuilder) cosmicCrateManager
                                            .getMysteryCrate().setPlayer(player))
                                            .addNamePlaceholder("%Slot%", String.valueOf(pickedSlot)))
                                            .addLorePlaceholder("%Slot%", String.valueOf(pickedSlot));
                                    builder.setAmount(pickedSlot);
                                    String tierName = tier.getName();
                                    cosmicCrateManager.setTier(builder, tierName);
                                    event.setCurrentItem(builder.asItemStack());
                                    cosmicCrateManager.removePickedPrize(player, slot);
                                    SoundUtils.playSound(crate, player.getLocation(), "click-sound", "ui.button.click");
                                    // crate.playSound(player, player.getLocation(), "click-sound",
                                    // "ui.button.click", Source.PLAYER);
                                }

                                final String fileName = crate.getFileName();
                                final String fancyName = crate.getCrateName();
                                int size = cosmicCrateManager.getPrizes(player).size();
                                if (size >= totalPrizes) {
                                    KeyType playerType = this.crateManager.getPlayerKeyType(player);
                                    final KeyType type = playerType == null ? KeyType.virtual_key : playerType;
                                    // Fix: Take keys IMMEDIATELY before starting animation to prevent "quit to save key" exploit
                                    int keyAmount = crate.useRequiredKeys() ? crate.getRequiredKeys() : 1;
                                    int totalKeys = type == KeyType.physical_key
                                            ? this.userManager.getPhysicalKeys(uuid, fileName)
                                            : this.userManager.getVirtualKeys(uuid, fileName);

                                    if (type == KeyType.physical_key && !this.userManager.hasPhysicalKey(uuid, fileName,
                                            this.crateManager.getHand(player))) {
                                        Map<String, String> placeholders = new HashMap();
                                        placeholders.put("{crate}", fancyName);
                                        placeholders.put("{key}", crate.getKeyName());
                                        Messages.no_keys.sendMessage(player, placeholders);
                                        this.crateManager.removePlayerFromOpeningList(player);
                                        this.crateManager.removePlayerKeyType(player);
                                        this.crateManager.removeTier(player);
                                        this.crateManager.removeHands(player);
                                        cosmicCrateManager.removePickedPlayer(player);
                                        player.closeInventory();
                                        return;
                                    }

                                    if (totalKeys < keyAmount) {
                                        MiscUtils.failedToTakeKey(player, fileName);
                                        this.crateManager.removePlayerFromOpeningList(player);
                                        this.crateManager.removePlayerKeyType(player);
                                        this.crateManager.removeTier(player);
                                        this.crateManager.removeHands(player);
                                        cosmicCrateManager.removePickedPlayer(player);
                                        player.closeInventory();
                                        return;
                                    }

                                    boolean keyTaken = this.userManager.takeKeys(uuid, fileName, type, keyAmount, this.crateManager.getHand(player));
                                    if (!keyTaken) {
                                        Messages.no_keys.sendMessage(player, Map.of("{crate}", fancyName, "{key}", crate.getKeyName()));
                                        this.crateManager.removePlayerFromOpeningList(player);
                                        this.crateManager.removePlayerKeyType(player);
                                        this.crateManager.removeTier(player);
                                        this.crateManager.removeHands(player);
                                        cosmicCrateManager.removePickedPlayer(player);
                                        player.closeInventory();
                                        return;
                                    }

                                    // Add points to TurtleTop immediately as well
                                    com.badbones69.crazycrates.support.TurtleTopSupport.addPoint(player.getName(), "crazy-crates-" + fileName, keyAmount);

                                    // Đảm bảo keyType được lưu (dù đã trừ rồi nhưng các task khác có thể cần)
                                    if (!this.crateManager.hasPlayerKeyType(player)) {
                                        this.crateManager.addPlayerKeyType(player, type);
                                    }

                                    String shufflingName = fancyName + " - Shuffling";
                                    holder.title(shufflingName);
                                    holder.sendTitleChange();
                                    view.getTopInventory().clear();
                                    YamlConfiguration configuration = crate.getFile();
                                    String broadcastMessage = configuration.getString("Crate.BroadCast", "");
                                    boolean broadcastToggle = configuration.getBoolean("Crate.OpeningBroadCast", false);
                                    if (broadcastToggle && !broadcastMessage.trim().isEmpty()) {
                                        String builder = Support.placeholder_api.isEnabled()
                                                ? PlaceholderAPI.setPlaceholders(player, broadcastMessage)
                                                : broadcastMessage;
                                        this.plugin.getServer()
                                                .broadcast(ColorUtils.toComponent(
                                                         builder.replaceAll("%crate%", fancyName)
                                                                 .replaceAll("%prefix%", Methods.getPrefix())
                                                                 .replaceAll("%player%", player.getName())));
                                    }

                                    EventManager.logEvent(EventType.event_crate_opened, player, player, crate, type, 1);
                                    this.crateManager.addRepeatingCrateTask(player, new TimerTask() {
                                        int time = 0;

                                        public void run() {
                                            try {
                                                CosmicCrateListener.this.startRollingAnimation(player, view, holder);
                                            } catch (Exception exception) {
                                                Bukkit.getScheduler().runTask(CosmicCrateListener.this.plugin,
                                                        (scheduledTask) -> {
                                                            // On failure, we don't refund because it's hard to verify state, 
                                                            // but we clean up.
                                                            CosmicCrateListener.this.crateManager
                                                                    .removePlayerFromOpeningList(player);
                                                            CosmicCrateListener.this.crateManager
                                                                    .removePlayerKeyType(player);
                                                            CosmicCrateListener.this.crateManager
                                                                    .removeTier(player);
                                                            CosmicCrateListener.this.crateManager
                                                                    .removeCrateTask(player);
                                                            CosmicCrateListener.this.crateManager
                                                                    .removeHands(player);
                                                            cosmicCrateManager.removePickedPlayer(player);
                                                            if (MiscUtils.isLogging()) {
                                                                CosmicCrateListener.this.plugin.getLogger().log(
                                                                        Level.SEVERE,
                                                                        "An issue occurred when the user "
                                                                                + player.getName()
                                                                                + " was using the " + fileName
                                                                                + " crate.",
                                                                        exception);
                                                            }

                                                            SoundUtils.playSound(crate, player.getLocation(),
                                                                    "click-sound", "block.anvil.place");
                                                        });
                                                this.cancel();
                                                return;
                                            }

                                            ++this.time;
                                            if (this.time == 40) {
                                                CosmicCrateListener.this.crateManager.removeCrateTask(player);
                                                CosmicCrateListener.this.showRewards(player, view, holder,
                                                        cosmicCrateManager);
                                                SoundUtils.playSound(crate, player.getLocation(), "stop-sound",
                                                        "block.anvil.place");
                                                this.cancel();
                                            }

                                        }
                                    }, 0L, 80L);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void startRollingAnimation(Player player, InventoryView view, CratePrizeMenu cosmic) {
        Crate crate = cosmic.getCrate();

        for (int slot = 0; slot < cosmic.getSize(); ++slot) {
            Tier tier = PrizeManager.getTier(crate);
            if (tier != null) {
                view.getTopInventory().setItem(slot, tier.getTierItem(player, crate));
            }
        }

        SoundUtils.playSound(crate, player.getLocation(), "cycle-sound", "block.note_block.xylophone");
        // crate.playSound(player, player.getLocation(), "cycle-sound",
        // "block.note_block.xylophone", Source.PLAYER);
        player.updateInventory();
    }

    private void showRewards(final Player player, InventoryView view, CratePrizeMenu cosmic,
            CosmicCrateManager cosmicCrateManager) {
        Crate crate = cosmic.getCrate();
        String rewardsName = crate.getCrateName() + " - Prizes";
        cosmic.title(rewardsName);
        cosmic.sendTitleChange();
        view.getTopInventory().clear();
        cosmicCrateManager.getPrizes(player).forEach((slot, tier) -> {
            Inventory inventory = view.getTopInventory();
            inventory.setItem(slot, tier.getTierItem(player, crate));
        });
        player.updateInventory();
        if (ConfigManager.getConfig().getProperty(ConfigKeys.cosmic_crate_timeout)) {
            this.crateManager.addCrateTask(player, new TimerTask() {
                public void run() {
                    Bukkit.getScheduler().runTask(CosmicCrateListener.this.plugin, () -> player.closeInventory());
                    if (MiscUtils.isLogging()) {
                        List<String> var10000 = Arrays.asList(
                                player.getName()
                                        + " spent 10 seconds staring at a gui instead of collecting their prizes",
                                "The task has been cancelled, They have been given their prizes and the gui is closed.");
                        Logger var10001 = CosmicCrateListener.this.plugin.getLogger();
                        Objects.requireNonNull(var10001);
                        var10000.forEach(v -> {
                            var10001.info(v);
                        });
                    }

                }
            }, 10000L);
        }

    }
}
