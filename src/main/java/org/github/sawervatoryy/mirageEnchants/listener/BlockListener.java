package org.github.sawervatoryy.mirageEnchants.listener;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.util.RayTraceResult;
import org.github.sawervatoryy.mirageEnchants.MirageEnchants;
import org.github.sawervatoryy.mirageEnchants.enchant.CustomEnchant;
import org.github.sawervatoryy.mirageEnchants.enchant.EnchantItems;
import org.github.sawervatoryy.mirageEnchants.enchant.EnchantRegistry;
import org.github.sawervatoryy.mirageEnchants.util.ItemGroups;
import org.github.sawervatoryy.mirageEnchants.util.ItemUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Бур, Мега-бур, Дровосек, Магнетизм, Автоплавка, Опытный, Неразрушимость, Деликатный, Фермер, Посев, Фильтр. */
public final class BlockListener implements Listener {
    private final MirageEnchants plugin;
    private final EnchantItems items;
    private final EnchantRegistry registry;
    private final Set<UUID> busy = new HashSet<>();
    private final Map<Material, Material> smeltCache = new HashMap<>();
    private final Random random = new Random();

    public BlockListener(MirageEnchants plugin) {
        this.plugin = plugin;
        this.items = plugin.items();
        this.registry = plugin.registry();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (tool.getType().isAir()) return;
        Map<String, Integer> lv = items.active(tool);
        if (lv.isEmpty()) return;
        Block b = e.getBlock();

        // Деликатный: не ломает неспелые культуры
        Integer del = lv.get("delicate");
        if (del != null) {
            CustomEnchant ce = registry.get("delicate");
            if (ce.settingList("crops").contains(b.getType().name())
                    && b.getBlockData() instanceof Ageable a && a.getAge() < a.getMaximumAge()) {
                e.setCancelled(true);
                return;
            }
        }

        // Неразрушимость: нельзя использовать при малой прочности
        Integer ind = lv.get("indestructible");
        if (ind != null) {
            int min = (int) registry.get("indestructible").setting("min-durability", ind, 10);
            if (ItemUtil.remaining(tool) < min) {
                e.setCancelled(true);
                plugin.messages().actionBar(p, "indestructible-blocked", "durability", String.valueOf(min));
                return;
            }
        }

        // Опытный: больше опыта
        Integer exp = lv.get("experienced");
        if (exp != null && e.getExpToDrop() > 0) {
            double pct = registry.get("experienced").setting("bonus-percent", exp, 10.0 * exp);
            e.setExpToDrop((int) Math.round(e.getExpToDrop() * (1 + pct / 100.0)));
        }

        // Посев: повторная посадка
        Integer seed = lv.get("seeding");
        if (seed != null) trySeed(b, seed);

        // Мульти-ломание
        if (busy.contains(p.getUniqueId())) return;
        var cfg = plugin.getConfig();
        if (cfg.getBoolean("multi-break.only-survival", true) && p.getGameMode() != GameMode.SURVIVAL) return;
        if (cfg.getBoolean("multi-break.disable-when-sneaking", true) && p.isSneaking()) return;
        List<Block> targets = collect(p, b, tool, lv);
        if (targets.isEmpty()) return;
        busy.add(p.getUniqueId());
        try {
            for (Block t : targets) {
                ItemStack cur = p.getInventory().getItemInMainHand();
                if (cur.getType().isAir()) break;
                if (lv.containsKey("indestructible")
                        && ItemUtil.remaining(cur) < (int) registry.get("indestructible").setting("min-durability", 1, 10)) break;
                p.breakBlock(t);
            }
        } finally {
            busy.remove(p.getUniqueId());
        }
    }

