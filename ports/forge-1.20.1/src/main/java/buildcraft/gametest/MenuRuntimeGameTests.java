/*
 * Copyright (c) 2026 BuildCraft Neo contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.gametest;

import java.util.List;

import buildcraft.api.core.BCLog;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.menu.ContainerArchitectTable;
import buildcraft.builders.menu.ContainerBuilder;
import buildcraft.builders.menu.ContainerElectronicLibrary;
import buildcraft.builders.menu.ContainerFiller;
import buildcraft.energy.menu.ContainerDynamoMJ;
import buildcraft.energy.menu.ContainerEngineIron_BC8;
import buildcraft.energy.menu.ContainerEngineRF;
import buildcraft.energy.menu.ContainerEngineStone_BC8;
import buildcraft.factory.container.ContainerAutoCraftItems;
import buildcraft.factory.container.ContainerChute;
import buildcraft.factory.container.ContainerDistiller;
import buildcraft.factory.container.ContainerTank;
import buildcraft.robotics.container.ContainerRequester;
import buildcraft.robotics.container.ContainerZonePlanner;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.container.ContainerAdvancedCraftingTable;
import buildcraft.silicon.container.ContainerAssemblyTable;
import buildcraft.silicon.container.ContainerChargingTable;
import buildcraft.silicon.container.ContainerGate;
import buildcraft.silicon.container.ContainerIntegrationTable;
import buildcraft.silicon.container.ContainerProgrammingTable;
import buildcraft.transport.container.ContainerFilteredBuffer_BC8;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Runtime coverage for real server menus and client block-entity load races. */
@GameTestHolder("buildcraft")
@PrefixGameTestTemplate(false)
public final class MenuRuntimeGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    private MenuRuntimeGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void restoredTileMenusKeepTypesSlotsAndServerValidity(GameTestHelper helper) {
        List<MenuCase> cases = List.of(
            new MenuCase(
                BCBuildersBlocks.ARCHITECT.get(),
                "buildcraftbuilders:architect_menu",
                ContainerArchitectTable.class,
                38
            ),
            new MenuCase(
                BCBuildersBlocks.BUILDER.get(),
                "buildcraftbuilders:builder_menu",
                ContainerBuilder.class,
                88
            ),
            new MenuCase(
                BCBuildersBlocks.LIBRARY.get(),
                "buildcraftbuilders:elibrary_menu",
                ContainerElectronicLibrary.class,
                40
            ),
            new MenuCase(
                BCSiliconBlocks.ADVANCED_CRAFTING_TABLE_BLOCK.get(),
                "buildcraftsilicon:advanced_crafting_table_menu",
                ContainerAdvancedCraftingTable.class,
                70
            ),
            new MenuCase(
                BCSiliconBlocks.CHARGING_TABLE_BLOCK.get(),
                "buildcraftsilicon:charging_table_menu",
                ContainerChargingTable.class,
                37
            ),
            new MenuCase(
                BCSiliconBlocks.ASSEMBLY_TABLE_BLOCK.get(),
                "buildcraftsilicon:assembly_table_menu",
                ContainerAssemblyTable.class,
                60
            ),
            new MenuCase(
                BCSiliconBlocks.INTERGRATION_TABLE_BLOCK.get(),
                "buildcraftsilicon:integration_table_menu",
                ContainerIntegrationTable.class,
                47
            ),
            new MenuCase(
                BCSiliconBlocks.PROGRAMMING_TABLE_BLOCK.get(),
                "buildcraftsilicon:programming_table_menu",
                ContainerProgrammingTable.class,
                62
            )
        );

        BlockPos relativePos = new BlockPos(1, 1, 1);
        BlockPos absolutePos = helper.absolutePos(relativePos);
        Player player = helper.makeMockPlayer();
        player.setPos(absolutePos.getX() + 0.5, absolutePos.getY() + 0.5, absolutePos.getZ() + 0.5);

