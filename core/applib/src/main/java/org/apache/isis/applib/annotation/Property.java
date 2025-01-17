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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.jdo.annotations.NotPersistent;

import org.apache.isis.applib.conmap.ContentMappingServiceForCommandDto;
import org.apache.isis.applib.conmap.ContentMappingServiceForCommandsDto;
import org.apache.isis.applib.services.command.Command;
import org.apache.isis.applib.services.command.CommandDtoProcessor;
import org.apache.isis.applib.services.command.CommandWithDto;
import org.apache.isis.applib.services.command.spi.CommandService;
import org.apache.isis.applib.services.eventbus.PropertyDomainEvent;
import org.apache.isis.applib.spec.Specification;
import org.apache.isis.applib.value.Blob;
import org.apache.isis.applib.value.Clob;

/**
 * Domain semantics for domain object property.
 */
@Inherited
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@DomainObject(nature=Nature.MIXIN, mixinMethod = Property.MIXIN_METHOD) // meta annotation, only applies at class level
public @interface Property {

    final static String MIXIN_METHOD = "prop";

    /**
     * Indicates that changes to the property that should be posted to the
     * {@link org.apache.isis.applib.services.eventbus.EventBusService event bus} using a custom (subclass of)
     * {@link org.apache.isis.applib.services.eventbus.PropertyDomainEvent}.
     *
     * <p>For example:
     * </p>
     *
     * <pre>
     * public static class StartDateChanged extends PropertyDomainEvent { ... }
     *
     * &#64;Property(domainEvent=StartDateChanged.class)
     * public LocalDate getStartDate() { ...}
     * </pre>
     *
     * <p>
     * This subclass must provide a no-arg constructor; the fields are set reflectively.
     * </p>
     */
    Class<? extends PropertyDomainEvent<?,?>> domainEvent() default PropertyDomainEvent.Default.class;

    /**
     * Indicates where the property is not visible to the user.
     */
    Where hidden() default Where.NOWHERE;

    /**
     * If set to {@link Projecting#PROJECTED projected}, then indicates that the owner of this property is a view model
     * which is a projection of some other entity, and that the property holds a reference to that
     * &quot;underlying&quot;.
     *
     * <p>
     *     This is used to automatically redirect any bookmarks to the view model (projection) to instead be directed
     *     at the underlying entity.
     * </p>
     *
     * <p>
     *     Only one such property should be marked as being a projection with a view model.
     * </p>
     */
    Projecting projecting() default Projecting.NOT_SPECIFIED;

    /**
     * Whether the properties of this domain object can be edited, or collections of this object be added to/removed from.
     *
     * <p>
     *     Note that non-editable objects can nevertheless have actions invoked upon them.
     * </p>
     */
    Editing editing() default Editing.AS_CONFIGURED;

    /**
     * If {@link #editing()} is set to {@link Editing#DISABLED},
     * then the reason to provide to the user as to why this property cannot be edited.
     */
    String editingDisabledReason() default "";

    /**
     * Whether the property edit should be reified into a {@link org.apache.isis.applib.services.command.Command} object.
     *
     * @Deprecated replaced with commandPublishing
     */
    @Deprecated
    CommandReification command() default CommandReification.AS_CONFIGURED;

    /**
     * Whether property edits, captured as {@link Command}s,
     * should be published to {@link CommandSubscriber}s.
     *
     * @see Action#commandPublishing()
     * @see Property#commandDtoProcessor()
     */
    Publishing commandPublishing()
            default Publishing.NOT_SPECIFIED;

    /**
     * How the {@link org.apache.isis.applib.services.command.Command Command} object provided by the
     * {@link org.apache.isis.applib.services.command.CommandContext CommandContext} domain service should be persisted.
     *
     * @Deprecated replaced by commandPublishing
     */
    @Deprecated
    CommandPersistence commandPersistence() default CommandPersistence.PERSISTED;

    /**
     * How the command/property edit should be executed.
     *
     * <p>
     * If the corresponding {@link org.apache.isis.applib.services.command.Command Command} object is persisted,
     * then its {@link org.apache.isis.applib.services.command.Command#getExecuteIn() invocationType} property
     * will be set to this value.
     * </p>
     */
    @Deprecated
    CommandExecuteIn commandExecuteIn() default CommandExecuteIn.FOREGROUND;



