package org.github.sawervatoryy.mirageEnchants.util;

import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/** MirageStudio | sawervatoryy */
public final class ItemUtil {
    private ItemUtil() {}

    public static int maxDurability(ItemStack item) {
        if (item == null) return 0;
        ItemMeta m = item.getItemMeta();
        if (m instanceof Damageable d && d.hasMaxDamage()) return d.getMaxDamage();
        return item.getType().getMaxDurability();
    }

    public static int damage(ItemStack item) {
        if (item != null && item.getItemMeta() instanceof Damageable d) return d.getDamage();
        return 0;
    }

    /** Оставшаяся прочность; Integer.MAX_VALUE если предмет не ломается. */
    public static int remaining(ItemStack item) {
        int max = maxDurability(item);
        if (max <= 0) return Integer.MAX_VALUE;
        return max - damage(item);
    }

    /** Прямой износ предмета в слоте (без Unbreaking и событий). */
    public static void wear(LivingEntity ent, EquipmentSlot slot, int amount) {
        if (amount <= 0) return;
        EntityEquipment eq = ent.getEquipment();
        if (eq == null) return;
        ItemStack it = eq.getItem(slot);
        if (it.getType().isAir()) return;
        int max = maxDurability(it);
        if (max <= 0) return;
        if (!(it.getItemMeta() instanceof Damageable d)) return;
        if (d.isUnbreakable()) return;
        int nd = d.getDamage() + amount;
        if (nd >= max) {
            eq.setItem(slot, null);
            ent.getWorld().playSound(ent.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
        } else {
            d.setDamage(nd);
            it.setItemMeta(d);
            eq.setItem(slot, it);
        }
    }
}
