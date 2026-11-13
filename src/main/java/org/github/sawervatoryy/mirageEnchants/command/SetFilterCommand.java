package org.github.sawervatoryy.mirageEnchants.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.gui.FilterGui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** /setfilter — настройка фильтра для зачарования «Фильтр». MirageStudio | sawervatoryy */
public final class SetFilterCommand implements CommandExecutor, TabCompleter {
    private final MirageEnchants plugin;

    public SetFilterCommand(MirageEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { plugin.messages().send(sender, "player-only"); return true; }
        if (!p.hasPermission("mirageenchants.filter")) { plugin.messages().send(p, "no-permission"); return true; }
        if (args.length == 0) { new FilterGui(plugin, p).open(); return true; }

        Set<Material> set = new LinkedHashSet<>(plugin.filters().get(p));
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add", "remove" -> {
                Material m;
                if (args.length > 1) m = Material.matchMaterial(args[1]);
                else {
                    ItemStack hand = p.getInventory().getItemInMainHand();
                    m = hand.getType().isAir() ? null : hand.getType();
                }
                if (m == null || !m.isItem()) { plugin.messages().send(p, "filter.usage"); return true; }
                if (args[0].equalsIgnoreCase("add")) {
                    if (set.size() >= 27) { plugin.messages().send(p, "filter.full"); return true; }
                    set.add(m);
                    plugin.messages().send(p, "filter.added", "material", m.name());
                } else {
                    set.remove(m);
                    plugin.messages().send(p, "filter.removed", "material", m.name());
                }
                plugin.filters().set(p, set);
            }
            case "clear" -> {
                plugin.filters().set(p, new LinkedHashSet<>());
                plugin.messages().send(p, "filter.cleared");
            }
            case "list" -> plugin.messages().send(p, "filter.list", "list",
                    set.isEmpty() ? "-" : set.stream().map(Material::name).collect(Collectors.joining(", ")));
            default -> plugin.messages().send(p, "filter.usage");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("add", "remove", "clear", "list"))
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
        }
        return out;
    }
}
