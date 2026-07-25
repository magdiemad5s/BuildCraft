/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.menu;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import buildcraft.api.filler.IFillerPattern;
import buildcraft.builders.BCBuildersGuis;
import buildcraft.builders.addon.AddonFillerPlanner;
import buildcraft.builders.filler.FillerType;
import buildcraft.core.marker.volume.Addon;
import buildcraft.core.marker.volume.EnumAddonSlot;
import buildcraft.core.marker.volume.VolumeBox;
import buildcraft.core.marker.volume.WorldSavedDataVolumeBoxes;
import buildcraft.lib.gui.MenuBC_Neptune;
import buildcraft.lib.statement.FullStatement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

/**
 * Menu for the volume-box Filler Planner addon.
 *
 * <p>The legacy GUI used builders GUI ordinal {@value #LEGACY_GUI_ID}. Its
 * modern registry path remains {@value #MENU_ID_PATH}, while the opening
 * payload carries only server-authored target identity.</p>
 */
public class ContainerFillerPlanner extends MenuBC_Neptune implements IContainerFilling {
    public static final int LEGACY_GUI_ID = 5;
    public static final String MENU_ID_PATH = "filler_planner";

    private final UUID volumeBoxId;
    private final EnumAddonSlot addonSlot;
    private final ResourceLocation dimensionId;

    @Nullable
    public final AddonFillerPlanner addon;

    private final FullStatement<IFillerPattern> patternStatementRemote = new FullStatement<>(
        FillerType.INSTANCE,
        4,
        null
    );
    private final FullStatement<IFillerPattern> patternStatementClient = new FullStatement<>(
        FillerType.INSTANCE,
        4,
        (statement, paramIndex) -> onStatementChange()
    );
    private boolean invertedRemote;

    /** Client-side menu factory. Target data in {@code buffer} was authored by the server. */
    public ContainerFillerPlanner(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        super(playerInventory, BCBuildersGuis.MENU_FILLER_PLANNER.get(), containerId);
        if (buffer == null) {
            throw new IllegalArgumentException("Filler Planner menu requires target data");
        }
        volumeBoxId = buffer.readUUID();
        addonSlot = buffer.readEnum(EnumAddonSlot.class);
        dimensionId = buffer.readResourceLocation();
        addon = null;
        init();
    }

    private ContainerFillerPlanner(int containerId, Inventory playerInventory, Target target) {
        super(playerInventory, BCBuildersGuis.MENU_FILLER_PLANNER.get(), containerId);
        volumeBoxId = target.volumeBox.id;
        addonSlot = target.slot;
        dimensionId = target.volumeBox.world.dimension().location();
        addon = target.addon;
        init();
    }

    /**
     * Opens the planner only when the logical server can independently resolve
     * the exact addon the player is looking at.
     */
    public static boolean open(Player player, AddonFillerPlanner requestedAddon) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        Target target = resolveSelectedTarget(serverPlayer, requestedAddon);
        if (target == null) {
            return false;
        }

