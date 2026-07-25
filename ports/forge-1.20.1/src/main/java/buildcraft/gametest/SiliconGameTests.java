/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;

import java.util.Map;

import buildcraft.api.enums.EnumEngineType;
import buildcraft.api.enums.EnumRedstoneChipset;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.core.BCCoreItems;
import buildcraft.lib.recipe.AssemblyRecipeBasic;
import buildcraft.robotics.BCRoboticsBoards;
import buildcraft.robotics.BCRoboticsBoards.BoardEntry;
import buildcraft.robotics.item.ItemRedstoneBoard;
import buildcraft.robotics.item.ItemRobot;
import buildcraft.robotics.recipes.RobotIntegrationRecipe;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.EnumAssemblyRecipeState;
import buildcraft.silicon.gate.EnumGateLogic;
import buildcraft.silicon.gate.EnumGateMaterial;
import buildcraft.silicon.gate.EnumGateModifier;
import buildcraft.silicon.gate.GateVariant;
import buildcraft.silicon.item.ItemPluggableGate;
import buildcraft.silicon.tile.TileAdvancedCraftingTable;
import buildcraft.silicon.tile.TileAssemblyTable;
import buildcraft.silicon.tile.TileChargingTable;
import buildcraft.silicon.tile.TileIntegrationTable;
import buildcraft.silicon.tile.TileLaser;
import buildcraft.silicon.tile.TileLaserTableBase;
import buildcraft.silicon.tile.TileProgrammingTable_Neptune;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Tick-driven runtime gates for the restored BuildCraft Silicon production workflows. */
@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class SiliconGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final ResourceLocation TIMER_RECIPE =
        new ResourceLocation("buildcraftsilicon", "assembly/plug_timer");
    private static final ResourceLocation PULSAR_RECIPE =
        new ResourceLocation("buildcraftsilicon", "assembly/plug_pulsar");
    private static final ResourceLocation IRON_CHIPSET_RECIPE =
        new ResourceLocation("buildcraftsilicon", "assembly/iron_chipset");
    private static final ResourceLocation IRON_GATE_RECIPE =
        new ResourceLocation("buildcraftsilicon", "assembly/gate/and_iron_no_modifier");
    private static final ResourceLocation TEST_ENERGY_CAPABILITY_ID =
        new ResourceLocation("buildcraftsilicon", "charging_table_test_energy");
    private static final String TEST_ENERGY_MARKER = "BuildCraftNeoChargingTableTest";
    private static final int TEST_ENERGY_CAPACITY = 4_096;
    private static boolean testEnergyListenerRegistered;

    private SiliconGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void allChipsetGateAndPlugRecipesRetainLegacyOutputs(GameTestHelper helper) {
        for (EnumRedstoneChipset chipset : EnumRedstoneChipset.values()) {
            String recipeName = chipset == EnumRedstoneChipset.RED
                ? "redstone_chipset"
                : chipset.getSerializedName() + "_chipset";
            ResourceLocation recipeId = new ResourceLocation("buildcraftsilicon", "assembly/" + recipeName);
            ItemStack output = requireAssemblyRecipe(helper, recipeId)
                .getResultItem(helper.getLevel().registryAccess());
            if (!output.is(BCSiliconItems.REDSTONE_CHIPSET.get())
                || EnumRedstoneChipset.fromStack(output) != chipset) {
                helper.fail("Assembly recipe " + recipeId + " produced the wrong chipset variant: " + output);
                return;
            }
            assertPreviewContains(helper, recipeId, output);
        }

        EnumGateMaterial[] materials = {
            EnumGateMaterial.IRON,
            EnumGateMaterial.NETHER_BRICK,
            EnumGateMaterial.GOLD
        };
        for (EnumGateLogic logic : EnumGateLogic.VALUES) {
            for (EnumGateMaterial material : materials) {
                for (EnumGateModifier modifier : EnumGateModifier.VALUES) {
                    String path = "assembly/gate/"
                        + (modifier == EnumGateModifier.NO_MODIFIER ? "" : "modifier/")
                        + logic.tag + "_" + material.tag + "_" + modifier.tag;
                    ResourceLocation recipeId = new ResourceLocation("buildcraftsilicon", path);
                    ItemStack output = requireAssemblyRecipe(helper, recipeId)
                        .getResultItem(helper.getLevel().registryAccess());
                    if (!output.is(BCSiliconItems.PLUG_GATE_ITEM.get())) {
                        helper.fail("Assembly recipe " + recipeId + " did not produce a gate");
                        return;
                    }
                    GateVariant variant = ItemPluggableGate.getVariant(output);
                    if (variant.logic != logic || variant.material != material || variant.modifier != modifier) {
                        helper.fail("Assembly recipe " + recipeId + " produced the wrong legacy gate NBT");
                        return;
                    }
                    assertPreviewContains(helper, recipeId, output);
                }
            }
        }

        Map<ResourceLocation, Item> simplePlugs = Map.of(
            TIMER_RECIPE, BCSiliconItems.PLUG_TIMER_ITEM.get(),
            PULSAR_RECIPE, BCSiliconItems.PLUG_PULSAR_ITEM.get(),
            new ResourceLocation("buildcraftsilicon", "assembly/light_sensor"),
                BCSiliconItems.PLUG_LIGHT_SENSOR_ITEM.get(),
            new ResourceLocation("buildcraftsilicon", "assembly/gate_copier"),
                BCSiliconItems.GATE_COPIER_ITEM.get()
        );
        for (Map.Entry<ResourceLocation, Item> entry : simplePlugs.entrySet()) {
            ItemStack output = requireAssemblyRecipe(helper, entry.getKey())
                .getResultItem(helper.getLevel().registryAccess());
            if (!output.is(entry.getValue())) {
                helper.fail("Assembly recipe " + entry.getKey() + " produced " + output);
                return;
            }
            assertPreviewContains(helper, entry.getKey(), output);
        }

        for (DyeColor colour : DyeColor.values()) {
            assertLensRecipe(
                helper,
                new ResourceLocation("buildcraftsilicon", "assembly/lens/regular/" + colour.getSerializedName()),
                colour.getId()
            );
            assertLensRecipe(
                helper,
                new ResourceLocation("buildcraftsilicon", "assembly/lens/filter/" + colour.getSerializedName()),
                colour.getId() + 16
            );
        }
        assertLensRecipe(helper, new ResourceLocation("buildcraftsilicon", "assembly/lens/lens_regular"), 32);
        assertLensRecipe(helper, new ResourceLocation("buildcraftsilicon", "assembly/lens/lens_filter"), 33);
        helper.succeed();
    }
    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 180)
    public static void laserPowersAssemblyTableAcrossBlockEntitySaveLoad(GameTestHelper helper) {
        BlockPos laserPos = new BlockPos(2, 1, 1);
        BlockPos tablePos = new BlockPos(2, 1, 3);
        BlockPos outputPos = new BlockPos(3, 1, 3);
        helper.setBlock(outputPos, Blocks.BARREL);
        helper.setBlock(tablePos, BCSiliconBlocks.ASSEMBLY_TABLE_BLOCK.get());
        helper.setBlock(
            laserPos,
            BCSiliconBlocks.LASER_BLOCK.get().defaultBlockState()
                .setValue(BuildCraftProperties.BLOCK_FACING_6, Direction.SOUTH)
        );

        long[] savedPower = {-1};
        helper.runAfterDelay(2, () -> {
            TileAssemblyTable table = requireBlockEntity(helper, tablePos, TileAssemblyTable.class);
            TileLaser laser = requireBlockEntity(helper, laserPos, TileLaser.class);
            table.inv.setStackInSlot(0, new ItemStack(Items.CLOCK));

            IMjReceiver receiver = laser.getCapability(MjAPI.CAP_RECEIVER).orElse(null);
            if (receiver == null) {
                helper.fail("Laser did not expose its MJ receiver capability");
                return;
            }
            long requested = receiver.getPowerRequested();
            long unused = receiver.receivePower(requested, FluidAction.EXECUTE);
            if (requested <= 0 || unused != 0) {
                helper.fail("Laser rejected " + unused + " of " + requested + " requested microjoules");
            }
        });

        helper.runAfterDelay(4, () -> {
            TileAssemblyTable table = requireBlockEntity(helper, tablePos, TileAssemblyTable.class);
            TileAssemblyTable.AssemblyInstruction instruction = selectAssemblyRecipe(helper, table, TIMER_RECIPE);
            if (!instruction.output.is(BCSiliconItems.PLUG_TIMER_ITEM.get())) {
                helper.fail("Timer assembly recipe exposed the wrong output " + instruction.output);
                return;
            }
            if (table.getTarget() != 500L * MjAPI.MJ) {
                helper.fail("Timer assembly recipe requested " + table.getTarget() + " microjoules");
            }
        });

        helper.runAfterDelay(25, () -> {
            TileAssemblyTable original = requireBlockEntity(helper, tablePos, TileAssemblyTable.class);
            if (original.power <= 0 || original.power >= original.getTarget()) {
                helper.fail("Laser did not partially power the Assembly Table before save/load: " + original.power);
                return;
            }
            savedPower[0] = original.power;
            CompoundTag saved = original.saveWithFullMetadata();
            BlockPos absolutePos = helper.absolutePos(tablePos);
            helper.getLevel().removeBlockEntity(absolutePos);

            TileAssemblyTable restored = new TileAssemblyTable(absolutePos, helper.getBlockState(tablePos));
            restored.load(saved);
            helper.getLevel().setBlockEntity(restored);
        });

        helper.runAfterDelay(28, () -> {
            TileAssemblyTable restored = requireBlockEntity(helper, tablePos, TileAssemblyTable.class);
            if (restored.power < savedPower[0]) {
                helper.fail("Reloaded Assembly Table lost saved laser power");
                return;
            }
            TileAssemblyTable.AssemblyInstruction instruction = findAssemblyRecipe(restored, TIMER_RECIPE);
            if (instruction == null
                || restored.recipesStates.get(instruction) != EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE) {
                helper.fail("Reloaded Assembly Table lost its active timer recipe selection");
                return;
            }

            helper.succeedWhen(() -> {
                Container output = requireContainer(helper, outputPos);
                if (findItem(output, BCSiliconItems.PLUG_TIMER_ITEM.get()).isEmpty()) {
                    helper.fail("Laser-powered Assembly Table has not produced the timer plug");
                    return;
                }
                TileAssemblyTable tickingTable = requireBlockEntity(helper, tablePos, TileAssemblyTable.class);
                if (!tickingTable.inv.getStackInSlot(0).isEmpty()) {
                    helper.fail("Timer assembly completed without consuming its clock");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 80)
    public static void assemblyTableCombinesCountedIngredientsSplitAcrossSlots(GameTestHelper helper) {
        BlockPos tablePos = new BlockPos(2, 1, 2);
        BlockPos outputPos = new BlockPos(3, 1, 2);
        helper.setBlock(outputPos, Blocks.BARREL);
        helper.setBlock(tablePos, BCSiliconBlocks.ASSEMBLY_TABLE_BLOCK.get());

        helper.runAfterDelay(2, () -> {
            TileAssemblyTable table = requireBlockEntity(helper, tablePos, TileAssemblyTable.class);
            table.inv.setStackInSlot(0, BCCoreItems.getEngineStack(EnumEngineType.WOOD));
            table.inv.setStackInSlot(1, new ItemStack(Items.IRON_INGOT));
            table.inv.setStackInSlot(2, new ItemStack(Items.IRON_INGOT));
        });

        helper.runAfterDelay(4, () -> {
            TileAssemblyTable table = requireBlockEntity(helper, tablePos, TileAssemblyTable.class);
            TileAssemblyTable.AssemblyInstruction instruction = selectAssemblyRecipe(helper, table, PULSAR_RECIPE);
            if (instruction.recipe.getOutputPreviews().stream()
                .noneMatch(stack -> stack.is(BCSiliconItems.PLUG_PULSAR_ITEM.get()))) {
                helper.fail("Pulsar assembly recipe is missing its output preview");
                return;
            }
            powerLaserTarget(helper, table);

            helper.succeedWhen(() -> {
                Container output = requireContainer(helper, outputPos);
                if (findItem(output, BCSiliconItems.PLUG_PULSAR_ITEM.get()).isEmpty()) {
                    helper.fail("Assembly Table has not produced the pulsar plug from split iron stacks");
                    return;
                }
                TileAssemblyTable tickingTable = requireBlockEntity(helper, tablePos, TileAssemblyTable.class);
                for (int slot = 0; slot < 3; slot++) {
                    if (!tickingTable.inv.getStackInSlot(slot).isEmpty()) {
                        helper.fail("Pulsar assembly left an ingredient in slot " + slot);
                        return;
                    }
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 80)
    public static void chipsetAndGateRecipesProduceLegacyNbtVariants(GameTestHelper helper) {
        BlockPos chipsetTablePos = new BlockPos(1, 1, 1);
        BlockPos chipsetOutputPos = new BlockPos(1, 1, 2);
        BlockPos gateTablePos = new BlockPos(3, 1, 1);
        BlockPos gateOutputPos = new BlockPos(3, 1, 2);
        helper.setBlock(chipsetOutputPos, Blocks.BARREL);
        helper.setBlock(gateOutputPos, Blocks.BARREL);
        helper.setBlock(chipsetTablePos, BCSiliconBlocks.ASSEMBLY_TABLE_BLOCK.get());
        helper.setBlock(gateTablePos, BCSiliconBlocks.ASSEMBLY_TABLE_BLOCK.get());

        helper.runAfterDelay(2, () -> {
            TileAssemblyTable chipsetTable = requireBlockEntity(helper, chipsetTablePos, TileAssemblyTable.class);
            chipsetTable.inv.setStackInSlot(0, new ItemStack(Items.REDSTONE));
            chipsetTable.inv.setStackInSlot(1, new ItemStack(Items.IRON_INGOT));

            TileAssemblyTable gateTable = requireBlockEntity(helper, gateTablePos, TileAssemblyTable.class);
            gateTable.inv.setStackInSlot(0, EnumRedstoneChipset.IRON.getStack());
        });

        helper.runAfterDelay(4, () -> {
            TileAssemblyTable chipsetTable = requireBlockEntity(helper, chipsetTablePos, TileAssemblyTable.class);
            TileAssemblyTable gateTable = requireBlockEntity(helper, gateTablePos, TileAssemblyTable.class);
            selectAssemblyRecipe(helper, chipsetTable, IRON_CHIPSET_RECIPE);
            selectAssemblyRecipe(helper, gateTable, IRON_GATE_RECIPE);
            powerLaserTarget(helper, chipsetTable);
            powerLaserTarget(helper, gateTable);

            helper.succeedWhen(() -> {
                ItemStack chipset = findItem(requireContainer(helper, chipsetOutputPos), BCSiliconItems.REDSTONE_CHIPSET.get());
                if (chipset.isEmpty() || EnumRedstoneChipset.fromStack(chipset) != EnumRedstoneChipset.IRON) {
                    helper.fail("Iron chipset recipe produced the wrong legacy Damage variant: " + chipset);
                    return;
                }

                ItemStack gate = findItem(requireContainer(helper, gateOutputPos), BCSiliconItems.PLUG_GATE_ITEM.get());
                if (gate.isEmpty()) {
                    helper.fail("Iron gate assembly recipe has not produced a gate");
                    return;
                }
                ItemStack roundTripped = ItemStack.of(gate.save(new CompoundTag()));
                GateVariant variant = ItemPluggableGate.getVariant(roundTripped);
                if (variant.logic != EnumGateLogic.AND
                    || variant.material != EnumGateMaterial.IRON
                    || variant.modifier != EnumGateModifier.NO_MODIFIER) {
                    helper.fail("Gate recipe lost its legacy gate NBT across ItemStack save/load");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void chargingTableChargesForgeEnergyItemAcrossBlockEntitySaveLoad(GameTestHelper helper) {
        ensureTestEnergyCapabilityRegistered();
        BlockPos tablePos = new BlockPos(2, 1, 2);
        int firstCharge = 1_536;
        long firstPower = (long) firstCharge * MjAPI.MJ;
        long fullTarget = (long) TEST_ENERGY_CAPACITY * MjAPI.MJ;
        helper.setBlock(tablePos, BCSiliconBlocks.CHARGING_TABLE_BLOCK.get());

        helper.runAfterDelay(2, () -> {
            TileChargingTable original = requireBlockEntity(helper, tablePos, TileChargingTable.class);
            ItemStack cell = createTestEnergyCell(helper);
            IEnergyStorage cellEnergy = requireEnergyStorage(helper, cell);
            if (cellEnergy == null) {
                return;
            }
            if (cellEnergy.getEnergyStored() != 0 || cellEnergy.getMaxEnergyStored() != TEST_ENERGY_CAPACITY) {
                helper.fail("Charging Table test cell started with invalid Forge Energy state");
                return;
            }
            if (!original.invCharge.isItemValid(0, cell)) {
                helper.fail("Charging Table rejected a real receive-only Forge Energy item");
                return;
            }
            ItemStack leftover = original.invCharge.insertItem(0, cell, false);
            if (!leftover.isEmpty()) {
                helper.fail("Charging Table failed to insert its Forge Energy test cell");
                return;
            }
            ItemStack installed = original.invCharge.getStackInSlot(0);
            IEnergyStorage installedEnergy = requireEnergyStorage(helper, installed);
            if (installedEnergy == null || original.getTarget() != fullTarget) {
                helper.fail("Charging Table did not request the cell's complete missing energy");
                return;
            }

            IMjReceiver receiver = original.getCapability(MjAPI.CAP_RECEIVER).orElse(null);
            if (receiver == null) {
                helper.fail("Charging Table did not expose its direct MJ receiver capability");
                return;
            }
            long rejectedNegative = -MjAPI.MJ;
            if (receiver.receivePower(rejectedNegative, FluidAction.EXECUTE) != rejectedNegative
                || original.power != 0) {
                helper.fail("Charging Table consumed non-positive direct MJ input");
                return;
            }
            if (receiver.receivePower(firstPower, FluidAction.SIMULATE) != 0 || original.power != 0) {
                helper.fail("Charging Table direct-MJ simulation mutated state or rejected requested power");
                return;
            }
            if (receiver.receivePower(firstPower, FluidAction.EXECUTE) != 0 || original.power != firstPower) {
                helper.fail("Charging Table rejected or miscounted executable direct MJ input");
                return;
            }

            CompoundTag saved = original.saveWithFullMetadata();
            BlockPos absolutePos = helper.absolutePos(tablePos);

            CompoundTag oversized = saved.copy();
            oversized.putLong("power", Long.MAX_VALUE);
            TileChargingTable clamped = new TileChargingTable(absolutePos, helper.getBlockState(tablePos));
            clamped.load(oversized);
            if (clamped.power != fullTarget) {
                helper.fail("Charging Table did not clamp oversized saved MJ to its current item target");
                return;
            }

            helper.getLevel().removeBlockEntity(absolutePos);
            TileChargingTable restored = new TileChargingTable(absolutePos, helper.getBlockState(tablePos));
            restored.load(saved);
            ItemStack restoredCell = restored.invCharge.getStackInSlot(0);
            IEnergyStorage restoredEnergy = requireEnergyStorage(helper, restoredCell);
            if (!restoredCell.is(Items.REDSTONE) || restoredEnergy == null
                || restoredEnergy.getEnergyStored() != 0
                || restoredEnergy.getMaxEnergyStored() != TEST_ENERGY_CAPACITY) {
                helper.fail("Charging Table lost its Forge Energy item capability across block-entity NBT load");
                return;
            }
            if (restored.power != firstPower || restored.getRequiredLaserPower() != fullTarget - firstPower) {
                helper.fail("Charging Table lost or misbounded its accumulated MJ across block-entity NBT load");
                return;
            }
            helper.getLevel().setBlockEntity(restored);
        });

        helper.runAfterDelay(5, () -> {
            TileChargingTable restored = requireBlockEntity(helper, tablePos, TileChargingTable.class);
            IEnergyStorage restoredEnergy = requireEnergyStorage(helper, restored.invCharge.getStackInSlot(0));
            if (restoredEnergy == null) {
                return;
            }
            if (restoredEnergy.getEnergyStored() != firstCharge || restored.power != 0) {
                helper.fail("Reloaded Charging Table did not convert its saved MJ into exact Forge Energy");
                return;
            }

            long remaining = restored.getRequiredLaserPower();
            long expectedRemaining = (long) (TEST_ENERGY_CAPACITY - firstCharge) * MjAPI.MJ;
            if (remaining != expectedRemaining || restored.receiveLaserPower(remaining) != 0) {
                helper.fail("Reloaded Charging Table rejected the laser power needed to finish charging");
                return;
            }

            helper.succeedWhen(() -> {
                TileChargingTable tickingTable = requireBlockEntity(helper, tablePos, TileChargingTable.class);
                ItemStack chargedCell = tickingTable.invCharge.getStackInSlot(0);
                IEnergyStorage chargedEnergy = requireEnergyStorage(helper, chargedCell);
                if (chargedEnergy == null) {
                    return;
                }
                if (chargedEnergy.getEnergyStored() != TEST_ENERGY_CAPACITY) {
                    helper.fail("Charging Table has not filled the Forge Energy item to capacity");
                    return;
                }
                if (tickingTable.power != 0 || tickingTable.getTarget() != 0
                    || tickingTable.getRequiredLaserPower() != 0) {
                    helper.fail("Charging Table retained MJ demand after completing the item charge");
                    return;
                }

                CompoundTag completedSave = tickingTable.saveWithFullMetadata();
                TileChargingTable roundTripped = new TileChargingTable(
                    helper.absolutePos(tablePos), helper.getBlockState(tablePos)
                );
                roundTripped.load(completedSave);
                IEnergyStorage roundTrippedEnergy =
                    requireEnergyStorage(helper, roundTripped.invCharge.getStackInSlot(0));
                if (roundTrippedEnergy == null
                    || roundTrippedEnergy.getEnergyStored() != TEST_ENERGY_CAPACITY
                    || roundTripped.power != 0
                    || roundTripped.getTarget() != 0) {
                    helper.fail("Fully charged item or Charging Table state did not survive a second NBT round-trip");
                    return;
                }
                IMjReceiver completedReceiver = roundTripped.getCapability(MjAPI.CAP_RECEIVER).orElse(null);
                if (completedReceiver == null || completedReceiver.getPowerRequested() != 0) {
                    helper.fail("Completed Charging Table continued requesting direct MJ");
                    return;
                }

                CompoundTag negative = completedSave.copy();
                negative.putLong("power", -MjAPI.MJ);
                TileChargingTable sanitized = new TileChargingTable(
                    helper.absolutePos(tablePos), helper.getBlockState(tablePos)
                );
                sanitized.load(negative);
                if (sanitized.power != 0) {
                    helper.fail("Charging Table retained negative saved MJ");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void integrationTableProgramsRobotAcrossBlockEntitySaveLoad(GameTestHelper helper) {
        BlockPos tablePos = new BlockPos(2, 1, 2);
        helper.setBlock(tablePos, BCSiliconBlocks.INTERGRATION_TABLE_BLOCK.get());
        BCRoboticsBoards.init();
        BoardEntry picker = BCRoboticsBoards.getByKey("picker");
        int preservedEnergy = 4_321;
        long[] savedPower = {-1};

        helper.runAfterDelay(2, () -> {
            TileIntegrationTable table = requireBlockEntity(helper, tablePos, TileIntegrationTable.class);
            table.invTarget.setStackInSlot(0, ItemRobot.createRobotStack(BCRoboticsBoards.EMPTY, preservedEnergy));
            table.invToIntegrate.setStackInSlot(0, ItemRedstoneBoard.createStack(picker));
        });

        helper.runAfterDelay(5, () -> {
            TileIntegrationTable original = requireBlockEntity(helper, tablePos, TileIntegrationTable.class);
            if (original.recipe == null || !RobotIntegrationRecipe.ID.equals(original.recipe.name)) {
                helper.fail("Integration Table did not resolve the registered robot-board recipe");
                return;
            }
            long target = original.getTarget();
            long offered = target / 2;
            if (target <= 0 || original.receiveLaserPower(offered) != 0) {
                helper.fail("Integration Table rejected partial laser power for its robot recipe");
                return;
            }
            savedPower[0] = original.power;
            CompoundTag saved = original.saveWithFullMetadata();
            BlockPos absolutePos = helper.absolutePos(tablePos);
            helper.getLevel().removeBlockEntity(absolutePos);

            TileIntegrationTable restored = new TileIntegrationTable(absolutePos, helper.getBlockState(tablePos));
            restored.load(saved);
            helper.getLevel().setBlockEntity(restored);
        });

        helper.runAfterDelay(8, () -> {
            TileIntegrationTable restored = requireBlockEntity(helper, tablePos, TileIntegrationTable.class);
            if (restored.recipe == null || !RobotIntegrationRecipe.ID.equals(restored.recipe.name)
                || restored.power != savedPower[0]) {
                helper.fail("Reloaded Integration Table lost its recipe or accumulated power");
                return;
            }
            if (restored.receiveLaserPower(restored.getRequiredLaserPower()) != 0) {
                helper.fail("Reloaded Integration Table rejected its remaining requested laser power");
                return;
            }

            helper.succeedWhen(() -> {
                TileIntegrationTable tickingTable = requireBlockEntity(helper, tablePos, TileIntegrationTable.class);
                ItemStack result = tickingTable.invResult.getStackInSlot(0);
                if (result.isEmpty() || BCRoboticsBoards.getRobotBoard(result) != picker) {
                    helper.fail("Integration Table has not produced the picker robot");
                    return;
                }
                if (ItemRobot.getEnergy(result) != preservedEnergy) {
                    helper.fail("Integration Table changed the robot's stored energy");
                    return;
                }
                if (!tickingTable.invTarget.getStackInSlot(0).isEmpty()
                    || !tickingTable.invToIntegrate.getStackInSlot(0).isEmpty()) {
                    helper.fail("Integration Table produced a robot without consuming both inputs");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void programmingTableProgramsBoardAcrossBlockEntitySaveLoad(GameTestHelper helper) {
        BlockPos tablePos = new BlockPos(2, 1, 2);
        helper.setBlock(tablePos, BCSiliconBlocks.PROGRAMMING_TABLE_BLOCK.get());
        BCRoboticsBoards.init();
        BoardEntry picker = BCRoboticsBoards.getByKey("picker");
        long[] savedPower = {-1};

        helper.runAfterDelay(2, () -> {
            TileProgrammingTable_Neptune original =
                requireBlockEntity(helper, tablePos, TileProgrammingTable_Neptune.class);
            original.invInput.setStackInSlot(0, ItemRedstoneBoard.createStack(BCRoboticsBoards.EMPTY));
            int option = original.getOptions().indexOf(picker);
            if (option < 0) {
                helper.fail("Programming Table did not expose the picker board option");
                return;
            }
            original.selectOption(option);
            long offered = original.getTarget() / 2;
            if (offered <= 0 || original.receiveLaserPower(offered) != 0) {
                helper.fail("Programming Table rejected partial laser power");
                return;
            }
            savedPower[0] = original.power;
            CompoundTag saved = original.saveWithFullMetadata();
            BlockPos absolutePos = helper.absolutePos(tablePos);
            helper.getLevel().removeBlockEntity(absolutePos);

            TileProgrammingTable_Neptune restored =
                new TileProgrammingTable_Neptune(absolutePos, helper.getBlockState(tablePos));
            restored.load(saved);
            helper.getLevel().setBlockEntity(restored);
        });

        helper.runAfterDelay(5, () -> {
            TileProgrammingTable_Neptune restored =
                requireBlockEntity(helper, tablePos, TileProgrammingTable_Neptune.class);
            if (restored.getSelectedBoard() != picker || restored.power != savedPower[0]) {
                helper.fail("Reloaded Programming Table lost its board selection or accumulated power");
                return;
            }
            if (restored.receiveLaserPower(restored.getRequiredLaserPower()) != 0) {
                helper.fail("Reloaded Programming Table rejected its remaining requested laser power");
                return;
            }

            helper.succeedWhen(() -> {
                TileProgrammingTable_Neptune tickingTable =
                    requireBlockEntity(helper, tablePos, TileProgrammingTable_Neptune.class);
                ItemStack result = tickingTable.invOutput.getStackInSlot(0);
                if (result.isEmpty() || BCRoboticsBoards.getBoard(result) != picker) {
                    helper.fail("Programming Table has not produced the selected picker board");
                    return;
                }
                if (!tickingTable.invInput.getStackInSlot(0).isEmpty() || tickingTable.selectedOption != -1) {
                    helper.fail("Programming Table did not consume the blank board and clear its selection");
                }
            });
        });
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 120)
    public static void advancedCraftingTableCraftsAfterBlockEntitySaveLoad(GameTestHelper helper) {
        BlockPos tablePos = new BlockPos(2, 1, 2);
        helper.setBlock(tablePos, BCSiliconBlocks.ADVANCED_CRAFTING_TABLE_BLOCK.get());

        helper.runAfterDelay(2, () -> {
            TileAdvancedCraftingTable table =
                requireBlockEntity(helper, tablePos, TileAdvancedCraftingTable.class);
            Recipe<?> found = helper.getLevel().getRecipeManager()
                .byKey(new ResourceLocation("minecraft", "stick"))
                .orElse(null);
            if (!(found instanceof CraftingRecipe stickRecipe)) {
                helper.fail("Vanilla stick recipe was not loaded as a crafting recipe");
                return;
            }
            table.invMaterials.setStackInSlot(0, new ItemStack(Items.BIRCH_PLANKS));
            table.invMaterials.setStackInSlot(1, new ItemStack(Items.OAK_PLANKS));
            if (!table.applyRecipeSelection(stickRecipe)) {
                helper.fail("Advanced Crafting Table rejected the server-resolved stick recipe");
            }
        });

        helper.runAfterDelay(5, () -> {
            TileAdvancedCraftingTable original =
                requireBlockEntity(helper, tablePos, TileAdvancedCraftingTable.class);
            if (!original.getWorkbenchCrafting().canCraft()) {
                helper.fail("Advanced Crafting Table did not recognize its materials through normal ticks");
                return;
            }
            CompoundTag saved = original.saveWithFullMetadata();
            BlockPos absolutePos = helper.absolutePos(tablePos);
            helper.getLevel().removeBlockEntity(absolutePos);

            TileAdvancedCraftingTable restored =
                new TileAdvancedCraftingTable(absolutePos, helper.getBlockState(tablePos));
            restored.load(saved);
            helper.getLevel().setBlockEntity(restored);
        });

        helper.runAfterDelay(8, () -> {
            TileAdvancedCraftingTable restored =
                requireBlockEntity(helper, tablePos, TileAdvancedCraftingTable.class);
            if (!restored.invBlueprint.getStackInSlot(1).is(Items.BIRCH_PLANKS)
                || !restored.invBlueprint.getStackInSlot(4).is(Items.OAK_PLANKS)) {
                helper.fail("Reloaded Advanced Crafting Table lost its centered phantom blueprint");
                return;
            }
            IMjReceiver receiver = restored.getCapability(MjAPI.CAP_RECEIVER).orElse(null);
            if (receiver == null) {
                helper.fail("Advanced Crafting Table did not expose its direct MJ receiver");
                return;
            }
            long requested = receiver.getPowerRequested();
            if (requested <= 0 || receiver.receivePower(requested, FluidAction.EXECUTE) != 0) {
                helper.fail("Reloaded Advanced Crafting Table rejected its requested MJ");
                return;
            }

            helper.succeedWhen(() -> {
                TileAdvancedCraftingTable tickingTable =
                    requireBlockEntity(helper, tablePos, TileAdvancedCraftingTable.class);
                ItemStack result = tickingTable.invResults.getStackInSlot(0);
                if (!result.is(Items.STICK) || result.getCount() != 4) {
                    helper.fail("Advanced Crafting Table output is " + result + " instead of four sticks");
                    return;
                }
                if (!tickingTable.invMaterials.getStackInSlot(0).isEmpty()
                    || !tickingTable.invMaterials.getStackInSlot(1).isEmpty()) {
                    helper.fail("Advanced Crafting Table crafted without consuming its saved materials");
                }
            });
        });
    }

    private static synchronized void ensureTestEnergyCapabilityRegistered() {
        if (testEnergyListenerRegistered) {
            return;
        }
        MinecraftForge.EVENT_BUS.addGenericListener(
            ItemStack.class,
            SiliconGameTests::attachTestEnergyCapability
        );
        testEnergyListenerRegistered = true;
    }

    private static void attachTestEnergyCapability(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        CompoundTag tag = stack.getTag();
        if (stack.is(Items.REDSTONE) && tag != null && tag.getBoolean(TEST_ENERGY_MARKER)) {
            event.addCapability(TEST_ENERGY_CAPABILITY_ID, new TestEnergyCapability());
        }
    }

    private static ItemStack createTestEnergyCell(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.REDSTONE);
        stack.getOrCreateTag().putBoolean(TEST_ENERGY_MARKER, true);
        if (requireEnergyStorage(helper, stack) == null) {
            return ItemStack.EMPTY;
        }
        return stack;
    }

    private static IEnergyStorage requireEnergyStorage(GameTestHelper helper, ItemStack stack) {
        IEnergyStorage energy = stack.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        if (energy == null) {
            helper.fail("Expected the marked Charging Table test item to expose Forge Energy");
        }
        return energy;
    }

    private static final class TestEnergyCapability implements ICapabilitySerializable<Tag> {
        private final EnergyStorage storage =
            new EnergyStorage(TEST_ENERGY_CAPACITY, TEST_ENERGY_CAPACITY, 0);
        private final LazyOptional<IEnergyStorage> optional = LazyOptional.of(() -> storage);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return capability == ForgeCapabilities.ENERGY ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public Tag serializeNBT() {
            return storage.serializeNBT();
        }

        @Override
        public void deserializeNBT(Tag nbt) {
            storage.deserializeNBT(nbt);
        }
    }

    private static AssemblyRecipeBasic requireAssemblyRecipe(GameTestHelper helper, ResourceLocation recipeId) {
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(recipeId).orElse(null);
        if (!(recipe instanceof AssemblyRecipeBasic assemblyRecipe)) {
            helper.fail("Missing registered Silicon assembly recipe " + recipeId);
            return null;
        }
        return assemblyRecipe;
    }

    private static void assertPreviewContains(
        GameTestHelper helper,
        ResourceLocation recipeId,
        ItemStack expected
    ) {
        AssemblyRecipeBasic recipe = requireAssemblyRecipe(helper, recipeId);
        if (recipe.getOutputPreviews().stream()
            .noneMatch(preview -> ItemStack.isSameItemSameTags(preview, expected))) {
            helper.fail("Assembly recipe " + recipeId + " is missing its NBT-preserving output preview");
        }
    }

    private static void assertLensRecipe(GameTestHelper helper, ResourceLocation recipeId, int expectedDamage) {
        ItemStack output = requireAssemblyRecipe(helper, recipeId)
            .getResultItem(helper.getLevel().registryAccess());
        if (!output.is(BCSiliconItems.PLUG_LENS_ITEM.get()) || output.getDamageValue() != expectedDamage) {
            helper.fail("Assembly recipe " + recipeId + " produced the wrong lens variant: " + output);
            return;
        }
        assertPreviewContains(helper, recipeId, output);
    }
    private static TileAssemblyTable.AssemblyInstruction selectAssemblyRecipe(
        GameTestHelper helper,
        TileAssemblyTable table,
        ResourceLocation recipeId
    ) {
        TileAssemblyTable.AssemblyInstruction instruction = findAssemblyRecipe(table, recipeId);
        if (instruction == null) {
            helper.fail("Assembly Table did not discover registered recipe " + recipeId);
            return null;
        }
        table.recipesStates.put(instruction, EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE);
        return instruction;
    }

    private static TileAssemblyTable.AssemblyInstruction findAssemblyRecipe(
        TileAssemblyTable table,
        ResourceLocation recipeId
    ) {
        for (Map.Entry<TileAssemblyTable.AssemblyInstruction, EnumAssemblyRecipeState> entry
            : table.recipesStates.entrySet()) {
            if (entry.getKey().recipe.getId().equals(recipeId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static void powerLaserTarget(GameTestHelper helper, TileLaserTableBase table) {
        long requested = table.getRequiredLaserPower();
        if (requested <= 0 || table.receiveLaserPower(requested) != 0) {
            helper.fail("Laser target rejected its requested microjoules for " + table.getBlockPos());
        }
    }

    private static Container requireContainer(GameTestHelper helper, BlockPos pos) {
        BlockEntity blockEntity = helper.getBlockEntity(pos);
        if (!(blockEntity instanceof Container container)) {
            helper.fail("Expected an item container at " + pos + " but found " + blockEntity);
            return null;
        }
        return container;
    }

    private static ItemStack findItem(Container container, Item item) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static <T extends BlockEntity> T requireBlockEntity(
        GameTestHelper helper,
        BlockPos pos,
        Class<T> type
    ) {
        BlockEntity blockEntity = helper.getBlockEntity(pos);
        if (!type.isInstance(blockEntity)) {
            helper.fail("Expected " + type.getSimpleName() + " at " + pos + " but found " + blockEntity);
            return null;
        }
        return type.cast(blockEntity);
    }
}
