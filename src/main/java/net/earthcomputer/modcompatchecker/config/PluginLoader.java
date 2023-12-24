package net.earthcomputer.modcompatchecker.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

public final class PluginLoader {
    private static final List<Plugin> PLUGINS = loadPlugins();

    private PluginLoader() {
    }

    public static List<Plugin> plugins() {
        return PLUGINS;
    }

    private static List<Plugin> loadPlugins() {
        List<PluginCandidate> plugins = ServiceLoader.load(Plugin.class).stream().map(provider -> {
            Plugin plugin = provider.get();
            Plugin.Ordering order = plugin.order();
            return new PluginCandidate(plugin.id(), order, plugin, new HashSet<>(order.after));
        }).collect(Collectors.toCollection(ArrayList::new));

        Map<String, PluginCandidate> pluginMap = new HashMap<>();
        for (PluginCandidate plugin : plugins) {
            if (pluginMap.put(plugin.id, plugin) != null) {
                throw new IllegalStateException("Duplicate plugin id \"" + plugin.id + "\"");
            }
        }

        plugins.sort(Comparator.<PluginCandidate, Plugin.Ordering.Order>comparing(plugin -> plugin.order.order).thenComparing(plugin -> plugin.id));

        for (PluginCandidate plugin : plugins) {
            for (String after : plugin.order.before) {
                PluginCandidate beforePlugin = pluginMap.get(after);
                if (beforePlugin != null) {
                    beforePlugin.dependencies.add(plugin.id);
                }
            }
        }

        List<PluginCandidate> sortedPlugins = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (PluginCandidate plugin : plugins) {
            if (!visited.contains(plugin.id)) {
                topoSort(plugin, pluginMap, visited, sortedPlugins);
            }
        }

        visited.clear();

        for (int i = 0; i < sortedPlugins.size() - 1; i++) {
            if (sortedPlugins.get(i).order.order.compareTo(sortedPlugins.get(i + 1).order.order) > 0) {
                throw new IllegalStateException("Could not sort plugins");
            }
            if (sortedPlugins.get(i).dependencies.stream().anyMatch(dep -> !visited.contains(dep))) {
                throw new IllegalStateException("Could not sort plugins");
            }
            visited.add(sortedPlugins.get(i).id);
        }

        return sortedPlugins.stream().map(PluginCandidate::plugin).toList();
    }

    private static void topoSort(PluginCandidate currentPlugin, Map<String, PluginCandidate> pluginMap, Set<String> visited, List<PluginCandidate> sortedPlugins) {
        visited.add(currentPlugin.id);
        for (String dependency : currentPlugin.dependencies) {
            if (!visited.contains(dependency)) {
                PluginCandidate dependencyPlugin = pluginMap.get(dependency);
                if (dependencyPlugin != null) {
                    topoSort(dependencyPlugin, pluginMap, visited, sortedPlugins);
                }
            }
        }
        sortedPlugins.add(currentPlugin);
    }

    private record PluginCandidate(String id, Plugin.Ordering order, Plugin plugin, Set<String> dependencies) {
    }
}
