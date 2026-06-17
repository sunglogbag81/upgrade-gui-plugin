package io.github.sunglogbag81.upgradegui.model;

import java.util.List;

public record LevelRule(double successChance,
                        double downgradeChance,
                        double destroyChance,
                        List<String> successCommands) {
}
