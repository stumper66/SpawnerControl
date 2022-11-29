package me.stumper66.spawnercontrol.command;

import me.lokka30.microlib.messaging.MessageUtils;
import me.stumper66.spawnercontrol.SpawnerControl;
import me.stumper66.spawnercontrol.SpawnerInfo;
import me.stumper66.spawnercontrol.Utils;
import me.stumper66.spawnercontrol.processing.UpdateOperation;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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
        this.debugCommand = new DebugCommand(main, this);
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
            case "scan_near":
                scanNear(sender);
                break;
            default:
                showSyntax(sender, label);
                break;
        }

        return true;
    }

    private void scanNear(@NotNull final CommandSender sender){
        if (!hasPermission("spawnercontrol.scannear", sender)) return;

        if (!(sender instanceof Player)){
            main.sendPrefixedMessage(sender, "This command may only be ran by players.");
            return;
        }

        int count = 0;
        for (final Chunk chunk : Utils.getChunksAroundPlayer((Player) sender)){
            for (final BlockState state : chunk.getTileEntities()) {
                if (!(state instanceof CreatureSpawner)) continue;

                final CreatureSpawner cs = (CreatureSpawner) state;
                main.spawnerProcessor.updateSpawner(cs, UpdateOperation.ADD);
                count++;
            }
        }

        if (count == 0)
            main.sendPrefixedMessage(sender, "No spawners were detected");
        else
            main.sendPrefixedMessage(sender, "Processing " + count + " spawners");
    }

    private void showOrUpdateLabel(@NotNull final CommandSender sender, final @NotNull String label, final @NotNull String @NotNull [] args){
        if (!hasPermission("spawnercontrol.label", sender)) return;

        if (!(sender instanceof Player)){
            main.sendPrefixedMessage(sender, "This command may only be ran by players.");
            return;
        }

        CreatureSpawner cs = null;
        final Player player = (Player) sender;
        final BlockIterator blocks = new BlockIterator(player, 20);
        while (blocks.hasNext()){
            final Block block = blocks.next();
            if (block.getType() != Material.SPAWNER) continue;

            cs = (CreatureSpawner) block.getState();
            break;
        }

        if (cs == null){
            main.sendPrefixedMessage(sender, "You must be looking at a spawner first.");
            return;
        }

        final SpawnerInfo info = new SpawnerInfo(cs, main.spawnerOptions);
        final String customName = info.getSpawnerCustomName();
        if (customName == null)
            main.sendPrefixedMessage(sender, "The spawner &r" + Utils.showSpawnerLocation(cs) + "&7 has no name yet.");
        else
            main.sendPrefixedMessage(sender, "The spawner &r" + Utils.showSpawnerLocation(cs) + "&7 has the name '&r" + customName + "&7'.");

        if (args.length == 1) {
            main.sendPrefixedMessage(sender, "To change the name, try: &b/" + label + " <new name>");
        }

        final StringBuilder sb = new StringBuilder();
        for (int i = 1; i < args.length; i++){
            if (sb.length() > 0) sb.append(" ");
            sb.append(args[i]);
        }

        final String newName = sb.toString().trim();
        if (newName.isEmpty()) return;
        if (customName != null && customName.equals(newName)) {
            main.sendPrefixedMessage(sender, "The spawner's new name can't be the same as the spawner's old name.");
            return;
        }

        info.setSpawnerCustomName(newName, main);
        main.spawnerProcessor.spawnerGotRenamed(cs, customName, newName);
        main.sendPrefixedMessage(sender, "The spawner's name has been updated to '&r" + newName + "&7'.");
    }

    private void showSyntax(@NotNull final CommandSender sender, @NotNull final String label) {
        List.of(
                "&b&lSpawnerControl:&7 Available commands:",
                "&8 &m->&b /" + label + " reload &8- &7reload the config",
                "&8 &m->&b /" + label + " info &8- &7view info about the plugin",
                "&8 &m->&b /" + label + " enable &8- &7enable spawner controlling",
                "&8 &m->&b /" + label + " disable &8- &7disable spawner controlling",
                "&8 &m->&b /" + label + " debug &8- &7run debugging functionality",
                "&8 &m->&b /" + label + " spawners &8- &7view managed spawners",
                "&8 &m->&b /" + label + " scan_near &8- &7view managed spawners"
        ).forEach(msg -> sender.sendMessage(MessageUtils.colorizeAll(msg)));
    }

    private void enableOrDisable(@NotNull final CommandSender sender, final boolean doEnable){
        if (!hasPermission("spawnercontrol.toggle", sender)) return;

        if (doEnable) {
            if (main.isEnabled){
                main.sendPrefixedMessage(sender, "Spawner controlling is already &aenabled&7.");
                return;
            }

            main.isEnabled = true;
            main.sendPrefixedMessage(sender, "Spawner controlling has been &aenabled&7.");
        }
        else {
            if (!main.isEnabled){
                main.sendPrefixedMessage(sender, "Spawner controlling is already &cdisabled&7.");
                return;
            }

            main.isEnabled = false;
            main.sendPrefixedMessage(sender, "Spawner controlling has been &cdisabled&7.");
        }
    }

    private void showInfo(@NotNull final CommandSender sender) {
        final String version = main.getDescription().getVersion();
        final String description = main.getDescription().getDescription();

        sender.sendMessage(MessageUtils.colorizeAll("\n" +
                "&b&lSpawnerControl &fv" + version + "&r\n" +
                "&7&o" + description + "&r\n" +
                "&7Created by Stumper66"));
        List.of(
                "&8&m+------------------+",
                "&b&lSpawnerControl&f v" + version,
                "&7&o" + description,
                "&7Created by &bstumper66&7. Contributions from &bLokka30&7",
                "&8&m+------------------+"
        ).forEach(msg -> sender.sendMessage(MessageUtils.colorizeAll(msg)));
    }

    private void showSpawners(@NotNull final CommandSender sender){
        if (!hasPermission("spawnercontrol.spawners", sender)) return;

        main.sendPrefixedMessage(sender, "All known spawners count: &b" + main.spawnerProcessor.getAllKnownSpawnersCount());
        final Collection<SpawnerInfo> spawnerInfo = main.spawnerProcessor.getMonitoredSpawners();
        if (spawnerInfo.isEmpty()) {
            main.sendPrefixedMessage(sender, "There are currently no spawners being monitored.");
            return;
        }

        main.sendPrefixedMessage(sender, "Active spawners &8(&b" + main.spawnerProcessor.getActiveSpawnersCount() + "&8)&7:");
        for(final SpawnerInfo info : spawnerInfo) {
            final CreatureSpawner cs = info.getCs();
            sender.sendMessage(MessageUtils.colorizeAll("&8 &m->&r " + Utils.showSpawnerLocation(cs)));
        }
    }

    private void doReload(@NotNull final CommandSender sender) {
        if (!hasPermission("spawnercontrol.reload", sender)) return;

        main.sendPrefixedMessage(sender, "Reloading config...");
        main.loadConfig(true);
        main.sendPrefixedMessage(sender, "Reload complete.");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasPermission(final @NotNull String permisison, final @NotNull CommandSender sender){
        if (!sender.hasPermission(permisison)){
            main.sendPrefixedMessage(sender, "You don't have permission to access that.");
            return false;
        }
        return true;
    }

    private final List<String> SUBCOMMANDS = List.of("debug", "reload", "info", "label", "spawners", "enable", "disable", "scan_near");

    @Override
    public @Nullable List<String> onTabComplete(final @NotNull CommandSender commandSender, final @NotNull Command command, final @NotNull String label, final @NotNull String @NotNull [] args) {
        if (args.length > 1 && "debug".equals(args[0]))
            return debugCommand.onTabComplete(commandSender, args);
        else if (args.length == 1)
            return SUBCOMMANDS;

        return Collections.emptyList();
    }
}
