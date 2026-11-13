package org.github.sawervatoryy.mirageEnchants.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.enchant.CustomEnchant;
import org.github.sawervatoryy.mirageEnchants.gui.EditorGui;
import org.github.sawervatoryy.mirageEnchants.util.Watermark;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /mirage и /enchedit. MirageStudio | sawervatoryy */
public final class MirageCommand implements CommandExecutor, TabCompleter {
    private final MirageEnchants plugin;

    public MirageCommand(MirageEnchants plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("enchedit")) {
            openEditor(sender);
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            plugin.messages().sendList(sender, "help");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("about")) {
            plugin.messages().sendList(sender, "about", "version", plugin.getPluginMeta().getVersion(),
                    "studio", Watermark.STUDIO, "author", Watermark.AUTHOR, "discord", Watermark.DISCORD);
            return true;
        }
        if (sub.equals("editor")) { openEditor(sender); return true; }

        if (!sender.hasPermission("mirageenchants.admin")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        switch (sub) {
            case "reload" -> {
                plugin.reloadAll();
                plugin.messages().send(sender, "reload-done");
            }
            case "list" -> {
                plugin.messages().sendList(sender, "list-header");
                for (CustomEnchant ce : plugin.registry().all()) {
                    plugin.messages().send(sender, "list-line", "id", ce.id(), "name", ce.nameRaw(),
                            "max", String.valueOf(ce.maxLevel()),
                            "status", plugin.messages().raw(ce.enabled() ? "status-on" : "status-off"));
                }
            }
            case "book" -> book(sender, args);
            case "add" -> add(sender, args);
            case "remove" -> remove(sender, args);
            default -> plugin.messages().sendList(sender, "help");
        }
        return true;
    }

    private void openEditor(CommandSender sender) {
        if (!(sender instanceof Player p)) { plugin.messages().send(sender, "player-only"); return; }
        if (!p.hasPermission("mirageenchants.editor")) { plugin.messages().send(p, "no-permission"); return; }
        new EditorGui(plugin, p).open();
    }

    private int parseLevel(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    /** /mirage book <игрок> <чар> [уровень] [кол-во] */
    private void book(CommandSender sender, String[] args) {
        if (args.length < 3) { plugin.messages().send(sender, "usage-book"); return; }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { plugin.messages().send(sender, "player-not-found"); return; }
        CustomEnchant ce = plugin.registry().get(args[2]);
        if (ce == null) { plugin.messages().send(sender, "unknown-enchant", "id", args[2]); return; }
        int level = Math.max(1, Math.min(ce.maxLevel(), args.length > 3 ? parseLevel(args[3], 1) : 1));
        int amount = Math.max(1, Math.min(64, args.length > 4 ? parseLevel(args[4], 1) : 1));
        for (int i = 0; i < amount; i++) {
            ItemStack book = plugin.items().createBook(ce, level);
            for (ItemStack left : target.getInventory().addItem(book).values())
                target.getWorld().dropItem(target.getLocation(), left);
        }
        plugin.messages().send(sender, "book-given", "player", target.getName(), "name", ce.nameRaw(),
                "level", String.valueOf(level), "amount", String.valueOf(amount));
        plugin.messages().send(target, "book-received", "name", ce.nameRaw(), "level", String.valueOf(level));
    }

    /** /mirage add <чар> [уровень] [force] */
    private void add(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { plugin.messages().send(sender, "player-only"); return; }
        if (args.length < 2) { plugin.messages().send(sender, "usage-add"); return; }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) { plugin.messages().send(p, "hold-item"); return; }
        CustomEnchant ce = plugin.registry().get(args[1]);
        if (ce == null) { plugin.messages().send(p, "unknown-enchant", "id", args[1]); return; }
        boolean force = args.length > 3 && args[3].equalsIgnoreCase("force");
        int level = Math.max(1, args.length > 2 ? parseLevel(args[2], 1) : 1);
        if (!force) {
            level = Math.min(level, ce.maxLevel());
            if (!plugin.items().canApply(hand, ce)) { plugin.messages().send(p, "add-cannot", "name", ce.nameRaw()); return; }
            CustomEnchant other = plugin.items().conflict(hand, ce);
            if (other != null) { plugin.messages().send(p, "add-conflict", "name", ce.nameRaw(), "other", other.nameRaw()); return; }
        }
        plugin.items().setLevel(hand, ce, level);
        p.getInventory().setItemInMainHand(hand);
        plugin.messages().send(p, "add-done", "name", ce.nameRaw(), "level", String.valueOf(level));
    }

    /** /mirage remove <чар> */
    private void remove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { plugin.messages().send(sender, "player-only"); return; }
        if (args.length < 2) { plugin.messages().send(sender, "usage-remove"); return; }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) { plugin.messages().send(p, "hold-item"); return; }
        CustomEnchant ce = plugin.registry().get(args[1]);
        if (ce == null) { plugin.messages().send(p, "unknown-enchant", "id", args[1]); return; }
        if (plugin.items().rawLevel(hand, ce) <= 0) { plugin.messages().send(p, "remove-none", "name", ce.nameRaw()); return; }
        plugin.items().setLevel(hand, ce, 0);
        p.getInventory().setItemInMainHand(hand);
        plugin.messages().send(p, "remove-done", "name", ce.nameRaw());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (!cmd.getName().equalsIgnoreCase("mirage")) return out;
        if (args.length == 1) {
            for (String s : List.of("help", "about", "editor", "list", "book", "add", "remove", "reload"))
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            return out;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("book")) {
            if (args.length == 2) Bukkit.getOnlinePlayers().forEach(p -> { if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(p.getName()); });
            else if (args.length == 3) enchIds(args[2], out);
            else if (args.length == 4) levels(args[2], out);
        } else if (sub.equals("add") || sub.equals("remove")) {
            if (args.length == 2) enchIds(args[1], out);
            else if (args.length == 3 && sub.equals("add")) levels(args[1], out);
            else if (args.length == 4 && sub.equals("add")) out.add("force");
        }
        return out;
    }

    private void enchIds(String prefix, List<String> out) {
        for (CustomEnchant ce : plugin.registry().all())
            if (ce.id().startsWith(prefix.toLowerCase(Locale.ROOT))) out.add(ce.id());
    }

    private void levels(String id, List<String> out) {
        CustomEnchant ce = plugin.registry().get(id);
        if (ce == null) return;
        for (int i = 1; i <= Math.min(ce.maxLevel(), 20); i++) out.add(String.valueOf(i));
    }
}
