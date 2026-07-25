package buildcraft.transport.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import buildcraft.api.transport.pipe.PipeDefinition.PipeDefinitionBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ModelPipeItemContractTest {
    @Test
    void distinctDefinitionFacesEmitBottomCenterAndTopSections() throws Exception {
        PipeDefinitionBuilder definition = new PipeDefinitionBuilder().itemTex(2, 1, 0);

        assertEquals(14, ModelPipeItem.getBaseGeometryQuadCount(
            definition.itemModelCenter,
            definition.itemModelTop,
            definition.itemModelBottom
        ));

        String source = Files.readString(Path.of(
            "src/main/java/buildcraft/transport/client/model/ModelPipeItem.java"
        ));
        assertTrue(source.contains("addQuads(QUADS_DIFFERENT[INDEX_BOTTOM]"));
        assertTrue(source.contains("addQuads(QUADS_DIFFERENT[INDEX_CENTER]"));
        assertTrue(source.contains("addQuads(QUADS_DIFFERENT[INDEX_TOP]"));
        assertFalse(source.contains("\n        top = center;\n"));
        assertFalse(source.contains("\n        bottom = center;\n"));
        assertFalse(source.contains("// TEMP!"));
    }

    @Test
    void sharedDefinitionFaceRetainsTheOriginalSixQuadFastPath() {
        PipeDefinitionBuilder definition = new PipeDefinitionBuilder().itemTex(1);

        assertEquals(6, ModelPipeItem.getBaseGeometryQuadCount(
            definition.itemModelCenter,
            definition.itemModelTop,
            definition.itemModelBottom
        ));
    }
}
