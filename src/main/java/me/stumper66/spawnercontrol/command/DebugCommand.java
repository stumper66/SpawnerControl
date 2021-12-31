package me.stumper66.spawnercontrol.command;

import me.lokka30.microlib.messaging.MessageUtils;
import me.stumper66.spawnercontrol.DebugInfo;
import me.stumper66.spawnercontrol.DebugTypes;
import me.stumper66.spawnercontrol.SpawnerControl;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class DebugCommand {
    public DebugCommand(final @NotNull SpawnerControl main) {
        this.main = main;
        this.di = main.debugInfo;
    }

    private final SpawnerControl main;
    private final DebugInfo di;

    void onCommand(final @NotNull CommandSender sender, final @NotNull String label, final @NotNull String @NotNull [] args){
        if (args.length <= 1){
            showSyntax(sender, label);
            return;
        }

        switch (args[1].toLowerCase()){
            case "disable":
                di.debugIsEnabled = false;
                sender.sendMessage("Debug is disabled");
                break;
            case "enable":
                di.debugIsEnabled = true;
                sender.sendMessage("Debug is enabled");
                break;
            case "status":
                showStatus(sender);
                break;
            case "debug_types":
                showOrUpdateDebugTypes(sender, label, args);
                break;
            case "entity_types":
                showOrUpdateEntityTypes(sender, label, args);
                break;
            case "spawner_names":
                showOrUpdateSpawnerNames(sender, label, args);
                break;
            case "region_names":
                showOrUpdateRegionNames(sender, label, args);
                break;
        }
    }

    private void showSyntax(final @NotNull CommandSender sender, final @NotNull String label){
        sender.sendMessage("this is the syntax");
    }

    private void showStatus(final @NotNull CommandSender sender){
        final StringBuilder sb = new StringBuilder();
        sb.append("Debug is ");
        if (di.debugIsEnabled)
            sb.append("&2enabled");
        else
            sb.append("&6disabled");

        sb.append("&r\nSpawner names: &b");
        if (di.enabledSpawnerNames.isEmpty())
            sb.append("(all)");
        else
            sb.append(di.enabledSpawnerNames);

        sb.append("&r\nRegion names: &b");
        if (di.enabledRegionNames.isEmpty())
            sb.append("(all)");
        else
            sb.append(di.enabledRegionNames);

        sb.append("&r\nEntity types: &b");
        if (di.allEntityTypesEnabled)
            sb.append("(all)");
        else
            sb.append(di.enabledEntityTypes);

        sb.append("&r\nDebug types: &b");
        if (di.enabledDebugTypes.isEmpty())
            sb.append("(all)");
        else
            sb.append(di.enabledDebugTypes);

        sender.sendMessage(MessageUtils.colorizeAll(sb.toString()));
    }

    private void showOrUpdateDebugTypes(final @NotNull CommandSender sender, final @NotNull String label, final @NotNull String @NotNull [] args){
        if (args.length == 2){
            showEnabledDebugTypes(sender);
            return;
        }

        if ("add".equalsIgnoreCase(args[2])){
            for (int i = 3; i < args.length; i++) {
                if ("*".equals(args[i])){
                    di.enabledDebugTypes.addAll(Arrays.asList(DebugTypes.values()));
                    break;
                }
                DebugTypes type;
                try {
                    type = DebugTypes.valueOf(args[i].toUpperCase());
                } catch (Exception ignored) {
                    sender.sendMessage("Invalid debug type: " + args[i]);
                    continue;
                }

                di.enabledDebugTypes.add(type);
            }
            sender.sendMessage("Enabled debug type list has been updated");
        }
        else if ("remove".equalsIgnoreCase(args[2])){
            for (int i = 3; i < args.length; i++) {
                if ("*".equals(args[i])){
                    di.enabledDebugTypes.clear();
                    break;
                }
                DebugTypes type;
                try {
                    type = DebugTypes.valueOf(args[i].toUpperCase());
                } catch (Exception e) {
                    continue;
                }

                di.enabledDebugTypes.remove(type);
            }
            sender.sendMessage("Enabled debug type list has been updated");
        }
        else
            sender.sendMessage("Invalid option");
    }

    private void showOrUpdateEntityTypes(final @NotNull CommandSender sender, final @NotNull String label, final @NotNull String @NotNull [] args){
        if (args.length == 2){
            showEnabledDebugTypes(sender);
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
                } catch (Exception ignored) {
                    sender.sendMessage("Invalid entity type: " + args[i]);
                    continue;
                }

                di.enabledEntityTypes.add(type);
            }
            sender.sendMessage("Enabled entity type list has been updated");
        }
        else if ("remove".equalsIgnoreCase(args[2])){
            for (int i = 3; i < args.length; i++) {
                if ("*".equals(args[i])){
                    di.enabledEntityTypes.clear();
                    di.allEntityTypesEnabled = false;
                    break;
                }
                EntityType type;
                try {
                    type = EntityType.valueOf(args[i].toUpperCase());
                } catch (Exception e) {
                    continue;
                }

                di.enabledEntityTypes.remove(type);
            }
            sender.sendMessage("Enabled entity type list has been updated");
        }
        else
            sender.sendMessage("Invalid option");
    }

    private void showOrUpdateSpawnerNames(final @NotNull CommandSender sender, final @NotNull String label, final @NotNull String @NotNull [] args){
        if (args.length == 2){
            if (di.enabledSpawnerNames.isEmpty())
                sender.sendMessage("Spawner names: all");
            else
                sender.sendMessage("Spawner names: " + di.enabledSpawnerNames);
            return;
        }

        updateStringSet(sender, args, di.enabledSpawnerNames);
        sender.sendMessage("Updated spawner names list");
    }

    private void showOrUpdateRegionNames(final @NotNull CommandSender sender, final @NotNull String label, final @NotNull String @NotNull [] args){
        if (args.length == 2){
            if (di.enabledRegionNames.isEmpty())
                sender.sendMessage("Region names: all");
            else
                sender.sendMessage("Region names: " + di.enabledRegionNames);
            return;
        }

        updateStringSet(sender, args, di.enabledRegionNames);
        sender.sendMessage("Updated region names list");
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
            sender.sendMessage("Invalid option");
    }

    private void showEnabledDebugTypes(final @NotNull CommandSender sender){
        final StringBuilder sb = new StringBuilder();

        if (di.enabledDebugTypes.isEmpty())
            sb.append("No debug types are currently enabled");
        else {
            sb.append("Enabled types: ");
            int count = 0;
            for (final DebugTypes type : di.enabledDebugTypes){
                if (count > 0) sb.append(", ");

                sb.append(type.toString().toLowerCase());
                count++;
            }
        }

        if (di.enabledDebugTypes.size() == DebugTypes.values().length){
            sender.sendMessage(sb.toString());
            return;
        }

        sb.append("\nAvailable types: ");
        for (int i = 0; i < DebugTypes.values().length; i++){
            if (i > 0) sb.append(", ");
            sb.append(DebugTypes.values()[i].toString().toLowerCase());
        }

        sender.sendMessage(sb.toString());
    }

    List<String> onTabComplete(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final @NotNull String @NotNull [] args) {
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

    private List<String> tabCompleteForNames(final @NotNull String @NotNull [] args, final @NotNull Set<String> names){
        if (args.length == 3)
            return List.of("add", "remove");

        if ("remove".equalsIgnoreCase(args[2]))
            return new LinkedList<>(names);

        return Collections.emptyList();
    }

    private List<String> tabCompleteForDebugTypes(final @NotNull String @NotNull [] args){
        if (args.length == 3)
            return List.of("add", "remove");

        if ("add".equalsIgnoreCase(args[2])) {
            final List<DebugTypes> types = new LinkedList<>();
            Collections.addAll(types, DebugTypes.values());
            for (final DebugTypes type : di.enabledDebugTypes)
                types.remove(type);

            boolean hadWildcard = false;
            for (int i = 3; i < args.length; i++) {
                if ("*".equals(args[i])){
                    hadWildcard = true;
                    continue;
                }
                DebugTypes type;
                try {
                    type = DebugTypes.valueOf(args[i].toUpperCase());
                } catch (Exception e) {
                    continue;
                }

                types.remove(type);
            }

            List<String> results = new LinkedList<>();
            if (!hadWildcard) results.add("*");
            for (final DebugTypes debugType : types)
                results.add(debugType.toString().toLowerCase());

            return types.isEmpty() ?
                    Collections.emptyList() : results;
        }
        else if ("remove".equalsIgnoreCase(args[2])) {
            if (di.enabledDebugTypes.isEmpty())
                return Collections.emptyList();

            final List<String> types = new LinkedList<>();
            types.add("*");
            for (final DebugTypes type : di.enabledDebugTypes)
                types.add(type.toString().toLowerCase());

            return types;
        }

        return Collections.emptyList();
    }

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
