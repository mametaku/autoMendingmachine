package mametaku.automendingmachine;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.image.ImageProducer;
import java.util.HashMap;
import java.util.Map;


public final class AutoMendingmachine extends JavaPlugin implements Listener {

    Map itemAmount = new HashMap<Player, Integer>();//GUIにいれたアイテム数をプレイヤーごとに管理する

    @Override
    public void onEnable() {
        getLogger().info("autoexpmachinerun.");
        getServer().getPluginManager().registerEvents(this, this);
        // config.ymlが存在しない場合はファイルに出力します。
        saveDefaultConfig();
        // config.ymlを読み込みます。
        FileConfiguration config = getConfig();
        reloadConfig();
        if (config.getBoolean("mode")) {
            getLogger().info("autoexpmachine not run.");
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
            if (item != null && item.getType() == Material.STICK) {
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
        Integer amount = (Integer) itemAmount.get(player);
        while (amount / 64 != 0){
            if (amount < 64){
                ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE, amount % 64);
                player.getInventory().addItem(item);
            }
            ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE, amount - 64);
            player.getInventory().addItem(item);
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
        if(event.getView().getTitle() == "EXPtank" ) {
            Integer amount = 0;
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i) != null) {
                    if (inv.getItem(i).getType() == Material.EXPERIENCE_BOTTLE) {
                        amount += inv.getItem(i).getAmount();
                    }
                }
            }
            if (amount > 0) {
                p.sendMessage(amount + "個入れました");
                itemAmount.put(p, amount);
            }
        }
    }
}