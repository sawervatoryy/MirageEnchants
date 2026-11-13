package org.github.sawervatoryy.mirageEnchants.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.util.Text;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** GUI фильтра предметов. MirageStudio | sawervatoryy */
public final class FilterGui implements MirageGui {
    private final MirageEnchants plugin;
    private final Player player;
    private final Inventory inv;

    public FilterGui(MirageEnchants plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inv = Bukkit.createInventory(this, 27, plugin.messages().get("filter.title"));
        render();
    }

    public void open() { player.openInventory(inv); }

    @Override
    public Inventory getInventory() { return inv; }

    private void render() {
        inv.clear();
        int i = 0;
        for (Material m : plugin.filters().get(player)) {
            if (i >= 27) break;
            ItemStack it = new ItemStack(m);
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.lore(Text.c(plugin.messages().list("filter.hint-remove")));
                it.setItemMeta(meta);
            }
            inv.setItem(i++, it);
        }
    }

    @Override
    public void click(InventoryClickEvent e) {
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        Set<Material> set = new LinkedHashSet<>(plugin.filters().get(player));
        if (e.getClickedInventory() == inv) {
            set.remove(clicked.getType());
            plugin.messages().send(player, "filter.removed", "material", clicked.getType().name());
        } else {
            if (set.size() >= 27) { plugin.messages().send(player, "filter.full"); return; }
            if (!clicked.getType().isItem()) return;
            set.add(clicked.getType());
            plugin.messages().send(player, "filter.added", "material", clicked.getType().name());
        }
        plugin.filters().set(player, set);
        render();
    }
}
