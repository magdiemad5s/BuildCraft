package buildcraft.robotics.statements;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import buildcraft.api.boards.RedstoneBoardRobot;
import buildcraft.api.items.IList;
import buildcraft.api.robots.EntityRobotBase;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementParameterItemStack;
import buildcraft.lib.misc.StackUtil;
import buildcraft.robotics.BCRoboticsBoards;
import buildcraft.robotics.item.ItemRobot;

public class StatementParameterRobot extends StatementParameterItemStack {

    public static final String TAG = "buildcraft:robot";

    public StatementParameterRobot() { super(); }
    public StatementParameterRobot(ItemStack stack) { super(stack); }
    public StatementParameterRobot(CompoundTag nbt) { super(nbt); }

    @Override
    public String getUniqueTag() { return TAG; }

    @Override
    public StatementParameterRobot onClick(
            buildcraft.api.statements.IStatementContainer source,
            buildcraft.api.statements.IStatement stmt,
            ItemStack clicked,
            buildcraft.api.statements.StatementMouseClick mouse) {
        if (clicked.isEmpty()) return new StatementParameterRobot();
        // Accept any robot item (check via capability or item type is flexible)
        ItemStack copy = clicked.copy();
        copy.setCount(1);
        return new StatementParameterRobot(copy);
    }

    public static boolean matches(@Nullable IStatementParameter param, EntityRobotBase robot) {
        if (!(param instanceof StatementParameterRobot filter) || robot == null) {
            return false;
        }

        ItemStack held = filter.getItemStack();
        if (held.isEmpty()) {
            return false;
        }

        RedstoneBoardRobot board = robot.getBoard();
        String robotBoardId = board == null ? null : board.getNBTHandler().getID();

        if (held.getItem() instanceof IList list) {
            if (robotBoardId != null) {
                ItemStack robotStack = ItemRobot.createRobotStack(
                    BCRoboticsBoards.getById(robotBoardId),
                    robot.getEnergy()
                );
                if (list.matches(held, robotStack)) {
                    return true;
                }
            }
            for (ItemStack wearable : robot.getArmorSlots()) {
                if (!wearable.isEmpty() && list.matches(held, wearable)) {
                    return true;
                }
            }
            return false;
        }

        if (held.getItem() instanceof ItemRobot) {
            return RobotFilterPolicy.boardIdsMatch(BCRoboticsBoards.getRobotBoard(held).id(), robotBoardId);
        }

        for (ItemStack wearable : robot.getArmorSlots()) {
            if (!wearable.isEmpty() && StackUtil.isMatchingItem(held, wearable, true, true)) {
                return true;
            }
        }
        return false;
    }

    public static StatementParameterRobot readFromNbt(CompoundTag nbt) {
        return new StatementParameterRobot(nbt);
    }
}
