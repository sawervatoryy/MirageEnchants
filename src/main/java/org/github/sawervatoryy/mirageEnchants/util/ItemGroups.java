package org.github.sawervatoryy.mirageEnchants.util;

import org.bukkit.Material;

import java.util.Locale;

/** Группы предметов для applies-to. MirageStudio | sawervatoryy */
public final class ItemGroups {
    private ItemGroups() {}

    public static boolean matches(String group, Material m) {
        String n = m.name();
        switch (group.toUpperCase(Locale.ROOT)) {
            case "ALL": return true;
            case "PICKAXE": return n.endsWith("_PICKAXE");
            case "AXE": return n.endsWith("_AXE");
            case "SHOVEL": return n.endsWith("_SHOVEL");
            case "HOE": return n.endsWith("_HOE");
            case "SWORD": return n.endsWith("_SWORD");
            case "TRIDENT": return m == Material.TRIDENT;
            case "BOW": return m == Material.BOW;
            case "CROSSBOW": return m == Material.CROSSBOW;
            case "MACE": return m == Material.MACE;
            case "HELMET": return n.endsWith("_HELMET");
            case "CHESTPLATE": return n.endsWith("_CHESTPLATE");
            case "LEGGINGS": return n.endsWith("_LEGGINGS");
            case "BOOTS": return n.endsWith("_BOOTS");
            case "ELYTRA": return m == Material.ELYTRA;
            case "ARMOR": return n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE")
                    || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS") || m == Material.ELYTRA;
            case "TOOLS": return n.endsWith("_PICKAXE") || n.endsWith("_AXE")
                    || n.endsWith("_SHOVEL") || n.endsWith("_HOE");
            case "WEAPONS": return n.endsWith("_SWORD") || n.endsWith("_AXE") || m == Material.TRIDENT
                    || m == Material.BOW || m == Material.CROSSBOW || m == Material.MACE;
            default: return n.equalsIgnoreCase(group);
        }
    }
}
