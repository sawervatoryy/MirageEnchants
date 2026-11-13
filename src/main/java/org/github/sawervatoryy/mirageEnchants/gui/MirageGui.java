package org.github.sawervatoryy.mirageEnchants.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/** Базовый интерфейс GUI плагина. MirageStudio | sawervatoryy */
public interface MirageGui extends InventoryHolder {
    void click(InventoryClickEvent e);

    default void drag(InventoryDragEvent e) { e.setCancelled(true); }

    default void close(InventoryCloseEvent e) {}
}
