package org.github.sawervatoryy.mirageEnchants.listener;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.view.AnvilView;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.enchant.CustomEnchant;
import org.github.sawervatoryy.mirageEnchants.enchant.EnchantItems;

import java.util.HashMap;
import java.util.Map;

/** Наковальня: кастомные чары, расширенные уровни ванильных, запрет Починки/ремонта на броне. */
public final class AnvilListener implements Listener {
    private final MirageEnchants plugin;
    private final EnchantItems items;

    public AnvilListener(MirageEnchants plugin) {
        this.plugin = plugin;
        this.items = plugin.items();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepare(PrepareAnvilEvent e) {
        var cfg = plugin.getConfig();
        if (!cfg.getBoolean("anvil.enabled", true)) return;
        AnvilInventory inv = e.getInventory();
        ItemStack left = inv.getFirstItem();
        ItemStack right = inv.getSecondItem();
        if (left == null || left.getType().isAir()) return;

        ItemStack vanilla = e.getResult();
        ItemStack work = vanilla != null ? vanilla.clone() : left.clone();
        boolean touched = false;
        boolean enchantChange = false;
        int extraCost = 0;

        // Ремонт брони в наковальне запрещён
        if (vanilla != null && plugin.rules().blockAnvilRepair() && plugin.rules().isArmor(left.getType())
                && work.getItemMeta() instanceof Damageable wd && left.getItemMeta() instanceof Damageable ld
                && wd.getDamage() < ld.getDamage()) {
            wd.setDamage(ld.getDamage());
            work.setItemMeta(wd);
            touched = true;
        }

        // Починка на броню запрещена
        if (plugin.rules().blockMending() && plugin.rules().isArmor(work.getType())
                && work.containsEnchantment(Enchantment.MENDING)) {
            work.removeEnchantment(Enchantment.MENDING);
            touched = true;
        }

        if (right != null && !right.getType().isAir()) {
            boolean sameKind = right.getType() == Material.ENCHANTED_BOOK
                    || (right.getType() == left.getType() && cfg.getBoolean("anvil.allow-item-to-item", true));

            // Расширенные уровни ванильных чар (Эффективность 10, Острота 7 и т.д.)
            if (sameKind && cfg.getBoolean("anvil.extended-vanilla-levels", true)) {
                int costPer = cfg.getInt("anvil.extended-cost-per-level", 2);
                Map<Enchantment, Integer> rEnch = enchantsOf(right);
                for (Map.Entry<Enchantment, Integer> en : rEnch.entrySet()) {
                    Enchantment ench = en.getKey();
                    int cap = cfg.getInt("vanilla.anvil-max-levels." + ench.getKey().getKey(), ench.getMaxLevel());
                    if (cap <= ench.getMaxLevel()) continue;
                    if (plugin.rules().blockMending() && ench == Enchantment.MENDING && plugin.rules().isArmor(work.getType())) continue;
                    int lr = en.getValue();
                    int ll = levelOf(left, ench);
                    int target = ll > 0 ? (ll == lr ? ll + 1 : Math.max(ll, lr)) : lr;
                    target = Math.min(target, cap);
                    if (ll == 0) {
                        if (work.getType() != Material.ENCHANTED_BOOK && !ench.canEnchantItem(work)) continue;
                        boolean conflict = false;
                        for (Enchantment ex : enchantsOf(work).keySet()) {
                            if (ex != ench && ex.conflictsWith(ench)) { conflict = true; break; }
                        }
                        if (conflict) continue;
                    }
                    int cur = levelOf(work, ench);
                    if (target > cur) {
                        setVanilla(work, ench, target);
                        extraCost += (target - cur) * costPer;
                        enchantChange = true;
                    }
                }
            }

            // Кастомные чары
            if (sameKind) {
                int costPer = cfg.getInt("anvil.custom-cost-per-level", 2);
                Map<CustomEnchant, Integer> r = items.getEnchants(right);
                Map<CustomEnchant, Integer> l = items.getEnchants(left);
                for (Map.Entry<CustomEnchant, Integer> en : r.entrySet()) {
                    CustomEnchant ce = en.getKey();
                    if (!ce.enabled() || !ce.anvilEnabled()) continue;
                    int lr = en.getValue();
                    int ll = l.getOrDefault(ce, 0);
                    if (ll == 0) {
                        if (!items.canApply(work, ce)) continue;
                        if (items.conflict(work, ce) != null) continue;
                    }
                    int target = ll > 0 ? (ll == lr ? ll + 1 : Math.max(ll, lr)) : lr;
                    target = Math.min(target, ce.anvilMaxLevel());
                    int cur = items.rawLevel(work, ce);
                    if (target > cur) {
                        items.setLevel(work, ce, target);
                        extraCost += (target - cur) * costPer;
                        enchantChange = true;
                    }
                }
            }
        }

        if (vanilla == null && !enchantChange) return;
        if (vanilla != null && !touched && !enchantChange) return;

        if (vanilla != null && !enchantChange && work.isSimilar(left)) {
            e.setResult(null);
            return;
        }

        AnvilView view = e.getView();
        int cost = (vanilla != null ? view.getRepairCost() : 0) + extraCost;
        view.setMaximumRepairCost(cfg.getInt("anvil.max-repair-cost", 1000));
        view.setRepairCost(Math.max(1, cost));
        e.setResult(work);
    }

    private Map<Enchantment, Integer> enchantsOf(ItemStack it) {
        if (it.getItemMeta() instanceof EnchantmentStorageMeta m) return new HashMap<>(m.getStoredEnchants());
        return new HashMap<>(it.getEnchantments());
    }

    private int levelOf(ItemStack it, Enchantment en) {
        if (it.getItemMeta() instanceof EnchantmentStorageMeta m) return m.getStoredEnchantLevel(en);
        return it.getEnchantmentLevel(en);
    }

    private void setVanilla(ItemStack it, Enchantment en, int level) {
        if (it.getItemMeta() instanceof EnchantmentStorageMeta m) {
            m.addStoredEnchant(en, level, true);
            it.setItemMeta(m);
        } else {
            it.addUnsafeEnchantment(en, level);
        }
    }
}
