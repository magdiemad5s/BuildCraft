package buildcraft.silicon.gate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GateMenuStatePolicyTest {
    @Test
    void validStatementListsAreBounded() {
        assertFalse(GateMenuStatePolicy.isValidStatementCount(-1));
        assertTrue(GateMenuStatePolicy.isValidStatementCount(0));
        assertTrue(GateMenuStatePolicy.isValidStatementCount(
            GateMenuStatePolicy.MAX_VALID_STATEMENTS_PER_KIND
        ));
        assertFalse(GateMenuStatePolicy.isValidStatementCount(
            GateMenuStatePolicy.MAX_VALID_STATEMENTS_PER_KIND + 1
        ));
    }

    @Test
    void connectionsMustExistAndBeVisibleInTheGateLayout() {
        assertFalse(GateMenuStatePolicy.isSelectableConnectionIndex(1, false, 0));
        assertTrue(GateMenuStatePolicy.isSelectableConnectionIndex(2, false, 0));
        assertFalse(GateMenuStatePolicy.isSelectableConnectionIndex(2, false, 1));

        for (int index = 0; index < 7; index++) {
            if (index == 3) {
                assertFalse(GateMenuStatePolicy.isSelectableConnectionIndex(8, true, index));
            } else {
                assertTrue(GateMenuStatePolicy.isSelectableConnectionIndex(8, true, index));
            }
        }
    }

    @Test
    void serverMutationRequiresTheExactLiveGateMenu() {
        assertTrue(GateMenuStatePolicy.canMutateServerGate(true, true, true, true));
        assertFalse(GateMenuStatePolicy.canMutateServerGate(false, true, true, true));
        assertFalse(GateMenuStatePolicy.canMutateServerGate(true, false, true, true));
        assertFalse(GateMenuStatePolicy.canMutateServerGate(true, true, false, true));
        assertFalse(GateMenuStatePolicy.canMutateServerGate(true, true, true, false));
    }

    @Test
    void statementsMustBeOfferedFitTheGateAndHaveCanonicalParameters() {
        assertTrue(GateMenuStatePolicy.isValidStatementSelection(true, false, 99, 0, true));
        assertTrue(GateMenuStatePolicy.isValidStatementSelection(false, true, 1, 1, true));
        assertFalse(GateMenuStatePolicy.isValidStatementSelection(false, false, 0, 3, true));
        assertFalse(GateMenuStatePolicy.isValidStatementSelection(false, true, 2, 1, true));
        assertFalse(GateMenuStatePolicy.isValidStatementSelection(false, true, 0, 3, false));
    }
}
