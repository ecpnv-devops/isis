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
package org.apache.isis.core.runtimeservices.wrapper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import org.apache.isis.applib.annotation.OrderPrecedence;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.applib.services.bookmark.BookmarkService;
import org.apache.isis.applib.services.command.CommandExecutorService;
import org.apache.isis.applib.services.factory.FactoryService;
import org.apache.isis.applib.services.metamodel.MetaModelService;
import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.applib.services.wrapper.WrapperFactory;
import org.apache.isis.applib.services.wrapper.WrappingObject;
import org.apache.isis.applib.services.wrapper.control.AsyncControl;
import org.apache.isis.applib.services.wrapper.control.AsyncControlService;
import org.apache.isis.applib.services.wrapper.control.ExecutionMode;
import org.apache.isis.applib.services.wrapper.control.SyncControl;
import org.apache.isis.applib.services.wrapper.events.ActionArgumentEvent;
import org.apache.isis.applib.services.wrapper.events.ActionInvocationEvent;
import org.apache.isis.applib.services.wrapper.events.ActionUsabilityEvent;
import org.apache.isis.applib.services.wrapper.events.ActionVisibilityEvent;
import org.apache.isis.applib.services.wrapper.events.CollectionAccessEvent;
import org.apache.isis.applib.services.wrapper.events.CollectionAddToEvent;
import org.apache.isis.applib.services.wrapper.events.CollectionMethodEvent;
import org.apache.isis.applib.services.wrapper.events.CollectionRemoveFromEvent;
import org.apache.isis.applib.services.wrapper.events.CollectionUsabilityEvent;
import org.apache.isis.applib.services.wrapper.events.CollectionVisibilityEvent;
import org.apache.isis.applib.services.wrapper.events.InteractionEvent;
import org.apache.isis.applib.services.wrapper.events.ObjectTitleEvent;
import org.apache.isis.applib.services.wrapper.events.ObjectValidityEvent;
import org.apache.isis.applib.services.wrapper.events.PropertyAccessEvent;
import org.apache.isis.applib.services.wrapper.events.PropertyModifyEvent;
import org.apache.isis.applib.services.wrapper.events.PropertyUsabilityEvent;
import org.apache.isis.applib.services.wrapper.events.PropertyVisibilityEvent;
import org.apache.isis.applib.services.wrapper.listeners.InteractionListener;
import org.apache.isis.applib.services.xactn.TransactionService;
import org.apache.isis.core.commons.collections.ImmutableEnumSet;
import org.apache.isis.core.commons.internal.base._Casts;
import org.apache.isis.core.commons.internal.exceptions._Exceptions;
import org.apache.isis.core.commons.internal.plugins.codegen.ProxyFactoryService;
import org.apache.isis.core.metamodel.context.MetaModelContext;
import org.apache.isis.core.metamodel.facets.actions.action.invocation.CommandUtil;
import org.apache.isis.core.metamodel.objectmanager.ObjectManager;
import org.apache.isis.core.metamodel.services.command.CommandDtoServiceInternal;
import org.apache.isis.core.metamodel.spec.ManagedObject;
import org.apache.isis.core.metamodel.spec.feature.Contributed;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.spec.feature.ObjectMember;
import org.apache.isis.core.metamodel.spec.feature.OneToOneAssociation;
import org.apache.isis.core.metamodel.specloader.SpecificationLoader;
import org.apache.isis.core.metamodel.specloader.specimpl.ObjectActionMixedIn;
import org.apache.isis.core.metamodel.specloader.specimpl.dflt.ObjectSpecificationDefault;
import org.apache.isis.core.runtime.session.IsisSession;
import org.apache.isis.core.runtime.session.IsisSessionFactory;
import org.apache.isis.core.runtime.session.IsisSessionTracker;
import org.apache.isis.core.runtimeservices.wrapper.dispatchers.InteractionEventDispatcher;
import org.apache.isis.core.runtimeservices.wrapper.dispatchers.InteractionEventDispatcherTypeSafe;
import org.apache.isis.core.runtimeservices.wrapper.handlers.DomainObjectInvocationHandler;
import org.apache.isis.core.runtimeservices.wrapper.handlers.ProxyContextHandler;
import org.apache.isis.core.runtimeservices.wrapper.proxy.ProxyCreator;
import org.apache.isis.core.security.authentication.AuthenticationSession;
import org.apache.isis.core.security.authentication.standard.SimpleSession;
import org.apache.isis.schema.cmd.v2.CommandDto;

