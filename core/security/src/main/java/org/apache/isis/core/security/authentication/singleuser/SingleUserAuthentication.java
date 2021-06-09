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
package org.apache.isis.core.security.authentication.singleuser;

import org.apache.isis.applib.services.iactnlayer.InteractionContext;
import org.apache.isis.applib.services.user.UserMemento;
import org.apache.isis.core.security.authentication.AuthenticationAbstract;

public final class SingleUserAuthentication extends AuthenticationAbstract {

    private static final long serialVersionUID = 1L;

    private static final InteractionContext DEFAULT_SINGLE_USER_ENVIRONMENT =
            InteractionContext.ofUserWithSystemDefaults(
                    UserMemento.ofName("prototyping"));

    /**
     * Defaults session's authentication validation code to {@code ""}
     */
    public SingleUserAuthentication() {
        this(AuthenticationAbstract.DEFAULT_AUTH_VALID_CODE);
    }

    public SingleUserAuthentication(final String authValidationCode) {
        super(DEFAULT_SINGLE_USER_ENVIRONMENT, authValidationCode);
    }

}
