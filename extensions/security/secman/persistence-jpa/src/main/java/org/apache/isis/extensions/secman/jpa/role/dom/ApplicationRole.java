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
package org.apache.isis.extensions.secman.jpa.role.dom;

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.isis.applib.annotation.BookmarkPolicy;
import org.apache.isis.applib.annotation.Bounding;
import org.apache.isis.applib.annotation.Collection;
import org.apache.isis.applib.annotation.CollectionLayout;
import org.apache.isis.applib.annotation.DomainObject;
import org.apache.isis.applib.annotation.DomainObjectLayout;
import org.apache.isis.applib.annotation.Editing;
import org.apache.isis.applib.annotation.Property;
import org.apache.isis.applib.annotation.PropertyLayout;
import org.apache.isis.applib.types.DescriptionType;
import org.apache.isis.applib.util.Equality;
import org.apache.isis.applib.util.Hashing;
import org.apache.isis.applib.util.ObjectContracts;
import org.apache.isis.applib.util.ToString;
import org.apache.isis.extensions.secman.jpa.permission.dom.ApplicationPermission;
import org.apache.isis.extensions.secman.jpa.permission.dom.ApplicationPermissionRepository;
import org.apache.isis.extensions.secman.jpa.user.dom.ApplicationUser;
import org.apache.isis.persistence.jpa.applib.integration.JpaEntityInjectionPointResolver;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        schema = "isisExtensionsSecman",
        name = "ApplicationRole",
        uniqueConstraints =
            @UniqueConstraint(
                    name = "ApplicationRole_name_UNQ",
                    columnNames={"name"})
)
@NamedQueries({
    @NamedQuery(
            name = org.apache.isis.extensions.secman.api.role.dom.ApplicationRole.NAMED_QUERY_FIND_BY_NAME,
            query = "SELECT r "
                  + "FROM org.apache.isis.extensions.secman.jpa.role.dom.ApplicationRole r "
                  + "WHERE r.name = :name"),
    @NamedQuery(
            name = org.apache.isis.extensions.secman.api.role.dom.ApplicationRole.NAMED_QUERY_FIND_BY_NAME_CONTAINING,
            query = "SELECT r "
                  + "FROM org.apache.isis.extensions.secman.jpa.role.dom.ApplicationRole r "
                  + "WHERE r.name LIKE :regex"),
})
@EntityListeners(JpaEntityInjectionPointResolver.class)
@DomainObject(
        bounding = Bounding.BOUNDED,
        //		bounded = true,
        objectType = "isis.ext.secman.ApplicationRole",
        autoCompleteRepository = ApplicationRoleRepository.class,
        autoCompleteAction = "findMatching"
        )
@DomainObjectLayout(
        bookmarking = BookmarkPolicy.AS_ROOT
        )
public class ApplicationRole
implements
        org.apache.isis.extensions.secman.api.role.dom.ApplicationRole,
    Comparable<ApplicationRole> {

    @Inject private transient ApplicationPermissionRepository applicationPermissionRepository;


    @Id
    @GeneratedValue
    private Long id;


    // -- NAME

    @Column(nullable=false, length= Name.MAX_LENGTH)
    @Property(
            domainEvent = Name.DomainEvent.class,
            editing = Editing.DISABLED
            )
    @PropertyLayout(typicalLength= Name.TYPICAL_LENGTH, sequence = "1")
    @Getter @Setter
    private String name;


    // -- DESCRIPTION

    @Column(nullable=true, length=DescriptionType.Meta.MAX_LEN)
    @Property(
            domainEvent = Description.DomainEvent.class,
            editing = Editing.DISABLED
            )
    @PropertyLayout(
            typicalLength= Description.TYPICAL_LENGTH,
            sequence = "2")
    @Getter @Setter
    private String description;


    // -- USERS

    @ManyToMany
    @Collection(
            domainEvent = Users.DomainEvent.class
            )
    @CollectionLayout(
            defaultView="table",
            sequence = "20")
    @Getter @Setter
    private SortedSet<ApplicationUser> users = new TreeSet<>();


    // necessary for integration tests
    public void addToUsers(final ApplicationUser applicationUser) {
        getUsers().add(applicationUser);
    }
    // necessary for integration tests
    public void removeFromUsers(final ApplicationUser applicationUser) {
        getUsers().remove(applicationUser);
    }


    // -- PERMISSIONS
    // (derived collection)

    @Collection(
            domainEvent = Permissions.DomainEvent.class
    )
    @CollectionLayout(
            defaultView="table",
            sortedBy = ApplicationPermission.DefaultComparator.class,
            sequence = "10")
    public List<org.apache.isis.extensions.secman.api.permission.dom.ApplicationPermission> getPermissions() {
        return applicationPermissionRepository.findByRole(this);
    }


    // -- equals, hashCode, compareTo, toString

    private static final Comparator<ApplicationRole> comparator =
            Comparator.comparing(ApplicationRole::getName);

    private static final Equality<ApplicationRole> equality =
            ObjectContracts.checkEquals(ApplicationRole::getName);

    private static final Hashing<ApplicationRole> hashing =
            ObjectContracts.hashing(ApplicationRole::getName);

    private static final ToString<ApplicationRole> toString =
            ObjectContracts.toString("name", ApplicationRole::getName);


    @Override
    public int compareTo(final ApplicationRole o) {
        return comparator.compare(this, o);
    }

    @Override
    public boolean equals(final Object obj) {
        return equality.equals(this, obj);
    }

    @Override
    public int hashCode() {
        return hashing.hashCode(this);
    }

    @Override
    public String toString() {
        return toString.toString(this);
    }


}
