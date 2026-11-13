package org.github.sawervatoryy.mirageEnchants.listener;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.enchant.CustomEnchant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Кастомные чары выпадают на столе зачарований как обычные. */
public final class TableListener implements Listener {
    private final MirageEnchants plugin;
    private final Random random = new Random();

    public TableListener(MirageEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent e) {
        ItemStack item = e.getItem();
        if (plugin.rules().blockMending() && plugin.rules().isArmor(item.getType())) {
            e.getEnchantsToAdd().remove(Enchantment.MENDING);
        }
        var cfg = plugin.getConfig();
        if (!cfg.getBoolean("table.custom-enchants.enabled", true)) return;
        Material type = item.getType();
        if (type == Material.BOOK && !cfg.getBoolean("table.custom-enchants.allow-on-books", true)) return;

        int cost = e.getExpLevelCost();
        double mult = cfg.getDouble("table.custom-enchants.chance-multiplier", 1.0);
        int maxPer = Math.max(1, cfg.getInt("table.custom-enchants.max-per-item", 1));

        List<CustomEnchant> cands = new ArrayList<>();
        for (CustomEnchant ce : plugin.registry().enabled()) {
            if (!ce.tableEnabled() || cost < ce.tableMinCost()) continue;
            if (type != Material.BOOK && !ce.isApplicable(type)) continue;
            cands.add(ce);
        }
        Collections.shuffle(cands, random);

        Map<CustomEnchant, Integer> picked = new LinkedHashMap<>();
        for (CustomEnchant ce : cands) {
            if (picked.size() >= maxPer) break;
            boolean conflict = false;
            for (CustomEnchant other : picked.keySet()) {
                if (ce.conflictsWith(other.id()) || other.conflictsWith(ce.id())) { conflict = true; break; }
            }
            if (conflict) continue;
            if (random.nextDouble() * 100.0 >= ce.tableChance() * mult) continue;
            int maxLvl = Math.min(ce.tableMaxLevel(), ce.maxLevel());
            int cap = Math.max(1, (int) Math.ceil(maxLvl * Math.min(1.0, cost / 30.0)));
            picked.put(ce, 1 + random.nextInt(cap));
        }
        if (picked.isEmpty()) return;

        Inventory inv = e.getInventory();
        Player player = e.getEnchanter();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack it = inv.getItem(0);
            if (it == null || it.getType().isAir()) return;
            for (Map.Entry<CustomEnchant, Integer> en : picked.entrySet()) {
                plugin.items().setLevel(it, en.getKey(), en.getValue());
                plugin.messages().send(player, "table-custom", "name", en.getKey().nameRaw(),
                        "level", plugin.items().levelText(en.getKey(), en.getValue()));
            }
            inv.setItem(0, it);
        });
    }
}
