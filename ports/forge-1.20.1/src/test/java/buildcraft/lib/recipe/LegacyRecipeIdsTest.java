package buildcraft.lib.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import buildcraft.api.enums.EnumEngineType;

class LegacyRecipeIdsTest {
    @Test
    void preservesReleasedRecipeIds() {
        assertEquals("buildcraftcore:redstone_engine", LegacyRecipeIds.REDSTONE_ENGINE.toString());
        assertEquals("buildcraftenergy:stirling_engine", LegacyRecipeIds.STIRLING_ENGINE.toString());
        assertEquals("buildcraftenergy:combustion_engine", LegacyRecipeIds.COMBUSTION_ENGINE.toString());
        assertEquals("buildcraftfactory:autoworkbench_item", LegacyRecipeIds.AUTOWORKBENCH_ITEM.toString());
        assertEquals("buildcrafttransport:pipe_structure", LegacyRecipeIds.PIPE_STRUCTURE.toString());
        assertEquals("buildcrafttransport:pipe_sealant", LegacyRecipeIds.PIPE_SEALANT.toString());
        assertEquals(
            "buildcraftenergy:residue_to_pipe_sealant",
            LegacyRecipeIds.RESIDUE_TO_PIPE_SEALANT.toString()
        );
    }

    @Test
    void preservesUnifiedEngineDamageOrdinals() {
        assertEquals(0, EnumEngineType.WOOD.ordinal());
        assertEquals(1, EnumEngineType.STONE.ordinal());
        assertEquals(2, EnumEngineType.IRON.ordinal());
        assertEquals(3, EnumEngineType.CREATIVE.ordinal());
        assertEquals(4, EnumEngineType.RF.ordinal());
    }
}