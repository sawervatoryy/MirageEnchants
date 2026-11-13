package org.github.sawervatoryy.mirageEnchants.listener;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.enchant.CustomEnchant;
import org.github.sawervatoryy.mirageEnchants.enchant.EnchantItems;
import org.github.sawervatoryy.mirageEnchants.enchant.EnchantRegistry;
import org.github.sawervatoryy.mirageEnchants.util.ItemUtil;

import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Непробиваемый, Крушитель, Разрушитель, Критический, Богач, Фармер (опыт). */
public final class CombatListener implements Listener {
    private static final EquipmentSlot[] ARMOR_SLOTS =
            {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final MirageEnchants plugin;
    private final EnchantItems items;
    private final EnchantRegistry registry;
    private final Random random = new Random();

    public CombatListener(MirageEnchants plugin) {
        this.plugin = plugin;
        this.items = plugin.items();
        this.registry = plugin.registry();
    }

    /** Непробиваемый: шанс снизить входящий урон, шансы с частей брони складываются. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        EntityDamageEvent.DamageCause c = e.getCause();
        if (c == EntityDamageEvent.DamageCause.VOID || c == EntityDamageEvent.DamageCause.KILL
                || c == EntityDamageEvent.DamageCause.SUICIDE) return;
        CustomEnchant ce = registry.get("impenetrable");
        if (ce == null || !ce.enabled()) return;
        double chance = 0;
        for (ItemStack piece : p.getInventory().getArmorContents()) {
            if (piece == null || piece.getType().isAir()) continue;
            int l = items.level(piece, "impenetrable");
            if (l > 0) chance += ce.setting("chance", l, 2.0 * l);
        }
        if (chance <= 0) return;
        if (random.nextDouble() * 100.0 < chance) {
            double red = ce.setting("reduction-percent", 1, 50);
            e.setDamage(e.getDamage() * Math.max(0, 1 - red / 100.0));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && e.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;
        ItemStack weapon = p.getInventory().getItemInMainHand();
        if (weapon.getType().isAir()) return;
        Map<String, Integer> lv = items.active(weapon);
        if (lv.isEmpty()) return;

        // Крушитель: бонус урона только по игрокам
        Integer cr = lv.get("crusher");
        if (cr != null && e.getEntity() instanceof Player) {
            double pct = registry.get("crusher").setting("damage-bonus-percent", cr, 5.0 * cr);
            e.setDamage(e.getDamage() * (1 + pct / 100.0));
        }

        // Критический
        Integer crit = lv.get("critical");
        if (crit != null) {
            CustomEnchant ce = registry.get("critical");
            if (random.nextDouble() * 100.0 < ce.setting("chance", crit, 10.0 * crit)) {
                e.setDamage(e.getDamage() * (1 + ce.setting("damage-bonus-percent", crit, 15) / 100.0));
            }
        }

        // Разрушитель: броня жертвы изнашивается быстрее
        Integer br = lv.get("breaker");
        if (br != null && e.getEntity() instanceof LivingEntity victim && victim.getEquipment() != null) {
            double pct = registry.get("breaker").setting("extra-wear-percent", br, 12.5 * br);
            int base = Math.max(1, (int) (e.getFinalDamage() / 4.0));
            double extra = base * pct / 100.0;
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                int add = (int) extra;
                if (random.nextDouble() < extra - add) add++;
                if (add > 0) ItemUtil.wear(victim, slot, add);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (weapon.getType().isAir()) return;
        Map<String, Integer> lv = items.active(weapon);
        if (lv.isEmpty()) return;

        // Фармер: множитель опыта
        Integer xf = lv.get("xp_farmer");
        if (xf != null && e.getDroppedExp() > 0) {
            CustomEnchant ce = registry.get("xp_farmer");
            double per = ce.setting("multiplier-per-level", xf, 2.0);
            double mult = "EXPONENTIAL".equalsIgnoreCase(ce.settingString("mode", "LINEAR"))
                    ? Math.pow(per, xf) : per * xf;
            e.setDroppedExp((int) Math.round(e.getDroppedExp() * mult));
        }

        // Богач: дополнительные монеты за убийство
        Integer rich = lv.get("rich");
        if (rich != null) {
            CustomEnchant ce = registry.get("rich");
            if (e.getEntity() instanceof Player && !ce.settingBool("include-players", false)) return;
            ConfigurationSection rewards = ce.settingSection("mob-rewards");
            String type = e.getEntityType().name().toUpperCase(Locale.ROOT);
            double base = rewards == null ? 1.0 : rewards.getDouble(type, rewards.getDouble("default", 1.0));
            double bonus = base * ce.setting("bonus-percent", rich, 40.0 * rich) / 100.0;
            double pay = ce.settingBool("bonus-only", true) ? bonus : base + bonus;
            if (plugin.economy().give(killer, pay)) {
                plugin.messages().actionBar(killer, "rich-reward", "amount", String.format(Locale.US, "%.2f", pay));
            }
        }
    }
}
