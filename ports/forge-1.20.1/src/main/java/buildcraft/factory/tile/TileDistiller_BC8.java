/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.factory.tile;

import java.io.IOException;
import java.util.List;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.core.SafeTimeTracker;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjCapabilityHelper;
import buildcraft.api.recipes.BuildcraftRecipeRegistry;
import buildcraft.api.recipes.IRefineryRecipeManager;
import buildcraft.api.recipes.IRefineryRecipeManager.IDistillationRecipe;
import buildcraft.api.tiles.IDebuggable;
import buildcraft.api.tiles.TilesAPI;
import buildcraft.core.BCCoreConfig;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.container.ContainerDistiller;
import buildcraft.lib.block.BlockBCBase_Neptune;
import buildcraft.lib.expression.DefaultContexts;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.node.value.NodeVariableBoolean;
import buildcraft.lib.expression.node.value.NodeVariableLong;
import buildcraft.lib.expression.node.value.NodeVariableObject;
import buildcraft.lib.fluid.FluidCompatRegistry;
import buildcraft.lib.fluid.FluidSmoother;
import buildcraft.lib.fluid.FluidSmoother.IFluidDataSender;
import buildcraft.lib.fluid.Tank;
import buildcraft.lib.gui.help.ElementHelpInfo;
import buildcraft.lib.misc.CapUtil;
import buildcraft.lib.misc.ExpressionCompat;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.data.AverageLong;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.misc.data.ModelVariableData;
import buildcraft.lib.mj.MjBatteryReceiver;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.neo.forge1201.factory.DistillerMenuStatePolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

public class TileDistiller_BC8 extends TileBC_Neptune implements IDebuggable, MenuProvider {
    public static final FunctionContext MODEL_FUNC_CTX;
    private static final NodeVariableObject<Direction> MODEL_FACING;
    private static final NodeVariableBoolean MODEL_ACTIVE;
    private static final NodeVariableLong MODEL_POWER_AVG;
    private static final NodeVariableLong MODEL_POWER_MAX;

    public static final IdAllocator IDS = TileBC_Neptune.IDS.makeChild("Distiller");
    public static final int NET_TANK_IN = IDS.allocId("TANK_IN");
    public static final int NET_TANK_GAS_OUT = IDS.allocId("TANK_GAS_OUT");
    public static final int NET_TANK_LIQUID_OUT = IDS.allocId("TANK_LIQUID_OUT");
    
    static {
        ExpressionCompat.setup();
        MODEL_FUNC_CTX = DefaultContexts.createWithAll();
        MODEL_FACING = MODEL_FUNC_CTX.putVariableObject("direction", Direction.class);
        MODEL_POWER_AVG = MODEL_FUNC_CTX.putVariableLong("power_average");
        MODEL_POWER_MAX = MODEL_FUNC_CTX.putVariableLong("power_max");
        MODEL_ACTIVE = MODEL_FUNC_CTX.putVariableBoolean("active");
    }

    public static final long MAX_MJ_PER_TICK = 6 * MjAPI.MJ;

    public final Tank tankIn = new Tank(
        DistillerMenuStatePolicy.INPUT_TANK_NBT_KEY,
        4 * FluidType.BUCKET_VOLUME,
        this,
        this::isDistillableFluid
    );
    public final Tank tankGasOut = new Tank(
        DistillerMenuStatePolicy.GAS_TANK_NBT_KEY,
        4 * FluidType.BUCKET_VOLUME,
        this
    );
    public final Tank tankLiquidOut = new Tank(
        DistillerMenuStatePolicy.LIQUID_TANK_NBT_KEY,
        4 * FluidType.BUCKET_VOLUME,
        this
    );

    private final MjBattery mjBattery = new MjBattery(1024 * MjAPI.MJ);

    public final FluidSmoother smoothedTankIn;
    public final FluidSmoother smoothedTankGasOut;
    public final FluidSmoother smoothedTankLiquidOut;
    
    /** The model variables, used to keep track of the various state-based variables. */
    public final ModelVariableData clientModelData = new ModelVariableData();

    private IDistillationRecipe currentRecipe;
    private long distillPower = 0;
    private boolean hasWork, isActive = false;
    private final AverageLong powerAvg = new AverageLong(100);
    private final SafeTimeTracker updateTracker = new SafeTimeTracker(BCCoreConfig.networkUpdateRate, 2);
    private boolean changedSinceNetUpdate = true;

    private long powerAvgClient;

