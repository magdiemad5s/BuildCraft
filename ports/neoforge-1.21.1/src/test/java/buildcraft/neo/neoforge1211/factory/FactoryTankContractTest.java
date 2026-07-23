// SPDX-License-Identifier: MPL-2.0
package buildcraft.neo.neoforge1211.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FactoryTankContractTest {
    @Test
    void preservesTheLegacyFactoryRegistryIdentity() {
        assertEquals("buildcraftfactory", FactoryTankContract.MODULE_ID);
        assertEquals("tank", FactoryTankContract.REGISTRY_PATH);
        assertEquals("tank", FactoryTankContract.FLUID_NBT_KEY);
        assertEquals(16_000, FactoryTankContract.CAPACITY_MILLIBUCKETS);
    }
}
