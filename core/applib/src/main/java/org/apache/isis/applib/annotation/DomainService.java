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

package org.apache.isis.applib.annotation;

import javax.inject.Named;
import java.lang.annotation.*;

/**
 * Indicates that the class should be automatically recognized as a domain service.
 *
 * <p>
 * Also indicates whether the domain service acts as a repository for an entity, and menu ordering UI hints.
 * </p>
 */
@Inherited
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainService {



    /**
     * Provides the (first part of the) unique identifier (OID) for the service (the instanceId is always &quot;1&quot;).
     *
     * <p>
     * If not specified then either the optional &quot;getId()&quot is used, otherwise the class' name.
     */
    @Deprecated
    String objectType() default "";


    /**
     * If this domain service acts as a repository for an entity type, specify that entity type.
     */
    @Deprecated
    Class<?> repositoryFor() default Object.class;

    /**
     * The nature of this service, eg for menus, contributed actions, repository.
     */
    NatureOfService nature() default NatureOfService.VIEW;

    /**
     * Number in Dewey Decimal format representing the order.
     *
     * <p>
     * Same convention as {@link MemberOrder#sequence()}.  If not specified, placed after any named.
     * </p>
     *
     * <p>
     *     Either this attribute or {@link DomainServiceLayout#menuOrder()} can be used; they are equivalent.
     *     Typically this attribute is used for services with a {@link #nature() nature} of
     *     {@link NatureOfService#DOMAIN domain} (these are not visible in the UI) whereas
     *     {@link DomainServiceLayout#menuOrder()} is used for services with a nature of
     *     {@link NatureOfService#VIEW_MENU_ONLY} (which do appear in the UI)
     * </p>
     *
     * <p>
     *     The default value is set to "Integer.MAX_VALUE - 100" so that any domain services intended to override the
     *     default implementations provided by the framework itself will do so without having to specify the
     *     menuOrder (with the exception of <tt>EventBusServiceJdo</tt>, all framework implementations have a
     *     default order greater than Integer.MAX_VALUE - 50).
     * </p>
     */
    @Deprecated
    String menuOrder() default Constants.MENU_ORDER_DEFAULT  ;


    /**
     * The logical name of this object's type, that uniquely and fully qualifies it.
     * The logical name is analogous to - but independent of - the actual fully qualified class name.
     * eg. {@code sales.CustomerService} for a class 'org.mycompany.services.CustomerService'
     * <p>
     * This value, if specified, is used in the serialized form of the object's {@link Bookmark}.
     * A {@link Bookmark} is used by the framework to uniquely identify an object over time
     * (same concept as a URN).
     * Otherwise, if not specified, the fully qualified class name is used instead.
     * </p>
     *
     * @Deprecated use @Named instead
     * @see DomainObject#logicalTypeName()
     */
    @Deprecated
    String logicalTypeName()
            default "";
}
