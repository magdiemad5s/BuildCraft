package buildcraft.transport.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Preserves the intentionally different 1.12.2 identities used by pipe
 * definitions and their registered item forms.
 *
 * <p>Pipe NBT stores definition paths such as {@code wood_item} and
 * {@code cobblestone_item}. The item registry used {@code pipe_wood_item}
 * and the historical abbreviation {@code pipe_cobble_item}. Changing the
 * definition paths would break placed-pipe save data, so only item
 * registration passes through this mapping.</p>
 */
public final class TransportItemIdentity {
    public static final List<String> STANDARD_PIPE_DEFINITIONS = List.of(
        "structure",
        "wood_item", "wood_fluid", "wood_power",
        "stone_item", "stone_fluid", "stone_power",
        "cobblestone_item", "cobblestone_fluid", "cobblestone_power",
        "quartz_item", "quartz_fluid", "quartz_power",
        "gold_item", "gold_fluid", "gold_power",
        "sandstone_item", "sandstone_fluid", "sandstone_power",
        "iron_item", "iron_fluid", "iron_power",
        "diamond_item", "diamond_fluid", "diamond_power",
        "diamond_wood_item", "diamond_wood_fluid", "diamond_wood_power",
        "clay_item", "clay_fluid",
        "void_item", "void_fluid",
        "obsidian_item",
        "lapis_item", "daizuli_item", "emzuli_item", "stripes_item"
    );

    public static final List<String> FORGE_ENERGY_PIPE_DEFINITIONS = List.of(
        "wood_rf", "stone_rf", "cobblestone_rf", "quartz_rf", "gold_rf",
        "sandstone_rf", "iron_rf", "diamond_rf", "diamond_wood_rf"
    );

    private TransportItemIdentity() {
    }

    public static String pipeItemPath(String definitionPath) {
        Objects.requireNonNull(definitionPath, "definitionPath");
        if (definitionPath.isBlank()) {
            throw new IllegalArgumentException("definitionPath must not be blank");
        }
        if (definitionPath.startsWith("cobblestone_")) {
            return "pipe_cobble_" + definitionPath.substring("cobblestone_".length());
        }
        return "pipe_" + definitionPath;
    }

    public static List<String> livePipeDefinitionPaths() {
        List<String> paths = new ArrayList<>(
            STANDARD_PIPE_DEFINITIONS.size() + FORGE_ENERGY_PIPE_DEFINITIONS.size()
        );
        paths.addAll(STANDARD_PIPE_DEFINITIONS);
        paths.addAll(FORGE_ENERGY_PIPE_DEFINITIONS);
        return List.copyOf(paths);
    }

    public static List<String> livePipeItemPaths() {
        return livePipeDefinitionPaths().stream().map(TransportItemIdentity::pipeItemPath).toList();
    }
}
