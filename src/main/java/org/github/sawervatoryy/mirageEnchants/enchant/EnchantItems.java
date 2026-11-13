package org.github.sawervatoryy.mirageEnchants.enchant;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.util.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Чтение/запись кастомных чар на предметах (PDC + лор). MirageStudio | sawervatoryy */
public final class EnchantItems {
    private final MirageEnchants plugin;
    private final EnchantRegistry registry;
    private final NamespacedKey loreKey;

    public EnchantItems(MirageEnchants plugin, EnchantRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        this.loreKey = new NamespacedKey(plugin, "lore_lines");
    }

    /** Все кастомные чары на предмете (включая выключенные). */
    public Map<CustomEnchant, Integer> getEnchants(ItemStack item) { return read(item, false); }

    /** Только включённые чары: id -> уровень. */
    public Map<String, Integer> active(ItemStack item) {
        Map<CustomEnchant, Integer> m = read(item, true);
        if (m.isEmpty()) return Collections.emptyMap();
        Map<String, Integer> out = new HashMap<>();
        for (Map.Entry<CustomEnchant, Integer> e : m.entrySet()) out.put(e.getKey().id(), e.getValue());
        return out;
    }

    private Map<CustomEnchant, Integer> read(ItemStack item, boolean onlyEnabled) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return Collections.emptyMap();
        return readPdc(item.getItemMeta().getPersistentDataContainer(), onlyEnabled);
    }

    private Map<CustomEnchant, Integer> readPdc(PersistentDataContainer pdc, boolean onlyEnabled) {
        if (pdc.isEmpty()) return Collections.emptyMap();
        Map<CustomEnchant, Integer> m = new LinkedHashMap<>();
        for (CustomEnchant ce : registry.all()) {
            Integer lvl = pdc.get(ce.key(), PersistentDataType.INTEGER);
            if (lvl != null && lvl > 0 && (!onlyEnabled || ce.enabled())) m.put(ce, lvl);
        }
        return m;
    }

    public int rawLevel(ItemStack item, CustomEnchant ce) {
        return getEnchants(item).getOrDefault(ce, 0);
    }

    public int level(ItemStack item, String id) {
        CustomEnchant ce = registry.get(id);
        if (ce == null || !ce.enabled()) return 0;
        return getEnchants(item).getOrDefault(ce, 0);
    }

    public void setLevel(ItemStack item, CustomEnchant ce, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (level <= 0) pdc.remove(ce.key());
        else pdc.set(ce.key(), PersistentDataType.INTEGER, level);
        refresh(meta);
        item.setItemMeta(meta);
    }

    public boolean canApply(ItemStack target, CustomEnchant ce) {
        Material t = target.getType();
        if (t == Material.ENCHANTED_BOOK || t == Material.BOOK) return true;
        return ce.isApplicable(t);
    }

    public CustomEnchant conflict(ItemStack target, CustomEnchant ce) {
        for (CustomEnchant other : getEnchants(target).keySet()) {
            if (other != ce && (ce.conflictsWith(other.id()) || other.conflictsWith(ce.id()))) return other;
        }
        return null;
    }

    public String levelText(CustomEnchant ce, int level) {
        if (ce.maxLevel() == 1 && plugin.getConfig().getBoolean("lore.hide-level-if-max-1", true)) return "";
        return plugin.getConfig().getBoolean("lore.roman", true) ? Text.roman(level) : String.valueOf(level);
    }

    private void refresh(ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        int old = pdc.getOrDefault(loreKey, PersistentDataType.INTEGER, 0);
        for (int i = 0; i < old && !lore.isEmpty(); i++) lore.remove(0);

        String format = plugin.getConfig().getString("lore.format", "{name} {level}");
        List<Component> lines = new ArrayList<>();
        for (Map.Entry<CustomEnchant, Integer> en : readPdc(pdc, false).entrySet()) {
            String line = format.replace("{name}", en.getKey().nameRaw())
                    .replace("{level}", levelText(en.getKey(), en.getValue())).trim();
            lines.add(Text.c(line));
        }
        lore.addAll(0, lines);
        meta.lore(lore.isEmpty() ? null : lore);

        if (lines.isEmpty()) pdc.remove(loreKey);
        else pdc.set(loreKey, PersistentDataType.INTEGER, lines.size());

        if (!lines.isEmpty()) meta.setEnchantmentGlintOverride(true);
        else if (old > 0) meta.setEnchantmentGlintOverride(null);
    }

    /** Книга с чаром. */
    public ItemStack createBook(CustomEnchant ce, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        setLevel(book, ce, level);
        ItemMeta meta = book.getItemMeta();
        String nm = plugin.getConfig().getString("book.display-name", "&dКнига зачарования &8» {name} {level}")
                .replace("{name}", ce.nameRaw()).replace("{level}", levelText(ce, level));
        meta.displayName(Text.c(nm.trim()));
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        for (String d : ce.description()) lore.add(Text.c(d));
        List<String> groups = new ArrayList<>();
        for (String g : ce.appliesTo()) groups.add(plugin.messages().group(g));
        lore.add(Text.c(plugin.messages().raw("book-applies", "items", String.join(", ", groups))));
        lore.add(Text.c("&8MirageStudio"));
        meta.lore(lore);
        book.setItemMeta(meta);
        return book;
    }
}
