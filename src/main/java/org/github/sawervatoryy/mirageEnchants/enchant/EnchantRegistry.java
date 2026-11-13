package org.github.sawervatoryy.mirageEnchants.enchant;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** MirageStudio | sawervatoryy */
public final class EnchantRegistry {
    private final MirageEnchants plugin;
    private final Map<String, CustomEnchant> byId = new LinkedHashMap<>();

    public EnchantRegistry(MirageEnchants plugin) { this.plugin = plugin; }

    public void load(FileConfiguration cfg) {
        byId.clear();
        ConfigurationSection root = cfg.getConfigurationSection("enchants");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s != null) byId.put(id.toLowerCase(Locale.ROOT), new CustomEnchant(plugin, id.toLowerCase(Locale.ROOT), s));
        }
    }

    public CustomEnchant get(String id) { return id == null ? null : byId.get(id.toLowerCase(Locale.ROOT)); }
    public Collection<CustomEnchant> all() { return byId.values(); }

    public List<CustomEnchant> enabled() {
        List<CustomEnchant> out = new ArrayList<>();
        for (CustomEnchant c : byId.values()) if (c.enabled()) out.add(c);
        return out;
    }
}
