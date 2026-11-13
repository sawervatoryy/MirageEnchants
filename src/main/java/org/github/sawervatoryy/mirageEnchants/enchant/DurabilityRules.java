package org.github.sawervatoryy.mirageEnchants.enchant;

import org.bukkit.Material;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.util.ItemGroups;

/** Особенности «Прочности» и «Починки» для брони. MirageStudio | sawervatoryy */
public final class DurabilityRules {
    private final MirageEnchants plugin;

    public DurabilityRules(MirageEnchants plugin) { this.plugin = plugin; }

    public boolean isArmor(Material m) { return ItemGroups.matches("ARMOR", m); }

    public boolean blockMending() { return plugin.getConfig().getBoolean("durability.block-mending-on-armor", true); }
    public boolean blockAnvilRepair() { return plugin.getConfig().getBoolean("durability.block-anvil-repair-armor", true); }
    public boolean blockFix() { return plugin.getConfig().getBoolean("durability.block-fix-command-armor", true); }
    public boolean armorUnbreakingEnabled() { return plugin.getConfig().getBoolean("durability.armor-unbreaking.enabled", true); }

    /** Шанс (0..1) не потратить прочность: base + уровень * per-level. */
    public double armorSaveChance(int level) {
        double base = plugin.getConfig().getDouble("durability.armor-unbreaking.base-chance", 47);
        double per = plugin.getConfig().getDouble("durability.armor-unbreaking.per-level", 10);
        double max = plugin.getConfig().getDouble("durability.armor-unbreaking.max-chance", 100);
        return Math.min(max, base + level * per) / 100.0;
    }
}
