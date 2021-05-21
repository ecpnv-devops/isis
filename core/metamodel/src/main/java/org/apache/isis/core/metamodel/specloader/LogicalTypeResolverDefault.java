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

package org.apache.isis.core.metamodel.specloader;

import java.util.Map;
import java.util.Optional;

import org.apache.isis.applib.id.LogicalType;
import org.apache.isis.commons.internal.collections._Maps;
import org.apache.isis.core.metamodel.facets.object.objectspecid.ObjectTypeFacet;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;

import lombok.NonNull;
import lombok.val;
import lombok.extern.log4j.Log4j2;

@Log4j2
class LogicalTypeResolverDefault implements LogicalTypeResolver {

    private final Map<String, LogicalType> logicalTypeByName = _Maps.newConcurrentHashMap();

    @Override
    public void clear() {
        logicalTypeByName.clear();
    }

    @Override
    public Optional<LogicalType> lookup(final @NonNull String logicalTypeName) {
        return Optional.ofNullable(logicalTypeByName.get(logicalTypeName));
    }

    @Override
    public void register(final @NonNull ObjectSpecification spec) {

        // collect concrete classes (do not collect abstract or anonymous types or interfaces)
        if(!spec.isAbstract()
                && hasUsableObjectTypeFacet(spec)) {

            val key = spec.getLogicalTypeName();

            val previousMapping = logicalTypeByName.put(key, spec.getLogicalType());

            if(previousMapping!=null) {

                val msg = String.format("Overriding existing mapping\n"
                        + "%s -> %s,\n"
                        + "with\n "
                        + "%s -> %s\n "
                        + "This will result in the meta-model validation to fail.",
                        key, previousMapping.getCorrespondingClass(),
                        key, spec.getCorrespondingClass());

                log.warn(msg);

            }
        }
    }

    // -- HELPER

    private boolean hasUsableObjectTypeFacet(ObjectSpecification spec) {
        // anonymous inner classes (eg org.estatio.dom.WithTitleGetter$ToString$1)
        // don't have an ObjectType; hence the guard.
        return spec.containsNonFallbackFacet(ObjectTypeFacet.class);
    }

}
