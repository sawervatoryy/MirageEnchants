package org.github.sawervatoryy.mirageEnchants.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.enchant.CustomEnchant;
import org.github.sawervatoryy.mirageEnchants.util.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Редактор зачарований: ванильные (до 255) + кастомные. MirageStudio | sawervatoryy */
public final class EditorGui implements MirageGui {
    private static final int[] SLOTS = buildSlots();

    private final MirageEnchants plugin;
    private final Player player;
    private final int heldSlot;
    private final Inventory inv;
    private final Map<Integer, Integer> slotIndex = new HashMap<>();
    private final List<Object> entries = new ArrayList<>();
    private boolean custom = false;
    private int page = 0;

    public EditorGui(MirageEnchants plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.heldSlot = player.getInventory().getHeldItemSlot();
        this.inv = Bukkit.createInventory(this, 54, plugin.messages().get("editor.title"));
        for (int i = 0; i < SLOTS.length; i++) slotIndex.put(SLOTS[i], i);
        render();
    }

    private static int[] buildSlots() {
        int[] s = new int[28];
        int n = 0;
        for (int r = 1; r <= 4; r++) for (int c = 1; c <= 7; c++) s[n++] = r * 9 + c;
        return s;
    }

    public void open() { player.openInventory(inv); }

    @Override
    public Inventory getInventory() { return inv; }

    // ----------------------------------------------------------- отрисовка

