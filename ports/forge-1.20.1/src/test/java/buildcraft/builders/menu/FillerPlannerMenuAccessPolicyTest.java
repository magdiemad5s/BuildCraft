package buildcraft.builders.menu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FillerPlannerMenuAccessPolicyTest {
    @Test
    void validSelectedNearbyServerTargetCanOpen() {
        assertTrue(FillerPlannerMenuAccessPolicy.canOpen(
            true, true, true, true, true, true,
            FillerPlannerMenuAccessPolicy.MAX_DISTANCE_SQUARED
        ));
    }

    @Test
    void openingRequiresTheExactSelectedAddon() {
        assertFalse(FillerPlannerMenuAccessPolicy.canOpen(
            true, true, true, true, true, false, 0.0D
        ));
        assertFalse(FillerPlannerMenuAccessPolicy.canOpen(
            true, true, true, true, false, true, 0.0D
        ));
        assertFalse(FillerPlannerMenuAccessPolicy.canOpen(
            true, true, true, false, true, true, 0.0D
        ));
    }

    @Test
    void continuedUseDoesNotRequireThePlayerToKeepLookingAtTheCorner() {
        assertTrue(FillerPlannerMenuAccessPolicy.canContinue(
            true, true, true, true, true, 4.0D
        ));
    }

    @Test
    void continuedUseRejectsWrongSideDimensionDeadPlayerAndExcessDistance() {
        assertFalse(FillerPlannerMenuAccessPolicy.canContinue(
            false, true, true, true, true, 0.0D
        ));
        assertFalse(FillerPlannerMenuAccessPolicy.canContinue(
            true, false, true, true, true, 0.0D
        ));
        assertFalse(FillerPlannerMenuAccessPolicy.canContinue(
            true, true, false, true, true, 0.0D
        ));
        assertFalse(FillerPlannerMenuAccessPolicy.canContinue(
            true, true, true, true, true,
            Math.nextUp(FillerPlannerMenuAccessPolicy.MAX_DISTANCE_SQUARED)
        ));
    }

    @Test
    void malformedDistancesAreNeverAccepted() {
        assertFalse(FillerPlannerMenuAccessPolicy.canContinue(
            true, true, true, true, true, -1.0D
        ));
        assertFalse(FillerPlannerMenuAccessPolicy.canContinue(
            true, true, true, true, true, Double.NaN
        ));
        assertFalse(FillerPlannerMenuAccessPolicy.canContinue(
            true, true, true, true, true, Double.POSITIVE_INFINITY
        ));
    }
}
