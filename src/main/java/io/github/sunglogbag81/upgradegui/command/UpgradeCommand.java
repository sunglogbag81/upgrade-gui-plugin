package io.github.sunglogbag81.upgradegui.command;

import io.github.sunglogbag81.upgradegui.UpgradeGuiPlugin;
import io.github.sunglogbag81.upgradegui.config.UpgradeConfig;
import io.github.sunglogbag81.upgradegui.model.UpgradeTicketDefinition;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class UpgradeCommand implements CommandExecutor, TabCompleter {
    private final UpgradeGuiPlugin plugin;
    private final UpgradeConfig config;

    public UpgradeCommand(UpgradeGuiPlugin plugin, UpgradeConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                config.sendMessage(sender, "player-only");
                return true;
            }
            if (config.isRequirePermission() && !player.hasPermission("upgradegui.use")) {
                config.sendMessage(player, "no-permission");
                return true;
            }
            plugin.openMenu(player);
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            case "give" -> handleGive(sender, args);
            case "setup" -> handleSetup(sender);
            default -> {
                config.sendMessage(sender, "usage-admin");
                yield true;
            }
        };
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("upgradegui.reload") && !sender.hasPermission("upgradegui.admin")) {
            config.sendMessage(sender, "no-permission");
            return true;
        }
        plugin.reloadPlugin();
        config.sendMessage(sender, "config-reloaded");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("upgradegui.list") && !sender.hasPermission("upgradegui.admin")) {
            config.sendMessage(sender, "no-permission");
            return true;
        }
        sender.sendMessage("강화권: " + String.join(", ", config.getTickets().keySet()));
        sender.sendMessage("등록된 단계 아이템: " + config.getConfiguredLevels());
        sender.sendMessage("강화 지연시간(ticks): " + config.getAttemptDelayTicks());
        return true;
    }

    private boolean handleSetup(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            config.sendMessage(sender, "player-only");
            return true;
        }
        if (!player.hasPermission("upgradegui.setup") && !player.hasPermission("upgradegui.admin")) {
            config.sendMessage(player, "no-permission");
            return true;
        }
        plugin.openSetupMenu(player);
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("upgradegui.give") && !sender.hasPermission("upgradegui.admin")) {
            config.sendMessage(sender, "no-permission");
            return true;
        }
        if (args.length < 3) {
            config.sendMessage(sender, "usage-admin");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("플레이어를 찾을 수 없습니다: " + args[1]);
            return true;
        }
        UpgradeTicketDefinition definition = config.getTicket(args[2]);
        if (definition == null || !definition.enabled()) {
            config.sendMessage(sender, "unknown-ticket", Map.of("%key%", args[2]));
            return true;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException ignored) {
            }
        }
        var item = plugin.createTicketItem(definition);
        item.setAmount(amount);
        target.getInventory().addItem(item);
        plugin.playGiveSound(target);
        config.sendMessage(sender, "ticket-given", Map.of(
                "%player%", target.getName(),
                "%ticket%", definition.displayName(),
                "%amount%", String.valueOf(amount)
        ));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("reload", "list", "give", "setup"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filter(new ArrayList<>(config.getTickets().keySet()), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String input) {
        String lowered = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lowered)).toList();
    }
}
