package buildcraft.lib.statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.core.render.ISprite;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementMouseClick;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

class FullStatementRegressionTest {
    private static final StatementType<DummyStatement> TYPE = new StatementType<>(DummyStatement.class, null) {
        @Override
        public DummyStatement readFromNbt(CompoundTag nbt) {
            return null;
        }

        @Override
        public CompoundTag writeToNbt(DummyStatement slot) {
            return new CompoundTag();
        }

        @Override
        public DummyStatement readFromBuffer(FriendlyByteBuf buffer) {
            return null;
        }

        @Override
        public void writeToBuffer(FriendlyByteBuf buffer, DummyStatement slot) {
        }

        @Override
        public DummyStatement convertToType(Object value) {
            return value instanceof DummyStatement statement ? statement : null;
        }
    };

    @Test
    void parameterAtExactMaximumIndexIsCleared() {
        FullStatement<DummyStatement> full = new FullStatement<>(TYPE, 2, null);
        DummyStatement twoParameters = new DummyStatement(2);
        DummyParameter first = new DummyParameter();
        DummyParameter second = new DummyParameter();
        full.set(twoParameters);
        full.set(0, first);
        full.set(1, second);

        full.set(new DummyStatement(1));

        assertSame(first, full.get(0));
        assertNull(full.get(1));
    }

    @Test
    void bulkSetCopiesParameterZero() {
        FullStatement<DummyStatement> full = new FullStatement<>(TYPE, 1, null);
        DummyParameter parameter = new DummyParameter();

        full.set(new DummyStatement(1), new IStatementParameter[] { parameter });

        assertSame(parameter, full.get(0));
    }

    @Test
    void wrapperRotationKeepsVerticalSidesAndRotatesHorizontalSides() {
        DummyStatement delegate = new DummyStatement(0);
        DummyWrapper up = new DummyWrapper(delegate, EnumPipePart.fromFacing(Direction.UP));
        DummyWrapper north = new DummyWrapper(delegate, EnumPipePart.fromFacing(Direction.NORTH));

        up.rotateLeft();
        north.rotateLeft();

        assertEquals(Direction.UP, up.getSourcePart().face);
        assertEquals(Direction.EAST, north.getSourcePart().face);
        assertEquals(2, delegate.rotations);
    }

    private static final class DummyStatement implements IStatement {
        private final int maxParameters;
        private int rotations;

        private DummyStatement(int maxParameters) {
            this.maxParameters = maxParameters;
        }

        @Override public int maxParameters() { return maxParameters; }
        @Override public int minParameters() { return 0; }
        @Override public IStatementParameter createParameter(int index) { return new DummyParameter(); }
        @Override public IStatement rotateLeft() { rotations++; return this; }
        @Override public IStatement[] getPossible() { return new IStatement[] { this }; }
        @Override public String getUniqueTag() { return "buildcraft:test_statement"; }
        @Override public Component getDescription() { return Component.empty(); }
        @Override public ISprite getSprite() { return null; }
    }

    private static final class DummyWrapper extends StatementWrapper {
        private DummyWrapper(IStatement delegate, EnumPipePart sourcePart) {
            super(delegate, sourcePart);
        }

        @Override
        public StatementWrapper[] getPossible() {
            return new StatementWrapper[] { this };
        }
    }

    private static final class DummyParameter implements IStatementParameter {
        @Override public ItemStack getItemStack() { return ItemStack.EMPTY; }
        @Override public IStatementParameter onClick(IStatementContainer source, IStatement stmt, ItemStack stack,
            StatementMouseClick mouse) { return this; }
        @Override public void writeToNbt(CompoundTag nbt) { }
        @Override public IStatementParameter rotateLeft() { return this; }
        @Override public IStatementParameter[] getPossible(IStatementContainer source) {
            return new IStatementParameter[] { this };
        }
        @Override public String getUniqueTag() { return "buildcraft:test_parameter"; }
        @Override public Component getDescription() { return Component.empty(); }
        @Override public ISprite getSprite() { return null; }
    }
}
