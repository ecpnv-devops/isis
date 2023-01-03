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
package org.apache.isis.core.metamodel.facets.actions.action.command;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.Command.ExecuteIn;
import org.apache.isis.applib.annotation.Command.Persistence;
import org.apache.isis.applib.annotation.CommandExecuteIn;
import org.apache.isis.applib.annotation.CommandPersistence;
import org.apache.isis.applib.annotation.CommandReification;
import org.apache.isis.applib.services.command.CommandDtoProcessor;
import org.apache.isis.core.commons.config.IsisConfiguration;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facets.actions.command.CommandFacet;
import org.apache.isis.core.metamodel.facets.actions.command.CommandFacetAbstract;
import org.apache.isis.core.metamodel.facets.actions.semantics.ActionSemanticsFacet;
import org.apache.isis.core.metamodel.services.ServicesInjector;

public class CommandFacetForActionAnnotation extends CommandFacetAbstract {

    public static CommandFacet create(
            final Action action,
            final IsisConfiguration configuration,
            final ServicesInjector servicesInjector,
            final FacetHolder holder) {

        CommandReification commandReification = CommandReification.AS_CONFIGURED;
        CommandPersistence commandPersistence = CommandPersistence.PERSISTED;
        CommandExecuteIn commandExecuteIn = CommandExecuteIn.FOREGROUND;  // in Estatio, arch rule enforces that we BACKGROUND is disallowed, so this is safe.

        if(action!=null) {
            switch (action.commandPublishing()) {
                // v2
                case ENABLED:
                    commandReification = CommandReification.ENABLED;
                    commandPersistence = CommandPersistence.PERSISTED;
                    commandExecuteIn = CommandExecuteIn.FOREGROUND; // in Estatio, arch rule enforces that we BACKGROUND is disallowed, so this is safe.
                    break;
                case DISABLED:
                    commandReification = CommandReification.DISABLED;
                    commandPersistence = CommandPersistence.NOT_PERSISTED;
                    commandExecuteIn = CommandExecuteIn.FOREGROUND; // in Estatio, arch rule enforces that we BACKGROUND is disallowed, so this is safe.
                    break;
                case AS_CONFIGURED:
                    commandReification = CommandReification.DISABLED;
                    commandPersistence = CommandPersistence.PERSISTED;  // important for Estatio as the key 'isis.services.command.actions' *is* set, and so a facet is installed if not safe semantics (see below)
                    commandExecuteIn = CommandExecuteIn.FOREGROUND; // in Estatio, arch rule enforces that we BACKGROUND is disallowed, so this is safe.
                    break;
                case NOT_SPECIFIED: // this is the default if there is no attribute set.
                default:
                    // v1
                    commandPersistence = action.commandPersistence();
                    commandReification = action.command();
                    commandExecuteIn = action.commandExecuteIn();
            }
        }
        final Class<? extends CommandDtoProcessor> processorClass =
                action != null
                    ? action.commandDtoProcessor()
                    : null;
        final CommandDtoProcessor processor = newProcessorElseNull(processorClass);

        if(processor != null) {
            commandReification = CommandReification.ENABLED;
            commandPersistence = CommandPersistence.PERSISTED;
        }
        final Persistence persistence = CommandPersistence.from(commandPersistence);
        final ExecuteIn executeIn = CommandExecuteIn.from(commandExecuteIn);

        switch (commandReification) {
            case AS_CONFIGURED:
                final CommandActionsConfiguration setting = CommandActionsConfiguration.parse(configuration);
                switch (setting) {
                    case NONE:
                        return null;
                    case IGNORE_SAFE:
                        // in Estatio, this is the branch we go down if AS_CONFIGURED, because the key 'isis.services.command.actions' is set to 'ignoreQueryOnly'
                        final ActionSemanticsFacet actionSemanticsFacet = holder.getFacet(ActionSemanticsFacet.class);
                        if(actionSemanticsFacet == null) {
                            throw new IllegalStateException("Require ActionSemanticsFacet in order to process");
                        }
                        if(actionSemanticsFacet.value().isSafeInNature()) {
                            return  null;
                        }
                        // else fall through
                    default:
                        return action != null
                                ? new CommandFacetForActionAnnotationAsConfigured(persistence, executeIn, Enablement.ENABLED, holder,
                                servicesInjector)
                                : CommandFacetFromConfiguration.create(holder, servicesInjector);
                }
            case DISABLED:
                return null;
            case ENABLED:
                return new CommandFacetForActionAnnotation(
                        persistence, executeIn, Enablement.ENABLED, processor,
                        holder, servicesInjector);
        }

        return null;
    }


    CommandFacetForActionAnnotation(
            final Persistence persistence,
            final ExecuteIn executeIn,
            final Enablement enablement,
            final CommandDtoProcessor processor,
            final FacetHolder holder,
            final ServicesInjector servicesInjector) {
        super(persistence, executeIn, enablement, processor, holder, servicesInjector);
    }


}
