/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 */
package buildcraft.energy.tile;

import java.io.IOException;

import javax.annotation.Nonnull;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.MjAPI;
import buildcraft.core.BCCoreItems;
import buildcraft.core.client.render.RenderEngine_BC8;
import buildcraft.energy.BCEnergyBlocks;
import buildcraft.energy.BCEnergyConfig;
import buildcraft.energy.menu.ContainerEngineRF;
import buildcraft.lib.engine.EngineConnector;
import buildcraft.lib.engine.TileEngineBase_BC8;
import buildcraft.lib.tile.item.ItemHandlerManager.EnumAccess;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import buildcraft.lib.tile.item.StackInsertionFunction;
import buildcraft.neo.energy.EnergyConversionPolicy;
import buildcraft.neo.energy.EnergyConversionPolicy.Transfer;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

/** Converts Forge Energy into BuildCraft MJ while preserving the legacy RF-engine limits and NBT keys. */
public class TileEngineRF extends TileEngineBase_BC8 implements MenuProvider {
    public static final int MAX_FORGE_ENERGY = 10_000;
    public static final double HEAT_RATE = 0.06;
    public static final double COOLDOWN_RATE = 0.01;

    private int currentForgeEnergy;
    public final ItemHandlerSimple invUpgrades;
    private final IEnergyStorage forgeEnergyStorage = new ForgeEnergyInput();

    public TileEngineRF(BlockPos pos, BlockState state) {
        super(BCEnergyBlocks.ENGINE_RF_TILE.get(), pos, state);
        caps.addCapabilityInstance(ForgeCapabilities.ENERGY, forgeEnergyStorage, EnumPipePart.VALUES);
        invUpgrades = itemManager.addInvHandler(
            "upgrades",
            4,
            this::isValidUpgrade,
            StackInsertionFunction.getInsertionFunction(1),
            EnumAccess.NONE
        );
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        currentForgeEnergy = Math.max(0, Math.min(MAX_FORGE_ENERGY, nbt.getInt("currentRF")));
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        // Keep the original key for old-world compatibility even though Forge now calls the unit FE.
        nbt.putInt("currentRF", currentForgeEnergy);
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, NetworkEvent.Context ctx)
        throws IOException {
        super.readPayload(id, buffer, side, ctx);
        if (side == LogicalSide.CLIENT && (id == NET_GUI_DATA || id == NET_GUI_TICK)) {
            currentForgeEnergy = Math.max(0, Math.min(MAX_FORGE_ENERGY, buffer.readVarInt()));
        }
    }

    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER && (id == NET_GUI_DATA || id == NET_GUI_TICK)) {
            buffer.writeVarInt(currentForgeEnergy);
        }
    }

    private boolean isValidUpgrade(int slot, ItemStack stack) {
        return getUpgradeMicroMj(stack) > 0;
    }

    public static long getUpgradeMicroMj(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(BCCoreItems.GEAR_IRON.get())) {
            return 2 * MjAPI.MJ;
        }
        if (stack.is(BCCoreItems.GEAR_GOLD.get())) {
            return 3 * MjAPI.MJ;
        }
        return 0;
    }

    public long getMjPerTick() {
        long value = 4 * MjAPI.MJ;
        for (int slot = 0; slot < invUpgrades.getSlots(); slot++) {
            value += getUpgradeMicroMj(invUpgrades.getStackInSlot(slot));
        }
        return value;
    }

    public int getForgeEnergyConsumptionRate() {
        return getForgeEnergyConsumptionRate(BCEnergyConfig.microMjPerForgeEnergy);
    }

    private int getForgeEnergyConsumptionRate(long microMjPerForgeEnergy) {
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, getMjPerTick() / microMjPerForgeEnergy));
    }

    @Override
    protected void burn() {
        currentOutput = 0;
        if (!isRedstonePowered || currentForgeEnergy <= 0) {
            return;
        }

        long availableMjSpace = Math.max(0, getMaxPower() - power);
        long microMjPerForgeEnergy = BCEnergyConfig.microMjPerForgeEnergy;
        Transfer transfer = EnergyConversionPolicy.forgeEnergyToMj(
            currentForgeEnergy,
            getForgeEnergyConsumptionRate(microMjPerForgeEnergy),
            availableMjSpace,
            microMjPerForgeEnergy
        );
        if (transfer.forgeEnergy() <= 0) {
            return;
        }

        currentForgeEnergy -= transfer.forgeEnergy();
        currentOutput = transfer.microJoules();
        addPower(transfer.microJoules());
        heat = Math.min(200, heat + HEAT_RATE);
        setChanged();
    }

    @Nonnull
    @Override
    protected IMjConnector createConnector() {
        return new EngineConnector(false);
    }

    @Override
    public boolean isBurning() {
        return currentForgeEnergy > 0 && isRedstonePowered;
    }

    @Override
    public double getPistonSpeed() {
        return switch (getPowerStage()) {
            case BLUE -> 0.04;
            case GREEN -> 0.05;
            case YELLOW -> 0.06;
            case RED -> 0.07;
            default -> 0;
        };
    }

    @Override
    public void updateHeatLevel() {
        heat = Math.max(MIN_HEAT, heat - COOLDOWN_RATE);
        getPowerStage();
    }

    @Override
    public long getMaxPower() {
        return 1_000 * MjAPI.MJ;
    }

    @Override
    public long maxPowerReceived() {
        return 200 * MjAPI.MJ;
    }

    @Override
    public long maxPowerExtracted() {
        return 500 * MjAPI.MJ;
    }

    @Override
    public float explosionRange() {
        return 4;
    }

    @Override
    protected int getMaxChainLength() {
        return 4;
    }

    @Override
    public long getCurrentOutput() {
        return currentOutput;
    }

    public int getCurrentForgeEnergy() {
        return currentForgeEnergy;
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, this, worldPosition);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerEngineRF(
            id,
            inventory,
            invUpgrades,
            ContainerLevelAccess.create(level, worldPosition)
        );
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("tile.engineRf.name");
    }

    @Override
    public TextureAtlasSprite getTextureBack() {
        return RenderEngine_BC8.RF_BACK;
    }

    @Override
    public TextureAtlasSprite getTextureSide() {
        return RenderEngine_BC8.RF_SIDE;
    }

    private final class ForgeEnergyInput implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) {
                return 0;
            }
            int accepted = Math.min(maxReceive, MAX_FORGE_ENERGY - currentForgeEnergy);
            if (!simulate && accepted > 0) {
                currentForgeEnergy += accepted;
                setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return currentForgeEnergy;
        }

        @Override
        public int getMaxEnergyStored() {
            return MAX_FORGE_ENERGY;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
