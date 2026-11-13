package org.github.sawervatoryy.mirageEnchants;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Фильтр предметов игрока (хранится в PDC игрока). MirageStudio | sawervatoryy */
public final class FilterStore {
    private final NamespacedKey key;

    public FilterStore(MirageEnchants plugin) { this.key = new NamespacedKey(plugin, "filter"); }

    public Set<Material> get(Player p) {
        Set<Material> out = new LinkedHashSet<>();
        String s = p.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (s == null || s.isEmpty()) return out;
        for (String part : s.split(",")) {
            Material m = Material.matchMaterial(part);
            if (m != null) out.add(m);
        }
        return out;
    }

    public void set(Player p, Set<Material> mats) {
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (mats.isEmpty()) pdc.remove(key);
        else pdc.set(key, PersistentDataType.STRING, mats.stream().map(Material::name).collect(Collectors.joining(",")));
    }
}
