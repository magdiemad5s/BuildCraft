package buildcraft.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

class CreativeTabManagerTest {
    @Test
    void providerExceptionFailsWithTabAndProviderContext() {
        IllegalArgumentException providerFailure = new IllegalArgumentException("expected failure");
        Supplier<List<String>> provider = () -> {
            throw providerFailure;
        };
        AtomicReference<RuntimeException> reportedFailure = new AtomicReference<>();

        IllegalStateException reported = assertThrows(
            IllegalStateException.class,
            () -> CreativeTabProviderCollector.collect(
                "buildcraft.test_provider_exception",
                List.of(provider),
                ignored -> {
                },
                true,
                (tabName, failedProvider, exception) -> reportedFailure.set(exception)
            )
        );

        assertSame(providerFailure, reported.getCause());
        assertSame(providerFailure, reportedFailure.get());
        assertTrue(reported.getMessage().contains("buildcraft.test_provider_exception"));
    }

    @Test
    void nullProviderResultIsNotSilentlyIgnored() {
        IllegalStateException reported = assertThrows(
            IllegalStateException.class,
            () -> CreativeTabProviderCollector.collect(
                "buildcraft.test_null_provider",
                List.<Supplier<List<String>>>of(() -> null),
                ignored -> {
                },
                true,
                (tabName, failedProvider, exception) -> {
                }
            )
        );

        assertTrue(reported.getCause().getMessage().contains("returned null"));
    }

    @Test
    void productionModeReportsFailureAndContinuesRemainingProviders() {
        List<String> collected = new ArrayList<>();
        AtomicInteger failures = new AtomicInteger();
        List<Supplier<? extends List<String>>> providers = List.of(
            () -> {
                throw new IllegalStateException("optional integration unavailable");
            },
            () -> List.of("remaining item")
        );

        CreativeTabProviderCollector.collect(
            "buildcraft.production",
            providers,
            collected::add,
            false,
            (tabName, failedProvider, exception) -> failures.incrementAndGet()
        );

        assertEquals(1, failures.get());
        assertEquals(List.of("remaining item"), collected);
    }
}