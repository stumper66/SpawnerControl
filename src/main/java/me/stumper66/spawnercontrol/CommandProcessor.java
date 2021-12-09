package me.stumper66.spawnercontrol;

import me.lokka30.microlib.messaging.MessageUtils;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CommandProcessor implements CommandExecutor, TabCompleter  {
    public CommandProcessor(final SpawnerControl main){
        this.main = main;
    }
    private final SpawnerControl main;

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd, final @NotNull String label, final @NotNull String @NotNull [] args) {
        if (args.length == 0){
            showSyntax(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()){
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
            default:
                showSyntax(sender, label);
                break;
        }

        return true;
    }

    private void showSyntax(@NotNull final CommandSender sender, @NotNull final String label) {
        sender.sendMessage(MessageUtils.colorizeAll("&b&lSpawnerControl: &7Syntax: &b/" + label + " reload | info | enable | disable | spawners"));
    }

    private void enableOrDisable(@NotNull final CommandSender sender, final boolean doEnable){
        if (!sender.hasPermission("spawnercontrol.toggle")){
            sender.sendMessage(MessageUtils.colorizeAll("&b&lSpawnerControl: &7You don't have permissions to run this command"));
            return;
        }

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
        if (!sender.hasPermission("spawnercontrol.spawners")){
            sender.sendMessage(MessageUtils.colorizeAll("&b&lSpawnerControl: &7You don't have permissions to run this command"));
            return;
        }

        final Collection<SpawnerInfo> spawnerInfos = main.spawnerProcessor.getMonitoredSpawners();
        if (spawnerInfos.isEmpty()){
            sender.sendMessage("There are no spawners currently monitored");
            return;
        }

        final StringBuilder sb = new StringBuilder();
        for (final SpawnerInfo info : spawnerInfos){
            final CreatureSpawner cs = info.cs;
            if (sb.length() > 0) sb.append("\n");
            sb.append(Utils.showSpawnerLocation(cs));
        }

        sender.sendMessage("Active spawners: " + main.spawnerProcessor.getActiveSpawnersCount() + "\n" + sb);
    }

    private void doReload(@NotNull final CommandSender sender) {
        if (!sender.hasPermission("spawnercontrol.reload")){
            sender.sendMessage(MessageUtils.colorizeAll("&b&lSpawnerControl: &7You don't have permissions to run this command"));
            return;
        }

        sender.sendMessage(MessageUtils.colorizeAll("&b&lSpawnerControl: &7Reloading config..."));
        main.loadConfig(true);
        sender.sendMessage(MessageUtils.colorizeAll("&b&lSpawnerControl: &7Reload complete."));
    }

    @Nullable
    @Override
    public List<String> onTabComplete(final @NotNull CommandSender commandSender, final @NotNull Command command, final @NotNull String label, final @NotNull String @NotNull [] args) {
        if (args.length == 1)
            return List.of("reload", "info", "spawners", "disable", "enable");

        return Collections.emptyList();
    }
}