	public TileDistiller_BC8(BlockPos pos, BlockState bs) {
		super(BCFactoryBlocks.ENTITYBLOCKDISTILLER.get(), pos, bs);
        tankIn.setCanDrain(false);
        tankGasOut.setCanFill(false);
        tankLiquidOut.setCanFill(false);

        tankIn.helpInfo = new ElementHelpInfo(
            "buildcraft.help.distiller.tank_in.title",
            0xFF_E2_63_63,
            Tank.DEFAULT_HELP_KEY,
            "buildcraft.help.distiller.tank_in.desc"
        );
        tankGasOut.helpInfo = new ElementHelpInfo(
            "buildcraft.help.distiller.tank_gas_out.title",
            0xFF_E4_E4_00,
            Tank.DEFAULT_HELP_KEY,
            "buildcraft.help.distiller.tank_gas_out.desc"
        );
        tankLiquidOut.helpInfo = new ElementHelpInfo(
            "buildcraft.help.distiller.tank_liquid_out.title",
            0xFF_B5_00_FF,
            Tank.DEFAULT_HELP_KEY,
            "buildcraft.help.distiller.tank_liquid_out.desc"
        );

        tankManager.add(tankIn);
        tankManager.add(tankGasOut);
        tankManager.addLast(tankLiquidOut);

        smoothedTankIn = new FluidSmoother(createSender(NET_TANK_IN), tankIn);
        smoothedTankGasOut = new FluidSmoother(createSender(NET_TANK_GAS_OUT), tankGasOut);
        smoothedTankLiquidOut = new FluidSmoother(createSender(NET_TANK_LIQUID_OUT), tankLiquidOut);

        caps.addCapabilityInstance(CapUtil.CAP_FLUIDS, tankIn, EnumPipePart.HORIZONTALS);
        caps.addCapabilityInstance(CapUtil.CAP_FLUIDS, tankGasOut, EnumPipePart.UP);
        caps.addCapabilityInstance(CapUtil.CAP_FLUIDS, tankLiquidOut, EnumPipePart.DOWN);
        caps.addCapabilityInstance(TilesAPI.CAP_HAS_WORK, () -> hasWork, EnumPipePart.VALUES);
        caps.addProvider(new MjCapabilityHelper(new MjBatteryReceiver(mjBattery)));
    }

    private IFluidDataSender createSender(int netId) {
        return writer -> createAndSendMessage(netId, writer);
    }

