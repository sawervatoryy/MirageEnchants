package org.github.sawervatoryy.mirageEnchants.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.gui.GrindstoneGui;
import org.github.sawervatoryy.mirageEnchants.gui.MirageGui;

/** Диспетчер GUI + подмена ванильного точила. */
public final class GuiListener implements Listener {
    private final MirageEnchants plugin;

    public GuiListener(MirageEnchants plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof MirageGui g) g.click(e);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof MirageGui g) g.drag(e);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof MirageGui g) g.close(e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        if (e.getClickedBlock() == null || e.getClickedBlock().getType() != Material.GRINDSTONE) return;
        if (!plugin.getConfig().getBoolean("grindstone.replace-vanilla", true)) return;
        Player p = e.getPlayer();
        if (p.isSneaking() || !p.hasPermission("mirageenchants.grindstone")) return;
        e.setCancelled(true);
        new GrindstoneGui(plugin, p).open();
    }
}
