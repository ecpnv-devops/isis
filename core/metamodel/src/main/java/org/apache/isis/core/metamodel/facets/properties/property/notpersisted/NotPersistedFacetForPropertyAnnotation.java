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

package org.apache.isis.core.metamodel.facets.properties.property.notpersisted;

import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.Publishing;
import org.apache.isis.applib.annotation.Snapshot;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facets.propcoll.notpersisted.NotPersistedFacet;
import org.apache.isis.core.metamodel.facets.propcoll.notpersisted.NotPersistedFacetAbstract;

public class NotPersistedFacetForPropertyAnnotation extends NotPersistedFacetAbstract {

    public NotPersistedFacetForPropertyAnnotation(final FacetHolder holder) {
        super(holder);
    }

    public static NotPersistedFacet create(
            final Property property,
            final FacetHolder holder) {

        if (property == null) {
            return null;
        }

        // v2 support
        final Publishing publishing = property.entityChangePublishing();
        switch (publishing) {
            case ENABLED:
            case AS_CONFIGURED:
                return null;
            case DISABLED:
                return new NotPersistedFacetForPropertyAnnotation(holder);
            case NOT_SPECIFIED:
                break;
        }

        // v1 support
        return property.notPersisted()
                ? new NotPersistedFacetForPropertyAnnotation(holder)
                : null;
    }
}