        int menuId = 1;
        for (MenuCase testCase : cases) {
            helper.setBlock(relativePos, testCase.block());
            BlockEntity blockEntity = helper.getBlockEntity(relativePos);
            if (!(blockEntity instanceof MenuProvider provider)) {
                helper.fail(testCase.menuId() + " block entity is not a MenuProvider: " + blockEntity);
                return;
            }

            AbstractContainerMenu menu = provider.createMenu(menuId++, player.getInventory(), player);
            assertMenuContract(helper, testCase, menu);
            if (!menu.stillValid(player)) {
                helper.fail(testCase.menuId() + " rejected a nearby server player");
                return;
            }

            player.setPos(absolutePos.getX() + 100.5, absolutePos.getY() + 0.5, absolutePos.getZ() + 0.5);
            if (menu.stillValid(player)) {
                helper.fail(testCase.menuId() + " accepted a player 100 blocks away");
                return;
            }
            player.setPos(absolutePos.getX() + 0.5, absolutePos.getY() + 0.5, absolutePos.getZ() + 0.5);
            menu.removed(player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void tileMenuFactoriesSurviveAMissingClientBlockEntity(GameTestHelper helper) {
        List<ClientMenuCase> cases = List.of(
            new ClientMenuCase(
                "buildcraftbuilders:architect_menu", ContainerArchitectTable::new, ContainerArchitectTable.class, 38
            ),
            new ClientMenuCase("buildcraftbuilders:builder_menu", ContainerBuilder::new, ContainerBuilder.class, 88),
            new ClientMenuCase(
                "buildcraftbuilders:elibrary_menu", ContainerElectronicLibrary::new,
                ContainerElectronicLibrary.class, 40
            ),
            new ClientMenuCase("buildcraftbuilders:filler_menu", ContainerFiller::new, ContainerFiller.class, 63),
            new ClientMenuCase(
                "buildcraftenergy:engine_stone_menu", ContainerEngineStone_BC8::new,
                ContainerEngineStone_BC8.class, 37
            ),
            new ClientMenuCase(
                "buildcraftenergy:engine_iron_menu", ContainerEngineIron_BC8::new,
                ContainerEngineIron_BC8.class, 36
            ),
            new ClientMenuCase(
                "buildcraftenergy:engine_rf_menu", ContainerEngineRF::new, ContainerEngineRF.class, 40
            ),
            new ClientMenuCase(
                "buildcraftenergy:dynamo_mj_menu", ContainerDynamoMJ::new, ContainerDynamoMJ.class, 40
            ),
            new ClientMenuCase(
                "buildcraftfactory:menu.autoworkbench_item", ContainerAutoCraftItems::create,
                ContainerAutoCraftItems.class, 65, 19
            ),
            new ClientMenuCase(
                "buildcraftfactory:menu.chute", ContainerChute::new, ContainerChute.class, 40
            ),
            new ClientMenuCase(
                "buildcraftfactory:menu.tank", ContainerTank::new, ContainerTank.class, 36
            ),
            new ClientMenuCase(
                "buildcraftfactory:menu.distiller", ContainerDistiller::new, ContainerDistiller.class, 36
            ),
            new ClientMenuCase(
                "buildcraftrobotics:menu.zone_planner", ContainerZonePlanner::new, ContainerZonePlanner.class, 39
            ),
            new ClientMenuCase(
                "buildcraftrobotics:menu.requester", ContainerRequester::new, ContainerRequester.class, 76
            ),
            new ClientMenuCase(
                "buildcrafttransport:pipe_filtered_buffer", ContainerFilteredBuffer_BC8::new,
                ContainerFilteredBuffer_BC8.class, 54
            ),
            new ClientMenuCase(
                "buildcraftsilicon:advanced_crafting_table_menu", ContainerAdvancedCraftingTable::new,
                ContainerAdvancedCraftingTable.class, 70
            ),
            new ClientMenuCase(
                "buildcraftsilicon:charging_table_menu", ContainerChargingTable::new, ContainerChargingTable.class, 37
            ),
            new ClientMenuCase(
                "buildcraftsilicon:assembly_table_menu", ContainerAssemblyTable::new, ContainerAssemblyTable.class, 60
            ),
            new ClientMenuCase(
                "buildcraftsilicon:integration_table_menu", ContainerIntegrationTable::new,
                ContainerIntegrationTable.class, 47
            ),
            new ClientMenuCase(
                "buildcraftsilicon:programming_table_menu", ContainerProgrammingTable::new,
                ContainerProgrammingTable.class, 62
            )
        );

        Player player = helper.makeMockPlayer();
        Inventory inventory = player.getInventory();
        BlockPos missingPos = helper.absolutePos(new BlockPos(20, 5, 20));
        int menuId = 100;
        for (ClientMenuCase testCase : cases) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            AbstractContainerMenu menu;
            try {
                if (testCase.leadingInt() >= 0) {
                    buffer.writeInt(testCase.leadingInt());
                }
                buffer.writeBlockPos(missingPos);
                try {
                    menu = testCase.factory().create(menuId++, inventory, buffer);
                } catch (RuntimeException exception) {
                    BCLog.logger.error(
                        "Client menu fallback failed for {} with no block entity at {}",
                        testCase.menuId(),
                        missingPos,
                        exception
                    );
                    helper.fail(testCase.menuId() + " fallback threw " + exception);
                    return;
                }
            } finally {
                buffer.release();
            }

            assertMenuContract(
                helper,
                new MenuCase(null, testCase.menuId(), testCase.menuClass(), testCase.slotCount()),
                menu
            );
            if (menu.stillValid(player)) {
                helper.fail(testCase.menuId() + " accepted a missing server block entity");
                return;
            }
            if (menu instanceof ContainerBuilder builder && builder.widgetTanks.size() != 4) {
                helper.fail("Builder fallback menu created " + builder.widgetTanks.size() + " fluid widgets instead of 4");
                return;
            }
            menu.removed(player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 100)
    public static void missingGateHolderReturnsAClosableMenuInsteadOfNull(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ContainerGate menu;
        try {
            buffer.writeBlockPos(helper.absolutePos(new BlockPos(20, 5, 20)));
            buffer.writeEnum(Direction.NORTH);
            try {
                menu = ContainerGate.creatClientMenu(200, player.getInventory(), buffer);
            } catch (RuntimeException exception) {
                BCLog.logger.error("Missing gate fallback construction failed", exception);
                helper.fail("Missing gate fallback threw " + exception);
                return;
            }
        } finally {
            buffer.release();
        }

        if (menu == null || menu.isValidGateMenu() || menu.slots.size() != 36 || menu.stillValid(player)) {
            helper.fail("Missing gate holder did not produce the expected safe 36-slot fallback menu");
            return;
        }
        menu.removed(player);
        helper.succeed();
    }

    private static void assertMenuContract(
        GameTestHelper helper,
        MenuCase testCase,
        AbstractContainerMenu menu
    ) {
        if (menu == null) {
            helper.fail(testCase.menuId() + " returned a null menu");
            return;
        }
        if (!testCase.menuClass().isInstance(menu)) {
            helper.fail(testCase.menuId() + " created " + menu.getClass().getName());
        }
        ResourceLocation actualId = ForgeRegistries.MENU_TYPES.getKey(menu.getType());
        if (!new ResourceLocation(testCase.menuId()).equals(actualId)) {
            helper.fail(testCase.menuId() + " resolved as menu type " + actualId);
        }
        if (menu.slots.size() != testCase.slotCount()) {
            helper.fail(
                testCase.menuId() + " has " + menu.slots.size()
                    + " slots instead of " + testCase.slotCount()
            );
        }
    }

    private record MenuCase(Block block, String menuId, Class<?> menuClass, int slotCount) {
    }

    private record ClientMenuCase(
        String menuId,
        ClientMenuFactory factory,
        Class<?> menuClass,
        int slotCount,
        int leadingInt
    ) {
        private ClientMenuCase(String menuId, ClientMenuFactory factory, Class<?> menuClass, int slotCount) {
            this(menuId, factory, menuClass, slotCount, -1);
        }
    }

    @FunctionalInterface
    private interface ClientMenuFactory {
        AbstractContainerMenu create(int menuId, Inventory inventory, FriendlyByteBuf buffer);
    }
}
