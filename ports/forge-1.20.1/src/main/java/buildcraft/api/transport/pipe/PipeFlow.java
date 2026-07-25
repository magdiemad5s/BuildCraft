package buildcraft.api.transport.pipe;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.transport.pipe.IPipeHolder.IWriter;
import buildcraft.api.transport.pipe.IPipeHolder.PipeMessageReceiver;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.LogicalSide;

public abstract class PipeFlow implements ICapabilityProvider {
    /** The ID for completely refreshing the state of this flow. */
    public static final int NET_ID_FULL_STATE = 0;
    /** The ID for updating what has changed since the last NET_ID_FULL_STATE or NET_ID_UPDATE has been sent. */
    // Wait, what? How is that a good idea or even sensible to make updates work this way?
    public static final int NET_ID_UPDATE = 1;

    public final IPipe pipe;
    private final Map<EnumPipePart, Map<Capability<?>, LazyOptional<?>>> cachedCapabilities =
        new EnumMap<>(EnumPipePart.class);
    private boolean capabilitiesValid = true;

    public PipeFlow(IPipe pipe) {
        this.pipe = pipe;
        initializeCapabilityCache();
    }

    public PipeFlow(IPipe pipe, CompoundTag nbt) {
        this.pipe = pipe;
        initializeCapabilityCache();
    }

    private void initializeCapabilityCache() {
        for (EnumPipePart part : EnumPipePart.VALUES) {
            cachedCapabilities.put(part, new HashMap<>());
        }
    }

    public CompoundTag writeToNbt() {
        return new CompoundTag();
    }

    /** Writes a payload with the specified id. Standard ID's are NET_ID_FULL_STATE and NET_ID_UPDATE. */
    public void writePayload(int id, FriendlyByteBuf buffer, LogicalSide side) {}

    /** Reads a payload with the specified id. Standard ID's are NET_ID_FULL_STATE and NET_ID_UPDATE. */
    public void readPayload(int id, FriendlyByteBuf buffer, LogicalSide side) throws IOException {}

    public void sendPayload(int id) {
        @SuppressWarnings("resource")
		final LogicalSide side = pipe.getHolder().getPipeWorld().isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER;
        sendCustomPayload(id, (buf) -> writePayload(id, buf, side));
    }

    public final void sendCustomPayload(int id, IWriter writer) {
        pipe.getHolder().sendMessage(PipeMessageReceiver.FLOW, buffer -> {
            buffer.writeBoolean(true);
            buffer.writeShort(id);
            writer.write(buffer);
        });
    }

    public abstract boolean canConnect(Direction face, PipeFlow other);

    public abstract boolean canConnect(Direction face, BlockEntity oTile);

    /** Used to force a connection to a given tile, even if the {@link PipeBehaviour} wouldn't normally connect to
     * it. */
    public boolean shouldForceConnection(Direction face, BlockEntity oTile) {
        return false;
    }

    public void onTick() {}

    /**
     * Called after every pluggable on the holder has ticked.
     *
     * <p>Item flows use this to retain items that reached their destination
     * during the pipe tick long enough for gate triggers to observe them.</p>
     */
    public void postPluggableTick() {}

    public void addDrops(NonNullList<ItemStack> toDrop, int fortune) {}

    public boolean onFlowActivate(Player player, BlockHitResult trace, Level level,
        EnumPipePart part) {
        return false;
    }

    


    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        return LazyOptional.empty();
    }

    protected final <T> @NotNull LazyOptional<T> getCachedCapability(
        @Nonnull Capability<T> capability, Direction facing, T instance
    ) {
        if (!capabilitiesValid || instance == null) {
            return LazyOptional.empty();
        }
        Map<Capability<?>, LazyOptional<?>> byCapability =
            cachedCapabilities.get(EnumPipePart.fromFacing(facing));
        LazyOptional<?> cached = byCapability.get(capability);
        if (cached == null) {
            cached = LazyOptional.of(() -> instance);
            byCapability.put(capability, cached);
        }
        return cached.cast();
    }

    public void invalidateCapabilities() {
        capabilitiesValid = false;
        invalidateCachedCapabilities();
    }

    public void reviveCapabilities() {
        capabilitiesValid = true;
        invalidateCachedCapabilities();
    }

    private void invalidateCachedCapabilities() {
        for (Map<Capability<?>, LazyOptional<?>> byCapability : cachedCapabilities.values()) {
            for (LazyOptional<?> optional : byCapability.values()) {
                optional.invalidate();
            }
            byCapability.clear();
        }
    }
}
