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
package org.apache.isis.core.metamodel.facets.object.recreatable;

import java.util.UUID;

import org.apache.isis.applib.services.jaxb.JaxbService;
import org.apache.isis.applib.services.urlencoding.UrlEncodingService;
import org.apache.isis.commons.internal.debug._Debug;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facets.HasPostConstructMethodCache;

import lombok.Getter;

public class RecreatableObjectFacetForXmlRootElementAnnotation
extends RecreatableObjectFacetAbstract {

    public RecreatableObjectFacetForXmlRootElementAnnotation(
            final FacetHolder holder,
            final HasPostConstructMethodCache postConstructMethodCache) {

        super(holder, RecreationMechanism.INSTANTIATES, postConstructMethodCache, Precedence.HIGH);
    }

    @Override
    protected Object doInstantiate(final Class<?> viewModelClass, final String mementoStr) {
        final String xmlStr = getUrlEncodingService().decodeToString(mementoStr);
        final Object viewModelPojo = getJaxbService().fromXml(viewModelClass, xmlStr);
        return viewModelPojo;
    }

    @Override
    public String memento(final Object vmPojo) {
        final String xml = getJaxbService().toXml(vmPojo);
        final String encoded = getUrlEncodingService().encodeString(xml);
        //FIXME[ISIS-2903] gets called about 4 times per same object, why?
        _Debug.onCondition(true, ()->{
            _Debug.log(100, "%s => %s",
                    super.getMetaModelContext().getInteractionProvider().getInteractionId()
                    .map(UUID::toString)
                    .orElse("no-interaction"),
                    encoded);
        });
        return encoded;
    }

    // -- DEPENDENCIES

    @Getter(lazy=true)
    private final JaxbService jaxbService =
        getServiceRegistry().lookupServiceElseFail(JaxbService.class);

    @Getter(lazy=true)
    private final UrlEncodingService urlEncodingService =
        getServiceRegistry().lookupServiceElseFail(UrlEncodingService.class);

}
