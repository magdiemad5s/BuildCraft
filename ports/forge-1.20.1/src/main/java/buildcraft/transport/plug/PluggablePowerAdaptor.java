package buildcraft.transport.plug;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import buildcraft.api.mj.IMjReadable;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.api.transport.pluggable.PluggableModelKey;
import buildcraft.energy.BCEnergyConfig;
import buildcraft.neo.energy.FeToMjReceiveAccounting;
import buildcraft.neo.energy.MjTransferAccounting;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.client.model.key.KeyPlugPowerAdaptor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public class PluggablePowerAdaptor extends PipePluggable {
    private static final VoxelShape[] BOXES = new VoxelShape[6];

    static {
        double ll = 0D;
        double lu = 4D;
        double ul = 12D;
        double uu = 16D;

        double min = 3D;
        double max = 13D;

        BOXES[Direction.DOWN.get3DDataValue()] = Block.box(min, ll, min, max, lu, max);
        BOXES[Direction.UP.get3DDataValue()] = Block.box(min, ul, min, max, uu, max);
        BOXES[Direction.NORTH.get3DDataValue()] = Block.box(min, min, ll, max, max, lu);
        BOXES[Direction.SOUTH.get3DDataValue()] = Block.box(min, min, ul, max, max, uu);
        BOXES[Direction.WEST.get3DDataValue()] = Block.box(ll, min, min, lu, max, max);
        BOXES[Direction.EAST.get3DDataValue()] = Block.box(ul, min, min, uu, max, max);
    }

    /**
     * Legacy NBT key. This is prepaid micro-MJ left after an integer FE unit
     * was only partly accepted by the MJ receiver.
     */
    private long storedMJ;
    private final LazyOptional<IEnergyStorage> energyCapability;

    public PluggablePowerAdaptor(PluggableDefinition definition, IPipeHolder holder, Direction side) {
        super(definition, holder, side);
        energyCapability = LazyOptional.of(PowerAdaptorEnergyStorage::new);
    }

    public PluggablePowerAdaptor(
        PluggableDefinition definition, IPipeHolder holder, Direction side, CompoundTag nbt
    ) {
        this(definition, holder, side);
        storedMJ = Math.max(0, nbt.getLong("storedMJ"));
    }

    @Override
    public CompoundTag writeToNbt() {
        CompoundTag nbt = super.writeToNbt();
        nbt.putLong("storedMJ", storedMJ);
        return nbt;
    }

    @Override
    public VoxelShape getBoundingBox() {
        return BOXES[side.get3DDataValue()];
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public ItemStack getPickStack() {
        return new ItemStack(BCTransportItems.plugPowerAdaptor.get());
    }

    @Override
    @Nullable
    @OnlyIn(Dist.CLIENT)
    public PluggableModelKey getModelRenderKey(RenderType layer) {
        if (layer == RenderType.cutout()) {
            return new KeyPlugPowerAdaptor(side);
        }
        return null;
    }

    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {
        if (cap == MjAPI.CAP_CONNECTOR || cap == MjAPI.CAP_RECEIVER || cap == MjAPI.CAP_REDSTONE_RECEIVER) {
            return holder.getPipe().getBehaviour().getCapability(cap, side);
        }
        if (cap == ForgeCapabilities.ENERGY && getMjReceiver() != null) {
            return energyCapability.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public void onRemove() {
        energyCapability.invalidate();
    }

    private IMjReceiver getMjReceiver() {
        return holder.getPipe().getBehaviour()
            .getCapability(MjAPI.CAP_RECEIVER, side)
            .orElse(null);
    }

    private final class PowerAdaptorEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) {
                return 0;
            }
            IMjReceiver receiver = getMjReceiver();
            if (receiver == null || !receiver.canReceive()) {
                return 0;
            }

            long conversionRate = Math.max(1, BCEnergyConfig.microMjPerForgeEnergy);
            long prepaid = FeToMjReceiveAccounting.normalizePrepaidCredit(storedMJ, conversionRate);
            long offeredMicroJoules = prepaid + conversionRate * (long) maxReceive;
            FluidAction action = simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE;
            long returnedExcess = receiver.receivePower(offeredMicroJoules, action);
            long acceptedMicroJoules =
                MjTransferAccounting.accepted(offeredMicroJoules, returnedExcess);
            FeToMjReceiveAccounting.Result result = FeToMjReceiveAccounting.account(
                maxReceive, prepaid, acceptedMicroJoules, conversionRate
            );

            if (!simulate) {
                storedMJ = result.remainingPrepaidMicroJoules();
                holder.getPipeTile().setChanged();
            }
            return result.acceptedForgeEnergy();
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            IMjReceiver receiver = getMjReceiver();
            if (!(receiver instanceof IMjReadable readable)) {
                return 0;
            }
            long conversionRate = Math.max(1, BCEnergyConfig.microMjPerForgeEnergy);
            return (int) Math.min(Integer.MAX_VALUE, readable.getCapacity() / conversionRate);
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            IMjReceiver receiver = getMjReceiver();
            return receiver != null && receiver.canReceive();
        }
    }
}
