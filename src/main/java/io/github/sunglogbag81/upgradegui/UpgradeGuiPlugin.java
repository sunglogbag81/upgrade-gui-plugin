package io.github.sunglogbag81.upgradegui;

import io.github.sunglogbag81.upgradegui.command.UpgradeCommand;
import io.github.sunglogbag81.upgradegui.config.UpgradeConfig;
import io.github.sunglogbag81.upgradegui.gui.UpgradeMenuHolder;
import io.github.sunglogbag81.upgradegui.listener.CitizensUpgradeListener;
import io.github.sunglogbag81.upgradegui.listener.UpgradeGuiListener;
import io.github.sunglogbag81.upgradegui.model.LevelRule;
import io.github.sunglogbag81.upgradegui.model.UpgradeTicketDefinition;
import io.github.sunglogbag81.upgradegui.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class UpgradeGuiPlugin extends JavaPlugin {
    private UpgradeConfig upgradeConfig;
    private NamespacedKey ticketKey;
    private NamespacedKey levelKey;
    private NamespacedKey baseNameKey;
    private NamespacedKey baseLoreKey;
    private final Set<UUID> processingPlayers = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ticketKey = new NamespacedKey(this, "upgrade-ticket-key");
        levelKey = new NamespacedKey(this, "upgrade-level");
        baseNameKey = new NamespacedKey(this, "upgrade-base-name");
        baseLoreKey = new NamespacedKey(this, "upgrade-base-lore");
        upgradeConfig = new UpgradeConfig(this);
        upgradeConfig.reload();

        PluginCommand command = getCommand("강화");
        if (command != null) {
            UpgradeCommand upgradeCommand = new UpgradeCommand(this, upgradeConfig);
            command.setExecutor(upgradeCommand);
            command.setTabCompleter(upgradeCommand);
        }

        getServer().getPluginManager().registerEvents(new UpgradeGuiListener(this, upgradeConfig), this);
        if (Bukkit.getPluginManager().getPlugin("Citizens") != null) {
            getServer().getPluginManager().registerEvents(new CitizensUpgradeListener(this, upgradeConfig), this);
        }
    }

    public void reloadPlugin() {
        upgradeConfig.reload();
    }

    public boolean isProcessing(Player player) {
        return player != null && processingPlayers.contains(player.getUniqueId());
    }

    public String getTicketKey(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir() || !itemStack.hasItemMeta()) {
            return null;
        }
        return itemStack.getItemMeta().getPersistentDataContainer().get(ticketKey, PersistentDataType.STRING);
    }

    public int getUpgradeLevel(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir() || !itemStack.hasItemMeta()) {
            return 0;
        }
        Integer value = itemStack.getItemMeta().getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        return value == null ? 0 : Math.max(0, value);
    }

    public ItemStack createTicketItem(UpgradeTicketDefinition definition) {
        ItemStack itemStack = new ItemStack(definition.material());
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }
        meta.setDisplayName(definition.displayName());
        meta.setLore(definition.lore());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        if (definition.customModelData() != null) {
            meta.setCustomModelData(definition.customModelData());
        }
        if (definition.glowing()) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
        }
        meta.getPersistentDataContainer().set(ticketKey, PersistentDataType.STRING, definition.key());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public Inventory createMenu() {
        UpgradeMenuHolder holder = new UpgradeMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, upgradeConfig.getGuiSize(), upgradeConfig.getGuiTitle());
        holder.setInventory(inventory);

        ItemStack filler = new ItemStack(upgradeConfig.getFillerMaterial());
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(upgradeConfig.getFillerName());
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        setIfValid(inventory, upgradeConfig.getItemSlot(), null);
        setIfValid(inventory, upgradeConfig.getTicketSlot(), null);
        setIfValid(inventory, upgradeConfig.getPreviewSlot(), guide(Material.NETHER_STAR, upgradeConfig.getPreviewName(), upgradeConfig.renderPreviewLore(Map.of("%level%", "0", "%next_level%", "1", "%delay_ticks%", String.valueOf(upgradeConfig.getAttemptDelayTicks())))));
        setIfValid(inventory, upgradeConfig.getApplySlot(), guide(Material.EMERALD, upgradeConfig.getApplyName(), upgradeConfig.renderApplyLore(Map.of("%delay_ticks%", String.valueOf(upgradeConfig.getAttemptDelayTicks())))));
        return inventory;
    }

    public void openMenu(Player player) {
        player.openInventory(createMenu());
        upgradeConfig.sendMessage(player, "opened");
        playSound(player, upgradeConfig.getOpenSound());
    }

    public void attemptUpgrade(Player player, Inventory inventory) {
        if (isProcessing(player)) {
            upgradeConfig.sendMessage(player, "already-processing");
            return;
        }

        ItemStack item = inventory.getItem(upgradeConfig.getItemSlot());
        ItemStack ticket = inventory.getItem(upgradeConfig.getTicketSlot());
        if (item == null || item.getType().isAir()) {
            upgradeConfig.sendMessage(player, "invalid-item");
            return;
        }
        if (!upgradeConfig.isMaterialAllowed(item.getType())) {
            upgradeConfig.sendMessage(player, "invalid-material");
            return;
        }
        if (upgradeConfig.isAllowStackSizeOneOnly() && item.getAmount() != 1) {
            upgradeConfig.sendMessage(player, "item-stack-not-supported");
            return;
        }
        String ticketId = getTicketKey(ticket);
        UpgradeTicketDefinition definition = upgradeConfig.getTicket(ticketId);
        if (definition == null || !definition.enabled()) {
            upgradeConfig.sendMessage(player, "invalid-ticket");
            return;
        }
        int currentLevel = getUpgradeLevel(item);
        if (currentLevel >= upgradeConfig.getMaxLevel()) {
            upgradeConfig.sendMessage(player, "already-max-level");
            return;
        }

        ItemStack processingItem = item.clone();
        inventory.setItem(upgradeConfig.getItemSlot(), null);
        consumeOne(inventory, upgradeConfig.getTicketSlot());
        inventory.setItem(upgradeConfig.getPreviewSlot(), guide(Material.CLOCK, upgradeConfig.getProcessingName(), upgradeConfig.renderProcessingLore(Map.of(
                "%level%", String.valueOf(currentLevel),
                "%next_level%", String.valueOf(Math.min(upgradeConfig.getMaxLevel(), currentLevel + 1)),
                "%delay_ticks%", String.valueOf(upgradeConfig.getAttemptDelayTicks())
        ))));

        processingPlayers.add(player.getUniqueId());
        upgradeConfig.sendMessage(player, "processing-started", Map.of("%delay_ticks%", String.valueOf(upgradeConfig.getAttemptDelayTicks())));
        playSound(player, upgradeConfig.getStartSound());

        Bukkit.getScheduler().runTaskLater(this, () -> finishUpgrade(player.getUniqueId(), inventory, processingItem, currentLevel), upgradeConfig.getAttemptDelayTicks());
    }

    private void finishUpgrade(UUID playerId, Inventory inventory, ItemStack item, int currentLevel) {
        processingPlayers.remove(playerId);
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }

        LevelRule rule = upgradeConfig.getRule(currentLevel);
        double success = rule.successChance();
        double downgrade = rule.downgradeChance();
        double destroy = rule.destroyChance();
        double total = success + downgrade + destroy;
        if (total <= 0.0D) {
            success = 100.0D;
            total = 100.0D;
        }

        double roll = ThreadLocalRandom.current().nextDouble(total);
        int nextLevel;
        String messageKey;
        String previewName;
        String soundKey;
        if (roll < success) {
            nextLevel = Math.min(upgradeConfig.getMaxLevel(), currentLevel + 1);
            messageKey = "success";
            previewName = upgradeConfig.getSuccessPreviewName();
            soundKey = upgradeConfig.getSuccessSound();
        } else if (roll < success + downgrade) {
            nextLevel = Math.max(0, currentLevel - 1);
            messageKey = "downgrade";
            previewName = upgradeConfig.getDowngradePreviewName();
            soundKey = upgradeConfig.getDowngradeSound();
        } else {
            nextLevel = 0;
            messageKey = "destroyed";
            previewName = upgradeConfig.getDestroyPreviewName();
            soundKey = upgradeConfig.getDestroySound();
        }

        applyUpgradeMetadata(item, nextLevel);
        ItemStack result = item.clone();

        boolean deliveredToGui = false;
        if (player.getOpenInventory().getTopInventory().equals(inventory) && inventory.getHolder() instanceof UpgradeMenuHolder) {
            inventory.setItem(upgradeConfig.getItemSlot(), result);
            inventory.setItem(upgradeConfig.getPreviewSlot(), guide(Material.ENCHANTED_BOOK, previewName, upgradeConfig.renderResultPreviewLore(Map.of(
                    "%level%", String.valueOf(currentLevel),
                    "%next_level%", String.valueOf(nextLevel),
                    "%success%", String.valueOf(success),
                    "%downgrade%", String.valueOf(downgrade),
                    "%destroy%", String.valueOf(destroy)
            ))));
            deliveredToGui = true;
        } else if (upgradeConfig.isDeliverResultToInventoryIfClosed()) {
            player.getInventory().addItem(result).values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            upgradeConfig.sendMessage(player, "result-delivered");
        }

        upgradeConfig.sendMessage(player, messageKey, Map.of("%level%", String.valueOf(nextLevel)));
        playSound(player, soundKey);
        if (!deliveredToGui && !upgradeConfig.isDeliverResultToInventoryIfClosed()) {
            player.getInventory().addItem(result).values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
        if ("success".equals(messageKey) && !rule.successCommands().isEmpty()) {
            for (String command : rule.successCommands()) {
                String prepared = command
                        .replace("%player%", player.getName())
                        .replace("%level%", String.valueOf(currentLevel))
                        .replace("%next_level%", String.valueOf(nextLevel))
                        .replace("%item_name%", getDisplayName(result));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), prepared);
            }
        }
    }

    public void applyUpgradeMetadata(ItemStack itemStack, int level) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        var pdc = meta.getPersistentDataContainer();
        String materialName = prettifyMaterial(itemStack.getType());
        if (!pdc.has(baseNameKey, PersistentDataType.STRING)) {
            pdc.set(baseNameKey, PersistentDataType.STRING, meta.hasDisplayName() ? meta.getDisplayName() : materialName);
        }
        if (!pdc.has(baseLoreKey, PersistentDataType.STRING)) {
            List<String> lore = meta.hasLore() && meta.getLore() != null ? meta.getLore() : List.of();
            pdc.set(baseLoreKey, PersistentDataType.STRING, String.join("\n", lore));
        }

        String baseName = pdc.getOrDefault(baseNameKey, PersistentDataType.STRING, materialName);
        meta.setDisplayName(upgradeConfig.getLevelFormat()
                .replace("%level%", String.valueOf(level))
                .replace("%name%", baseName));

        List<String> lore = new ArrayList<>();
        if (upgradeConfig.isPreserveExistingLore()) {
            String storedLore = pdc.getOrDefault(baseLoreKey, PersistentDataType.STRING, "");
            if (!storedLore.isBlank()) {
                lore.addAll(List.of(storedLore.split("\n")));
            }
        }
        lore.removeIf(line -> ColorUtil.colorize(line).equals(ColorUtil.colorize(upgradeConfig.getLoreLine().replace("%level%", String.valueOf(level)))) || line.contains("강화 단계:"));
        lore.add(upgradeConfig.getLoreLine().replace("%level%", String.valueOf(level)));
        meta.setLore(lore);
        pdc.set(levelKey, PersistentDataType.INTEGER, level);
        Integer cmd = upgradeConfig.getCustomModelData(level);
        if (cmd != null) {
            meta.setCustomModelData(cmd);
        }
        itemStack.setItemMeta(meta);
    }

    public void playGiveSound(Player player) {
        playSound(player, upgradeConfig.getGiveSound());
    }

    private String getDisplayName(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta() || itemStack.getItemMeta() == null) {
            return prettifyMaterial(itemStack == null ? Material.STONE : itemStack.getType());
        }
        ItemMeta meta = itemStack.getItemMeta();
        return meta.hasDisplayName() ? meta.getDisplayName() : prettifyMaterial(itemStack.getType());
    }

    private String prettifyMaterial(Material material) {
        String lowered = material.name().toLowerCase().replace('_', ' ');
        String[] parts = lowered.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private void consumeOne(Inventory inventory, int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null) {
            return;
        }
        if (item.getAmount() <= 1) {
            inventory.setItem(slot, null);
            return;
        }
        item.setAmount(item.getAmount() - 1);
        inventory.setItem(slot, item);
    }

    private void setIfValid(Inventory inventory, int slot, ItemStack itemStack) {
        if (upgradeConfig.isSlotInBounds(slot) && slot < inventory.getSize()) {
            inventory.setItem(slot, itemStack);
        }
    }

    private ItemStack guide(Material material, String name, List<String> lore) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }
        meta.setDisplayName(ColorUtil.colorize(name));
        meta.setLore(ColorUtil.colorize(lore));
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private void playSound(Player player, String soundName) {
        if (player == null || soundName == null || soundName.isBlank()) {
            return;
        }
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase(Locale.ROOT)), 1.0F, 1.0F);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
