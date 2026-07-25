package buildcraft.api.transport.pipe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

class PipeFlowCapabilityLifecycleTest {
    private static final Capability<TestValue> TEST_CAPABILITY = createTestCapability();

    @SuppressWarnings("unchecked")
    private static Capability<TestValue> createTestCapability() {
        try {
            var constructor = Capability.class.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            return (Capability<TestValue>) constructor.newInstance("buildcraft_pipe_flow_test");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to construct an isolated test capability", exception);
        }
    }

    @Test
    void cachedHandleInvalidatesAndRevivesWithFreshIdentity() {
        TestFlow flow = new TestFlow();

        LazyOptional<TestValue> first = flow.getCapability(TEST_CAPABILITY, Direction.NORTH);
        LazyOptional<TestValue> second = flow.getCapability(TEST_CAPABILITY, Direction.NORTH);
        assertSame(first, second);
        assertTrue(first.isPresent());

        flow.invalidateCapabilities();
        assertFalse(first.isPresent());
        assertFalse(flow.getCapability(TEST_CAPABILITY, Direction.NORTH).isPresent());

        flow.reviveCapabilities();
        LazyOptional<TestValue> revived = flow.getCapability(TEST_CAPABILITY, Direction.NORTH);
        assertTrue(revived.isPresent());
        assertNotSame(first, revived);
        assertSame(flow.value, revived.orElseThrow(() -> new AssertionError("Capability did not revive")));
    }

    private static final class TestFlow extends PipeFlow {
        private final TestValue value = new TestValue();

        private TestFlow() {
            super((IPipe) null);
        }

        @Override
        public boolean canConnect(Direction face, PipeFlow other) {
            return false;
        }

        @Override
        public boolean canConnect(Direction face, BlockEntity other) {
            return false;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction facing) {
            return capability == TEST_CAPABILITY
                ? getCachedCapability(TEST_CAPABILITY, facing, value).cast()
                : LazyOptional.empty();
        }
    }

    private static final class TestValue {
    }
}