import static org.apache.isis.applib.services.metamodel.MetaModelService.Mode.*;
import static org.apache.isis.applib.services.wrapper.control.SyncControl.control;

import lombok.Data;
import lombok.val;
import lombok.extern.log4j.Log4j2;

/**
 * This service provides the ability to 'wrap' a domain object such that it can
 * be interacted with, while enforcing the hide/disable/validate rules as implied by
 * the Isis programming model.
 */
@Service
@Named("isisRuntimeServices.WrapperFactoryDefault")
@Order(OrderPrecedence.MIDPOINT)
@Primary
@Qualifier("Default")
@Log4j2
public class WrapperFactoryDefault implements WrapperFactory {
    
    @Inject private FactoryService factoryService;
    @Inject private MetaModelContext metaModelContext;
    @Inject private SpecificationLoader specificationLoader;
    @Inject private IsisSessionTracker isisSessionTracker;
    @Inject private IsisSessionFactory isisSessionFactory;
    @Inject private TransactionService transactionService;
    @Inject private CommandExecutorService commandExecutorService;
    @Inject protected ProxyFactoryService proxyFactoryService; // protected to allow JUnit test
    @Inject private CommandDtoServiceInternal commandDtoServiceInternal;
    @Inject private AsyncControlService asyncControlService;
    @Inject private BookmarkService bookmarkService;

    private final List<InteractionListener> listeners = new ArrayList<>();
    private final Map<Class<? extends InteractionEvent>, InteractionEventDispatcher>
        dispatchersByEventClass = new HashMap<>();
    private ProxyContextHandler proxyContextHandler;
    
    @PostConstruct
    public void init() {

        val proxyCreator = new ProxyCreator(proxyFactoryService);
        proxyContextHandler = new ProxyContextHandler(proxyCreator);
        
        putDispatcher(ObjectTitleEvent.class, InteractionListener::objectTitleRead);
        putDispatcher(PropertyVisibilityEvent.class, InteractionListener::propertyVisible);
        putDispatcher(PropertyUsabilityEvent.class, InteractionListener::propertyUsable);
        putDispatcher(PropertyAccessEvent.class, InteractionListener::propertyAccessed);
        putDispatcher(PropertyModifyEvent.class, InteractionListener::propertyModified);
        putDispatcher(CollectionVisibilityEvent.class, InteractionListener::collectionVisible);
        putDispatcher(CollectionUsabilityEvent.class, InteractionListener::collectionUsable);
        putDispatcher(CollectionAccessEvent.class, InteractionListener::collectionAccessed);
        putDispatcher(CollectionAddToEvent.class, InteractionListener::collectionAddedTo);
        putDispatcher(CollectionRemoveFromEvent.class, InteractionListener::collectionRemovedFrom);
        putDispatcher(ActionVisibilityEvent.class, InteractionListener::actionVisible);
        putDispatcher(ActionUsabilityEvent.class, InteractionListener::actionUsable);
        putDispatcher(ActionArgumentEvent.class, InteractionListener::actionArgument);
        putDispatcher(ActionInvocationEvent.class, InteractionListener::actionInvoked);
        putDispatcher(ObjectValidityEvent.class, InteractionListener::objectPersisted);
        putDispatcher(CollectionMethodEvent.class, InteractionListener::collectionMethodInvoked);
    }

    // -- WRAPPING
    
    @Override
    public <T> T wrap(T domainObject) {
        return wrap(domainObject, control());
    }

