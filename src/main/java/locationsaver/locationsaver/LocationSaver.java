package locationsaver.locationsaver;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LocationSaver extends JavaPlugin {
    private File dataFile;
    private FileConfiguration config;
    private Map<String, Map<String, Location>> playerLocations;

    @Override
    public void onEnable() {
        dataFile = new File(getDataFolder(), "locations.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(dataFile);
        playerLocations = new HashMap<>();
        loadLocations();
        Metrics metrics = new Metrics(this, 21773);
        this.getLogger().info("Thank you for using the LocationSaver plugin! If you enjoy using this plugin, please consider making a donation to support the development. You can donate at: https://donate.ashkiano.com");
    }

    @Override
    public void onDisable() {
        saveLocations();
        getLogger().info("LocationSaver has been disabled.");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use these commands!");
            return true;
        }

        Player player = (Player) sender;
        String playerName = player.getName();
        Map<String, Location> locations = playerLocations.computeIfAbsent(playerName, k -> new HashMap<>());

        switch (command.getName().toLowerCase()) {
            case "list":
                if (locations.isEmpty()) {
                    sender.sendMessage(ChatColor.RED + "You have no saved locations.");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "Your saved locations:");
                    locations.keySet().forEach(name -> sender.sendMessage(ChatColor.YELLOW + "- " + name));
                }
                break;
            case "save":
                if (args.length == 1) {
                    Location loc = player.getLocation();
                    loc.setX(Math.floor(loc.getX()));
                    loc.setY(Math.floor(loc.getY()));
                    loc.setZ(Math.floor(loc.getZ()));
                    loc.setPitch(0);
                    loc.setYaw(0);
                    locations.put(args[0], loc);
                    playerLocations.put(playerName, locations);
                    sender.sendMessage(ChatColor.GREEN + "Location saved as " + args[0] + ".");
                    saveLocations();
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /save <name>");
                }
                break;
            case "show":
                if (args.length == 1) {
                    Location loc = locations.get(args[0]);
                    if (loc != null) {
                        sender.sendMessage(ChatColor.YELLOW + "Location " + args[0] + ": " +
                                "World: " + loc.getWorld().getName() +
                                ", X: " + loc.getX() +
                                ", Y: " + loc.getY() +
                                ", Z: " + loc.getZ());
                    } else {
                        sender.sendMessage(ChatColor.RED + "No location found with name: " + args[0]);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /show <name>");
                }
                break;
            case "delete":
                if (args.length == 1) {
                    if (locations.remove(args[0]) != null) {
                        playerLocations.put(playerName, locations);
                        sender.sendMessage(ChatColor.GREEN + "Location " + args[0] + " deleted.");
                        saveLocations();
                    } else {
                        sender.sendMessage(ChatColor.RED + "No location found with name: " + args[0]);
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /delete <name>");
                }
                break;
        }
        return true;
    }

    private void loadLocations() {
        try {
            config = YamlConfiguration.loadConfiguration(dataFile);
            config.getKeys(false).forEach(playerName -> {
                Map<String, Location> locations = new HashMap<>();
                config.getConfigurationSection(playerName).getKeys(false).forEach(name -> {
                    Location loc = (Location) config.get(playerName + "." + name);
                    locations.put(name, loc);
                });
                playerLocations.put(playerName, locations);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveLocations() {
        try {
            playerLocations.forEach((playerName, locations) -> {
                locations.forEach((name, loc) -> {
                    Map<String, Object> simpleLoc = new HashMap<>();
                    simpleLoc.put("world", loc.getWorld().getName());
                    simpleLoc.put("x", (int) loc.getX());
                    simpleLoc.put("y", (int) loc.getY());
                    simpleLoc.put("z", (int) loc.getZ());
                    config.set(playerName + "." + name, simpleLoc);
                });
            });
            config.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}