        Component title = Component.translatable("item.buildcraftbuilders.filler_planner");
        SimpleMenuProvider provider = new SimpleMenuProvider(
            (containerId, inventory, ignored) -> new ContainerFillerPlanner(containerId, inventory, target),
            title
        );
        NetworkHooks.openScreen(serverPlayer, provider, buffer -> {
            buffer.writeUUID(target.volumeBox.id);
            buffer.writeEnum(target.slot);
            buffer.writeResourceLocation(target.volumeBox.world.dimension().location());
        });
        return true;
    }

    @Nullable
    private static Target resolveSelectedTarget(ServerPlayer player, AddonFillerPlanner requestedAddon) {
        if (requestedAddon == null || requestedAddon.volumeBox == null) {
            return null;
        }
        VolumeBox requestedBox = requestedAddon.volumeBox;
        if (requestedBox.world != player.level()) {
            return null;
        }

        WorldSavedDataVolumeBoxes savedData = WorldSavedDataVolumeBoxes.get(player.level());
        VolumeBox storedBox = savedData.getVolumeBoxFromId(requestedBox.id);
        EnumAddonSlot requestedSlot = null;
        if (storedBox == requestedBox) {
            for (Map.Entry<EnumAddonSlot, Addon> entry : storedBox.addons.entrySet()) {
                if (entry.getValue() == requestedAddon) {
                    requestedSlot = entry.getKey();
                    break;
                }
            }
        }

        AddonFillerPlanner storedAddon = requestedSlot == null
            ? null
            : asFillerPlanner(storedBox.addons.get(requestedSlot));
        var selected = EnumAddonSlot.getSelectingVolumeBoxAndSlot(player, savedData.volumeBoxes);
        boolean selectedTarget = storedBox != null
            && requestedSlot != null
            && selected.getLeft() == storedBox
            && selected.getRight() == requestedSlot;
        double distanceSquared = distanceSquared(player, storedBox, requestedSlot);
        boolean valid = FillerPlannerMenuAccessPolicy.canOpen(
            !player.level().isClientSide,
            player.isAlive() && !player.isRemoved(),
            requestedBox.world.dimension().location().equals(player.level().dimension().location()),
            storedBox == requestedBox,
            storedAddon == requestedAddon,
            selectedTarget,
            distanceSquared
        );
        return valid ? new Target(storedBox, requestedSlot, storedAddon) : null;
    }

    @Nullable
    private AddonFillerPlanner resolveCurrentServerAddon(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return null;
        }
        boolean sameDimension = dimensionId.equals(serverPlayer.level().dimension().location());
        if (!sameDimension) {
            return null;
        }

        WorldSavedDataVolumeBoxes savedData = WorldSavedDataVolumeBoxes.get(serverPlayer.level());
        VolumeBox storedBox = savedData.getVolumeBoxFromId(volumeBoxId);
        AddonFillerPlanner storedAddon = storedBox == null
            ? null
            : asFillerPlanner(storedBox.addons.get(addonSlot));
        double distanceSquared = distanceSquared(serverPlayer, storedBox, addonSlot);
        boolean valid = FillerPlannerMenuAccessPolicy.canContinue(
            !serverPlayer.level().isClientSide,
            serverPlayer.isAlive() && !serverPlayer.isRemoved(),
            sameDimension,
            storedBox != null,
            storedAddon != null && storedAddon == addon,
            distanceSquared
        );
        return valid ? storedAddon : null;
    }

    @Nullable
    private static AddonFillerPlanner asFillerPlanner(@Nullable Addon addon) {
        return addon instanceof AddonFillerPlanner planner ? planner : null;
    }

    private static double distanceSquared(
        Player player,
        @Nullable VolumeBox volumeBox,
        @Nullable EnumAddonSlot slot
    ) {
        if (volumeBox == null || slot == null) {
            return Double.POSITIVE_INFINITY;
        }
        Vec3 target = slot.getBoundingBox(volumeBox).getCenter();
        return player.getEyePosition().distanceToSqr(target);
    }

    @Override
    public Player getPlayer() {
        return playerInventory.player;
    }

    @Override
    public FullStatement<IFillerPattern> getPatternStatementClient() {
        return patternStatementClient;
    }

    @Override
    public FullStatement<IFillerPattern> getPatternStatement() {
        return addon == null ? patternStatementRemote : addon.patternStatement;
    }

    @Override
    public boolean isInverted() {
        return addon == null ? invertedRemote : addon.inverted;
    }

    @Override
    public void setInverted(boolean value) {
        if (addon == null) {
            invertedRemote = value;
        } else {
            addon.inverted = value;
        }
    }

    @Override
    public void valuesChanged() {
        if (getPlayer().level().isClientSide) {
            return;
        }
        AddonFillerPlanner current = resolveCurrentServerAddon(getPlayer());
        if (current == null) {
            return;
        }
        current.updateBuildingInfo();
        WorldSavedDataVolumeBoxes.get(getPlayer().level()).setDirty();
    }

    @Override
    public void readMessage(int id, FriendlyByteBuf buffer, LogicalSide side, NetworkEvent.Context ctx)
        throws IOException {
        if (side == LogicalSide.SERVER
            && (ctx.getSender() != getPlayer() || resolveCurrentServerAddon(getPlayer()) == null)) {
            buffer.skipBytes(buffer.readableBytes());
            return;
        }
        super.readMessage(id, buffer, side, ctx);
        IContainerFilling.super.readMessage(id, buffer, side, ctx);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide || resolveCurrentServerAddon(player) != null;
    }

    private static final class Target {
        private final VolumeBox volumeBox;
        private final EnumAddonSlot slot;
        private final AddonFillerPlanner addon;

        private Target(VolumeBox volumeBox, EnumAddonSlot slot, AddonFillerPlanner addon) {
            this.volumeBox = volumeBox;
            this.slot = slot;
            this.addon = addon;
        }
    }
}