    @Override
    public <T> T wrap(
            final T domainObject,
            final SyncControl syncControl) {
        final ImmutableEnumSet<ExecutionMode> modes = syncControl.getExecutionModes();
        if (domainObject instanceof WrappingObject) {
            val wrapperObject = (WrappingObject) domainObject;
            val executionMode = wrapperObject.__isis_executionModes();
            if(! equivalent(executionMode, modes)) {
                val underlyingDomainObject = wrapperObject.__isis_wrapped();
                return _Casts.uncheckedCast(createProxy(underlyingDomainObject, syncControl));
            }
            return domainObject;
        }
        return createProxy(domainObject, syncControl);
    }

    private static boolean equivalent(ImmutableEnumSet<ExecutionMode> first, ImmutableEnumSet<ExecutionMode> second) {
        return equivalent(first.toEnumSet(), second.toEnumSet());
    }

    private static boolean equivalent(EnumSet<ExecutionMode> first, EnumSet<ExecutionMode> second) {
        return first.containsAll(second) && second.containsAll(first);
    }

    @Override
    public <T> T wrapMixin(Class<T> mixinClass, Object mixedIn) {
        return wrapMixin(mixinClass, mixedIn, control());
    }

    @Override
    public <T> T wrapMixin(Class<T> mixinClass, Object mixedIn, SyncControl syncControl) {
        T mixin = factoryService.mixin(mixinClass, mixedIn);
        return wrap(mixin, syncControl);
    }

    protected <T> T createProxy(T domainObject, SyncControl syncControl) {
        return proxyContextHandler.proxy(metaModelContext, domainObject, syncControl);
    }

    @Override
    public boolean isWrapper(Object possibleWrappedDomainObject) {
        return possibleWrappedDomainObject instanceof WrappingObject;
    }

    @Override
    public <T> T unwrap(T possibleWrappedDomainObject) {
        if(isWrapper(possibleWrappedDomainObject)) {
            val wrappingObject = (WrappingObject) possibleWrappedDomainObject;
            return _Casts.uncheckedCast(wrappingObject.__isis_wrapped());
        }
        return possibleWrappedDomainObject;
    }


    // -- ASYNC WRAPPING


