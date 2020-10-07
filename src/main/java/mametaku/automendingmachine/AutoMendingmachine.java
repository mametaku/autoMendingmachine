package mametaku.automendingmachine;

import jdk.nashorn.internal.ir.Block;
import jdk.nashorn.internal.ir.CallNode;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;

import java.util.*;


public final class AutoMendingmachine extends JavaPlugin implements Listener {

    Map<Player, Integer> itemAmount = new HashMap<>();//GUIにいれたアイテム数をプレイヤーごとに管理する

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player p = (Player) sender;

        if (args.length == 0){
            ItemStack item = new ItemStack(Material.COMPASS, 1);

            ItemMeta itemmeta = item.getItemMeta();
            itemmeta.setDisplayName("自動修繕供給装置");
            ArrayList<String> lore = new ArrayList<String>();
            lore.add("自動的に経験値瓶を消費修繕してくれる装置。オフハンドに持つ");
            itemmeta.setLore(lore);

            p.getInventory().addItem(item);
            return true;
        }

        if (!p.hasPermission("man10wiki.use")) {
            p.sendMessage("Unknown command. Type \"/help\" for help.");
            return true;
        }
        if (p.hasPermission("automending.reload")) {
            if (args[0].equalsIgnoreCase("reload")) {
                return true;
            }
            return true;
        }
        return true;
    }

    @Override
    public void onEnable() {
        getLogger().info("autoexpmachinerun.");
        getServer().getPluginManager().registerEvents(this, this);
        // config.ymlが存在しない場合はファイルに出力します。
        saveDefaultConfig();
        // config.ymlを読み込みます。
        FileConfiguration config = getConfig();
        reloadConfig();
        getCommand("automending").setExecutor(this);

        if (config.getBoolean("mode")) {
            getLogger().info("automendingmachine not run.");
        }
    }




    @EventHandler
    public void onPlayerClicks(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();
        FileConfiguration config = getConfig();
        if (!config.getBoolean("mode")) {
            getLogger().info("autoexpmachine is not run.");
            return;
        }
        if (action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)) {
                    if (item.getType() == Material.IRON_NUGGET){
                        player.sendMessage("経験値瓶を入れてください");
                        openGUI(player);
                    }

        }
    }


    //GUIを開かせる
    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, "EXPtank");
        FileConfiguration config = getConfig();
        if (!config.getBoolean("mode")) {
            getLogger().info("autoexpmachine not run.");
            return;
        }
        player.openInventory(inv);
        if (itemAmount.get(player) != null){
            Integer amount = (Integer) itemAmount.get(player);
            ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE, amount);
            inv.addItem(item);
        }
    }

    @EventHandler
    public void onItemClick(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();
        String q = event.getView().getTitle();
        FileConfiguration config = getConfig();
        if (!config.getBoolean("mode")) {
            getLogger().info("autoexpmachine not run.");
            return;
        }
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() != Material.EXPERIENCE_BOTTLE) {
            if ("EXPtank".equals(q)) {
                if (event.getCurrentItem() != null){
                    p.sendMessage("入れられるのは経験値瓶のみです！");
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void closeGUI(InventoryCloseEvent event){
        Player p = (Player) event.getPlayer();
        Inventory inv = event.getInventory();
        if(event.getView().getTitle().equals("EXPtank")) {
            Integer amount = 0;
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i) != null) {
                    if (Objects.requireNonNull(inv.getItem(i)).getType() == Material.EXPERIENCE_BOTTLE) {
                        amount += Objects.requireNonNull(inv.getItem(i)).getAmount();
                    }
                }
            }
            if (amount >= 0) {
                if (amount > 0){
                    p.sendMessage(amount + "個入れました");
                }
                itemAmount.put(p, amount);
            }
        }
    }

    @EventHandler
    public void AutoMending(BlockBreakEvent event) {
        Player p = event.getPlayer();
        Material name = p.getInventory().getItemInMainHand().getType();
        Random ran = new Random();
        int nowDurability = name.getMaxDurability()-p.getInventory().getItemInMainHand().getDurability();
        int maxDurability = name.getMaxDurability();
        Integer amount = itemAmount.get(p);


        if (amount == 0) return;
        Inventory i = p.getInventory();
        if (name.getMaxDurability() > 30){
            if (nowDurability < maxDurability * 0.8){
                if (p.getInventory().getItemInMainHand().getEnchantments().containsKey(Enchantment.MENDING)) {
                    while (nowDurability < maxDurability * 0.8){
                        amount = itemAmount.get(p);
                        if(amount == 0){
                            break;
                        }
                        p.giveExp(ran.nextInt(8)+3,true);
                        amount--;
                        itemAmount.put(p, amount);
                        nowDurability = name.getMaxDurability()-p.getInventory().getItemInMainHand().getDurability();
                    }
                }
            }
        }
    }
}