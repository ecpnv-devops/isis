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
package org.apache.isis.core.metamodel.facets.actions.notcontributed.derived;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;

import org.apache.isis.applib.annotation.DomainService;
import org.apache.isis.applib.annotation.NatureOfService;
import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facets.AbstractFacetFactoryJUnit4TestCase;
import org.apache.isis.core.metamodel.facets.FacetFactory;
import org.apache.isis.core.metamodel.facets.FacetedMethod;
import org.apache.isis.core.metamodel.facets.actions.notcontributed.NotContributedFacet;
import org.apache.isis.core.metamodel.facets.object.domainservice.DomainServiceFacet;
import org.apache.isis.core.metamodel.facets.object.domainservice.DomainServiceFacetAbstract;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class NotContributedFacetDerivedFromDomainServiceFacetFactoryTest extends AbstractFacetFactoryJUnit4TestCase {

    private NotContributedFacetDerivedFromDomainServiceFacetFactory facetFactory;

    @Before
    public void setUp() throws Exception {
        facetFactory = new NotContributedFacetDerivedFromDomainServiceFacetFactory();
        facetFactory.setServicesInjector(mockServicesInjector);
    }

    @Test
    public void whenView() throws Exception {

        // given
        @DomainService(nature = NatureOfService.VIEW)
        class CustomerService {

            public String name() {
                return "Joe";
            }
        }

        context.checking(new Expectations() {{
            allowing(mockSpecificationLoader).loadSpecification(CustomerService.class);
            will(returnValue(mockObjSpec));

            allowing(mockObjSpec).getFacet(DomainServiceFacet.class);
            will(returnValue(new DomainServiceFacetAbstract(mockObjSpec, null, NatureOfService.VIEW) {
            }));
        }});

        expectNoMethodsRemoved();

        facetedMethod = FacetedMethod.createForAction(CustomerService.class, "name", mockSpecificationLoader);

        // when
        facetFactory.process(new FacetFactory.ProcessMethodContext(CustomerService.class, null, null, facetedMethod.getMethod(), mockMethodRemover, facetedMethod));

        // then
        final Facet facet = facetedMethod.getFacet(NotContributedFacet.class);
        assertThat(facet, is(not(nullValue())));
        assertThat(facet instanceof NotContributedFacetDerivedFromDomainServiceFacet, is(true));
        final NotContributedFacetDerivedFromDomainServiceFacet facetDerivedFromDomainServiceFacet = (NotContributedFacetDerivedFromDomainServiceFacet) facet;
        assertThat(facetDerivedFromDomainServiceFacet.getNatureOfService(), equalTo(NatureOfService.VIEW));
    }

    @Test
    public void whenDomain() throws Exception {

        // given
        @DomainService(nature = NatureOfService.DOMAIN)
        class CustomerService {

            public String name() {
                return "Joe";
            }
        }

        context.checking(new Expectations() {{
            allowing(mockSpecificationLoader).loadSpecification(CustomerService.class);
            will(returnValue(mockObjSpec));

            allowing(mockObjSpec).getFacet(DomainServiceFacet.class);
            will(returnValue(new DomainServiceFacetAbstract(mockObjSpec, null, NatureOfService.DOMAIN) {
            }));
        }});

        expectNoMethodsRemoved();

        facetedMethod = FacetedMethod.createForAction(CustomerService.class, "name", mockSpecificationLoader);

        // when
        facetFactory.process(new FacetFactory.ProcessMethodContext(CustomerService.class, null, null, facetedMethod.getMethod(), mockMethodRemover, facetedMethod));

        // then
        final Facet facet = facetedMethod.getFacet(NotContributedFacet.class);
        assertThat(facet, is(not(nullValue())));
        assertThat(facet instanceof NotContributedFacetDerivedFromDomainServiceFacet, is(true));
        final NotContributedFacetDerivedFromDomainServiceFacet facetDerivedFromDomainServiceFacet = (NotContributedFacetDerivedFromDomainServiceFacet) facet;
        assertThat(facetDerivedFromDomainServiceFacet.getNatureOfService(), equalTo(NatureOfService.DOMAIN));
    }



    @Test
    public void whenNone() throws Exception {

        // given
        class CustomerService {

            public String name() {
                return "Joe";
            }
        }

        context.checking(new Expectations() {{
            allowing(mockSpecificationLoader).loadSpecification(CustomerService.class);
            will(returnValue(mockObjSpec));

            allowing(mockObjSpec).getFacet(DomainServiceFacet.class);
            will(returnValue(null));
        }});

        expectNoMethodsRemoved();

        facetedMethod = FacetedMethod.createForAction(CustomerService.class, "name", mockSpecificationLoader);

        // when
        facetFactory.process(new FacetFactory.ProcessMethodContext(CustomerService.class, null, null, facetedMethod.getMethod(), mockMethodRemover, facetedMethod));

        // then
        final Facet facet = facetedMethod.getFacet(NotContributedFacet.class);
        assertThat(facet, is(nullValue()));
    }

}