    private boolean isDistillableFluid(FluidStack fluid) {
        IRefineryRecipeManager manager = BuildcraftRecipeRegistry.refineryRecipes;
        IDistillationRecipe recipe = manager.getDistillationRegistry().getRecipeForInput(fluid);
        return recipe != null;
    }

    
    @Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
        nbt.put(DistillerMenuStatePolicy.TANKS_NBT_KEY, tankManager.serializeNBT());
        nbt.put(DistillerMenuStatePolicy.BATTERY_NBT_KEY, mjBattery.serializeNBT());
        nbt.putLong(DistillerMenuStatePolicy.DISTILL_POWER_NBT_KEY, distillPower);
        powerAvg.writeToNbt(nbt, DistillerMenuStatePolicy.POWER_AVERAGE_NBT_KEY);
	}

    
	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
        CompoundTag tanksTag = nbt.getCompound(DistillerMenuStatePolicy.TANKS_NBT_KEY);
        if (!tanksTag.contains(DistillerMenuStatePolicy.GAS_TANK_NBT_KEY)
            && tanksTag.contains(DistillerMenuStatePolicy.LEGACY_GAS_TANK_NBT_KEY)) {
            tanksTag.put(
                DistillerMenuStatePolicy.GAS_TANK_NBT_KEY,
                tanksTag.get(DistillerMenuStatePolicy.LEGACY_GAS_TANK_NBT_KEY).copy()
            );
        }
        if (!tanksTag.contains(DistillerMenuStatePolicy.LIQUID_TANK_NBT_KEY)
            && tanksTag.contains(DistillerMenuStatePolicy.LEGACY_LIQUID_TANK_NBT_KEY)) {
            tanksTag.put(
                DistillerMenuStatePolicy.LIQUID_TANK_NBT_KEY,
                tanksTag.get(DistillerMenuStatePolicy.LEGACY_LIQUID_TANK_NBT_KEY).copy()
            );
        }
        FactoryNbtSanitizer.clampFluidAmount(
            tanksTag.getCompound(DistillerMenuStatePolicy.INPUT_TANK_NBT_KEY),
            DistillerMenuStatePolicy.INPUT_TANK_NBT_KEY,
            tankIn.getCapacity()
        );
        FactoryNbtSanitizer.clampFluidAmount(
            tanksTag.getCompound(DistillerMenuStatePolicy.GAS_TANK_NBT_KEY),
            DistillerMenuStatePolicy.GAS_TANK_NBT_KEY,
            tankGasOut.getCapacity()
        );
        FactoryNbtSanitizer.clampFluidAmount(
            tanksTag.getCompound(DistillerMenuStatePolicy.LIQUID_TANK_NBT_KEY),
            DistillerMenuStatePolicy.LIQUID_TANK_NBT_KEY,
            tankLiquidOut.getCapacity()
        );
        tankManager.deserializeNBT(tanksTag);

        CompoundTag batteryTag = nbt.getCompound(DistillerMenuStatePolicy.BATTERY_NBT_KEY);
        if (batteryTag.isEmpty() && nbt.contains(DistillerMenuStatePolicy.LEGACY_BATTERY_NBT_KEY)) {
            batteryTag = nbt.getCompound(DistillerMenuStatePolicy.LEGACY_BATTERY_NBT_KEY);
        }
        mjBattery.deserializeNBT(FactoryNbtSanitizer.sanitizeBattery(batteryTag, mjBattery.getCapacity()));
        currentRecipe =
            BuildcraftRecipeRegistry.refineryRecipes.getDistillationRegistry().getRecipeForInput(tankIn.getFluid());
        long recipePower = currentRecipe == null ? 0 : currentRecipe.powerRequired();
        distillPower = FactoryNbtSanitizer.clampRecipeProgress(
            nbt.getLong(DistillerMenuStatePolicy.DISTILL_POWER_NBT_KEY),
            recipePower
        );
        powerAvg.readFromNbt(nbt, DistillerMenuStatePolicy.POWER_AVERAGE_NBT_KEY);
        if (powerAvg.getAverageLong() < 0 || powerAvg.getAverageLong() > MAX_MJ_PER_TICK) {
            powerAvg.clear();
        }
	}


    @Override
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {
        super.writePayload(id, buffer, side);
        if (side == LogicalSide.SERVER) {
            if (id == NET_RENDER_DATA) {
                writePayload(NET_TANK_IN, buffer, side);
                writePayload(NET_TANK_GAS_OUT, buffer, side);
                writePayload(NET_TANK_LIQUID_OUT, buffer, side);
                buffer.writeBoolean(isActive);
                powerAvgClient = powerAvg.getAverageLong();
                final long div = MjAPI.MJ / 2;
                //BCLog.d(powerAvgClient/(double)MjAPI.MJ + "");
                powerAvgClient = Math.round(powerAvgClient / (double) div) * div;
                buffer.writeLong(powerAvgClient);
            } else if (id == NET_TANK_IN) {
                smoothedTankIn.writeInit(buffer);
            } else if (id == NET_TANK_GAS_OUT) {
                smoothedTankGasOut.writeInit(buffer);
            } else if (id == NET_TANK_LIQUID_OUT) {
                smoothedTankLiquidOut.writeInit(buffer);
            }
        }
    }

    @Override
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side, NetworkEvent.Context ctx) throws IOException {
        super.readPayload(id, buffer, side, ctx);
        if (side == LogicalSide.CLIENT) {
            if (id == NET_RENDER_DATA) {
                readPayload(NET_TANK_IN, buffer, side, ctx);
                readPayload(NET_TANK_GAS_OUT, buffer, side, ctx);
                readPayload(NET_TANK_LIQUID_OUT, buffer, side, ctx);

                smoothedTankIn.resetSmoothing(getLevel());
                smoothedTankGasOut.resetSmoothing(getLevel());
                smoothedTankLiquidOut.resetSmoothing(getLevel());

                isActive = buffer.readBoolean();
                powerAvgClient = buffer.readLong();
            } else if (id == NET_TANK_IN) {
                smoothedTankIn.handleMessage(getLevel(), buffer);
            } else if (id == NET_TANK_GAS_OUT) {
                smoothedTankGasOut.handleMessage(getLevel(), buffer);
            } else if (id == NET_TANK_LIQUID_OUT) {
                smoothedTankLiquidOut.handleMessage(getLevel(), buffer);
            }
        }
    }

    public void setClientModelVariablesForItem() {
        DefaultContexts.RENDER_PARTIAL_TICKS.value = 1;
        MODEL_ACTIVE.value = false;
        MODEL_POWER_AVG.value = 0;
        MODEL_POWER_MAX.value = 6;
        MODEL_FACING.value = Direction.WEST;
    }

    public void setClientModelVariables(float partialTicks) {
        DefaultContexts.RENDER_PARTIAL_TICKS.value = partialTicks;

        MODEL_ACTIVE.value = isActive;
        MODEL_POWER_AVG.value = powerAvgClient / MjAPI.MJ;
        MODEL_POWER_MAX.value = MAX_MJ_PER_TICK / MjAPI.MJ;
        MODEL_FACING.value = Direction.WEST;

        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() == BCFactoryBlocks.DISTILLER_BLOCK.get()) {
            MODEL_FACING.value = state.getValue(BlockBCBase_Neptune.PROP_FACING);
        }
    }
    
    public void update() {
        smoothedTankIn.tick(getLevel());
        smoothedTankGasOut.tick(getLevel());
        smoothedTankLiquidOut.tick(getLevel());
        if (level.isClientSide) {
            setClientModelVariables(1);
            clientModelData.tick();
            return;
        }
        powerAvg.tick();
        changedSinceNetUpdate |= powerAvgClient != powerAvg.getAverageLong();

        currentRecipe =
            BuildcraftRecipeRegistry.refineryRecipes.getDistillationRegistry().getRecipeForInput(tankIn.getFluid());
        if (currentRecipe == null) {
            mjBattery.addPowerChecking(distillPower, FluidAction.EXECUTE);
            distillPower = 0;
            isActive = false;
            hasWork = false;
        } else {
            FluidStack reqIn = currentRecipe.in();
            FluidStack outLiquid = currentRecipe.outLiquid();
            FluidStack outGas = currentRecipe.outGas();

            FluidStack potentialIn = tankIn.drainInternal(reqIn, FluidAction.SIMULATE);
            boolean canExtract = FluidCompatRegistry.areEquivalent(reqIn, potentialIn) && reqIn.getAmount() == potentialIn.getAmount();

            boolean canFillLiquid = tankLiquidOut.fillInternal(outLiquid, FluidAction.SIMULATE) == outLiquid.getAmount();
            boolean canFillGas = tankGasOut.fillInternal(outGas, FluidAction.SIMULATE) == outGas.getAmount();

            if (canExtract && canFillLiquid && canFillGas) {
                hasWork = true;
                long max = MAX_MJ_PER_TICK;
                long storedForRate = Math.max(0, Math.min(mjBattery.getStored(), mjBattery.getCapacity()));
                max *= storedForRate + max;
                max /= mjBattery.getCapacity() / 2;
                max = Math.min(max, MAX_MJ_PER_TICK);
                long powerReq = currentRecipe.powerRequired();
                long power = mjBattery.extractPower(0, max);
                powerAvg.push(max);
                distillPower += power;
                isActive = power > 0;
                if (distillPower >= powerReq) {
                    isActive = true;
                    distillPower -= powerReq;
                    tankIn.drainInternal(reqIn, FluidAction.EXECUTE);
                    tankGasOut.fillInternal(outGas, FluidAction.EXECUTE);
                    tankLiquidOut.fillInternal(outLiquid, FluidAction.EXECUTE);
                }
            } else {
                hasWork = false;
                mjBattery.addPowerChecking(distillPower, FluidAction.EXECUTE);
                distillPower = 0;
                isActive = false;
            }
        }

        if (changedSinceNetUpdate && updateTracker.markTimeIfDelay(level)) {
            powerAvgClient = powerAvg.getAverageLong();
            sendNetworkUpdate(NET_RENDER_DATA);
            changedSinceNetUpdate = false;
        }
    }
    

    @Override
	public InteractionResult onActivated(Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult tankResult = super.onActivated(player, hand, hit);
        if (tankResult.consumesAction()) {
            return tankResult;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, this, worldPosition);
        }
        return InteractionResult.SUCCESS;
	}

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerDistiller(
            id,
            inventory,
            this,
            ContainerLevelAccess.create(level, worldPosition)
        );
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    public boolean isActive() {
        return isActive;
    }

	@Override
    public void getDebugInfo(List<String> left, List<String> right, Direction side) {
        left.add("In = " + tankIn.getDebugString());
        left.add("GasOut = " + tankGasOut.getDebugString());
        left.add("LiquidOut = " + tankLiquidOut.getDebugString());
        left.add("Battery = " + mjBattery.getDebugString());
        left.add("Progress = " + MjAPI.formatMj(distillPower) + " MJ");
        left.add("Rate = " + LocaleUtil.localizeMjFlow(powerAvgClient));
        left.add("CurrRecipe = " + currentRecipe);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void getClientDebugInfo(List<String> left, List<String> right, Direction side) {
        setClientModelVariables(1);
        left.add("Model Variables:");
        left.add("  facing = " + MODEL_FACING);
        left.add("  active = " + MODEL_ACTIVE);
        left.add("  power_average = " + MODEL_POWER_AVG);
        left.add("  power_max = " + MODEL_POWER_MAX);
        left.add("Current Model Variables:");
    }
}