    @Override
    public <T,R> T async(
            final T domainObject,
            final AsyncControl<R> asyncControl) {

        val proxyFactory = proxyFactoryService.factory((Class<T>) domainObject.getClass(), WrappingObject.class);

        return proxyFactory.createInstance(new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                if (isInheritedFromJavaLangObject(method)) {
                    return method.invoke(domainObject, args);
                }

                if (asyncControlService.shouldCheckRules(asyncControl)) {
                    val doih = new DomainObjectInvocationHandler<>(
                            metaModelContext, domainObject, control().withNoExecute(), null);
                    doih.invoke(null, method, args);
                }

                val memberAndTarget = forRegular(method, domainObject);
                if( ! memberAndTarget.isMemberFound()) {
                    return method.invoke(domainObject, args);
                }

                return submitAsync(memberAndTarget, args, asyncControl);
            }
        }, false);
    }

    @Override
    public <T, R> T asyncMixin(Class<T> mixinClass, Object mixedIn, AsyncControl<R> asyncControl) {

        T mixin = factoryService.mixin(mixinClass, mixedIn);
        val proxyFactory = proxyFactoryService.factory(mixinClass, new Class[]{WrappingObject.class}, new Class[]{mixedIn.getClass()});

        return proxyFactory.createInstance(new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                final boolean inheritedFromObject = isInheritedFromJavaLangObject(method);
                if (inheritedFromObject) {
                    return method.invoke(mixin, args);
                }

                if (asyncControlService.shouldCheckRules(asyncControl)) {
                    val doih = new DomainObjectInvocationHandler<>(
                            metaModelContext, mixin, control().withNoExecute(), null);
                    doih.invoke(null, method, args);
                }

                val actionAndTarget = forMixin(method, mixedIn);
                if (! actionAndTarget.isMemberFound()) {
                    return method.invoke(mixin, args);
                }

                return submitAsync(actionAndTarget, args, asyncControl);
            }
        }, new Object[]{ mixedIn });
    }

    private boolean isInheritedFromJavaLangObject(final Method method) {
        return method.getDeclaringClass().equals(Object.class);
    }

    private <R> Object submitAsync(
            final MemberAndTarget memberAndTarget,
            final Object[] args,
            final AsyncControl<R> asyncControl) {

        val executorService = asyncControl.getExecutorService();
        val isisSession = currentIsisSession();
        val asyncAuthSession = authSessionFrom(asyncControl, isisSession.getAuthenticationSession());

        val targetAdapter = memberAndTarget.getTarget();
        val method = memberAndTarget.getMethod();

        val argAdapters = Arrays.asList(WrapperFactoryDefault.this.adaptersFor(args));
        val targetList = Collections.singletonList(targetAdapter);

        CommandDto commandDto;
        switch (memberAndTarget.getType()) {
            case ACTION:
                val action = memberAndTarget.getAction();
                commandDto = commandDtoServiceInternal.asCommandDto(targetList, action, argAdapters);
                break;
            case PROPERTY:
                val property = memberAndTarget.getProperty();
                commandDto = commandDtoServiceInternal.asCommandDto(targetList, property, argAdapters.get(0));
                break;
            default:
                // shouldn't happen, already catered for this case previously
                return null;
        }
        val oidDto = commandDto.getTargets().getOid().get(0);

        asyncControlService.init(asyncControl, method, Bookmark.from(oidDto));

        Future future = executorService.submit(() ->
                isisSessionFactory.callAuthenticated(asyncAuthSession, () ->
                    transactionService.executeWithinTransaction(() -> {
                        Bookmark bookmark = commandExecutorService.executeCommand(commandDto);
                        if (bookmark != null) {
                            Object entity = bookmarkService.lookup(bookmark);
                            val metaModelService = WrapperFactoryDefault.this.getMetaModelService();
                            if (metaModelService.sortOf(bookmark, RELAXED).isEntity()) {
                                entity = WrapperFactoryDefault.this.getRepositoryService().detach(entity);
                            }
                            return entity;
                        }
                        return null;
                    })
        ));

        asyncControlService.update(asyncControl, future);

        return null;
    }

    private RepositoryService getRepositoryService() {
        return metaModelContext.getRepositoryService();
    }

    private MetaModelService getMetaModelService() {
        return metaModelContext.getServiceRegistry().lookupServiceElseFail(MetaModelService.class);
    }

    private <T> MemberAndTarget forRegular(Method method, T domainObject) {

        val targetObjSpec = (ObjectSpecificationDefault) specificationLoader.loadSpecification(method.getDeclaringClass());
        val objectMember = targetObjSpec.getMember(method);
        if(objectMember == null) {
            return MemberAndTarget.notFound();
        }

        val targetAdapter = currentObjectManager().adapt(domainObject);
        if (objectMember instanceof OneToOneAssociation) {
            return MemberAndTarget.foundProperty((OneToOneAssociation) objectMember, targetAdapter, method);
        }
        if (objectMember instanceof ObjectAction) {
            return MemberAndTarget.foundAction((ObjectAction) objectMember, targetAdapter, method);
        }

        throw new UnsupportedOperationException(
                "Only properties and actions can be executed in the background "
                        + "(method " + method.getName() + " represents a " + objectMember.getFeatureType().name() + "')");
    }

    private <T> MemberAndTarget forMixin(Method method, T mixedIn) {

        final ObjectSpecificationDefault mixinSpec = (ObjectSpecificationDefault) specificationLoader.loadSpecification(method.getDeclaringClass());
        final ObjectMember mixinMember = mixinSpec.getMember(method);
        if (mixinMember == null) {
            return MemberAndTarget.notFound();
        }

        // find corresponding action of the mixedIn (this is the 'real' target).
        val mixedInClass = mixedIn.getClass();
        val mixedInSpec = (ObjectSpecificationDefault) specificationLoader.loadSpecification(mixedInClass);

        // don't care about anything other than actions
        // (contributed properties and collections are read-only).
        Optional<ObjectActionMixedIn> targetActionIfAny = mixedInSpec.streamObjectActions(Contributed.INCLUDED)
                .filter(ObjectActionMixedIn.class::isInstance)
                .map(ObjectActionMixedIn.class::cast)
                .filter(x -> x.hasMixinAction((ObjectAction) mixinMember))
                .findFirst();

        if(!targetActionIfAny.isPresent()) {
            throw new UnsupportedOperationException(String.format(
                    "Could not locate objectAction delegating to mixinAction id='%s' on mixedIn class '%s'",
                    mixinMember.getId(), mixedInClass.getName()));
        }

        return MemberAndTarget.foundAction(targetActionIfAny.get(), currentObjectManager().adapt(mixedIn), method);
    }

    private static <R> SimpleSession authSessionFrom(AsyncControl<R> asyncControl, AuthenticationSession authSession) {
        val user = asyncControl.getUser();
        val roles = asyncControl.getRoles();
        return new SimpleSession(user != null ? user : authSession.getUserName(), roles != null ? roles : authSession.getRoles());
    }

    @Data
    static class MemberAndTarget {
        static MemberAndTarget notFound() {
            return new MemberAndTarget(Type.NONE, null, null, null, null);
        }
        static MemberAndTarget foundAction(ObjectAction action, ManagedObject target, final Method method) {
            return new MemberAndTarget(Type.ACTION, action, null, target, method);
        }
        static MemberAndTarget foundProperty(OneToOneAssociation property, ManagedObject target, final Method method) {
            return new MemberAndTarget(Type.PROPERTY, null, property, target, method);
        }

        public boolean isMemberFound() {
            return type != Type.NONE;
        }

        enum Type {
            ACTION,
            PROPERTY,
            NONE
        }
        private final Type type;
        /**
         * Populated if and only if {@link #type} is {@link Type#ACTION}.
         */
        private final ObjectAction action;
        /**
         * Populated if and only if {@link #type} is {@link Type#PROPERTY}.
         */
        private final OneToOneAssociation property;
        private final ManagedObject target;
        private final Method method;
    }

    private ManagedObject[] adaptersFor(final Object[] args) {
        final ObjectManager objectManager = currentObjectManager();
        return CommandUtil.adaptersFor(args, objectManager);
    }



    // -- LISTENERS

    @Override
    public List<InteractionListener> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    @Override
    public boolean addInteractionListener(InteractionListener listener) {
        return listeners.add(listener);
    }

    @Override
    public boolean removeInteractionListener(InteractionListener listener) {
        return listeners.remove(listener);
    }

    @Override
    public void notifyListeners(InteractionEvent interactionEvent) {
        val dispatcher = dispatchersByEventClass.get(interactionEvent.getClass());
        if (dispatcher == null) {
            val msg = String.format("Unknown InteractionEvent %s - "
                    + "needs registering into dispatchers map", interactionEvent.getClass());
            throw _Exceptions.unrecoverable(msg);
        }
        dispatcher.dispatch(interactionEvent);
    }
    
    // -- HELPER - SETUP
    
    private <T extends InteractionEvent> void putDispatcher(
            Class<T> type, BiConsumer<InteractionListener, T> onDispatch) {
    
        val dispatcher = new InteractionEventDispatcherTypeSafe<T>() {
            @Override
            public void dispatchTypeSafe(T interactionEvent) {
                for (InteractionListener l : listeners) {
                    onDispatch.accept(l, interactionEvent);
                }
            }
        };
        
        dispatchersByEventClass.put(type, dispatcher);
    }


    private IsisSession currentIsisSession() {
        return isisSessionTracker.currentSession().orElseThrow(() -> new RuntimeException("No IsisSession is open"));
    }

    private ObjectManager currentObjectManager() {
        return currentIsisSession().getObjectManager();
    }

}
