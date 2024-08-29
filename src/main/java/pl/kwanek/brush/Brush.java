package pl.kwanek.brush;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.text.SimpleDateFormat;
import java.util.*;

public class Brush extends JavaPlugin implements CommandExecutor, Listener {

    @Override
    public void onEnable() {
        this.getCommand("brush").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("BrushPlugin enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("BrushPlugin disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("brush")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Usage: /brush [give|timegive]");
                return true;
            }

            if (args[0].equalsIgnoreCase("give")) {
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /brush give [player] [radius]");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }

                int radius;
                try {
                    radius = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Radius must be a number.");
                    return true;
                }

                giveBrush(target, radius);
                sender.sendMessage(ChatColor.GREEN + "Brush given to " + target.getName() + " with radius " + radius);
                return true;
            }

            if (args[0].equalsIgnoreCase("timegive")) {
                if (args.length != 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /brush timegive [player] [radius] [time in minutes]");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }

                int radius;
                int timeMinutes;
                try {
                    radius = Integer.parseInt(args[2]);
                    timeMinutes = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Radius and time must be numbers.");
                    return true;
                }

                giveTimeBrush(target, radius, timeMinutes);
                sender.sendMessage(ChatColor.GREEN + "Time-limited Brush given to " + target.getName() + " with radius " + radius + " for " + timeMinutes + " minutes.");
                return true;
            }

            sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
            return true;
        }
        return false;
    }

    private void giveBrush(Player player, int radius) {
        ItemStack brush = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = brush.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "Brush");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GOLD + "★ " + ChatColor.WHITE + "Owner: " + ChatColor.GREEN + player.getName());
            lore.add(ChatColor.RED + "♥ " + ChatColor.WHITE + "Radius: " + ChatColor.RED + radius + "x1");
            lore.add(ChatColor.BLUE + "⛏ " + ChatColor.WHITE + "Mined blocks: 0");

            meta.setLore(lore);
            brush.setItemMeta(meta);

            player.getInventory().addItem(brush);
        }
    }

    private void giveTimeBrush(Player player, int radius, int timeMinutes) {
        ItemStack brush = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = brush.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "Time-Limited Brush");

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, timeMinutes);
            Date endTime = calendar.getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GOLD + "★ " + ChatColor.WHITE + "Owner: " + ChatColor.GREEN + player.getName());
            lore.add(ChatColor.RED + "♥ " + ChatColor.WHITE + "Radius: " + ChatColor.RED + radius + "x1");
            lore.add(ChatColor.BLUE + "⛏ " + ChatColor.WHITE + "Mined blocks: 0");
            lore.add(ChatColor.GOLD + "⌚ " + ChatColor.WHITE + "Expires at: " + ChatColor.GOLD + sdf.format(endTime));

            meta.setLore(lore);
            brush.setItemMeta(meta);

            player.getInventory().addItem(brush);
        }
    }

    @EventHandler
    public void onPlayerUseBrush(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null && item.getType() == Material.BLAZE_ROD && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();

            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Brush")) {
                if (isExpired(item)) {
                    breakBrush(player, item);
                } else {
                    Block clickedBlock = event.getClickedBlock();
                    if (clickedBlock != null) {
                        int radius = extractRadiusFromLore(meta.getLore());
                        if (radius > 0) {
                            int blocksMinedThisTime = mineBlocks(player, clickedBlock, radius);
                            updateMinedBlocksCounter(item, blocksMinedThisTime);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            ItemStack item = event.getCurrentItem();

            if (item != null && item.getType() == Material.BLAZE_ROD && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Time-Limited Brush")) {
                    if (!event.getClickedInventory().equals(player.getInventory())) {
                        event.setCancelled(true);
                    } else if (event.getSlot() == 40) {
                        event.setCancelled(true);
                    } else {
                        if (isExpired(item)) {
                            breakBrush(player, item);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            for (ItemStack item : event.getNewItems().values()) {
                if (item != null && item.getType() == Material.BLAZE_ROD && item.hasItemMeta()) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Time-Limited Brush")) {
                        if (event.getRawSlots().stream().anyMatch(slot -> slot >= player.getInventory().getSize() || slot == 40)) {
                            event.setCancelled(true);
                        } else {
                            if (isExpired(item)) {
                                breakBrush(player, item);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();

        if (item != null && item.getType() == Material.BLAZE_ROD && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Time-Limited Brush")) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isExpired(ItemStack brush) {
        ItemMeta meta = brush.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            for (String line : lore) {
                if (line.contains("Expires at:")) {
                    String timeStr = ChatColor.stripColor(line).split(": ")[1];
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                    try {
                        Date expirationDate = sdf.parse(timeStr);
                        Calendar now = Calendar.getInstance();
                        Calendar expiration = Calendar.getInstance();
                        expiration.set(Calendar.HOUR_OF_DAY, expirationDate.getHours());
                        expiration.set(Calendar.MINUTE, expirationDate.getMinutes());
                        return now.after(expiration);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private int extractRadiusFromLore(List<String> lore) {
        for (String line : lore) {
            if (line.contains("Radius")) {
                try {
                    String[] parts = ChatColor.stripColor(line).split(" ");
                    String radiusPart = parts[2].split("x")[0];
                    return Integer.parseInt(radiusPart);
                } catch (Exception e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private int mineBlocks(Player player, Block centerBlock, int radius) {
        int blocksMined = 0;
        int adjustedRadius = radius / 2;

        for (int x = -adjustedRadius; x <= adjustedRadius; x++) {
            for (int z = -adjustedRadius; z <= adjustedRadius; z++) {
                Block block = centerBlock.getRelative(x, 0, z);

                if (block.getType() != Material.AIR) {
                    block.breakNaturally(new ItemStack(Material.DIAMOND_PICKAXE));
                    blocksMined++;
                }
            }
        }

        return blocksMined;
    }

    private void updateMinedBlocksCounter(ItemStack brush, int blocksMinedThisTime) {
        ItemMeta meta = brush.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);

                if (line.contains("Mined blocks")) {
                    String[] parts = ChatColor.stripColor(line).split(": ");
                    int currentBlocksMined = Integer.parseInt(parts[1]);

                    currentBlocksMined += blocksMinedThisTime;
                    lore.set(i, ChatColor.BLUE + "⛏ " + ChatColor.WHITE + "Mined blocks: " + currentBlocksMined);
                    break;
                }
            }
            meta.setLore(lore);
            brush.setItemMeta(meta);
        }
    }

    private void breakBrush(Player player, ItemStack brush) {
        player.getInventory().remove(brush);
        player.sendMessage(ChatColor.RED + "Your Brush has expired and has been removed.");
    }
}
