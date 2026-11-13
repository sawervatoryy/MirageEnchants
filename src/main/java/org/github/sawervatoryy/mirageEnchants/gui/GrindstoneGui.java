package org.github.sawervatoryy.mirageEnchants.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.enchant.CustomEnchant;
import org.github.sawervatoryy.mirageEnchants.util.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Точило: снятие выбранных чаров (ванильных и кастомных). MirageStudio | sawervatoryy */
public final class GrindstoneGui implements MirageGui {
    private static final int INPUT = 13;
    private static final int[] SLOTS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    private final MirageEnchants plugin;
    private final Player player;
    private final Inventory inv;
    private final Set<String> selected = new HashSet<>();
    private final Map<Integer, String> slotKey = new LinkedHashMap<>();
    private boolean returned = false;
    private ItemStack input;

    public GrindstoneGui(MirageEnchants plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inv = Bukkit.createInventory(this, 54, plugin.messages().get("grind.title"));
        render();
    }

    public void open() { player.openInventory(inv); }

    @Override
    public Inventory getInventory() { return inv; }

    private ItemStack named(Material m, Component name, List<Component> lore, boolean glint) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(name);
        if (lore != null) meta.lore(lore);
        if (glint) meta.setEnchantmentGlintOverride(true);
        it.setItemMeta(meta);
        return it;
    }

    private Map<Enchantment, Integer> vanillaOf(ItemStack it) {
        if (it.getItemMeta() instanceof EnchantmentStorageMeta m) return new LinkedHashMap<>(m.getStoredEnchants());
        return new LinkedHashMap<>(it.getEnchantments());
    }

    private boolean hasAnyEnchant(ItemStack it) {
        return !vanillaOf(it).isEmpty() || !plugin.items().getEnchants(it).isEmpty();
    }

    private void render() {
        inv.clear();
        slotKey.clear();
        ItemStack pane = named(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), null, false);
        for (int i = 0; i < 54; i++) inv.setItem(i, pane);

        ItemStack input = inputItem();
        inv.setItem(INPUT, input != null ? input : named(Material.GRINDSTONE, plugin.messages().get("grind.put-item"),
                Text.c(plugin.messages().list("grind.put-hint")), false));
        for (int s : SLOTS) inv.setItem(s, null);
        inv.setItem(49, named(Material.LIME_CONCRETE, plugin.messages().get("grind.remove-button"), null, false));
        inv.setItem(45, named(Material.YELLOW_CONCRETE, plugin.messages().get("grind.select-all"), null, false));
        inv.setItem(53, named(Material.NETHER_STAR, Text.c("&#9B6BFFMirageStudio"),
                Text.c(plugin.messages().list("editor.footer")), false));

        if (input == null) return;
        int n = 0;
        for (Map.Entry<Enchantment, Integer> en : vanillaOf(input).entrySet()) {
            if (n >= SLOTS.length) break;
            String key = "v:" + en.getKey().getKey().asString();
            Component name = Component.translatable(en.getKey().translationKey(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(" " + Text.roman(en.getValue()), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));
            put(n++, key, name);
        }
        for (Map.Entry<CustomEnchant, Integer> en : plugin.items().getEnchants(input).entrySet()) {
            if (n >= SLOTS.length) break;
            Component name = en.getKey().name().append(Component.text(
                    " " + plugin.items().levelText(en.getKey(), en.getValue())).decoration(TextDecoration.ITALIC, false));
            put(n++, "c:" + en.getKey().id(), name);
        }
    }

    private void put(int n, String key, Component name) {
        boolean sel = selected.contains(key);
        slotKey.put(SLOTS[n], key);
        inv.setItem(SLOTS[n], named(Material.ENCHANTED_BOOK, name,
                List.of(plugin.messages().get(sel ? "grind.selected" : "grind.not-selected")), sel));
    }

    private ItemStack inputItem() {
        return input == null || input.getType().isAir() ? null : input;
    }

    private void giveBack(ItemStack it) {
        if (it == null || it.getType().isAir()) return;
        for (ItemStack left : player.getInventory().addItem(it).values())
            player.getWorld().dropItem(player.getLocation(), left);
    }

    @Override
    public void click(InventoryClickEvent e) {
        e.setCancelled(true);
        int raw = e.getRawSlot();
        if (raw < 0) return;

        if (raw >= 54) { // инвентарь игрока
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType().isAir() || inputItem() != null) return;
            if (!hasAnyEnchant(clicked)) { plugin.messages().send(player, "grind.no-enchants"); return; }
            input = clicked.clone();
            e.setCurrentItem(null);
            selected.clear();
            render();
            return;
        }

        if (raw == INPUT) {
            ItemStack in = inputItem();
            if (in != null) {
                input = null;
                giveBack(in);
                selected.clear();
                render();
            }
            return;
        }
        if (raw == 45) {
            selected.addAll(slotKey.values());
            render();
            return;
        }
        if (raw == 49) { process(); return; }

        String key = slotKey.get(raw);
        if (key == null) return;
        if (!selected.remove(key)) selected.add(key);
        render();
    }

    private void process() {
        ItemStack it = inputItem();
        if (it == null) return;
        if (selected.isEmpty()) { plugin.messages().send(player, "grind.none-selected"); return; }
        int levels = 0;
        for (String key : new ArrayList<>(selected)) {
            if (key.startsWith("v:")) {
                for (Map.Entry<Enchantment, Integer> en : vanillaOf(it).entrySet()) {
                    if (!("v:" + en.getKey().getKey().asString()).equals(key)) continue;
                    levels += en.getValue();
                    if (it.getItemMeta() instanceof EnchantmentStorageMeta m) {
                        m.removeStoredEnchant(en.getKey());
                        it.setItemMeta(m);
                    } else {
                        it.removeEnchantment(en.getKey());
                    }
                }
            } else {
                CustomEnchant ce = plugin.registry().get(key.substring(2));
                if (ce != null) {
                    levels += plugin.items().rawLevel(it, ce);
                    plugin.items().setLevel(it, ce, 0);
                }
            }
        }
        if (it.getType() == Material.ENCHANTED_BOOK && !hasAnyEnchant(it)) it = new ItemStack(Material.BOOK);
        int xp = plugin.getConfig().getBoolean("grindstone.give-xp", true)
                ? levels * plugin.getConfig().getInt("grindstone.xp-per-enchant-level", 3) : 0;
        input = null;
        giveBack(it);
        if (xp > 0) player.giveExp(xp);
        selected.clear();
        plugin.messages().send(player, "grind.done", "xp", String.valueOf(xp));
        render();
    }

    @Override
    public void close(InventoryCloseEvent e) {
        if (returned) return;
        returned = true;
        giveBack(inputItem());
        input = null;
    }
}
