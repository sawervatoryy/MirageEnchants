package org.github.sawervatoryy.mirageEnchants.listener;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.enchant.CustomEnchant;

import java.util.Random;

/** Оглушение (лук/арбалет) и Самонаводка (трезубец). */
public final class ProjectileListener implements Listener {
    private final MirageEnchants plugin;
    private final NamespacedKey stunKey;
    private final NamespacedKey distKey;
    private final NamespacedKey radiusKey;
    private final Random random = new Random();

    public ProjectileListener(MirageEnchants plugin) {
        this.plugin = plugin;
        this.stunKey = new NamespacedKey(plugin, "stun_level");
        this.distKey = new NamespacedKey(plugin, "homing_dist");
        this.radiusKey = new NamespacedKey(plugin, "homing_radius");
    }

    // ------------------------------------------------------------ Оглушение

    @EventHandler(ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent e) {
        ItemStack bow = e.getBow();
        if (bow == null) return;
        int lvl = plugin.items().level(bow, "stun");
        if (lvl > 0) e.getProjectile().getPersistentDataContainer().set(stunKey, PersistentDataType.INTEGER, lvl);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrowHit(EntityDamageByEntityEvent e) {
        Entity d = e.getDamager();

        if (d instanceof Trident t) {
            Double dist = t.getPersistentDataContainer().get(distKey, PersistentDataType.DOUBLE);
            Double radius = t.getPersistentDataContainer().get(radiusKey, PersistentDataType.DOUBLE);
            CustomEnchant ce = plugin.registry().get("homing");
            if (dist != null && radius != null && radius > 0 && ce != null) {
                double maxRed = ce.setting("max-damage-reduction-percent", 1, 50);
                double ratio = Math.min(1.0, dist / radius);
                e.setDamage(e.getDamage() * (1 - ratio * maxRed / 100.0));
            }
        }

        if (d instanceof AbstractArrow arrow) {
            Integer lvl = arrow.getPersistentDataContainer().get(stunKey, PersistentDataType.INTEGER);
            if (lvl == null || !(e.getEntity() instanceof Player victim)) return;
            CustomEnchant ce = plugin.registry().get("stun");
            if (ce == null || !ce.enabled()) return;
            if (random.nextDouble() * 100.0 >= ce.setting("chance", lvl, 5)) return;
            int slowAmp = (int) ce.setting("slowness-amplifier", lvl, 3);
            int slowTicks = (int) (ce.setting("slowness-seconds", lvl, 3) * 20);
            int blindTicks = (int) (ce.setting("blindness-seconds", lvl, 2) * 20);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowTicks, slowAmp));
            if (blindTicks > 0) victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindTicks, 0));
        }
    }

    // ---------------------------------------------------------- Самонаводка

    @EventHandler(ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player shooter)) return;
        int lvl = plugin.items().level(trident.getItemStack(), "homing");
        if (lvl <= 0) return;
        CustomEnchant ce = plugin.registry().get("homing");

        double radius = ce.setting("radius", lvl, 8);
        double maxAngle = Math.toRadians(ce.setting("max-angle-degrees", lvl, 60));
        Player target = findTarget(shooter, trident, radius, maxAngle);
        if (target == null) return;

        double dist = shooter.getLocation().distance(target.getLocation());
        trident.getPersistentDataContainer().set(distKey, PersistentDataType.DOUBLE, dist);
        trident.getPersistentDataContainer().set(radiusKey, PersistentDataType.DOUBLE, radius);

        int maxTicks = (int) ce.setting("tracking-ticks", lvl, 30);
        final double speed = Math.max(trident.getVelocity().length(), 1.5);
        if (maxTicks <= 0) {
            Vector dir = aim(trident.getLocation(), target);
            trident.setVelocity(dir.multiply(speed));
            return;
        }
        trident.setGravity(false);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!trident.isValid() || trident.isDead() || trident.isInBlock() || !target.isOnline()
                        || target.isDead() || ++ticks > maxTicks) {
                    if (trident.isValid()) trident.setGravity(true);
                    cancel();
                    return;
                }
                trident.setVelocity(aim(trident.getLocation(), target).multiply(speed));
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private Vector aim(Location from, Player target) {
        Location t = target.getLocation().add(0, target.getHeight() * 0.6, 0);
        return t.toVector().subtract(from.toVector()).normalize();
    }

    private Player findTarget(Player shooter, Trident trident, double radius, double maxAngle) {
        Vector look = trident.getVelocity().clone();
        if (look.lengthSquared() < 1.0E-6) look = shooter.getLocation().getDirection();
        look.normalize();
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player other : shooter.getWorld().getPlayers()) {
            if (other.equals(shooter)) continue;
            if (other.getGameMode() == GameMode.SPECTATOR || other.getGameMode() == GameMode.CREATIVE) continue;
            double dist = other.getLocation().distance(shooter.getLocation());
            if (dist > radius || dist >= bestDist) continue;
            Vector to = other.getLocation().add(0, other.getHeight() / 2, 0).toVector()
                    .subtract(trident.getLocation().toVector()).normalize();
            if (Math.acos(Math.max(-1, Math.min(1, look.dot(to)))) > maxAngle) continue;
            best = other;
            bestDist = dist;
        }
        return best;
    }
}
