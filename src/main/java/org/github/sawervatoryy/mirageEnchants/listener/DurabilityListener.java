package org.github.sawervatoryy.mirageEnchants.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.util.ItemUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Особенности Прочности и Починки + Неразрушимость (износ). */
public final class DurabilityListener implements Listener {
    private final MirageEnchants plugin;
    private final Random random = new Random();

    public DurabilityListener(MirageEnchants plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(PlayerItemDamageEvent e) {
        ItemStack item = e.getItem();

        // Неразрушимость: не изнашивать ниже порога
        int ind = plugin.items().level(item, "indestructible");
        if (ind > 0) {
            int min = (int) plugin.registry().get("indestructible").setting("min-durability", ind, 10);
            if (ItemUtil.remaining(item) < min) {
                e.setCancelled(true);
                return;
            }
        }

        // Прочность брони: шанс не потратить = base + уровень * per-level
        if (plugin.rules().armorUnbreakingEnabled() && plugin.rules().isArmor(item.getType())) {
            int lvl = item.getEnchantmentLevel(Enchantment.UNBREAKING);
            if (lvl > 0) {
                double save = plugin.rules().armorSaveChance(lvl);
                int original = Math.max(e.getOriginalDamage(), e.getDamage());
                int dmg = 0;
                for (int i = 0; i < original; i++) if (random.nextDouble() >= save) dmg++;
                e.setDamage(dmg);
            }
        }
    }

    /** Починка опытом не работает на броне. */
    @EventHandler(ignoreCancelled = true)
    public void onMend(PlayerItemMendEvent e) {
        if (plugin.rules().blockMending() && plugin.rules().isArmor(e.getItem().getType())) e.setCancelled(true);
    }

    /** /fix и подобные команды не чинят броню (откат износа в следующем тике). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!plugin.rules().blockFix()) return;
        String msg = e.getMessage();
        if (msg.length() < 2) return;
        String cmd = msg.substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        int colon = cmd.indexOf(':');
        if (colon >= 0) cmd = cmd.substring(colon + 1);
        if (!plugin.getConfig().getStringList("durability.fix-commands").contains(cmd)) return;

        Player p = e.getPlayer();
        PlayerInventory inv = p.getInventory();
        Map<Integer, Integer> snapshot = new HashMap<>();
        Map<Integer, Material> types = new HashMap<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir() || !plugin.rules().isArmor(it.getType())) continue;
            if (it.getItemMeta() instanceof Damageable d) {
                snapshot.put(i, d.getDamage());
                types.put(i, it.getType());
            }
        }
        if (snapshot.isEmpty()) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Map.Entry<Integer, Integer> en : snapshot.entrySet()) {
                ItemStack it = inv.getItem(en.getKey());
                if (it == null || it.getType() != types.get(en.getKey())) continue;
                if (it.getItemMeta() instanceof Damageable d && d.getDamage() < en.getValue()) {
                    d.setDamage(en.getValue());
                    it.setItemMeta(d);
                    inv.setItem(en.getKey(), it);
                }
            }
        });
    }
}
