package confusedalex.thegoldeconomy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.ResourceBundle;

public class Converter {

    EconomyImplementer eco;
    ResourceBundle bundle;

    public Converter(EconomyImplementer economyImplementer, ResourceBundle bundle) {
        this.eco = economyImplementer;
        this.bundle = bundle;
    }

    public int getValue(Material material) {
        FileConfiguration config = eco.plugin.getConfig();
        String base = config.getString("base");
        String nugget = config.getString("nugget");
        String ingot = config.getString("ingot");
        String block = config.getString("block");

        if(base != null){
          if(base.equals("nuggets")) {
            if(nugget !=null) {
              if(material.equals(Material.matchMaterial(nugget))) return 1;
            }
            if(ingot != null) {
              if(material.equals(Material.matchMaterial(ingot))) return 9;
            }
            if(block != null) {
              if(material.equals(Material.matchMaterial(block))) return 81;
            }
          }
          if(base.equals("ingots")) {
              if(ingot != null) {
                if (material.equals(Material.matchMaterial(ingot))) return 1;
              }
              if(block != null) {
                if (material.equals(Material.matchMaterial(block))) return 9;
              }
          }
          if(base.equals("blocks")) {
              if(block != null) {
                if (material.equals(Material.matchMaterial(block))) return 1;
              }
          }
        }

        return 0;
    }

    public boolean isNotGold(Material material) {
        FileConfiguration config = eco.plugin.getConfig();
        String nuggetId = config.getString("nugget");
        String ingotId = config.getString("ingot");
        String blockId = config.getString("block");

        if(nuggetId != null) {
          Material nugget = Material.getMaterial(nuggetId);
          if(material.equals(nugget)) return false;
        }
        if(ingotId != null) {
          Material ingot = Material.getMaterial(ingotId);
          if(material.equals(ingot)) return false;
        }
        if(blockId != null) {
          Material block = Material.getMaterial(blockId);
          if(material.equals(block)) return false;
        };
      return true;
    }

    public int getInventoryValue(Player player){
        int value = 0;

        // calculating the value of all the gold in the inventory to nuggets
        for (ItemStack item : player.getInventory()) {
            if (item == null) continue;
            Material material = item.getType();

            if (isNotGold(material)) continue;

            value += (getValue(material) * item.getAmount());

        }
        return value;
    }

    public void remove(Player player, int amount){
        int value = 0;

        // calculating the value of all the gold in the inventory to nuggets
        for (ItemStack item : player.getInventory()) {
            if (item == null) continue;
            Material material = item.getType();

            if (isNotGold(material)) continue;

            value += (getValue(material) * item.getAmount());
        }

        // Checks if the Value of the items is greater than the amount to deposit
        if (value < amount) return;

        // Deletes all gold items
        for (ItemStack item : player.getInventory()) {
            if (item == null) continue;
            if (isNotGold(item.getType())) continue;

            item.setAmount(0);
            item.setType(Material.AIR);
        }

        int newBalance = value - amount;
        give(player, newBalance);
    }

    public void give(Player player, int value){
        FileConfiguration config = eco.plugin.getConfig();
        String base = config.getString("base");
        String nuggetId = config.getString("nugget");
        String ingotId = config.getString("ingot");
        String blockId = config.getString("block");

        boolean warning = false;

        assert nuggetId != null;
        int nuggetValue = getValue(Material.getMaterial(nuggetId));
        assert ingotId != null;
        int ingotValue = getValue(Material.getMaterial(ingotId));
        assert blockId != null;
        int blockValue = getValue(Material.getMaterial(blockId));

        if(base.equals("blocks")) {
            if (value / blockValue > 0) {
                HashMap<Integer, ItemStack> blocks = player.getInventory().addItem(new ItemStack(Material.getMaterial(blockId), value / blockValue));
                for (ItemStack item : blocks.values()) {
                    if (item != null && item.getType() == Material.GOLD_BLOCK && item.getAmount() > 0) {
                        player.getWorld().dropItem(player.getLocation(), item);
                        warning = true;
                    }
                }
            }

            value -= (value / blockValue) * blockValue;
        }

        if(base.equals("ingots")) {
            if (value / ingotValue > 0) {
                HashMap<Integer, ItemStack> ingots = player.getInventory().addItem(new ItemStack(Material.getMaterial(ingotId), value / ingotValue));
                for (ItemStack item : ingots.values()) {
                    if (item != null && item.getType() == Material.GOLD_INGOT && item.getAmount() > 0) {
                        player.getWorld().dropItem(player.getLocation(), item);
                        warning = true;
                    }
                }
            }

            value -= (value / ingotValue) * ingotValue;
        }

        if (base.equals("nuggets") && value > 0) {
            HashMap<Integer, ItemStack> nuggets = player.getInventory().addItem(new ItemStack(Material.getMaterial(ingotId), value));
            for (ItemStack item : nuggets.values()) {
                if (item != null && item.getType() == Material.getMaterial(ingotId) && item.getAmount() > 0) {
                    player.getWorld().dropItem(player.getLocation(), item);
                    warning = true;
                }
            }
        }

        if (warning) eco.util.sendMessageToPlayer(String.format(bundle.getString("warning.drops")), player);
    }


    public void withdrawAll(Player player){
        String uuid = player.getUniqueId().toString();

        // searches in the Hashmap for the balance, so that a player can't withdraw gold from his Inventory
        int value = eco.bank.getAccountBalance(player.getUniqueId().toString());
        eco.bank.setBalance(uuid, (0));

        give(player, value);
    }

    public void withdraw(Player player, int nuggets){
        String uuid = player.getUniqueId().toString();
        int oldBalance = eco.bank.getAccountBalance(player.getUniqueId().toString());

        // Checks balance in HashMap
        if (nuggets > eco.bank.getPlayerBank().get(player.getUniqueId().toString())) {
            eco.util.sendMessageToPlayer(bundle.getString("error.notenoughmoneywithdraw"), player);
            return;
        }
        eco.bank.setBalance(uuid, (oldBalance - nuggets));

        give(player, nuggets);

    }

    public void depositAll(Player player){
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
        int value = 0;

        for (ItemStack item : player.getInventory()) {
            if (item == null) continue;
            Material material = item.getType();

            if (isNotGold(material)) continue;

            value = value + (getValue(material) * item.getAmount());
            if (getValue(material) != 0) item.setAmount(0);
            item.setType(Material.AIR);
        }

        eco.depositPlayer(offlinePlayer, value);

    }

    public void deposit(Player player, int nuggets){
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());

        remove(player, nuggets);
        eco.depositPlayer(offlinePlayer, nuggets);
    }
}
