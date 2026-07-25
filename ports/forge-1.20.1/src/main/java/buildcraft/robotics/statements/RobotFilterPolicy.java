package buildcraft.robotics.statements;

/** Pure matching rules shared by robot gate parameters and their unit tests. */
final class RobotFilterPolicy {
    private RobotFilterPolicy() {
    }

    static boolean boardIdsMatch(String filterBoardId, String robotBoardId) {
        return filterBoardId != null && filterBoardId.equals(robotBoardId);
    }
}
