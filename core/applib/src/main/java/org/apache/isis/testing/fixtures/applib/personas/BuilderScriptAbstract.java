/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.testing.fixtures.applib.personas;

import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.testing.fixtures.applib.fixturescripts.FixtureScript;
import org.apache.isis.applib.fixturescripts.FixtureScriptWithExecutionStrategy;
import org.apache.isis.applib.fixturescripts.FixtureScripts;

@Programmatic
public abstract class BuilderScriptAbstract<T>
        extends FixtureScript
        implements FixtureScriptWithExecutionStrategy {

    private final FixtureScripts.MultipleExecutionStrategy executionStrategy;

    /**
     * Typically we expect builders to have value semantics, so this is provided as a convenience.
     */
    protected BuilderScriptAbstract() {
        this(FixtureScripts.MultipleExecutionStrategy.EXECUTE_ONCE_BY_VALUE);
    }

    protected BuilderScriptAbstract(final FixtureScripts.MultipleExecutionStrategy executionStrategy) {
        this.executionStrategy = executionStrategy;
    }

    @Override
    public FixtureScripts.MultipleExecutionStrategy getMultipleExecutionStrategy() {
        return executionStrategy;
    }

    @Programmatic
    public BuilderScriptAbstract<T> build(
            final FixtureScript parentFixtureScript,
            ExecutionContext executionContext) {

        final BuilderScriptAbstract<T> onFixture = BuilderScriptAbstract.this;
        parentFixtureScript.getContainer().injectServicesInto(onFixture);

        // returns the fixture script that is run
        // (either this one, or possibly one previously executed).
        return executionContext.executeChildT(parentFixtureScript, this);
    }


    @Override
    protected abstract void execute(final ExecutionContext executionContext);

    public abstract T getObject();

    public <P extends PersonaWithBuilderScript<X, B>, X, B extends BuilderScriptAbstract<X>> X objectFor(
            final P persona,
            final FixtureScript.ExecutionContext ec) {
        if(persona == null) {
            return null;
        }
        final B fixtureScript = persona.builder();
        return ec.executeChildT(this, fixtureScript).getObject();
    }

    public <P extends PersonaWithFinder<X>, X> X findUsing(final P persona) {
        if(persona == null) {
            return null;
        }
        return persona.findUsing(serviceRegistry);
    }


}

