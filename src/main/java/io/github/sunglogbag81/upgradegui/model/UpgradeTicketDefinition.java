package io.github.sunglogbag81.upgradegui.model;

import org.bukkit.Material;

import java.util.List;

public record UpgradeTicketDefinition(String key,
                                      boolean enabled,
                                      Material material,
                                      String displayName,
                                      List<String> lore,
                                      Integer customModelData,
                                      boolean glowing) {
}
