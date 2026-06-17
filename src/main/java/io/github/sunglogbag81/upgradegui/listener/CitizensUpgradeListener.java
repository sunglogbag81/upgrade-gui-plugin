package io.github.sunglogbag81.upgradegui.listener;

import io.github.sunglogbag81.upgradegui.UpgradeGuiPlugin;
import io.github.sunglogbag81.upgradegui.config.UpgradeConfig;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class CitizensUpgradeListener implements Listener {
    private final UpgradeGuiPlugin plugin;
    private final UpgradeConfig config;

    public CitizensUpgradeListener(UpgradeGuiPlugin plugin, UpgradeConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler(ignoreCancelled = true)
    public void onNpcRightClick(NPCRightClickEvent event) {
        if (!config.isCitizensEnabled()) {
            return;
        }
        if (!config.getCitizensNpcIds().contains(event.getNPC().getId())) {
            return;
        }
        Player player = event.getClicker();
        if (config.isCitizensRequirePermission() && config.isRequirePermission() && !player.hasPermission("upgradegui.use")) {
            config.sendMessage(player, "no-permission");
            return;
        }
        plugin.openMenu(player);
    }
}
