package me.stumper66.spawnercontrol.command;

import me.lokka30.microlib.messaging.MessageUtils;
import me.stumper66.spawnercontrol.SpawnerControl;
import me.stumper66.spawnercontrol.SpawnerInfo;
import me.stumper66.spawnercontrol.Utils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CommandProcessor implements CommandExecutor, TabCompleter  {
    public CommandProcessor(final SpawnerControl main) {
        this.main = main;
        this.debugCommand = new DebugCommand(main);
    }

    private final SpawnerControl main;
    private final DebugCommand debugCommand;

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd, final @NotNull String label, final @NotNull String @NotNull [] args) {
        if (args.length == 0){
            showSyntax(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()){
            case "debug":
                debugCommand.onCommand(sender, args);
                break;
            case "reload":
                doReload(sender);
                break;
            case "info":
                showInfo(sender);
                break;
            case "spawners":
                showSpawners(sender);
                break;
            case "enable":
                enableOrDisable(sender, true);
                break;
            case "disable":
                enableOrDisable(sender, false);
                break;
            case "label":
                showOrUpdateLabel(sender, label, args);
                break;
            default:
                showSyntax(sender, label);
                break;
        }

        return true;
    }

    private void showOrUpdateLabel(@NotNull final CommandSender sender, final @NotNull String label, final @NotNull String @NotNull [] args){
        if (!hasPermission("spawnercontrol.label", sender)) return;

        if (!(sender instanceof Player)){
            sender.sendMessage("Command must be run from by a player");
            return;
        }

        CreatureSpawner cs = null;
        final Player player = (Player) sender;
        BlockIterator blocks = new BlockIterator(player, 20);
        while (blocks.hasNext()){
            final Block block = blocks.next();
            if (block.getType() != Material.SPAWNER) continue;

            cs = (CreatureSpawner) block.getState();
            break;
        }

        if (cs == null){
            sender.sendMessage("You must be looking at a spawner first");
            return;
        }

        final SpawnerInfo info = new SpawnerInfo(cs, main.spawnerOptions);
        final String customName = info.getSpawnerCustomName(main);
        if (customName == null)
            sender.sendMessage("spawner " + Utils.showSpawnerLocation(cs) + " has no name currently");
        else
            sender.sendMessage("spawner " + Utils.showSpawnerLocation(cs) + " name: " + customName);

        if (args.length == 1){
            sender.sendMessage("to change the name, enter: /" + label + " <new name>");
        }

        final StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++){
            if (sb.length() > 0) sb.append(" ");
            sb.append(args[i]);
        }

        final String newName = sb.toString().trim();
        if (newName.isEmpty()) return;
        if (customName != null && customName.equals(newName)){
            sender.sendMessage("spawner new name is the same as the old name");
            return;
        }

        info.setSpawnerCustomName(newName, main);
        main.spawnerProcessor.spawnerGotRenamed(cs, customName, newName);
        sender.sendMessage("spawner name updated to: " + newName);
    }

    private void showSyntax(@NotNull final CommandSender sender, @NotNull final String label) {
        sender.sendMessage(MessageUtils.colorizeAll("&b&lSpawnerControl: &7Syntax: &b/" + label + " reload | info | enable | disable | spawners"));
    }

    private void enableOrDisable(@NotNull final CommandSender sender, final boolean doEnable){
        if (!hasPermission("spawnercontrol.toggle", sender)) return;

        if (doEnable) {
            if (main.isEnabled){
                sender.sendMessage("SpawnerControl was already enabled!");
                return;
            }

            main.isEnabled = true;
            sender.sendMessage("SpawnerControl has been enabled");
        }
        else {
            if (!main.isEnabled){
                sender.sendMessage("SpawnerControl was already disabled!");
                return;
            }

            main.isEnabled = false;
            sender.sendMessage("SpawnerControl has been disabled");
        }
    }

    private void showInfo(@NotNull final CommandSender sender) {
        final String version = main.getDescription().getVersion();
        final String description = main.getDescription().getDescription();

        sender.sendMessage(MessageUtils.colorizeAll("\n" +
                "&b&SpawnerControl &fv" + version + "&r\n" +
                "&7&o" + description + "&r\n" +
                "&7Created by Stumper66"));
    }

    private void showSpawners(@NotNull final CommandSender sender){
        if (!hasPermission("spawnercontrol.spawners", sender)) return;

        sender.sendMessage("All known spawners count: " + main.spawnerProcessor.getAllKnownSpawnersCount());
        final Collection<SpawnerInfo> spawnerInfos = main.spawnerProcessor.getMonitoredSpawners();
        if (spawnerInfos.isEmpty()){
            sender.sendMessage("There are no spawners currently monitored");
            return;
        }

        final StringBuilder sb = new StringBuilder();
        for (final SpawnerInfo info : spawnerInfos){
            final CreatureSpawner cs = info.getCs();
            if (sb.length() > 0) sb.append("\n");
            sb.append(Utils.showSpawnerLocation(cs));
        }

        sender.sendMessage("Active spawners: " + main.spawnerProcessor.getActiveSpawnersCount() + "\n" + sb);
    }

    private void doReload(@NotNull final CommandSender sender) {
        if (!hasPermission("spawnercontrol.reload", sender)) return;

        sender.sendMessage(MessageUtils.colorizeAll("&b&lSpawnerControl: &7Reloading config..."));
        main.loadConfig(true);
        sender.sendMessage(MessageUtils.colorizeAll("&b&lSpawnerControl: &7Reload complete."));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean hasPermission(final @NotNull String permisison, final @NotNull CommandSender sender){
        if (!sender.hasPermission(permisison)){
            sender.sendMessage(MessageUtils.colorizeAll("&b&lSpawnerControl: &7You don't have permissions to run this command"));
            return false;
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(final @NotNull CommandSender commandSender, final @NotNull Command command, final @NotNull String label, final @NotNull String @NotNull [] args) {
        if (args.length > 1 && "debug".equals(args[0]))
            return debugCommand.onTabComplete(commandSender, args);
        else if (args.length == 1)
            return List.of("debug", "reload", "info", "label", "spawners", "disable", "enable");

        return Collections.emptyList();
    }
}
