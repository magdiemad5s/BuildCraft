/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;

import java.util.List;

import buildcraft.api.robots.DockingStation;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.api.robots.IRobotRegistry;
import buildcraft.api.robots.RobotManager;
import buildcraft.lib.gui.slot.IPhantomSlot;
import buildcraft.robotics.BCRoboticsBlocks;
import buildcraft.robotics.BCRoboticsBoards;
import buildcraft.robotics.BCRoboticsEntities;
import buildcraft.robotics.BCRoboticsPlugs;
import buildcraft.robotics.DockingStationPipe;
import buildcraft.robotics.boards.BoardRobotHarvester;
import buildcraft.robotics.container.ContainerRequester;
import buildcraft.robotics.entity.EntityRobot;
import buildcraft.robotics.item.ItemRobot;
import buildcraft.robotics.plug.RobotStationPluggable;
import buildcraft.robotics.tile.TileRequester;
import buildcraft.robotics.tile.TileZonePlanner;
import buildcraft.robotics.zone.ZonePlan;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.tile.TilePipeHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Tick-driven runtime contracts for BuildCraft Robotics.
 *
 * <p>These complement the fast source/NBT contracts in
 * {@code RobotPersistenceAndExtractionContractTest}: they create registered blocks and entities in a real server
 * level, run the normal robot AI tick loop, and exercise the same NBT entry points used during chunk/world reload.</p>
 */