    private ItemStack named(Material m, Component name, List<Component> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(name);
        if (lore != null) meta.lore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private void buildEntries() {
        entries.clear();
        if (custom) {
            entries.addAll(plugin.registry().all());
        } else {
            Registry.ENCHANTMENT.stream()
                    .sorted(Comparator.comparing(en -> en.getKey().getKey()))
                    .forEach(entries::add);
        }
    }

    private void render() {
        inv.clear();
        ItemStack pane = named(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), null);
        for (int i = 0; i < 54; i++) inv.setItem(i, pane);

        ItemStack target = player.getInventory().getItem(heldSlot);
        boolean has = target != null && !target.getType().isAir();
        inv.setItem(4, has ? target.clone()
                : named(Material.BARRIER, plugin.messages().get("editor.no-item"), null));

        inv.setItem(2, named(custom ? Material.BOOK : Material.ENCHANTED_BOOK,
                plugin.messages().get("editor.tab-vanilla"), null));
        inv.setItem(6, named(custom ? Material.ENCHANTED_BOOK : Material.BOOK,
                plugin.messages().get("editor.tab-custom"), null));

        buildEntries();
        int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) SLOTS.length));
        if (page >= pages) page = pages - 1;
        int start = page * SLOTS.length;
        for (int i = 0; i < SLOTS.length; i++) {
            int idx = start + i;
            inv.setItem(SLOTS[i], idx < entries.size() && has ? button(entries.get(idx), target) : null);
        }

        inv.setItem(45, page > 0 ? named(Material.ARROW, plugin.messages().get("editor.prev"), null) : pane);
        inv.setItem(53, page < pages - 1 ? named(Material.ARROW, plugin.messages().get("editor.next"), null) : pane);
        inv.setItem(49, named(Material.TNT, plugin.messages().get("editor.clear-all"), null));
        inv.setItem(47, named(Material.NETHER_STAR, Text.c("&#9B6BFFMirageStudio"),
                Text.c(plugin.messages().list("editor.footer"))));
    }

    private ItemStack button(Object entry, ItemStack target) {
        int cur = current(target, entry);
        int max = max(entry);
        Component name;
        if (entry instanceof Enchantment en) {
            name = Component.translatable(en.translationKey(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false);
        } else {
            name = ((CustomEnchant) entry).name();
        }
        name = name.append(Component.text(" " + (cur > 0 ? Text.roman(cur) : "-"), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(plugin.messages().get("editor.level-line", "level", String.valueOf(cur), "max", String.valueOf(max)));
        lore.addAll(Text.c(plugin.messages().list("editor.controls")));
        ItemStack it = named(cur > 0 ? Material.ENCHANTED_BOOK : Material.BOOK, name, lore);
        return it;
    }

    // ------------------------------------------------------------- логика

    private int current(ItemStack it, Object entry) {
        if (entry instanceof Enchantment en) {
            if (it.getItemMeta() instanceof EnchantmentStorageMeta m) return m.getStoredEnchantLevel(en);
            return it.getEnchantmentLevel(en);
        }
        return plugin.items().rawLevel(it, (CustomEnchant) entry);
    }

    private int max(Object entry) {
        if (entry instanceof Enchantment en) {
            int def = plugin.getConfig().getInt("editor.max-level", 255);
            int v = plugin.getConfig().getInt("editor.vanilla-max." + en.getKey().getKey(), def);
            return Math.max(1, Math.min(255, v));
        }
        CustomEnchant ce = (CustomEnchant) entry;
        return plugin.getConfig().getBoolean("editor.custom-above-max", false)
                ? Math.min(255, plugin.getConfig().getInt("editor.max-level", 255)) : ce.maxLevel();
    }

    private boolean apply(ItemStack it, Object entry, int level) {
        if (entry instanceof Enchantment en) {
            if (level > 0 && en.equals(Enchantment.MENDING) && plugin.rules().blockMending()
                    && plugin.rules().isArmor(it.getType())) {
                player.sendMessage(plugin.messages().get("editor.mending-armor"));
                return false;
            }
            if (it.getItemMeta() instanceof EnchantmentStorageMeta m) {
                if (level <= 0) m.removeStoredEnchant(en);
                else m.addStoredEnchant(en, level, true);
                it.setItemMeta(m);
            } else {
                if (level <= 0) it.removeEnchantment(en);
                else it.addUnsafeEnchantment(en, level);
            }
        } else {
            plugin.items().setLevel(it, (CustomEnchant) entry, level);
        }
        return true;
    }

    @Override
    public void click(InventoryClickEvent e) {
        e.setCancelled(true);
        int raw = e.getRawSlot();
        if (raw < 0 || raw >= 54) return;
        ItemStack target = player.getInventory().getItem(heldSlot);
        if (target == null || target.getType().isAir()) { player.closeInventory(); return; }

        switch (raw) {
            case 2 -> { custom = false; page = 0; render(); return; }
            case 6 -> { custom = true; page = 0; render(); return; }
            case 45 -> { if (page > 0) page--; render(); return; }
            case 53 -> { page++; render(); return; }
            case 49 -> {
                if (target.getItemMeta() instanceof EnchantmentStorageMeta m) {
                    for (Enchantment en : new ArrayList<>(m.getStoredEnchants().keySet())) m.removeStoredEnchant(en);
                    target.setItemMeta(m);
                } else {
                    for (Enchantment en : new ArrayList<>(target.getEnchantments().keySet())) target.removeEnchantment(en);
                }
                for (CustomEnchant ce : new ArrayList<>(plugin.items().getEnchants(target).keySet()))
                    plugin.items().setLevel(target, ce, 0);
                player.getInventory().setItem(heldSlot, target);
                render();
                return;
            }
            default -> { }
        }

        Integer idx = slotIndex.get(raw);
        if (idx == null) return;
        int ei = page * SLOTS.length + idx;
        if (ei >= entries.size()) return;
        Object entry = entries.get(ei);

        int cur = current(target, entry);
        int max = max(entry);
        ClickType ct = e.getClick();
        int nl;
        switch (ct) {
            case LEFT -> nl = cur + 1;
            case RIGHT -> nl = cur - 1;
            case SHIFT_LEFT -> nl = cur + 10;
            case SHIFT_RIGHT -> nl = cur - 10;
            case DROP, CONTROL_DROP -> nl = max;
            case SWAP_OFFHAND -> nl = 0;
            default -> { return; }
        }
        nl = Math.max(0, Math.min(max, nl));
        if (nl == cur) return;
        if (!apply(target, entry, nl)) return;
        player.getInventory().setItem(heldSlot, target);
        render();
    }
}
