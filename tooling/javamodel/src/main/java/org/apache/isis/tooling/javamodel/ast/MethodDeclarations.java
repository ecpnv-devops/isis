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
package org.apache.isis.tooling.javamodel.ast;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import lombok.NonNull;

public final class MethodDeclarations {

    public static boolean isEffectivePublic(
            final @NonNull MethodDeclaration md, final @NonNull ClassOrInterfaceDeclaration td) {
        
        if(td.isInterface()) {
            return true;
        }
       
        //TODO effective public requires more context, eg. is the container an interface 
        return !md.isPrivate() 
                && !md.isAbstract() 
                && !md.isProtected()
                //&& !md.isDefault()
                ;
    }
    
    /**
     * Returns given {@link MethodDeclaration} as normal text, without formatting.
     */
    public static String toNormalizedMethodDeclaration(final @NonNull MethodDeclaration md) {
        return md.getDeclarationAsString(false, false, true).trim();
    }
    
}
