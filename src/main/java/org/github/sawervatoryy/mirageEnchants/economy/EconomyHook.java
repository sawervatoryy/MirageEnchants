package org.github.sawervatoryy.mirageEnchants.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;

import java.lang.reflect.Method;
import java.util.Locale;

/** Экономика через Vault (Essentials и любые другие плагины с Vault) без compile-зависимости. */
public final class EconomyHook {
    private final MirageEnchants plugin;
    private Object economy;
    private Method deposit;

    public EconomyHook(MirageEnchants plugin) { this.plugin = plugin; }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setup() {
        economy = null;
        deposit = null;
        if (!"VAULT".equalsIgnoreCase(plugin.getConfig().getString("economy.mode", "VAULT"))) return;
        try {
            Class c = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider rsp = Bukkit.getServicesManager().getRegistration(c);
            if (rsp == null) {
                plugin.getLogger().warning("Vault найден, но экономика не зарегистрирована (нужен Essentials/другой плагин валюты).");
                return;
            }
            economy = rsp.getProvider();
            deposit = c.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            plugin.getLogger().info("Экономика подключена через Vault.");
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().warning("Vault не установлен: зачарование «Богач» не будет выдавать монеты (или используйте economy.mode: COMMAND).");
        } catch (Exception ex) {
            plugin.getLogger().warning("Ошибка подключения к Vault: " + ex.getMessage());
        }
    }

    public boolean give(Player p, double amount) {
        if (amount <= 0) return false;
        String mode = plugin.getConfig().getString("economy.mode", "VAULT").toUpperCase(Locale.ROOT);
        if (mode.equals("COMMAND")) {
            String cmd = plugin.getConfig().getString("economy.give-command", "eco give {player} {amount}")
                    .replace("{player}", p.getName())
                    .replace("{amount}", String.format(Locale.US, "%.2f", amount));
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
        if (economy == null || deposit == null) return false;
        try {
            deposit.invoke(economy, p, amount);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
