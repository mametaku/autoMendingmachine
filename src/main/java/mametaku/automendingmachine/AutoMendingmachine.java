package mametaku.automendingmachine;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public final class AutoMendingmachine extends JavaPlugin implements Listener {

    Map<Player, Integer> itemAmount = new HashMap<>();//GUIにいれたアイテム数をプレイヤーごとに管理する
    MySQLManager data = new MySQLManager(this,"automendingmachine");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player p = (Player) sender;

        if (args.length == 0){
            ItemStack item = new ItemStack(Material.DIAMOND_HOE, 1);
            ItemMeta itemmeta = item.getItemMeta();
            itemmeta.setUnbreakable(true);
            itemmeta.setCustomModelData(10);
            itemmeta.setDisplayName("§d§l自動修繕供給装置");
            ArrayList<String> lore = new ArrayList<String>();
            lore.add("§f§l自動的に経験値瓶を消費修繕してくれる装置。");
            lore.add("§f§lオフハンドに持って右クリックで装置を開く。");
            itemmeta.setLore(lore);
            item.setItemMeta(itemmeta);

            p.getInventory().addItem(item);
            return true;
        }

        if (!p.hasPermission("automending.use")) {
            p.sendMessage("§3§l[§a§lAutoMendingMachine§3§l]§f§lあなたはまだその機能を使えません！");
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
        getLogger().info("automendingmachine is run.");
        getServer().getPluginManager().registerEvents(this, this);
        // config.ymlが存在しない場合はファイルに出力します。
        saveDefaultConfig();
        // config.ymlを読み込みます。
        FileConfiguration config = getConfig();
        reloadConfig();
        getCommand("amm").setExecutor(this);
        if (!config.getBoolean("mode")) {
            getLogger().info("automendingmachine is not run.");
        }
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        String uuid = event.getPlayer().getUniqueId().toString();
            try {
                ResultSet rs = data.query(String.format("select amount from uuid_to_amount where uuid='" + uuid + "';"));
                rs.next();
                Integer amount = rs.getInt("amount");
                itemAmount.put(p,amount);
            } catch (Exception e) {

        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        Player p = event.getPlayer();
        String uuid = event.getPlayer().getUniqueId().toString();
        String player = event.getPlayer().getName();
        if (itemAmount.get(p) == null) return;
        String amount = itemAmount.get(p).toString();
        ResultSet rs = data.query(String.format("select amount from uuid_to_amount where uuid='" + uuid + "';"));
        try {
            if (!rs.next()) {
                data.execute(String.format("insert into uuid_to_amount values('" + uuid + "', '" + player + "', " + amount + ");"));
            }else {
                data.execute(String.format("update uuid_to_amount set player='"+ player +"', amount="+ amount +" where uuid='"+ uuid +"';"));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }




    @EventHandler
    public void onPlayerClicks(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        Action action = event.getAction();
        FileConfiguration config = getConfig();
        if (!config.getBoolean("mode")) {
            getLogger().info("autoexpmachine is not run.");
            return;
        }
        ItemStack item = new ItemStack(Material.DIAMOND_HOE, 1);

        ItemMeta itemmeta = item.getItemMeta();
        itemmeta.setUnbreakable(true);
        itemmeta.setCustomModelData(10);
        itemmeta.setDisplayName("§d§l自動修繕供給装置");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("§f§l自動的に経験値瓶を消費修繕してくれる装置。");
        lore.add("§f§lオフハンドに持って右クリックで装置を開く。");
        itemmeta.setLore(lore);
        item.setItemMeta(itemmeta);

        if (p.hasPermission("automending.use")){

            if (action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)) {

                if (p.getInventory().getItemInOffHand().getType() == Material.DIAMOND_HOE){

                    if (p.getInventory().getItemInOffHand().getItemMeta().getDisplayName().equals("§d§l自動修繕供給装置")){

                        if (p.getInventory().getItemInOffHand().getItemMeta().getLore().get(0).equals("§f§l自動的に経験値瓶を消費修繕してくれる装置。")){

                            openGUI(p);
                        }
                    }
                }
            }
        }
    }


    //GUIを開かせる
    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§l[EXPtank]§f§l経験値瓶を入れてください");
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
            if ("§l[EXPtank]§f§l経験値瓶を入れてください".equals(q)) {
                if (event.getCurrentItem() != null){
                    p.sendMessage("§3§l[§a§lAutoMendingMachine§3§l]§f§l入れられるのは経験値瓶のみです！");
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void closeGUI(InventoryCloseEvent event){
        Player p = (Player) event.getPlayer();
        Inventory inv = event.getInventory();
        if(event.getView().getTitle().equals("§l[EXPtank]§f§l経験値瓶を入れてください")){
            Integer amount = 0;
            for (int i = 0; i < inv.getSize(); i++) {
                if (inv.getItem(i) != null) {
                    if (Objects.requireNonNull(inv.getItem(i)).getType() == Material.EXPERIENCE_BOTTLE) {
                        amount += Objects.requireNonNull(inv.getItem(i)).getAmount();
                    }
                }
            }
            if (amount >= 0) {
                itemAmount.put(p, amount);
            }
        }
    }

    @EventHandler
    public void AutoMending(BlockBreakEvent event) {
        Player p = event.getPlayer();

        if (!p.hasPermission("automending.use")) return;

        Material name = p.getInventory().getItemInMainHand().getType();
        Random ran = new Random();
        int nowDurability = name.getMaxDurability() - p.getInventory().getItemInMainHand().getDurability();
        int maxDurability = name.getMaxDurability();
        Integer amount = itemAmount.get(p);
        ItemStack offhand = p.getInventory().getItemInOffHand();

        ItemStack item = new ItemStack(Material.DIAMOND_HOE, 1);

        ItemMeta itemmeta = item.getItemMeta();
        itemmeta.setUnbreakable(true);
        itemmeta.setCustomModelData(10);
        itemmeta.setDisplayName("§d§l自動修繕供給装置");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add("§f§l自動的に経験値瓶を消費修繕してくれる装置。");
        lore.add("§f§lオフハンドに持って右クリックで装置を開く。");
        itemmeta.setLore(lore);
        item.setItemMeta(itemmeta);

        if (amount == null) return;

        if (amount == 0) return;

        if (offhand == null) return;

        if (offhand.getType() != Material.DIAMOND_HOE)return;

        if (!(offhand.getItemMeta().getDisplayName().equals("§d§l自動修繕供給装置")))return;

        if (!(offhand.getItemMeta().getLore().get(0).equals("§f§l自動的に経験値瓶を消費修繕してくれる装置。")))return;

        if (!(maxDurability >= 30))return;

        if (!(nowDurability < maxDurability * 0.7)) return;

        if (!(p.getInventory().getItemInMainHand().getEnchantments().containsKey(Enchantment.MENDING))) return;

        while (nowDurability < maxDurability * 0.7) {
            amount = itemAmount.get(p);
            if (amount == 0) {
                break;
            }
            p.giveExp(ran.nextInt(8) + 3, true);
            amount--;
            itemAmount.put(p, amount);
            nowDurability = name.getMaxDurability() - p.getInventory().getItemInMainHand().getDurability();
        }
    }
}