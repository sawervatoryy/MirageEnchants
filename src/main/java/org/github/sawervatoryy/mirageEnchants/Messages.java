package org.github.sawervatoryy.mirageEnchants;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.github.sawervatoryy.mirageEnchants.util.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** MirageStudio | sawervatoryy */
public final class Messages {
    private final MirageEnchants plugin;
    private YamlConfiguration cfg = new YamlConfiguration();

    public Messages(MirageEnchants plugin) { this.plugin = plugin; }

    public void reload() {
        cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
    }

    private static String apply(String s, String... kv) {
        for (int i = 0; i + 1 < kv.length; i += 2) s = s.replace("{" + kv[i] + "}", kv[i + 1]);
        return s;
    }

    public String raw(String key, String... kv) {
        String s = cfg.getString(key);
        return apply(s == null ? key : s, kv);
    }

    public Component get(String key, String... kv) { return Text.c(raw(key, kv)); }

    public List<String> list(String key, String... kv) {
        List<String> out = new ArrayList<>();
        for (String s : cfg.getStringList(key)) out.add(apply(s, kv));
        return out;
    }

    public void send(CommandSender to, String key, String... kv) {
        to.sendMessage(Text.c(raw("prefix") + raw(key, kv)));
    }

    public void sendList(CommandSender to, String key, String... kv) {
        for (String s : list(key, kv)) to.sendMessage(Text.c(s));
    }

    public void actionBar(Player p, String key, String... kv) { p.sendActionBar(get(key, kv)); }

    public String group(String g) { return cfg.getString("groups." + g.toUpperCase(), g); }
}
