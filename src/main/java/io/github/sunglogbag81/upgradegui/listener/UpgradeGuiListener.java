package io.github.sunglogbag81.upgradegui.listener;

import io.github.sunglogbag81.upgradegui.UpgradeGuiPlugin;
import io.github.sunglogbag81.upgradegui.config.UpgradeConfig;
import io.github.sunglogbag81.upgradegui.gui.UpgradeMenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public final class UpgradeGuiListener implements Listener {
    private final UpgradeGuiPlugin plugin;
    private final UpgradeConfig config;

    public UpgradeGuiListener(UpgradeGuiPlugin plugin, UpgradeConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof UpgradeMenuHolder)) {
            return;
        }
        if (config.isLockGuiDuringDelay() && event.getWhoClicked() instanceof Player player && plugin.isProcessing(player)) {
            event.setCancelled(true);
            return;
        }
        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        boolean topInventory = rawSlot >= 0 && rawSlot < topSize;

        if (topInventory) {
            int slot = event.getSlot();
            if (slot == config.getApplySlot()) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player player) {
                    plugin.attemptUpgrade(player, event.getView().getTopInventory());
                }
                return;
            }
            if (slot == config.getPreviewSlot()) {
                event.setCancelled(true);
                return;
            }
            if (slot != config.getItemSlot() && slot != config.getTicketSlot()) {
                event.setCancelled(true);
                return;
            }
        } else if (event.isShiftClick() && config.isBlockShiftMoveIntoGui()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof UpgradeMenuHolder)) {
            return;
        }
        if (config.isLockGuiDuringDelay() && event.getWhoClicked() instanceof Player player && plugin.isProcessing(player)) {
            event.setCancelled(true);
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize && rawSlot != config.getItemSlot() && rawSlot != config.getTicketSlot()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof UpgradeMenuHolder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player) || !config.isCloseReturnItems()) {
            return;
        }
        boolean returned = false;
        for (int slot : new int[]{config.getItemSlot(), config.getTicketSlot()}) {
            if (!config.isSlotInBounds(slot) || slot >= event.getInventory().getSize()) {
                continue;
            }
            ItemStack item = event.getInventory().getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            event.getInventory().setItem(slot, null);
            player.getInventory().addItem(item).values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
            returned = true;
        }
        if (returned) {
            config.sendMessage(player, "returned-items");
        }
    }
}
