/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.isis.testdomain.tests;

import static org.apache.isis.commons.internal.collections._Collections.toStringJoiningNewLine;
import static org.apache.isis.commons.internal.collections._Sets.intersectSorted;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.isis.applib.fixturescripts.FixtureScripts;
import org.apache.isis.applib.services.fixturespec.FixtureScriptsDefault;
import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.applib.services.xactn.TransactionService;
import org.apache.isis.commons.internal.resources._Json;
import org.apache.isis.commons.internal.resources._Resources;
import org.apache.isis.commons.ioc.BeanAdapter;
import org.apache.isis.core.runtime.system.context.IsisContext;
import org.apache.isis.core.runtime.system.session.IsisSessionFactory;
import org.apache.isis.runtime.spring.IsisBoot;
import org.apache.isis.testdomain.jdo.JdoTestDomainModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import lombok.val;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = {
                //HeadlessTransactionSupportDefault.class,
                IsisBoot.class,
                FixtureScriptsDefault.class,
                JdoTestDomainModule.class,
                },
        properties = {
        		"logging.config=log4j2-test.xml",
                //"isis.reflector.introspector.parallelize=false",
                //"logging.level.org.apache.isis.core.metamodel.specloader.specimpl.ObjectSpecificationAbstract=TRACE"
                }
        )
class SpringServiceProvisioningTest {

    @Inject IsisSessionFactory isisSessionFactory;
    @Inject TransactionService transactionService;
    @Inject FixtureScripts fixtureScripts;
    @Inject RepositoryService repository;
    
    @BeforeEach
    void beforeEach() {
        System.out.println("================== START ====================");
    }
    
    @Test
    void builtInServicesShouldBeSetUp() throws IOException {
        
        val serviceRegistry = IsisContext.getServiceRegistry();
        val managedServices = serviceRegistry.streamRegisteredBeans()
                .map(BeanAdapter::getBeanClass)
                .map(Class::getName)
                .collect(Collectors.toCollection(TreeSet::new));
        
        val singletonJson = _Resources.loadAsString(this.getClass(), "builtin-IsisBoot.json", StandardCharsets.UTF_8);
        val singletonSet = new TreeSet<>(_Json.readJsonList(String.class, singletonJson));
        
        // same as managedServices.containsAll(singletonSet) but more verbose in case of failure        
        assertEquals(
                toStringJoiningNewLine(singletonSet), 
                toStringJoiningNewLine(intersectSorted(managedServices, singletonSet)));
        
        //TODO also test for request-scoped service (requires a means to mock a request-context)
        
    }
 
        
}
