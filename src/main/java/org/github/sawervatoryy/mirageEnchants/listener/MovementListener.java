package org.github.sawervatoryy.mirageEnchants.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.enchant.CustomEnchant;
import org.github.sawervatoryy.mirageEnchants.util.ItemUtil;

/** Лаваход. */
public final class MovementListener implements Listener {
    private final MirageEnchants plugin;

    public MovementListener(MirageEnchants plugin) { this.plugin = plugin; }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom(), to = e.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;
        Player p = e.getPlayer();
        ItemStack boots = p.getInventory().getBoots();
        if (boots == null || boots.getType().isAir()) return;
        int lvl = plugin.items().level(boots, "lava_walker");
        if (lvl <= 0) return;
        CustomEnchant ce = plugin.registry().get("lava_walker");

        int size = (int) ce.setting("size", lvl, 3);
        int r = Math.max(0, (size - 1) / 2);
        Material result = Material.matchMaterial(ce.settingString("result-block", "OBSIDIAN"));
        if (result == null) result = Material.OBSIDIAN;
        boolean onlySources = ce.settingBool("only-sources", true);
        int revert = (int) ce.setting("revert-seconds", lvl, 0);

        Block base = to.getBlock().getRelative(BlockFace.DOWN);
        boolean converted = false;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                Block b = base.getRelative(dx, 0, dz);
                if (b.getType() != Material.LAVA) continue;
                if (onlySources && !(b.getBlockData() instanceof Levelled lv && lv.getLevel() == 0)) continue;
                if (!b.getRelative(BlockFace.UP).getType().isAir()) continue;
                b.setType(result);
                converted = true;
                if (revert > 0) {
                    final Material res = result;
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (b.getType() == res) b.setType(Material.LAVA);
                    }, revert * 20L);
                }
            }
        }
        if (converted) ItemUtil.wear(p, EquipmentSlot.FEET, (int) ce.setting("durability-cost", lvl, 1));
    }
}
