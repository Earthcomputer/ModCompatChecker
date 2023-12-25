package net.earthcomputer.modcompatchecker.fabric;

import net.earthcomputer.modcompatchecker.BinaryCompatFixture;
import net.earthcomputer.modcompatchecker.checker.Errors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;

public class FabricCheckerTest {
    private static final BinaryCompatFixture fixture = new BinaryCompatFixture().extraPlugins(FabricPlugin.class, AccessWidenerPlugin.class);

    @BeforeAll
    public static void setup() throws IOException {
        fixture.setup();
    }

    @AfterAll
    public static void tearDown() {
        fixture.tearDown();
    }

    private void registerAll() {
        fixture.register("testFabricMod/TestEntryPoint", Errors.METHOD_THROWS_TYPE_REMOVED);
        fixture.register("testFabricMod/TestAccessWidener");
        fixture.register("testFabricMod/TestAccessWidener2");
        fixture.register("testFabricMod/TestAccessWidener3");
    }

    @TestFactory
    public Iterable<DynamicTest> testRegistered() {
        fixture.clearTests();
        registerAll();
        return fixture.getTests();
    }
}
