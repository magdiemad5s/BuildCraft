package buildcraft.robotics.ai;

import buildcraft.api.robots.AIRobot;
import buildcraft.api.robots.DockingStation;
import buildcraft.api.robots.IRobotRegistry;
import buildcraft.api.robots.EntityRobotBase;

public class AIRobotGotoSleep extends AIRobot {
    public AIRobotGotoSleep(EntityRobotBase robot) {
        super(robot);
    }

    @Override
    public void start() {
        IRobotRegistry registry = robot.getRegistry();
        if (registry != null) {
            registry.releaseResources(robot);
        }

        DockingStation linkedStation = robot.getLinkedStation();
        if (linkedStation == null) {
            // A robot can temporarily exist without a resolvable home station while its chunk or pipe pluggable is
            // loading. Stay in the station-independent sleep AI instead of unwinding the board's complete delegate
            // chain and restarting the same failed search every tick.
            startDelegateAI(new AIRobotSleep(robot));
            return;
        }
        startDelegateAI(new AIRobotGotoStation(robot, linkedStation));
    }

    @Override
    public void delegateAIEnded(AIRobot ai) {
        if (ai instanceof AIRobotGotoStation) {
            if (ai.success()) {
                startDelegateAI(new AIRobotSleep(robot));
            } else {
                setSuccess(false);
                terminate();
            }
        } else if (ai instanceof AIRobotSleep) {
            terminate();
        }
    }
}
