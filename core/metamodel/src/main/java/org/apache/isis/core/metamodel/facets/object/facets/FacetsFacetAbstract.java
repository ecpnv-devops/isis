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

package org.apache.isis.core.metamodel.facets.object.facets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.isis.commons.internal.factory._InstanceUtil;
import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facetapi.FacetAbstract;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facets.FacetFactory;

import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class FacetsFacetAbstract extends FacetAbstract implements FacetsFacet {

    private static final Class<? extends Facet> type() {
        return FacetsFacet.class;
    }

    private final Class<? extends FacetFactory>[] facetFactories;

    public FacetsFacetAbstract(final String[] names, final Class<?>[] classes, final FacetHolder holder) {
        super(type(), holder);
        final List<Class<? extends FacetFactory>> facetFactories = new ArrayList<Class<? extends FacetFactory>>();
        for (final String name : names) {
            final Class<? extends FacetFactory> facetFactory = facetFactoryOrNull(name);
            if (facetFactory != null) {
                facetFactories.add(facetFactory);
            }
        }
        for (final Class<?> classe : classes) {
            final Class<? extends FacetFactory> facetFactory = facetFactoryOrNull(classe);
            if (facetFactory != null) {
                facetFactories.add(facetFactory);
            }
        }
        this.facetFactories = asArray(facetFactories);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends FacetFactory>[] asArray(final List<Class<? extends FacetFactory>> facetFactories) {
        return facetFactories.toArray(new Class[] {});
    }

    @Override
    public Class<? extends FacetFactory>[] facetFactories() {
        return facetFactories;
    }

    private Class<? extends FacetFactory> facetFactoryOrNull(final String classCandidateName) {
        if (classCandidateName == null) {
            return null;
        }
        Class<?> classCandidate = null;
        try {
            classCandidate = _InstanceUtil.loadClass(classCandidateName);
            return facetFactoryOrNull(classCandidate);
        } catch (final Exception ex) {
            log.warn("failed to load class by name {}", classCandidateName, ex);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends FacetFactory> facetFactoryOrNull(final Class<?> classCandidate) {
        if (classCandidate == null) {
            return null;
        }
        return (Class<? extends FacetFactory>) (FacetFactory.class.isAssignableFrom(classCandidate) ? classCandidate : null);
    }

    @Override
    public void visitAttributes(final BiConsumer<String, Object> visitor) {
        super.visitAttributes(visitor);
        visitor.accept("facetFactories", facetFactories);
    }
}
