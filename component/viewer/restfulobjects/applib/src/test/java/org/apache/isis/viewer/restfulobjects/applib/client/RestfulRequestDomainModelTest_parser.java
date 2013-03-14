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
package org.apache.isis.viewer.restfulobjects.applib.client;

import static org.junit.Assert.assertSame;

import org.apache.isis.viewer.restfulobjects.applib.client.RestfulRequest.DomainModel;
import org.apache.isis.viewer.restfulobjects.applib.util.Parser;
import org.junit.Test;

public class RestfulRequestDomainModelTest_parser {

    @Test
    public void parser_roundtrips() {
        final Parser<DomainModel> parser = RestfulRequest.DomainModel.parser();
        for (final DomainModel domainModel : DomainModel.values()) {
            final String asString = parser.asString(domainModel);
            final DomainModel roundtripped = parser.valueOf(asString);
            assertSame(roundtripped, domainModel);
        }
    }

}
