package buildcraft.robotics.statements;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class StatementParameterRobotMatchTest {
    @Test
    void boardFiltersRequireTheExactPersistedBoardId() {
        assertTrue(RobotFilterPolicy.boardIdsMatch(
            "buildcraft:boardRobotMiner",
            "buildcraft:boardRobotMiner"
        ));
        assertFalse(RobotFilterPolicy.boardIdsMatch(
            "buildcraft:boardRobotMiner",
            "buildcraft:boardRobotPicker"
        ));
        assertFalse(RobotFilterPolicy.boardIdsMatch(null, "buildcraft:boardRobotMiner"));
    }

    @Test
    void missingOrWrongParameterNeverBecomesAWildcardMatch() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/buildcraft/robotics/statements/StatementParameterRobot.java"
        ));

        assertTrue(source.contains("!(param instanceof StatementParameterRobot filter) || robot == null"));
        assertTrue(source.contains("if (held.isEmpty())"));
    }

    @Test
    void listAndWearableBranchesRemainPartOfTheRobotFilterContract() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/buildcraft/robotics/statements/StatementParameterRobot.java"
        ));

        assertTrue(source.contains("held.getItem() instanceof IList list"));
        assertTrue(source.contains("list.matches(held, robotStack)"));
        assertTrue(source.contains("list.matches(held, wearable)"));
        assertTrue(source.contains("robot.getArmorSlots()"));
        assertTrue(source.contains("StackUtil.isMatchingItem(held, wearable, true, true)"));
        assertFalse(source.contains("TODO: match by robot board type"));
    }
}
