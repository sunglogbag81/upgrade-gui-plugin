package io.github.sunglogbag81.upgradegui.config;

import io.github.sunglogbag81.upgradegui.model.LevelRule;
import io.github.sunglogbag81.upgradegui.model.UpgradeTicketDefinition;
import io.github.sunglogbag81.upgradegui.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class UpgradeConfig {
    private final JavaPlugin plugin;
    private final Map<String, UpgradeTicketDefinition> tickets = new LinkedHashMap<>();
    private final Map<Integer, LevelRule> levelRules = new TreeMap<>();
    private final Map<Integer, Integer> customModelDataByLevel = new TreeMap<>();
    private final Set<Integer> citizensNpcIds = new LinkedHashSet<>();
    private final Set<Material> allowedMaterials = new LinkedHashSet<>();
    private final Set<Material> blockedMaterials = new LinkedHashSet<>();

    private String prefix;
    private String guiTitle;
    private int guiSize;
    private int itemSlot;
    private int ticketSlot;
    private int previewSlot;
    private int applySlot;
    private Material fillerMaterial;
    private String fillerName;
    private String previewName;
    private List<String> previewLore;
    private String applyName;
    private List<String> applyLore;
    private String processingName;
    private List<String> processingLore;
    private String successPreviewName;
    private String downgradePreviewName;
    private String destroyPreviewName;
    private List<String> resultPreviewLore;
    private boolean requirePermission;
    private boolean closeReturnItems;
    private boolean blockShiftMoveIntoGui;
    private boolean allowStackSizeOneOnly;
    private boolean preserveExistingLore;
    private boolean lockGuiDuringDelay;
    private boolean deliverResultToInventoryIfClosed;
    private boolean citizensEnabled;
    private boolean citizensRequirePermission;
    private int maxLevel;
    private String levelFormat;
    private String loreLine;
    private long attemptDelayTicks;
    private String openSound;
    private String startSound;
    private String successSound;
    private String downgradeSound;
    private String destroySound;
    private String giveSound;

    public UpgradeConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        tickets.clear();
        levelRules.clear();
        customModelDataByLevel.clear();
        citizensNpcIds.clear();
        allowedMaterials.clear();
        blockedMaterials.clear();

        prefix = optionalText(config, "messages.prefix");
        guiTitle = ColorUtil.colorize(config.getString("gui.title", "&6강화소"));
        guiSize = normalizeSize(config.getInt("gui.size", 27));
        itemSlot = config.getInt("gui.item-slot", 11);
        ticketSlot = config.getInt("gui.ticket-slot", 15);
        previewSlot = config.getInt("gui.preview-slot", 13);
        applySlot = config.getInt("gui.apply-slot", 22);
        fillerMaterial = material(config.getString("gui.filler-material"), Material.BLACK_STAINED_GLASS_PANE);
        fillerName = ColorUtil.colorize(config.getString("gui.filler-name", " "));
        previewName = ColorUtil.colorize(config.getString("gui.preview-name", "&6강화 정보"));
        previewLore = ColorUtil.colorize(config.getStringList("gui.preview-lore"));
        applyName = ColorUtil.colorize(config.getString("gui.apply-name", "&a강화 실행"));
        applyLore = ColorUtil.colorize(config.getStringList("gui.apply-lore"));
        processingName = ColorUtil.colorize(config.getString("gui.processing-name", "&e강화 진행 중..."));
        processingLore = ColorUtil.colorize(config.getStringList("gui.processing-lore"));
        successPreviewName = ColorUtil.colorize(config.getString("gui.success-preview-name", "&a강화 성공"));
        downgradePreviewName = ColorUtil.colorize(config.getString("gui.downgrade-preview-name", "&e강화 하락"));
        destroyPreviewName = ColorUtil.colorize(config.getString("gui.destroy-preview-name", "&c강화 초기화"));
        resultPreviewLore = ColorUtil.colorize(config.getStringList("gui.result-preview-lore"));

        requirePermission = config.getBoolean("features.require-permission", true);
        closeReturnItems = config.getBoolean("features.close-return-items", true);
        blockShiftMoveIntoGui = config.getBoolean("features.block-shift-move-into-gui", true);
        allowStackSizeOneOnly = config.getBoolean("features.allow-stack-size-one-only", true);
        preserveExistingLore = config.getBoolean("features.preserve-existing-lore", true);
        lockGuiDuringDelay = config.getBoolean("features.lock-gui-during-delay", true);
        deliverResultToInventoryIfClosed = config.getBoolean("features.deliver-result-to-inventory-if-closed", true);

        citizensEnabled = config.getBoolean("citizens.enabled", false);
        citizensRequirePermission = config.getBoolean("citizens.require-permission", true);
        for (Object value : config.getList("citizens.npc-ids", List.of())) {
            Integer parsed = parseOptionalInt(value);
            if (parsed != null && parsed >= 0) {
                citizensNpcIds.add(parsed);
            }
        }

        maxLevel = Math.max(0, config.getInt("upgrade.max-level", 10));
        levelFormat = ColorUtil.colorize(config.getString("upgrade.level-format", "&6+%level% &f%name%"));
        loreLine = ColorUtil.colorize(config.getString("upgrade.lore-line", "&7강화 단계: &6+%level%"));
        attemptDelayTicks = Math.max(0L, config.getLong("upgrade.attempt-delay-ticks", 20L));

        loadMaterials(config.getStringList("upgrade.allowed-materials"), allowedMaterials);
        loadMaterials(config.getStringList("upgrade.blocked-materials"), blockedMaterials);

        ConfigurationSection cmdSection = config.getConfigurationSection("upgrade.custom-model-data-by-level");
        if (cmdSection != null) {
            for (String key : cmdSection.getKeys(false)) {
                Integer level = parseOptionalInt(key);
                Integer value = parseOptionalInt(cmdSection.get(key));
                if (level != null && value != null) {
                    customModelDataByLevel.put(level, value);
                }
            }
        }

        openSound = config.getString("sounds.open", "BLOCK_ENCHANTMENT_TABLE_USE");
        startSound = config.getString("sounds.start", "BLOCK_NOTE_BLOCK_PLING");
        successSound = config.getString("sounds.success", "ENTITY_PLAYER_LEVELUP");
        downgradeSound = config.getString("sounds.downgrade", "ENTITY_EXPERIENCE_ORB_PICKUP");
        destroySound = config.getString("sounds.destroy", "ENTITY_ITEM_BREAK");
        giveSound = config.getString("sounds.give", "ENTITY_ITEM_PICKUP");

        ConfigurationSection ticketSection = config.getConfigurationSection("upgrade-tickets");
        if (ticketSection != null) {
            for (String key : ticketSection.getKeys(false)) {
                ConfigurationSection entry = ticketSection.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                tickets.put(key.toLowerCase(Locale.ROOT), new UpgradeTicketDefinition(
                        key,
                        entry.getBoolean("enabled", true),
                        material(entry.getString("material"), Material.PAPER),
                        ColorUtil.colorize(entry.getString("display-name", key)),
                        ColorUtil.colorize(entry.getStringList("lore")),
                        optionalInt(entry, "custom-model-data"),
                        entry.getBoolean("glowing", false)
                ));
            }
        }

        ConfigurationSection ruleSection = config.getConfigurationSection("level-rules");
        if (ruleSection != null) {
            for (String key : ruleSection.getKeys(false)) {
                Integer level = parseOptionalInt(key);
                ConfigurationSection entry = ruleSection.getConfigurationSection(key);
                if (level == null || entry == null) {
                    continue;
                }
                levelRules.put(level, new LevelRule(
                        Math.max(0.0D, entry.getDouble("success", 100.0D)),
                        Math.max(0.0D, entry.getDouble("downgrade", 0.0D)),
                        Math.max(0.0D, entry.getDouble("destroy", 0.0D)),
                        entry.getStringList("success-commands")
                ));
            }
        }
    }

    private void loadMaterials(List<String> values, Set<Material> target) {
        for (String raw : values) {
            Material material = material(raw, null);
            if (material != null) {
                target.add(material);
            }
        }
    }

    private int normalizeSize(int raw) {
        int value = Math.max(9, raw);
        return Math.min(54, ((value + 8) / 9) * 9);
    }

    private Material material(String raw, Material fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
        return material == null ? fallback : material;
    }

    private String optionalText(FileConfiguration config, String path) {
        String raw = config.getString(path);
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return ColorUtil.colorize(raw);
    }

    private Integer optionalInt(ConfigurationSection section, String path) {
        if (section == null || !section.contains(path)) {
            return null;
        }
        return parseOptionalInt(section.get(path));
    }

    private Integer parseOptionalInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public String message(String key) {
        String raw = plugin.getConfig().getString("messages." + key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return ColorUtil.colorize(prefix + raw);
    }

    public String message(String key, Map<String, String> replacements) {
        String value = message(key);
        if (value == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            value = value.replace(entry.getKey(), entry.getValue());
        }
        return value;
    }

    public void sendMessage(CommandSender sender, String key) {
        String value = message(key);
        if (value != null && sender != null) {
            sender.sendMessage(value);
        }
    }

    public void sendMessage(CommandSender sender, String key, Map<String, String> replacements) {
        String value = message(key, replacements);
        if (value != null && sender != null) {
            sender.sendMessage(value);
        }
    }

    public List<String> renderPreviewLore(Map<String, String> replacements) {
        return replaceAll(previewLore, replacements);
    }

    public List<String> renderApplyLore(Map<String, String> replacements) {
        return replaceAll(applyLore, replacements);
    }

    public List<String> renderProcessingLore(Map<String, String> replacements) {
        return replaceAll(processingLore, replacements);
    }

    public List<String> renderResultPreviewLore(Map<String, String> replacements) {
        return replaceAll(resultPreviewLore, replacements);
    }

    private List<String> replaceAll(List<String> source, Map<String, String> replacements) {
        List<String> result = new ArrayList<>();
        for (String line : source) {
            String value = line;
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                value = value.replace(entry.getKey(), entry.getValue());
            }
            result.add(value);
        }
        return result;
    }

    public boolean isSlotInBounds(int slot) {
        return slot >= 0 && slot < guiSize;
    }

    public boolean isMaterialAllowed(Material material) {
        if (material == null) {
            return false;
        }
        if (!allowedMaterials.isEmpty() && !allowedMaterials.contains(material)) {
            return false;
        }
        return !blockedMaterials.contains(material);
    }

    public LevelRule getRule(int level) {
        LevelRule exact = levelRules.get(level);
        if (exact != null) {
            return exact;
        }
        LevelRule fallback = levelRules.get(0);
        return fallback == null ? new LevelRule(100.0D, 0.0D, 0.0D, List.of()) : fallback;
    }

    public Integer getCustomModelData(int level) {
        return customModelDataByLevel.get(level);
    }

    public Map<String, UpgradeTicketDefinition> getTickets() { return Collections.unmodifiableMap(tickets); }
    public UpgradeTicketDefinition getTicket(String key) { return key == null ? null : tickets.get(key.toLowerCase(Locale.ROOT)); }
    public String getGuiTitle() { return guiTitle; }
    public int getGuiSize() { return guiSize; }
    public int getItemSlot() { return itemSlot; }
    public int getTicketSlot() { return ticketSlot; }
    public int getPreviewSlot() { return previewSlot; }
    public int getApplySlot() { return applySlot; }
    public Material getFillerMaterial() { return fillerMaterial; }
    public String getFillerName() { return fillerName; }
    public String getPreviewName() { return previewName; }
    public String getApplyName() { return applyName; }
    public String getProcessingName() { return processingName; }
    public String getSuccessPreviewName() { return successPreviewName; }
    public String getDowngradePreviewName() { return downgradePreviewName; }
    public String getDestroyPreviewName() { return destroyPreviewName; }
    public boolean isRequirePermission() { return requirePermission; }
    public boolean isCloseReturnItems() { return closeReturnItems; }
    public boolean isBlockShiftMoveIntoGui() { return blockShiftMoveIntoGui; }
    public boolean isAllowStackSizeOneOnly() { return allowStackSizeOneOnly; }
    public boolean isPreserveExistingLore() { return preserveExistingLore; }
    public boolean isLockGuiDuringDelay() { return lockGuiDuringDelay; }
    public boolean isDeliverResultToInventoryIfClosed() { return deliverResultToInventoryIfClosed; }
    public boolean isCitizensEnabled() { return citizensEnabled; }
    public boolean isCitizensRequirePermission() { return citizensRequirePermission; }
    public Set<Integer> getCitizensNpcIds() { return Collections.unmodifiableSet(citizensNpcIds); }
    public int getMaxLevel() { return maxLevel; }
    public String getLevelFormat() { return levelFormat; }
    public String getLoreLine() { return loreLine; }
    public long getAttemptDelayTicks() { return attemptDelayTicks; }
    public String getOpenSound() { return openSound; }
    public String getStartSound() { return startSound; }
    public String getSuccessSound() { return successSound; }
    public String getDowngradeSound() { return downgradeSound; }
    public String getDestroySound() { return destroySound; }
    public String getGiveSound() { return giveSound; }
}