    private void trySeed(Block b, int level) {
        Material mat = b.getType();
        CustomEnchant ce = registry.get("seeding");
        if (!ce.settingList("crops").contains(mat.name())) return;
        if (!(b.getBlockData() instanceof Ageable a) || a.getAge() < a.getMaximumAge()) return;
        double chance = ce.setting("chance", level, 50);
        if (random.nextDouble() * 100.0 >= chance) return;
        Location loc = b.getLocation();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Block blk = loc.getBlock();
            if (!blk.getType().isAir()) return;
            Material need = mat == Material.NETHER_WART ? Material.SOUL_SAND : Material.FARMLAND;
            if (blk.getRelative(BlockFace.DOWN).getType() == need) blk.setType(mat);
        });
    }

    private List<Block> collect(Player p, Block origin, ItemStack tool, Map<String, Integer> lv) {
        Material type = tool.getType();
        if (ItemGroups.matches("PICKAXE", type)) {
            String id = lv.containsKey("mega_drill") ? "mega_drill" : lv.containsKey("drill") ? "drill" : null;
            if (id != null) {
                CustomEnchant ce = registry.get(id);
                int l = lv.get(id);
                return shape(p, origin, tool, (int) ce.setting("size", l, 3), (int) ce.setting("depth", l, 1));
            }
        }
        if (ItemGroups.matches("AXE", type) && lv.containsKey("lumberjack") && Tag.LOGS.isTagged(origin.getType())) {
            CustomEnchant ce = registry.get("lumberjack");
            int max = (int) ce.setting("max-blocks", lv.get("lumberjack"), 8);
            return logs(origin, max, ce.settingBool("require-leaves", false));
        }
        return List.of();
    }

    private List<Block> shape(Player p, Block origin, ItemStack tool, int size, int depth) {
        BlockFace face = faceOf(p, origin);
        int nx = face.getModX(), ny = face.getModY(), nz = face.getModZ();
        int[] u, v;
        if (nx != 0) { u = new int[]{0, 1, 0}; v = new int[]{0, 0, 1}; }
        else if (ny != 0) { u = new int[]{1, 0, 0}; v = new int[]{0, 0, 1}; }
        else { u = new int[]{1, 0, 0}; v = new int[]{0, 1, 0}; }
        int r = Math.max(0, (size - 1) / 2);
        List<Block> out = new ArrayList<>();
        for (int k = 0; k < depth; k++) {
            for (int a = -r; a <= r; a++) {
                for (int c = -r; c <= r; c++) {
                    if (k == 0 && a == 0 && c == 0) continue;
                    int x = -nx * k + u[0] * a + v[0] * c;
                    int y = -ny * k + u[1] * a + v[1] * c;
                    int z = -nz * k + u[2] * a + v[2] * c;
                    Block t = origin.getRelative(x, y, z);
                    if (breakable(t, tool)) out.add(t);
                }
            }
        }
        return out;
    }

    private BlockFace faceOf(Player p, Block origin) {
        RayTraceResult rr = p.rayTraceBlocks(8.0, FluidCollisionMode.NEVER);
        if (rr != null && rr.getHitBlock() != null && rr.getHitBlock().equals(origin) && rr.getHitBlockFace() != null) {
            return rr.getHitBlockFace();
        }
        float pitch = p.getLocation().getPitch();
        if (pitch > 55) return BlockFace.UP;
        if (pitch < -55) return BlockFace.DOWN;
        return p.getFacing().getOppositeFace();
    }

    private boolean breakable(Block t, ItemStack tool) {
        Material m = t.getType();
        if (m.isAir() || t.isLiquid()) return false;
        if (m.getHardness() < 0) return false;
        var cfg = plugin.getConfig();
        if (cfg.getStringList("multi-break.blacklist").contains(m.name())) return false;
        if (cfg.getBoolean("multi-break.only-preferred-tool", true) && !t.isPreferredTool(tool)) return false;
        if (t.getState(false) instanceof TileState) return false;
        return true;
    }

    private List<Block> logs(Block origin, int max, boolean requireLeaves) {
        int limit = Math.max(0, Math.min(max, 512) - 1);
        Set<Block> seen = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        List<Block> out = new ArrayList<>();
        boolean leaves = false;
        queue.add(origin);
        seen.add(origin);
        outer:
        while (!queue.isEmpty()) {
            Block cur = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block n = cur.getRelative(dx, dy, dz);
                        if (Tag.LEAVES.isTagged(n.getType())) leaves = true;
                        if (!seen.add(n)) continue;
                        if (Tag.LOGS.isTagged(n.getType())) {
                            queue.add(n);
                            out.add(n);
                            if (out.size() >= limit) break outer;
                        }
                    }
                }
            }
        }
        if (requireLeaves && !leaves) return List.of();
        return out;
    }

    // ---------------------------------------------------------------- дропы

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent e) {
        Player p = e.getPlayer();
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (tool.getType().isAir()) return;
        Map<String, Integer> lv = items.active(tool);
        if (lv.isEmpty()) return;

        boolean magnet = lv.containsKey("magnetism");
        boolean filter = lv.containsKey("filter");
        boolean farmer = lv.containsKey("farmer");
        boolean smelt = lv.containsKey("autosmelt");
        if (!magnet && !filter && !farmer && !smelt) return;
        if (e.getItems().isEmpty()) return;

        List<ItemStack> out = new ArrayList<>();
        for (Item it : e.getItems()) out.add(it.getItemStack().clone());

        if (filter) {
            Set<Material> f = plugin.filters().get(p);
            if (!f.isEmpty()) out.removeIf(s -> f.contains(s.getType()));
        }

        if (farmer) {
            CustomEnchant ce = registry.get("farmer");
            if (isFarmable(e.getBlockState(), ce)) {
                int mult = (int) Math.max(1, Math.round(ce.setting("multiplier", lv.get("farmer"), 2)));
                List<String> exclude = ce.settingList("exclude");
                for (ItemStack s : out) {
                    if (!exclude.contains(s.getType().name())) s.setAmount(s.getAmount() * mult);
                }
            }
        }

        if (smelt) {
            CustomEnchant ce = registry.get("autosmelt");
            for (ItemStack s : out) {
                if (!smeltAllowed(s.getType(), ce)) continue;
                Material res = smelt(s.getType());
                if (res != s.getType()) s.setType(res);
            }
        }

        e.getItems().clear();
        Location loc = e.getBlock().getLocation().add(0.5, 0.5, 0.5);
        boolean dropLeft = registry.get("magnetism") == null || registry.get("magnetism").settingBool("drop-leftover", true);
        for (ItemStack s : out) {
            for (ItemStack part : split(s)) {
                if (magnet) {
                    Map<Integer, ItemStack> left = p.getInventory().addItem(part);
                    if (dropLeft) for (ItemStack l : left.values()) loc.getWorld().dropItem(loc, l);
                } else {
                    loc.getWorld().dropItem(loc, part);
                }
            }
        }
    }

    private boolean isFarmable(BlockState st, CustomEnchant ce) {
        String n = st.getType().name();
        if (!ce.settingList("crops").contains(n)) return false;
        if (ce.settingList("age-checked").contains(n) && st.getBlockData() instanceof Ageable a) {
            return a.getAge() >= a.getMaximumAge();
        }
        return true;
    }

    private boolean smeltAllowed(Material m, CustomEnchant ce) {
        String n = m.name();
        if (ce.settingList("whitelist").contains(n)) return true;
        if (ce.settingBool("ores-auto", true)) {
            return n.startsWith("RAW_") || n.endsWith("_ORE") || m == Material.ANCIENT_DEBRIS;
        }
        return false;
    }

    private Material smelt(Material in) {
        return smeltCache.computeIfAbsent(in, m -> {
            ItemStack probe = new ItemStack(m);
            Iterator<Recipe> it = plugin.getServer().recipeIterator();
            while (it.hasNext()) {
                if (it.next() instanceof FurnaceRecipe fr && fr.getInputChoice().test(probe)) {
                    return fr.getResult().getType();
                }
            }
            return m;
        });
    }

    private List<ItemStack> split(ItemStack s) {
        List<ItemStack> parts = new ArrayList<>();
        int max = Math.max(1, s.getMaxStackSize());
        int left = s.getAmount();
        while (left > 0) {
            ItemStack c = s.clone();
            c.setAmount(Math.min(max, left));
            parts.add(c);
            left -= c.getAmount();
        }
        return parts;
    }
}
