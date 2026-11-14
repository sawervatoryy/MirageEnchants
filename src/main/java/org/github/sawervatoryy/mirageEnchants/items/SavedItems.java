package org.github.sawervatoryy.mirageEnchants.items;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Сохранённые предметы (items.yml): предмет из руки сохраняется целиком (все данные, включая
 * зачарования MirageEnchants) и выдаётся командой /mirage item give.
 * MirageStudio | sawervatoryy
 */
public final class SavedItems {
    private final MirageEnchants plugin;
    private final File file;
    private final Map<String, ItemStack> items = new LinkedHashMap<>();

    public SavedItems(MirageEnchants plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "items.yml");
    }

    public static boolean validName(String s) {
        return s != null && s.matches("[A-Za-z0-9_\\-]{1,32}");
    }

    public void load() {
        items.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = y.getConfigurationSection("items");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            String data = sec.getString(key + ".data");
            if (data == null || data.isEmpty()) continue;
            try {
                items.put(key.toLowerCase(Locale.ROOT), ItemStack.deserializeBytes(Base64.getDecoder().decode(data)));
            } catch (Exception ex) {
                plugin.getLogger().warning("Не удалось загрузить предмет '" + key + "' из items.yml: " + ex.getMessage());
            }
        }
    }

    public boolean has(String name) { return items.containsKey(name.toLowerCase(Locale.ROOT)); }

    /** Копия сохранённого предмета (количество 1) или null. */
    public ItemStack get(String name) {
        ItemStack it = items.get(name.toLowerCase(Locale.ROOT));
        return it == null ? null : it.clone();
    }

    public List<String> names() { return new ArrayList<>(items.keySet()); }

    public boolean save(String name, ItemStack item) {
        ItemStack copy = item.clone();
        copy.setAmount(1);
        items.put(name.toLowerCase(Locale.ROOT), copy);
        return write();
    }

    public boolean delete(String name) {
        if (items.remove(name.toLowerCase(Locale.ROOT)) == null) return false;
        write();
        return true;
    }

    private boolean write() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, ItemStack> en : items.entrySet()) {
            y.set("items." + en.getKey() + ".material", en.getValue().getType().name());
            y.set("items." + en.getKey() + ".data", Base64.getEncoder().encodeToString(en.getValue().serializeAsBytes()));
        }
        try {
            y.save(file);
            return true;
        } catch (IOException ex) {
            plugin.getLogger().warning("Не удалось сохранить items.yml: " + ex.getMessage());
            return false;
        }
    }
}
