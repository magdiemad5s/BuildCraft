/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.menu;

/**
 * Side-independent access rules for the Filler Planner menu.
 *
 * <p>The original 1.12.2 menu trusted an unbounded client interaction. The
 * modern menu keeps the legacy behaviour while requiring the selected addon to
 * remain a live, nearby server-side target.</p>
 */
public final class FillerPlannerMenuAccessPolicy {
    public static final double MAX_DISTANCE = 8.0D;
    public static final double MAX_DISTANCE_SQUARED = MAX_DISTANCE * MAX_DISTANCE;

    private FillerPlannerMenuAccessPolicy() {
    }

    public static boolean canOpen(
        boolean logicalServer,
        boolean playerAlive,
        boolean sameDimension,
        boolean storedVolumeBox,
        boolean fillerPlannerAtSlot,
        boolean selectedTarget,
        double distanceSquared
    ) {
        return selectedTarget && canContinue(
            logicalServer,
            playerAlive,
            sameDimension,
            storedVolumeBox,
            fillerPlannerAtSlot,
            distanceSquared
        );
    }

    public static boolean canContinue(
        boolean logicalServer,
        boolean playerAlive,
        boolean sameDimension,
        boolean storedVolumeBox,
        boolean fillerPlannerAtSlot,
        double distanceSquared
    ) {
        return logicalServer
            && playerAlive
            && sameDimension
            && storedVolumeBox
            && fillerPlannerAtSlot
            && Double.isFinite(distanceSquared)
            && distanceSquared >= 0.0D
            && distanceSquared <= MAX_DISTANCE_SQUARED;
    }
}
