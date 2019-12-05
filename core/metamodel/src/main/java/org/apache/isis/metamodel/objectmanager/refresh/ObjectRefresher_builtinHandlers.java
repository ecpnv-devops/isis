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
package org.apache.isis.metamodel.objectmanager.refresh;

import org.apache.isis.commons.internal.exceptions._Exceptions;
import org.apache.isis.metamodel.context.MetaModelContext;
import org.apache.isis.metamodel.facets.object.entity.EntityFacet;
import org.apache.isis.metamodel.spec.ManagedObject;

import lombok.Data;
import lombok.val;

/**
 * 
 * @since 2.0
 *
 */
final class ObjectRefresher_builtinHandlers {

    // -- NULL GUARD
    
    @Data
    public static class GuardAgainstNull implements ObjectRefresher.Handler {
        
        private MetaModelContext metaModelContext;
        
        @Override
        public boolean isHandling(ManagedObject managedObject) {
            
            if(managedObject==null || managedObject.getPojo()==null) {
                return true;
            }
            
            return false;
        }

        @Override
        public Void handle(ManagedObject managedObject) {
            return null; // noop
        }

    }
    
    @Data
    public static class RefreshEntity implements ObjectRefresher.Handler {
        
        @Override
        public boolean isHandling(ManagedObject request) {
            val spec = request.getSpecification();
            return spec.isEntity();
        }

        @Override
        public Void handle(ManagedObject request) {
            
            val spec = request.getSpecification();
            val entityFacet = spec.getFacet(EntityFacet.class);
            if(entityFacet==null) {
                throw _Exceptions.illegalArgument(
                        "ObjectSpecification is missing an EntityFacet: %s", spec);
            }
            
            entityFacet.refresh(request.getPojo());
            
            // we assume that we don't need to inject services again, because this should 
            // already have been done, when the entity object got fetched with the ObjectLoader
            
            return null;
        }
        
    }
    
    @Data
    public static class RefreshOther implements ObjectRefresher.Handler {
        
        @Override
        public boolean isHandling(ManagedObject request) {
            // if no one else feels responsible, we do
            return true;
        }

        @Override
        public Void handle(ManagedObject request) {
            return null; // noop
        }
        
    }
    
    
    
}
