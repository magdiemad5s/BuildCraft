/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import buildcraft.api.robots.AIRobot;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.robotics.BCRoboticsBoards;
import buildcraft.robotics.BCRoboticsBoards.BoardEntry;
import buildcraft.robotics.BCRoboticsItems;
import buildcraft.robotics.entity.EntityRobot;
import buildcraft.robotics.item.ItemRedstoneBoard;
import buildcraft.robotics.item.ItemRobot;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runtime registration, item-codec, and AI bootstrap contracts for every restored classic robot board. */
@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class RoboticsBoardGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    private static final Map<String, String> EXPECTED_BOARD_CLASSES = Map.ofEntries(
        Map.entry("picker", "BoardRobotPicker"),
        Map.entry("carrier", "BoardRobotCarrier"),
        Map.entry("fluid_carrier", "BoardRobotFluidCarrier"),
        Map.entry("lumberjack", "BoardRobotLumberjack"),
        Map.entry("harvester", "BoardRobotHarvester"),
        Map.entry("miner", "BoardRobotMiner"),
        Map.entry("planter", "BoardRobotPlanter"),
        Map.entry("farmer", "BoardRobotFarmer"),
        Map.entry("leave_cutter", "BoardRobotLeaveCutter"),
        Map.entry("butcher", "BoardRobotButcher"),
        Map.entry("shovelman", "BoardRobotShovelman"),
        Map.entry("pump", "BoardRobotPump"),
        Map.entry("delivery", "BoardRobotDelivery"),
        Map.entry("knight", "BoardRobotKnight"),
        Map.entry("bomber", "BoardRobotBomber"),
        Map.entry("stripes", "BoardRobotStripes"),
        Map.entry("builder", "BoardRobotBuilder")
    );

    private RoboticsBoardGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void everyRestoredBoardRoundTripsAndBootstrapsItsAi(GameTestHelper helper) {
        BCRoboticsBoards.init();
        if (BCRoboticsBoards.entriesWithEmpty().size() != EXPECTED_BOARD_CLASSES.size() + 1
            || BCRoboticsBoards.robotEntries().size() != EXPECTED_BOARD_CLASSES.size()) {
            helper.fail("Robot board registry does not contain exactly one clean board and all 17 classic boards");
            return;
        }

        Set<String> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();
        Set<Integer> modelIndices = new HashSet<>();
        for (BoardEntry entry : BCRoboticsBoards.entriesWithEmpty()) {
            if (!ids.add(entry.id()) || !keys.add(entry.key()) || !modelIndices.add(entry.modelIndex())) {
                helper.fail("Duplicate robot board identity for " + entry.key());
                return;
            }
            if (BCRoboticsBoards.getById(entry.id()) != entry || BCRoboticsBoards.getByKey(entry.key()) != entry) {
                helper.fail("Robot board lookup did not preserve " + entry.id() + " / " + entry.key());
                return;
            }

            ItemStack boardStack = ItemRedstoneBoard.createStack(entry);
            ItemStack reloadedBoardStack = ItemStack.of(boardStack.save(new CompoundTag()));
            if (BCRoboticsBoards.getBoard(boardStack) != entry
                || BCRoboticsBoards.getBoard(reloadedBoardStack) != entry) {
                helper.fail("Redstone-board item lost its legacy ID for " + entry.key());
                return;
            }

            if (entry == BCRoboticsBoards.EMPTY) {
                continue;
            }

            ItemStack robotStack = ItemRobot.createRobotStack(entry, EntityRobotBase.MAX_ENERGY);
            ItemStack reloadedRobotStack = ItemStack.of(robotStack.save(new CompoundTag()));
            if (BCRoboticsBoards.getRobotBoard(robotStack) != entry
                || BCRoboticsBoards.getRobotBoard(reloadedRobotStack) != entry
                || ItemRobot.getEnergy(reloadedRobotStack) != EntityRobotBase.MAX_ENERGY) {
                helper.fail("Robot item lost its board or energy for " + entry.key());
                return;
            }

            EntityRobot robot = ((ItemRobot) BCRoboticsItems.ROBOT.get()).createRobot(reloadedRobotStack, helper.getLevel());
            String expectedClass = EXPECTED_BOARD_CLASSES.get(entry.key());
            if (robot == null || robot.getBoardEntry() != entry || robot.getBoard() == null
                || !expectedClass.equals(robot.getBoard().getClass().getSimpleName())) {
                helper.fail("Robot board " + entry.key() + " did not create " + expectedClass);
                return;
            }

            CompoundTag aiTag = new CompoundTag();
            robot.getBoard().writeToNBT(aiTag);
            AIRobot restoredAi = AIRobot.loadAI(aiTag, robot);
            if (restoredAi == null || restoredAi.getClass() != robot.getBoard().getClass()) {
                helper.fail("Robot board AI did not survive its codec for " + entry.key());
                return;
            }

            try {
                robot.getBoard().update();
            } catch (Throwable throwable) {
                helper.fail("Robot board failed its first server update for " + entry.key() + ": " + throwable);
                return;
            }
            if (robot.getBoard().getDelegateAI() == null) {
                helper.fail("Robot board did not schedule work or safe sleep for " + entry.key());
                return;
            }
        }

        helper.succeed();
    }
}
