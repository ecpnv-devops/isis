package org.apache.isis.testing.fixtures.applib.modules;

import org.apache.isis.applib.fixturescripts.FixtureScript;

public interface ModuleWithFixtures {

    /**
     * Optionally each module can define a {@link FixtureScript} which holds immutable "reference data".
     *
     * <p>
     *     By default, returns a {@link FixtureScript#NOOP noop}.
     * </p>
     */
    default FixtureScript getRefDataSetupFixture() {
        return org.apache.isis.testing.fixtures.applib.fixturescripts.FixtureScript.NOOP;
    }

    /**
     * Optionally each module can define a tear-down {@link FixtureScript}, used to remove the contents of <i>all</i>
     * transactional entities (both reference data and operational/transactional data).
     *
     * <p>
     *     By default, returns a {@link FixtureScript#NOOP noop}.
     * </p>
     */
    default FixtureScript getTeardownFixture() {
        return org.apache.isis.testing.fixtures.applib.fixturescripts.FixtureScript.NOOP;
    }

}
