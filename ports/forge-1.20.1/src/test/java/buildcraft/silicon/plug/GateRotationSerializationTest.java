package buildcraft.silicon.plug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.core.render.ISprite;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementMouseClick;
import buildcraft.lib.statement.FullStatement;
import buildcraft.lib.statement.StatementType;
import buildcraft.core.statements.StatementParameterDirection;
import buildcraft.lib.statement.StatementWrapper;
import buildcraft.silicon.gate.EnumGateLogic;
import buildcraft.silicon.gate.EnumGateMaterial;
import buildcraft.silicon.gate.EnumGateModifier;
import buildcraft.silicon.gate.GateVariant;
import buildcraft.test.MinecraftTestBootstrap;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

class GateRotationSerializationTest {
    private static final StatementType<DummyWrapper> TYPE = new StatementType<>(DummyWrapper.class, null) {
        @Override public DummyWrapper readFromNbt(CompoundTag nbt) { return null; }
        @Override public CompoundTag writeToNbt(DummyWrapper slot) { return new CompoundTag(); }
        @Override public DummyWrapper readFromBuffer(FriendlyByteBuf buffer) { return null; }
        @Override public void writeToBuffer(FriendlyByteBuf buffer, DummyWrapper slot) { }
        @Override public DummyWrapper convertToType(Object value) {
            return value instanceof DummyWrapper wrapper ? wrapper : null;
        }
    };

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootStrap();
    }

    @Test
    void rotatingGateStatementAlsoRotatesItsParameters() {
        FullStatement<DummyWrapper> full = new FullStatement<>(TYPE, 1, null);
        DummyStatement delegate = new DummyStatement();
        DummyWrapper wrapper = new DummyWrapper(delegate, EnumPipePart.fromFacing(Direction.NORTH));
        DummyParameter parameter = new DummyParameter();
        full.set(wrapper);
        full.set(0, parameter);

        PluggableGate.rotateStatementLeft(full);

        assertSame(wrapper, full.get());
        assertEquals(Direction.EAST, wrapper.getSourcePart().face);
        assertEquals(1, delegate.rotations);
        assertEquals(1, parameter.rotations);
        assertSame(parameter, full.get(0));
    }

    @SuppressWarnings("deprecation")
    @Test
    void directionalGateParameterUsesTheSameClockwiseRotationAsStatements() {
        StatementParameterDirection north = new StatementParameterDirection(Direction.NORTH);
        StatementParameterDirection up = new StatementParameterDirection(Direction.UP);

        StatementParameterDirection rotatedNorth = (StatementParameterDirection) north.rotateLeft();
        StatementParameterDirection rotatedUp = (StatementParameterDirection) up.rotateLeft();

        assertEquals(Direction.EAST, rotatedNorth.getDirection());
        assertEquals(Direction.UP, rotatedUp.getDirection());
    }

    @Test
    void everyGateVariantRoundTripsThroughNbtAndPacketLayout() {
        for (EnumGateLogic logic : EnumGateLogic.values()) {
            for (EnumGateMaterial material : EnumGateMaterial.values()) {
                for (EnumGateModifier modifier : EnumGateModifier.values()) {
                    GateVariant original = new GateVariant(logic, material, modifier);
                    GateVariant fromNbt = new GateVariant(original.writeToNBT());
                    assertEquals(original, fromNbt);
                    assertEquals(original.numSlots, fromNbt.numSlots);
                    assertEquals(original.numTriggerArgs, fromNbt.numTriggerArgs);
                    assertEquals(original.numActionArgs, fromNbt.numActionArgs);

                    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
                    try {
                        original.writeToBuffer(buffer);
                        GateVariant fromBuffer = new GateVariant(buffer);
                        assertEquals(original, fromBuffer);
                        assertEquals(0, buffer.readableBytes());
                    } finally {
                        buffer.release();
                    }
                }
            }
        }
    }

    private static final class DummyStatement implements IStatement {
        private int rotations;

        @Override public int maxParameters() { return 1; }
        @Override public int minParameters() { return 0; }
        @Override public IStatementParameter createParameter(int index) { return new DummyParameter(); }
        @Override public IStatement rotateLeft() { rotations++; return this; }
        @Override public IStatement[] getPossible() { return new IStatement[] { this }; }
        @Override public String getUniqueTag() { return "buildcraft:test_gate_statement"; }
        @Override public Component getDescription() { return Component.empty(); }
        @Override public ISprite getSprite() { return null; }
    }

    private static final class DummyWrapper extends StatementWrapper {
        private DummyWrapper(IStatement delegate, EnumPipePart sourcePart) {
            super(delegate, sourcePart);
        }

        @Override public StatementWrapper[] getPossible() { return new StatementWrapper[] { this }; }
    }

    private static final class DummyParameter implements IStatementParameter {
        private int rotations;

        @Override public ItemStack getItemStack() { return ItemStack.EMPTY; }
        @Override public IStatementParameter onClick(IStatementContainer source, IStatement stmt, ItemStack stack,
            StatementMouseClick mouse) { return this; }
        @Override public void writeToNbt(CompoundTag nbt) { }
        @Override public IStatementParameter rotateLeft() { rotations++; return this; }
        @Override public IStatementParameter[] getPossible(IStatementContainer source) {
            return new IStatementParameter[] { this };
        }
        @Override public String getUniqueTag() { return "buildcraft:test_gate_parameter"; }
        @Override public Component getDescription() { return Component.empty(); }
        @Override public ISprite getSprite() { return null; }
    }
}
