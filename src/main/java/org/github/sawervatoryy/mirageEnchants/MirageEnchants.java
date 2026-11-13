package org.github.sawervatoryy.mirageEnchants;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.github.sawervatoryy.mirageEnchants.command.MirageCommand;
import org.github.sawervatoryy.mirageEnchants.command.SetFilterCommand;
import org.github.sawervatoryy.mirageEnchants.economy.EconomyHook;
import org.github.sawervatoryy.mirageEnchants.enchant.DurabilityRules;
import org.github.sawervatoryy.mirageEnchants.enchant.EnchantItems;
import org.github.sawervatoryy.mirageEnchants.enchant.EnchantRegistry;
import org.github.sawervatoryy.mirageEnchants.listener.AnvilListener;
import org.github.sawervatoryy.mirageEnchants.listener.BlockListener;
import org.github.sawervatoryy.mirageEnchants.listener.CombatListener;
import org.github.sawervatoryy.mirageEnchants.listener.DurabilityListener;
import org.github.sawervatoryy.mirageEnchants.listener.GuiListener;
import org.github.sawervatoryy.mirageEnchants.listener.MovementListener;
import org.github.sawervatoryy.mirageEnchants.listener.ProjectileListener;
import org.github.sawervatoryy.mirageEnchants.listener.TableListener;
import org.github.sawervatoryy.mirageEnchants.util.Watermark;

import java.io.File;

/**
 * MirageEnchants — кастомные зачарования в стиле HolyWorld для Paper 1.21.4.
 * MirageStudio | Автор: sawervatoryy | https://discord.gg/mQbZT3x3Q
 */
public final class MirageEnchants extends JavaPlugin {

    private static MirageEnchants instance;
    private Messages messages;
    private EnchantRegistry registry;
    private EnchantItems items;
    private DurabilityRules rules;
    private EconomyHook economy;
    private FilterStore filters;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveIfMissing("enchants.yml");
        saveIfMissing("messages.yml");

        messages = new Messages(this);
        registry = new EnchantRegistry(this);
        rules = new DurabilityRules(this);
        items = new EnchantItems(this, registry);
        economy = new EconomyHook(this);
        filters = new FilterStore(this);
        reloadAll();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new CombatListener(this), this);
        pm.registerEvents(new ProjectileListener(this), this);
        pm.registerEvents(new DurabilityListener(this), this);
        pm.registerEvents(new AnvilListener(this), this);
        pm.registerEvents(new TableListener(this), this);
        pm.registerEvents(new MovementListener(this), this);
        pm.registerEvents(new GuiListener(this), this);

        MirageCommand cmd = new MirageCommand(this);
        getCommand("mirage").setExecutor(cmd);
        getCommand("mirage").setTabCompleter(cmd);
        getCommand("enchedit").setExecutor(cmd);
        SetFilterCommand sf = new SetFilterCommand(this);
        getCommand("setfilter").setExecutor(sf);
        getCommand("setfilter").setTabCompleter(sf);

        Watermark.banner(getLogger(), getPluginMeta().getVersion());
    }

    private void saveIfMissing(String name) {
        if (!new File(getDataFolder(), name).exists()) saveResource(name, false);
    }

    public void reloadAll() {
        reloadConfig();
        FileConfiguration ench = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "enchants.yml"));
        messages.reload();
        registry.load(ench);
        economy.setup();
    }

    public static MirageEnchants get() { return instance; }
    public Messages messages() { return messages; }
    public EnchantRegistry registry() { return registry; }
    public EnchantItems items() { return items; }
    public DurabilityRules rules() { return rules; }
    public EconomyHook economy() { return economy; }
    public FilterStore filters() { return filters; }
}
