package org.github.sawervatoryy.mirageEnchants.enchant;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.util.ItemGroups;
import org.github.sawervatoryy.mirageEnchants.util.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Описание кастомного зачарования (читается из enchants.yml). MirageStudio | sawervatoryy */
public final class CustomEnchant {
    private final String id;
    private final NamespacedKey key;
    private final ConfigurationSection sec;

    public CustomEnchant(MirageEnchants plugin, String id, ConfigurationSection sec) {
        this.id = id;
        this.key = new NamespacedKey(plugin, "e_" + id);
        this.sec = sec;
    }

    public String id() { return id; }
    public NamespacedKey key() { return key; }

    public boolean enabled() { return sec.getBoolean("enabled", true); }
    public String nameRaw() { return sec.getString("display-name", id); }
    public Component name() { return Text.c(nameRaw()); }
    public List<String> description() { return sec.getStringList("description"); }
    public int maxLevel() { return Math.max(1, sec.getInt("max-level", 1)); }
    public List<String> appliesTo() { return sec.getStringList("applies-to"); }

    public boolean isApplicable(Material m) {
        for (String g : appliesTo()) if (ItemGroups.matches(g, m)) return true;
        return false;
    }

    public boolean conflictsWith(String otherId) {
        for (String s : sec.getStringList("conflicts-with")) if (s.equalsIgnoreCase(otherId)) return true;
        return false;
    }

    public boolean bookEnabled() { return sec.getBoolean("book.enabled", true); }

    public boolean tableEnabled() { return sec.getBoolean("table.enabled", true); }
    public double tableChance() { return sec.getDouble("table.chance", 5.0); }
    public int tableMaxLevel() { return Math.max(1, sec.getInt("table.max-level", 1)); }
    public int tableMinCost() { return sec.getInt("table.min-cost", 1); }

    public boolean anvilEnabled() { return sec.getBoolean("anvil.enabled", true); }
    public int anvilMaxLevel() { return Math.max(1, sec.getInt("anvil.max-level", maxLevel())); }

    /** Значение настройки: число или таблица "уровень: значение" (ближайший меньший уровень). */
    public double setting(String key, int level, double def) {
        Object o = sec.get("settings." + key);
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof ConfigurationSection s) {
            int best = Integer.MIN_VALUE, lowest = Integer.MAX_VALUE;
            double val = def, lowVal = def;
            for (String k : s.getKeys(false)) {
                int lv;
                try { lv = Integer.parseInt(k); } catch (NumberFormatException ex) { continue; }
                if (lv <= level && lv > best) { best = lv; val = s.getDouble(k, def); }
                if (lv < lowest) { lowest = lv; lowVal = s.getDouble(k, def); }
            }
            return best != Integer.MIN_VALUE ? val : lowVal;
        }
        return def;
    }

    public boolean settingBool(String key, boolean def) { return sec.getBoolean("settings." + key, def); }
    public String settingString(String key, String def) { return sec.getString("settings." + key, def); }

    public List<String> settingList(String key) {
        List<String> out = new ArrayList<>();
        for (String s : sec.getStringList("settings." + key)) out.add(s.toUpperCase(Locale.ROOT));
        return out;
    }

    public ConfigurationSection settingSection(String key) { return sec.getConfigurationSection("settings." + key); }
}