@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class RoboticsGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final Direction STATION_SIDE = Direction.UP;

    private RoboticsGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 240)
    public static void robotItemDeploysAndHarvesterBoardCompletesWithoutDuplication(GameTestHelper helper) {
        BlockPos pipePos = new BlockPos(1, 1, 1);
        BlockPos cropPos = new BlockPos(3, 1, 1);
        EntityRobot[] deployedRobot = new EntityRobot[1];

        helper.setBlock(pipePos, BCTransportBlocks.pipeHolder.get());
        helper.setBlock(cropPos, Blocks.PUMPKIN);

        helper.runAfterDelay(1, () -> {
            TilePipeHolder pipe = requirePipeHolder(helper, pipePos);
            if (pipe == null) {
                return;
            }

            Player player = helper.makeMockPlayer();
            player.getAbilities().instabuild = false;
            RobotStationPluggable plug = installRobotStation(helper, pipe, player);
            if (plug == null) {
                return;
            }

            ItemStack robotItem = ItemRobot.createRobotStack(
                BCRoboticsBoards.getByKey("harvester"),
                EntityRobotBase.MAX_ENERGY
            );
            InteractionResult result = ItemRobot.placeOnStation(
                robotItem,
                player,
                helper.getLevel(),
                pipe,
                STATION_SIDE,
                plug
            );
            if (!result.consumesAction()) {
                helper.fail("Robot item placement did not consume the station interaction");
                return;
            }
        });

        helper.runAfterDelay(3, () -> {
            EntityRobot robot = findOnlyRobot(helper, pipePos);
            if (robot == null) {
                return;
            }
            if (!(robot.getBoard() instanceof BoardRobotHarvester)
                || robot.getBoardEntry() != BCRoboticsBoards.getByKey("harvester")) {
                helper.fail("Deployed robot did not initialize its Harvester board");
                return;
            }
            DockingStation station = robot.getLinkedStation();
            if (station == null
                || robot.getDockingStation() != station
                || station.robotTaking() != robot
                || !station.isMainStation()) {
                helper.fail("Deployed robot was not linked and docked at its main station");
                return;
            }
            deployedRobot[0] = robot;
        });

        helper.runAfterDelay(4, () -> helper.succeedWhen(() -> {
            EntityRobot robot = deployedRobot[0];
            if (robot == null || !robot.isAlive()) {
                helper.fail("Harvester robot disappeared before completing its job");
                return;
            }
            if (!helper.getBlockState(cropPos).isAir()) {
                helper.fail("Harvester board has not removed the mature pumpkin");
                return;
            }

            BlockPos absoluteCrop = helper.absolutePos(cropPos);
            AABB dropArea = AABB.ofSize(Vec3.atCenterOf(absoluteCrop), 8.0D, 8.0D, 8.0D);
            List<ItemEntity> pumpkinDrops = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                dropArea,
                entity -> entity.isAlive() && entity.getItem().is(Items.PUMPKIN)
            );
            int pumpkinCount = pumpkinDrops.stream().mapToInt(entity -> entity.getItem().getCount()).sum();
            if (pumpkinCount != 1) {
                helper.fail("Harvester produced " + pumpkinCount + " pumpkins instead of exactly one");
            }
        }));
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 160)
    public static void robotDockAndZonePlannerSurviveNbtReload(GameTestHelper helper) {
        BlockPos pipePos = new BlockPos(1, 1, 1);
        BlockPos plannerPos = new BlockPos(3, 1, 3);
        BlockPos zonePoint = helper.absolutePos(new BlockPos(4, 1, 4));
        EntityRobot[] restoredRobot = new EntityRobot[1];
        DockingStationPipe[] restoredStation = new DockingStationPipe[1];
        long[] expectedRobotId = new long[1];
        int[] expectedEnergy = new int[1];

        helper.setBlock(pipePos, BCTransportBlocks.pipeHolder.get());
        helper.setBlock(plannerPos, BCRoboticsBlocks.ZONE_PLANNER.get());

        helper.runAfterDelay(1, () -> {
            TilePipeHolder pipe = requirePipeHolder(helper, pipePos);
            if (pipe == null) {
                return;
            }
            Player player = helper.makeMockPlayer();
            player.getAbilities().instabuild = false;
            RobotStationPluggable plug = installRobotStation(helper, pipe, player);
            if (plug == null) {
                return;
            }

            ItemStack robotItem = ItemRobot.createRobotStack(
                BCRoboticsBoards.getByKey("harvester"),
                EntityRobotBase.MAX_ENERGY
            );
            ItemRobot.placeOnStation(robotItem, player, helper.getLevel(), pipe, STATION_SIDE, plug);
        });

        helper.runAfterDelay(4, () -> {
            EntityRobot originalRobot = findOnlyRobot(helper, pipePos);
            TilePipeHolder originalPipe = requirePipeHolder(helper, pipePos);
            if (originalRobot == null || originalPipe == null) {
                return;
            }
            if (!(helper.getBlockEntity(plannerPos) instanceof TileZonePlanner originalPlanner)) {
                helper.fail("Zone Planner block entity was not created");
                return;
            }
            if (!(originalRobot.getLinkedStation() instanceof DockingStationPipe originalStation)
                || originalRobot.getDockingStation() != originalStation) {
                helper.fail("Robot was not docked before the save/load cycle");
                return;
            }

            originalRobot.setItem(0, new ItemStack(Items.DIAMOND, 7));
            ZonePlan plan = new ZonePlan();
            plan.set(zonePoint.getX(), zonePoint.getZ(), true);
            originalPlanner.setMapName("Runtime Zone");
            originalPlanner.setArea(3, plan);
            originalPlanner.selectArea(3);

            CompoundTag robotTag = new CompoundTag();
            originalRobot.addAdditionalSaveData(robotTag);
            CompoundTag stationTag = new CompoundTag();
            originalStation.writeToNBT(stationTag);
            CompoundTag pipeTag = originalPipe.saveWithFullMetadata();
            CompoundTag plannerTag = originalPlanner.saveWithFullMetadata();
            Vec3 savedPosition = originalRobot.position();
            expectedRobotId[0] = originalRobot.getRobotId();
            expectedEnergy[0] = originalRobot.getEnergy();

            originalRobot.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);

            IRobotRegistry registry = RobotManager.registryProvider.getRegistry(helper.getLevel());
            registry.removeStation(originalStation);
            DockingStationPipe loadedStation = new DockingStationPipe();
            loadedStation.readFromNBT(stationTag);
            registry.registerStation(loadedStation);
            restoredStation[0] = loadedStation;

            BlockPos absolutePipePos = helper.absolutePos(pipePos);
            helper.getLevel().removeBlockEntity(absolutePipePos);
            TilePipeHolder loadedPipe = new TilePipeHolder(absolutePipePos, helper.getBlockState(pipePos));
            loadedPipe.load(pipeTag);
            helper.getLevel().setBlockEntity(loadedPipe);

            BlockPos absolutePlannerPos = helper.absolutePos(plannerPos);
            helper.getLevel().removeBlockEntity(absolutePlannerPos);
            TileZonePlanner loadedPlanner = new TileZonePlanner(
                absolutePlannerPos,
                helper.getBlockState(plannerPos)
            );
            loadedPlanner.load(plannerTag);
            helper.getLevel().setBlockEntity(loadedPlanner);

            EntityRobot loadedRobot = new EntityRobot(BCRoboticsEntities.ROBOT.get(), helper.getLevel());
            loadedRobot.readAdditionalSaveData(robotTag);
            if (loadedRobot.getEnergy() != expectedEnergy[0]) {
                helper.fail(
                    "Robot energy changed from " + expectedEnergy[0] + " to " + loadedRobot.getEnergy()
                        + " while decoding its saved NBT"
                );
                return;
            }
            loadedRobot.setPos(savedPosition.x, savedPosition.y, savedPosition.z);
            if (!helper.getLevel().addFreshEntity(loadedRobot)) {
                helper.fail("Reloaded robot entity could not be added to the server level");
                return;
            }
            restoredRobot[0] = loadedRobot;
        });

        helper.runAfterDelay(10, () -> {
            EntityRobot robot = restoredRobot[0];
            DockingStationPipe station = restoredStation[0];
            if (robot == null || station == null || !robot.isAlive()) {
                helper.fail("Robot or docking station disappeared during reload");
                return;
            }
            if (robot.getRobotId() != expectedRobotId[0]
                || robot.getBoardEntry() != BCRoboticsBoards.getByKey("harvester")
                || !(robot.getBoard() instanceof BoardRobotHarvester)) {
                helper.fail("Robot lost its stable ID or board while reloading");
                return;
            }
            if (robot.getEnergy() <= 0 || robot.getEnergy() > expectedEnergy[0]) {
                helper.fail(
                    "Reloaded robot energy is outside its valid post-tick range: " + robot.getEnergy()
                        + " (saved " + expectedEnergy[0] + ")"
                );
                return;
            }
            if (!robot.getItem(0).is(Items.DIAMOND) || robot.getItem(0).getCount() != 7) {
                helper.fail("Robot inventory did not survive NBT reload");
                return;
            }
            if (robot.getLinkedStation() != station
                || robot.getDockingStation() != station
                || station.robotTaking() != robot
                || station.robotIdTaking() != expectedRobotId[0]
                || !station.isMainStation()) {
                helper.fail("Robot did not rebind its persisted main docking station");
                return;
            }

            TilePipeHolder pipe = requirePipeHolder(helper, pipePos);
            if (pipe == null) {
                return;
            }
            if (!(pipe.getPluggable(STATION_SIDE) instanceof RobotStationPluggable loadedPlug)
                || loadedPlug.getStation() != station) {
                helper.fail("Reloaded pipe pluggable did not resolve the persisted docking station");
                return;
            }
            if (!(helper.getBlockEntity(plannerPos) instanceof TileZonePlanner planner)
                || planner.getCurrentSelectedArea() != 3
                || !"Runtime Zone".equals(planner.mapName)
                || !planner.layers[3].get(zonePoint.getX(), zonePoint.getZ())) {
                helper.fail("Zone Planner name, selected layer, or zone cells did not survive NBT reload");
                return;
            }

            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void requesterMenuTransferAndPersistenceContracts(GameTestHelper helper) {
        BlockPos requesterPos = new BlockPos(1, 1, 1);
        helper.setBlock(requesterPos, BCRoboticsBlocks.REQUESTER.get());

        helper.runAfterDelay(1, () -> {
            if (!(helper.getBlockEntity(requesterPos) instanceof TileRequester requester)) {
                helper.fail("Requester block entity was not created");
                return;
            }

            BlockPos absolutePos = helper.absolutePos(requesterPos);
            Player player = helper.makeMockPlayer();
            player.setPos(absolutePos.getX() + 0.5D, absolutePos.getY() + 0.5D, absolutePos.getZ() + 0.5D);
            if (!(requester.createMenu(70, player.getInventory(), player) instanceof ContainerRequester menu)) {
                helper.fail("Requester did not create its registered menu");
                return;
            }

            if (menu.slots.size() != 76) {
                helper.fail("Requester menu has " + menu.slots.size() + " slots instead of 76");
                return;
            }
            for (int slot = 0; slot < TileRequester.NB_ITEMS; slot++) {
                if (!(menu.slots.get(slot) instanceof IPhantomSlot)) {
                    helper.fail("Requester template slot " + slot + " is not phantom");
                    return;
                }
                if (menu.slots.get(TileRequester.NB_ITEMS + slot) instanceof IPhantomSlot) {
                    helper.fail("Requester storage slot " + slot + " is incorrectly phantom");
                    return;
                }
            }
            for (int slot = TileRequester.NB_ITEMS * 2; slot < menu.slots.size(); slot++) {
                if (menu.slots.get(slot).container != player.getInventory()) {
                    helper.fail("Requester player slot " + slot + " is not backed by the player inventory");
                    return;
                }
            }

            menu.setCarried(new ItemStack(Items.IRON_INGOT, 16));
            menu.clicked(0, 0, ClickType.PICKUP, player);
            ItemStack template = requester.getRequestTemplate(0);
            if (!template.is(Items.IRON_INGOT) || template.getCount() != 16) {
                helper.fail("Server menu did not apply the 16-item requester template");
                return;
            }

            player.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 10));
            ItemStack movedIntoRequester = menu.quickMoveStack(player, 40);
            if (!movedIntoRequester.is(Items.IRON_INGOT)
                || movedIntoRequester.getCount() != 10
                || !player.getInventory().getItem(9).isEmpty()
                || !requester.inv.getStackInSlot(0).is(Items.IRON_INGOT)
                || requester.inv.getStackInSlot(0).getCount() != 10) {
                helper.fail("Shift-click did not move matching items from the player into requester storage");
                return;
            }

            ItemStack movedBackToPlayer = menu.quickMoveStack(player, 20);
            if (!movedBackToPlayer.is(Items.IRON_INGOT)
                || movedBackToPlayer.getCount() != 10
                || !requester.inv.getStackInSlot(0).isEmpty()
                || !player.getInventory().getItem(8).is(Items.IRON_INGOT)
                || player.getInventory().getItem(8).getCount() != 10) {
                helper.fail("Shift-click did not move requester storage back into the player inventory");
                return;
            }
            player.getInventory().setItem(8, ItemStack.EMPTY);

            player.getInventory().setItem(9, new ItemStack(Items.GOLD_INGOT, 4));
            menu.quickMoveStack(player, 40);

            int goldInRequester = 0;
            for (int slot = 0; slot < TileRequester.NB_ITEMS; slot++) {
                ItemStack stored = requester.inv.getStackInSlot(slot);
                if (stored.is(Items.GOLD_INGOT)) {
                    goldInRequester += stored.getCount();
                }
            }
            int goldInPlayer = 0;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stored = player.getInventory().getItem(slot);
                if (stored.is(Items.GOLD_INGOT)) {
                    goldInPlayer += stored.getCount();
                }
            }
            if (goldInRequester != 0 || goldInPlayer != 4) {
                helper.fail("Requester accepted an item that did not match its template");
                return;
            }
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (player.getInventory().getItem(slot).is(Items.GOLD_INGOT)) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                }
            }

            requester.setRequest(1, new ItemStack(Items.DIAMOND, 6));
            ItemStack firstRemainder = requester.offerItem(1, new ItemStack(Items.DIAMOND, 4));
            ItemStack secondRemainder = requester.offerItem(1, new ItemStack(Items.DIAMOND, 4));
            ItemStack rejected = requester.offerItem(1, new ItemStack(Items.GOLD_INGOT, 3));
            if (!firstRemainder.isEmpty()
                || !secondRemainder.is(Items.DIAMOND)
                || secondRemainder.getCount() != 2
                || !rejected.is(Items.GOLD_INGOT)
                || rejected.getCount() != 3
                || requester.inv.getStackInSlot(1).getCount() != 6
                || !requester.getRequest(1).isEmpty()) {
                helper.fail("Requester offerItem did not cap delivery at the requested amount");
                return;
            }

            menu.setCarried(ItemStack.EMPTY);
            menu.clicked(0, 0, ClickType.PICKUP, player);
            if (!requester.getRequestTemplate(0).isEmpty()) {
                helper.fail("Server menu did not clear the requester template");
                return;
            }

            requester.setRequest(0, new ItemStack(Items.IRON_INGOT, 16));
            requester.inv.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 5));
            CompoundTag saved = requester.saveWithFullMetadata();
            if (!saved.contains("items") || !saved.contains("inv") || !saved.contains("req")) {
                helper.fail("Requester save is missing modern data or legacy inv/req compatibility keys");
                return;
            }

            TileRequester modernReload = new TileRequester(absolutePos, helper.getBlockState(requesterPos));
            modernReload.load(saved);
            if (!hasRequesterState(modernReload)) {
                helper.fail("Requester modern item-manager data did not survive NBT reload");
                return;
            }

            CompoundTag legacyOnly = saved.copy();
            legacyOnly.remove("items");
            TileRequester legacyReload = new TileRequester(absolutePos, helper.getBlockState(requesterPos));
            legacyReload.load(legacyOnly);
            if (!hasRequesterState(legacyReload)) {
                helper.fail("Requester legacy inv/req data did not migrate during NBT reload");
                return;
            }

            menu.removed(player);
            helper.succeed();
        });
    }

    private static boolean hasRequesterState(TileRequester requester) {
        ItemStack ironRequest = requester.getRequestTemplate(0);
        ItemStack ironStored = requester.inv.getStackInSlot(0);
        ItemStack diamondRequest = requester.getRequestTemplate(1);
        ItemStack diamondStored = requester.inv.getStackInSlot(1);
        return ironRequest.is(Items.IRON_INGOT)
            && ironRequest.getCount() == 16
            && ironStored.is(Items.IRON_INGOT)
            && ironStored.getCount() == 5
            && diamondRequest.is(Items.DIAMOND)
            && diamondRequest.getCount() == 6
            && diamondStored.is(Items.DIAMOND)
            && diamondStored.getCount() == 6;
    }

    private static RobotStationPluggable installRobotStation(
        GameTestHelper helper,
        TilePipeHolder pipe,
        Player player
    ) {
        pipe.onPlacedBy(player, new ItemStack(BCTransportItems.PIPE_ITEM_STONE.get()));
        if (BCRoboticsPlugs.robotStation == null) {
            helper.fail("Robot station pluggable definition was not initialized");
            return null;
        }
        RobotStationPluggable plug = new RobotStationPluggable(
            BCRoboticsPlugs.robotStation,
            pipe,
            STATION_SIDE
        );
        pipe.replacePluggable(STATION_SIDE, plug);
        plug.onPlacedBy(player);
        if (plug.getStation() == null) {
            helper.fail("Robot station did not register a docking station");
            return null;
        }
        return plug;
    }

    private static TilePipeHolder requirePipeHolder(GameTestHelper helper, BlockPos position) {
        if (helper.getBlockEntity(position) instanceof TilePipeHolder pipeHolder) {
            return pipeHolder;
        }
        helper.fail("Expected a BuildCraft pipe holder at " + position);
        return null;
    }

    private static EntityRobot findOnlyRobot(GameTestHelper helper, BlockPos nearPosition) {
        BlockPos absolutePosition = helper.absolutePos(nearPosition);
        AABB searchArea = AABB.ofSize(Vec3.atCenterOf(absolutePosition), 7.0D, 7.0D, 7.0D);
        List<EntityRobot> robots = helper.getLevel().getEntitiesOfClass(
            EntityRobot.class,
            searchArea,
            Entity::isAlive
        );
        if (robots.size() != 1) {
            helper.fail("Expected exactly one live robot near the station, found " + robots.size());
            return null;
        }
        return robots.get(0);
    }
}
