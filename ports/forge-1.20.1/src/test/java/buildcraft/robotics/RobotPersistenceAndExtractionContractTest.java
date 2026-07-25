package buildcraft.robotics;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RobotPersistenceAndExtractionContractTest {
    @Test
    void robotReloadRebindsThePersistedBoardAi() throws IOException {
        String source = read("src/main/java/buildcraft/robotics/entity/EntityRobot.java");
        String loadMethod = source.substring(
            source.indexOf("public void readAdditionalSaveData"),
            source.indexOf("private static CompoundTag writeStation")
        );

        assertTrue(loadMethod.contains("mainAI.getDelegateAI() instanceof RedstoneBoardRobot"));
        assertTrue(loadMethod.contains("tag.contains(\"boardAI\")"));
        assertTrue(loadMethod.contains("tag.contains(\"board\")"));
        assertTrue(loadMethod.contains("board = loadedBoard;"));
    }

    @Test
    void temporaryStationCanReleaseItsOwnerAfterEntityReferenceInvalidation() throws IOException {
        String source = read("src/main/java/buildcraft/api/robots/DockingStation.java");
        String releaseMethod = source.substring(
            source.indexOf("public void release(EntityRobotBase robot)"),
            source.indexOf("public void unsafeRelease(EntityRobotBase robot)")
        );

        assertTrue(releaseMethod.contains("robotTakingId == robot.getRobotId()"));
        assertTrue(releaseMethod.contains("!linkIsMain"));
        assertTrue(releaseMethod.contains("registry.release(this, robot.getRobotId())"));
    }

    @Test
    void toolFetchUsesTheSameGuardedSidedExtractionPathAsRobotLoading() throws IOException {
        String fetch = read("src/main/java/buildcraft/robotics/ai/AIRobotFetchAndEquipItemStack.java");
        String load = read("src/main/java/buildcraft/robotics/ai/AIRobotLoad.java");
        String extractionMethod = load.substring(
            load.indexOf("public static ItemStack takeMatching"),
            load.indexOf("public static boolean load")
        );

        assertTrue(fetch.contains("AIRobotLoad.takeMatching"));
        assertFalse(fetch.contains("inventory.removeItem"));
        assertTrue(extractionMethod.contains("getSlots(container, side)"));
        assertTrue(extractionMethod.contains("canTake(container, slot, stack, side)"));
        assertTrue(extractionMethod.contains("canStationProvide(station, stack, filter)"));
        assertTrue(extractionMethod.contains("container.removeItem(slot, toTake)"));
    }

    @Test
    void fluidLoadRestoresAnyAmountRejectedAfterTheSourceDrainExecutes() throws IOException {
        String source = read("src/main/java/buildcraft/robotics/ai/AIRobotLoadFluids.java");

        assertTrue(source.contains("remainder.setAmount(drained.getAmount() - filled)"));
        assertTrue(source.contains("handler.fill(remainder, FluidAction.EXECUTE)"));
    }

    @Test
    void loadedRobotRegistrationDoesNotDirtySavedDataEveryTick() throws IOException {
        String entity = read("src/main/java/buildcraft/robotics/entity/EntityRobot.java");
        String registry = read("src/main/java/buildcraft/robotics/SimpleRobotRegistryProvider.java");
        String tickMethod = entity.substring(
            entity.indexOf("public void tick()"),
            entity.indexOf("private void firstUpdate()")
        );

        assertFalse(tickMethod.contains("registerRobot(this)"));
        assertTrue(registry.contains("boolean registrationChanged"));
        assertTrue(registry.contains("if (registrationChanged)"));
    }

    @Test
    void pickerReservationsResetForEveryIntegratedOrDedicatedServerStart() throws IOException {
        String source = read("src/main/java/buildcraft/robotics/BCRobotics.java");
        String constructor = source.substring(
            source.indexOf("public BCRobotics()"),
            source.indexOf("private void init")
        );

        assertTrue(source.contains("public static void onServerStarting(ServerStartingEvent event)"));
        assertTrue(source.contains("BoardRobotPicker.onServerStart();"));
        assertFalse(constructor.contains("BoardRobotPicker.onServerStart();"));
    }

    @Test
    void pickerWithoutAHomeStationUsesStationIndependentSleep() throws IOException {
        String source = read("src/main/java/buildcraft/robotics/boards/BoardRobotPicker.java");
        String noStationBranch = source.substring(
            source.indexOf("if (station == null)"),
            source.indexOf("startDelegateAI(new AIRobotFetchItem")
        );

        assertTrue(noStationBranch.contains("startDelegateAI(new AIRobotSleep(robot));"));
        assertFalse(noStationBranch.contains("startDelegateAI(new AIRobotGotoSleep(robot));"));
        assertTrue(source.contains("ai instanceof AIRobotGotoSleep || ai instanceof AIRobotSleep"));
    }

    @Test
    void requesterStorageSlotsRemainTemplateFiltered() throws IOException {
        String tile = read("src/main/java/buildcraft/robotics/tile/TileRequester.java");
        String canSetRealSlot = tile.substring(
            tile.indexOf("public boolean canSetRealSlot"),
            tile.indexOf("public void setRequest")
        );
        String container = read("src/main/java/buildcraft/robotics/container/ContainerRequester.java");

        assertTrue(canSetRealSlot.contains("!template.isEmpty()"));
        assertTrue(canSetRealSlot.contains("StackUtil.isMatchingItemOrList(template, stack)"));
        assertTrue(container.contains("requester.canSetRealSlot(handlerIndex, stack)"));
    }

    @Test
    void robotHarvestUsesTheToolAwareLootPathAfterProtectionChecks() throws IOException {
        String source = read("src/main/java/buildcraft/lib/misc/BlockUtil.java");
        String harvestMethod = source.substring(
            source.indexOf("public static boolean harvestBlock"),
            source.indexOf("public static boolean destroyBlock")
        );

        assertTrue(harvestMethod.contains("MinecraftForge.EVENT_BUS.post(breakEvent)"));
        assertTrue(harvestMethod.contains("if (breakEvent.isCanceled())"));
        assertTrue(harvestMethod.contains("ItemStack harvestTool = tool.copy()"));
        assertTrue(harvestMethod.contains("world.removeBlock(pos, false)"));
        assertTrue(harvestMethod.contains(".playerDestroy("));
        assertFalse(harvestMethod.contains("world.destroyBlock(pos"));
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }
}
