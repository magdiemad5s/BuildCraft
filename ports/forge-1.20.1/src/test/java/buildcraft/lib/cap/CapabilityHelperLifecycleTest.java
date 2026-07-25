package buildcraft.lib.cap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import buildcraft.api.core.EnumPipePart;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

class CapabilityHelperLifecycleTest {
    private static final Capability<TestValue> TEST_CAPABILITY = createTestCapability();

    @SuppressWarnings("unchecked")
    private static Capability<TestValue> createTestCapability() {
        try {
            var constructor = Capability.class.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            return (Capability<TestValue>) constructor.newInstance("buildcraft_neo_test");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to construct an isolated test capability", exception);
        }
    }

    @Test
    void directCapabilityIsStableUntilInvalidatedAndFreshAfterRevive() {
        CapabilityHelper helper = new CapabilityHelper();
        TestValue value = new TestValue();
        helper.addCapabilityInstance(TEST_CAPABILITY, value, EnumPipePart.CENTER);

        LazyOptional<TestValue> first = helper.getCapability(TEST_CAPABILITY, null);
        LazyOptional<TestValue> second = helper.getCapability(TEST_CAPABILITY, null);

        assertTrue(first.isPresent());
        assertSame(first, second);
        assertSame(value, first.orElseThrow(() -> new AssertionError("Expected capability to resolve")));

        helper.invalidate();
        assertFalse(first.isPresent());
        assertFalse(helper.getCapability(TEST_CAPABILITY, null).isPresent());

        helper.revive();
        LazyOptional<TestValue> revived = helper.getCapability(TEST_CAPABILITY, null);
        assertTrue(revived.isPresent());
        assertNotSame(first, revived);
        assertSame(value, revived.orElseThrow(() -> new AssertionError("Expected capability to resolve")));
    }

    @Test
    void additionalProviderIsWrappedAndCanBeRevived() {
        CapabilityHelper helper = new CapabilityHelper();
        TestValue value = new TestValue();
        LazyOptional<TestValue> providerOptional = LazyOptional.of(() -> value);
        ICapabilityProvider provider = new ICapabilityProvider() {
            @Override
            public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
                return capability == TEST_CAPABILITY ? providerOptional.cast() : LazyOptional.empty();
            }
        };
        helper.addProvider(provider);

        LazyOptional<TestValue> exposed = helper.getCapability(TEST_CAPABILITY, null);
        assertTrue(exposed.isPresent());

        helper.invalidate();
        assertFalse(exposed.isPresent());
        assertTrue(providerOptional.isPresent(), "The helper must not destroy a provider-owned capability");

        helper.revive();
        LazyOptional<TestValue> revived = helper.getCapability(TEST_CAPABILITY, null);
        assertTrue(revived.isPresent());
        assertNotSame(exposed, revived);
        assertSame(value, revived.orElseThrow(() -> new AssertionError("Expected capability to resolve")));
    }

    @Test
    void temporarilyMissingDirectionalCapabilityDoesNotPoisonLaterQueries() {
        CapabilityHelper helper = new CapabilityHelper();
        AtomicReference<TestValue> current = new AtomicReference<>();
        helper.addCapability(TEST_CAPABILITY, side -> current.get(), EnumPipePart.CENTER);

        assertFalse(helper.getCapability(TEST_CAPABILITY, null).isPresent());

        TestValue value = new TestValue();
        current.set(value);
        assertSame(value, helper.getCapability(TEST_CAPABILITY, null).orElseThrow(() -> new AssertionError("Expected capability to resolve")));
    }

    private static final class TestValue {
    }
}