package me.stumper66.spawnercontrol.command;

import me.lokka30.microlib.messaging.MessageUtils;
import me.stumper66.spawnercontrol.DebugInfo;
import me.stumper66.spawnercontrol.DebugType;
import me.stumper66.spawnercontrol.SpawnerControl;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DebugCommand {

    public DebugCommand(final @NotNull SpawnerControl main, final CommandProcessor commandProcessor) {
        this.di = main.debugInfo;
        this.main = main;
        this.commandProcessor = commandProcessor;
    }
    private final SpawnerControl main;
    private final DebugInfo di;
    private final CommandProcessor commandProcessor;

    void onCommand(final @NotNull CommandSender sender, final @NotNull String @NotNull [] args){
        if(!commandProcessor.hasPermission("spawnercontrol.debug", sender)) return;

        if (args.length <= 1){
            showSyntax(sender);
            return;
        }

        switch (args[1].toLowerCase()){
            case "disable":
                di.debugIsEnabled = false;
                main.sendPrefixedMessage(sender, "The debug system has been &cdisabled&7.");
                break;
            case "enable":
                di.debugIsEnabled = true;
                main.sendPrefixedMessage(sender, "The debug system has been &aenabled&7.");
                break;
            case "status":
                showStatus(sender);
                break;
            case "debug_types":
                showOrUpdateDebugTypes(sender, args);
                break;
            case "mob_types":
                showOrUpdateEntityTypes(sender, args);
                break;
            case "spawner_names":
                showOrUpdateSpawnerNames(sender, args);
                break;
            case "region_names":
                showOrUpdateRegionNames(sender, args);
                break;
        }
    }

    private void showSyntax(final @NotNull CommandSender sender){
        main.sendPrefixedMessage(sender, "Available debug options: &bdisable&7, &benable&7, &bstatus&7, &bdebug_types&7, &bmob_types&7, &bspawner_names&7 and &bregion_names&7.");
    }

    private void showStatus(final @NotNull CommandSender sender) {
        main.sendPrefixedMessage(sender, "Debugging status:");

        final StringBuilder sb = new StringBuilder();
        sb.append("&7Debugging is ");
        if (di.debugIsEnabled)
            sb.append("&aenabled");
        else
            sb.append("&cdisabled");

        sb.append("&7.\n&7Spawner names: &b");
        if (di.enabledSpawnerNames.isEmpty())
            sb.append("(all)");
        else
            sb.append(di.enabledSpawnerNames);

        sb.append("&r\n&7Region names: &b");
        if (di.enabledRegionNames.isEmpty())
            sb.append("(all)");
        else
            sb.append(di.enabledRegionNames);

        sb.append("&r\n&7Mob types: &b");
        if (di.allEntityTypesEnabled)
            sb.append("(all)");
        else
            sb.append(di.enabledEntityTypes);

        sb.append("&r\n&7Debug types: &b");
        if (di.enabledDebugTypes.isEmpty())
            sb.append("(all)");
        else
            sb.append(di.enabledDebugTypes);

        sender.sendMessage(MessageUtils.colorizeAll(sb.toString()));
    }

    private void showOrUpdateDebugTypes(final @NotNull CommandSender sender, final @NotNull String @NotNull [] args){
        if (args.length == 2){
            showEnabledDebugTypes(sender);
            return;
        }

        if ("add".equalsIgnoreCase(args[2])){
            for (int i = 3; i < args.length; i++) {
                if ("*".equals(args[i])){
                    di.enabledDebugTypes.addAll(List.of(DebugType.values()));
                    break;
                }
                DebugType type;
                try {
                    type = DebugType.valueOf(args[i].toUpperCase());
                } catch (Exception ignored) {
                    main.sendPrefixedMessage(sender, "Invalid debug type '&b" + args[i] + "&7'.");
                    continue;
                }

                di.enabledDebugTypes.add(type);
            }
            main.sendPrefixedMessage(sender, "The enabled debug type list has been updated.");
        }
        else if ("remove".equalsIgnoreCase(args[2])){
            for (int i = 3; i < args.length; i++) {
                if ("*".equals(args[i])){
                    di.enabledDebugTypes.clear();
                    break;
                }
                DebugType type;
                try {
                    type = DebugType.valueOf(args[i].toUpperCase());
                } catch (Exception e) {
                    continue;
                }

                di.enabledDebugTypes.remove(type);
            }
            main.sendPrefixedMessage(sender, "The enabled debug-type list has been updated.");
        } else {
            main.sendPrefixedMessage(sender, "Invalid option '&b" + args[2] + "&7'.");
        }
    }

    private void showOrUpdateEntityTypes(final @NotNull CommandSender sender, final @NotNull String @NotNull [] args){
        if (args.length == 2){
            showEnabledMobTypes(sender);
            return;
        }

        if ("add".equalsIgnoreCase(args[2])){
            for (int i = 3; i < args.length; i++) {
                if ("*".equals(args[i])){
                    di.enabledEntityTypes.clear();
                    di.allEntityTypesEnabled = true;
                    break;
                }
                EntityType type;
                try {
                    type = EntityType.valueOf(args[i].toUpperCase());
                    di.allEntityTypesEnabled = false;
                } catch (Exception ignored) {
                    main.sendPrefixedMessage(sender, "Invalid entity type '&b" + args[i] + "&7'.");
                    continue;
                }

                di.enabledEntityTypes.add(type);
            }
            main.sendPrefixedMessage(sender, "The enabled mob type list has been updated.");
        }
        else if ("remove".equalsIgnoreCase(args[2])){
            for (int i = 3; i < args.length; i++) {
                if ("*".equals(args[i])){
                    di.enabledEntityTypes.clear();
                    break;
                }
                EntityType type;
                try {
                    type = EntityType.valueOf(args[i].toUpperCase());
                } catch (Exception e) {
                    continue;
                }

                di.enabledEntityTypes.remove(type);
                di.allEntityTypesEnabled = di.enabledEntityTypes.isEmpty();
            }
            main.sendPrefixedMessage(sender, "The enabled mob type list has been updated.");
        }
        else
            main.sendPrefixedMessage(sender, "Invalid option '&b" + args[2] + "&7'.");
    }

    private void showOrUpdateSpawnerNames(final @NotNull CommandSender sender, final @NotNull String @NotNull [] args){
        if (args.length == 2){
            if (di.enabledSpawnerNames.isEmpty())
                main.sendPrefixedMessage(sender, "Spawner names: &b(all)");
            else
                main.sendPrefixedMessage(sender, "Spawner names: &b" + di.enabledSpawnerNames);
            return;
        }

        updateStringSet(sender, args, di.enabledSpawnerNames);
        main.sendPrefixedMessage(sender, "The spawner names list has been updated.");
    }

    private void showOrUpdateRegionNames(final @NotNull CommandSender sender, final @NotNull String @NotNull [] args){
        if (args.length == 2){
            if (di.enabledRegionNames.isEmpty())
                main.sendPrefixedMessage(sender, "Region names: &b(all)");
            else
                main.sendPrefixedMessage(sender, "Region names: &b" + di.enabledRegionNames);
            return;
        }

        updateStringSet(sender, args, di.enabledRegionNames);
        main.sendPrefixedMessage(sender, "The region names list has been updated.");
    }

    private void updateStringSet(final @NotNull CommandSender sender, final @NotNull String @NotNull [] args, final @NotNull Set<String> names){
        if ("add".equalsIgnoreCase(args[2]))
            names.addAll(Arrays.asList(args).subList(3, args.length));
        else if ("remove".equalsIgnoreCase(args[2])){
            for (int i = 3; i < args.length; i++) {
                names.remove(args[i]);
            }
        }
        else
            main.sendPrefixedMessage(sender, "Invalid option '&b" + args[2] + "&7'.");
    }

    private void showEnabledDebugTypes(final @NotNull CommandSender sender) {
        main.sendPrefixedMessage(sender, "Enabled debug types status:");

        final StringBuilder sb = new StringBuilder();
        if (di.enabledDebugTypes.isEmpty())
            sb.append("&7No debug types are currently enabled.");
        else {
            sb.append("&7Enabled types: &b");
            int count = 0;
            for (final DebugType type : di.enabledDebugTypes){
                if (count > 0) sb.append("&7, &b");

                sb.append(type.toString().toLowerCase());
                count++;
            }
        }

        if (di.enabledDebugTypes.size() == DebugType.values().length){
            sender.sendMessage(sb.toString());
            return;
        }

        sb.append("\n&7Available types: ");
        for (int i = 0; i < DebugType.values().length; i++){
            if (i > 0) sb.append(", ");
            sb.append(DebugType.values()[i].toString().toLowerCase());
        }

        sender.sendMessage(MessageUtils.colorizeAll(sb.toString()));
    }

    private void showEnabledMobTypes(final @NotNull CommandSender sender) {
        main.sendPrefixedMessage(sender, "Enabled mob types status:");

        final StringBuilder sb = new StringBuilder();
        if (di.enabledEntityTypes.isEmpty())
            sb.append("&7No mob types are currently enabled.");
        else {
            sb.append("&7Enabled types: &b");
            int count = 0;
            for (final EntityType type : di.enabledEntityTypes){
                if (count > 0) sb.append("&7, &b");

                sb.append(type.toString().toLowerCase());
                count++;
            }
        }

        sender.sendMessage(MessageUtils.colorizeAll(sb.toString()));
    }

    @NotNull
    List<String> onTabComplete(final @NotNull CommandSender sender, final @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("spawnercontrol.debug")) return Collections.emptyList();

        if (args.length == 2)
            return List.of("disable", "enable", "status", "debug_types", "mob_types", "spawner_names", "region_names");

        if ("debug_types".equalsIgnoreCase(args[1]))
             return tabCompleteForDebugTypes(args);

        if ("mob_types".equalsIgnoreCase(args[1]))
            return tabCompleteForEntityTypes(args);

        if ("region_names".equalsIgnoreCase(args[1]))
            return tabCompleteForNames(args, di.enabledRegionNames);

        if ("spawner_names".equalsIgnoreCase(args[1]))
            return tabCompleteForNames(args, di.enabledSpawnerNames);

        return Collections.emptyList();
    }

    @NotNull
    private List<String> tabCompleteForNames(final @NotNull String @NotNull [] args, final @NotNull Set<String> names){
        if (args.length == 3)
            return List.of("add", "remove");

        if ("remove".equalsIgnoreCase(args[2]))
            return new LinkedList<>(names);

        return Collections.emptyList();
    }

    @NotNull
    private List<String> tabCompleteForDebugTypes(final @NotNull String @NotNull [] args){
        if (args.length == 3)
            return List.of("add", "remove");

        if ("add".equalsIgnoreCase(args[2])) {
            final List<DebugType> types = new LinkedList<>();
            Collections.addAll(types, DebugType.values());
            for (final DebugType type : di.enabledDebugTypes)
                types.remove(type);

            boolean hadWildcard = false;
            for (int i = 3; i < args.length; i++) {
                if ("*".equals(args[i])){
                    hadWildcard = true;
                    continue;
                }
                DebugType type;
                try {
                    type = DebugType.valueOf(args[i].toUpperCase());
                } catch (Exception e) {
                    continue;
                }

                types.remove(type);
            }

            List<String> results = new LinkedList<>();
            if (!hadWildcard) results.add("*");
            for (final DebugType debugType : types)
                results.add(debugType.toString().toLowerCase());

            return types.isEmpty() ?
                    Collections.emptyList() : results;
        }
        else if ("remove".equalsIgnoreCase(args[2])) {
            if (di.enabledDebugTypes.isEmpty())
                return Collections.emptyList();

            final List<String> types = new LinkedList<>();
            types.add("*");
            for (final DebugType type : di.enabledDebugTypes)
                types.add(type.toString().toLowerCase());

            return types;
        }

        return Collections.emptyList();
    }

    @NotNull
    private List<String> tabCompleteForEntityTypes(final @NotNull String @NotNull [] args){
        if (args.length == 3)
            return List.of("add", "remove");

        if ("add".equalsIgnoreCase(args[2])) {
            final List<EntityType> types = new LinkedList<>();
            Collections.addAll(types, EntityType.values());
            for (final EntityType type : di.enabledEntityTypes)
                types.remove(type);

            boolean hadWildcard = false;
            for (int i = 3; i < args.length; i++) {
                if ("*".equals(args[i])){
                    hadWildcard = true;
                    continue;
                }
                EntityType type;
                try {
                    type = EntityType.valueOf(args[i].toUpperCase());
                } catch (Exception e) {
                    continue;
                }

                types.remove(type);
            }

            List<String> results = new LinkedList<>();
            if (!hadWildcard) results.add("*");
            for (final EntityType type : types)
                results.add(type.toString().toLowerCase());

            return types.isEmpty() ?
                    Collections.emptyList() : results;
        }
        else if ("remove".equalsIgnoreCase(args[2])) {
            if (di.enabledEntityTypes.isEmpty())
                return Collections.emptyList();

            final List<String> types = new LinkedList<>();
            types.add("*");
            for (final EntityType type : di.enabledEntityTypes)
                types.add(type.toString().toLowerCase());

            return types;
        }

        return Collections.emptyList();
    }
}
