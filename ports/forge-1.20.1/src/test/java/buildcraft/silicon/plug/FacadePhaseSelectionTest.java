package buildcraft.silicon.plug;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import buildcraft.test.MinecraftTestBootstrap;
import net.minecraft.world.item.DyeColor;

class FacadePhaseSelectionTest {
    @BeforeAll
    static void bootStrapMinecraft() {
        MinecraftTestBootstrap.bootStrap();
    }

    private static FacadePhasedState state(DyeColor colour) {
        return new FacadePhasedState(null, colour);
    }

    private static int select(Set<DyeColor> powered, DyeColor... colours) {
        FacadePhasedState[] states = new FacadePhasedState[colours.length];
        for (int index = 0; index < colours.length; index++) {
            states[index] = state(colours[index]);
        }
        return PluggableFacade.selectActiveState(states, powered::contains);
    }

    @Test
    void unpoweredFacadeUsesItsExplicitDefaultState() {
        assertEquals(1, select(Set.of(), DyeColor.RED, null, DyeColor.BLUE));
    }

    @Test
    void firstPoweredColouredStateWins() {
        assertEquals(
            0,
            select(Set.of(DyeColor.RED, DyeColor.BLUE), DyeColor.RED, null, DyeColor.BLUE)
        );
        assertEquals(2, select(Set.of(DyeColor.BLUE), DyeColor.RED, null, DyeColor.BLUE));
    }

    @Test
    void missingDefaultFallsBackToFirstState() {
        assertEquals(0, select(Set.of(), DyeColor.RED, DyeColor.BLUE));
    }

    @Test
    void basicFacadeAlwaysUsesItsOnlyState() {
        assertEquals(0, select(Set.of(DyeColor.RED), (DyeColor) null));
    }
}