    /**
     * Whether the property edit should be published.
     *
     * <p>
     * Requires that an implementation of the {@link org.apache.isis.applib.services.publish.PublishingService}
     * or {@link org.apache.isis.applib.services.publish.PublisherService} is registered with the framework.
     * </p>
     *
     * @Deprecated replaced by {@link #executionPublishing()}
     */
    @Deprecated
    Publishing publishing() default Publishing.AS_CONFIGURED;


    /**
     * Whether
     * {@link org.apache.isis.applib.services.iactn.Interaction.Execution}s
     * (triggered property edits), should be dispatched to
     * {@link org.apache.isis.applib.services.publish.PublisherService}s.
     *
     * @see Action#executionPublishing()
     */
    Publishing executionPublishing()
            default Publishing.NOT_SPECIFIED;

    /**
     * The {@link CommandDtoProcessor} to process this command's DTO.
     *
     * <p>
     *     Specifying a processor requires that the implementation of {@link CommandService} provides a
     *     custom implementation of {@link org.apache.isis.applib.services.command.Command} that additional extends
     *     from {@link CommandWithDto}.
     * </p>
     *
     * <p>
     *     Tprocessor itself is used by {@link ContentMappingServiceForCommandDto} and
     *     {@link ContentMappingServiceForCommandsDto} to dynamically transform the DTOs.
     * </p>
     */
    Class<? extends CommandDtoProcessor> commandDtoProcessor() default CommandDtoProcessor.class;




    /**
     * The maximum entry length of a field.
     *
     * <p>
     *     The default value (<code>-1</code>) indicates that no maxLength has been specified.
     * </p>
     */
    int maxLength() default -1;





    /**
     * The {@link org.apache.isis.applib.spec.Specification}(s) to be satisfied by this property.
     *
     * <p>
     * If more than one is provided, then all must be satisfied (in effect &quot;AND&quot;ed together).
     * </p>
     */
    Class<? extends Specification>[] mustSatisfy() default {};


    // //////////////////////////////////////

    /**
     * Indicates that the property should be excluded from snapshots.
     *
     * <p>
     *     To ensure that the property is actually not persisted in the objectstore, also annotate with {@link NotPersistent}.
     * </p>
     *
     * @deprecated replaced with {@link #entityChangePublishing()}
     */
    @Deprecated
    boolean notPersisted() default false;


    /**
     * When set to {@link Publishing#DISABLED},
     * vetoes publishing of updates for this property.
     * Otherwise has no effect, except when using {@link Publishing#ENABLED} to override
     * an inherited property annotation, which is a supported use-case.
     *
     * <p>
     * Relates to {@link DomainObject#entityChangePublishing()}, which controls
     * whether entity-change-publishing is enabled for the corresponding entity type.
     *
     * @see DomainObject#entityChangePublishing()
     * @apiNote does only apply to persistent properties of entity objects
     */
    Publishing entityChangePublishing()
            default Publishing.NOT_SPECIFIED;


    /**
     * Indicates whether the property should be included or excluded from mementos.
     *
     * <p>
     *     To ensure that the property is actually not persisted in the objectstore, also annotate with the JDO annotation
     *     {@link NotPersistent}
     * </p>
     */
    Snapshot snapshot()
            default Snapshot.NOT_SPECIFIED;



    /**
     * Whether this property is optional or is mandatory (ie required).
     *
     * <p>
     *     For properties the default value, {@link org.apache.isis.applib.annotation.Optionality#DEFAULT}, usually
     *     means that the property is required unless it has been overridden by {@link javax.jdo.annotations.Column}
     *     with its {@link javax.jdo.annotations.Column#allowsNull() allowNulls()} attribute set to true.
     * </p>
     */
    Optionality optionality() default Optionality.DEFAULT;





    /**
     * Regular expression pattern that a value should conform to, and can be formatted as.
     */
    String regexPattern() default "";

    /**
     * Pattern flags, as per {@link java.util.regex.Pattern#compile(String, int)} .
     *
     * <p>
     *     The default value, <code>0</code>, means that no flags have been specified.
     * </p>
     */
    int regexPatternFlags() default 0;

    /**
     * Replacement text for the pattern in generated error message.
     */
    String regexPatternReplacement() default "Doesn't match pattern";





    /**
     * For uploading {@link Blob} or {@link Clob}, optionally restrict the files accepted (eg <tt>.xslx</tt>).
     *
     * <p>
     * The value should be of the form "file_extension|audio/*|video/*|image/*|media_type".
     * </p>
     *
     * @see <a href="http://www.w3schools.com/tags/att_input_accept.asp">http://www.w3schools.com</a>
     */
    @Deprecated
    String fileAccept() default "";

}
