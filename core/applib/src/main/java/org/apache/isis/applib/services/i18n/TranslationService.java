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
package org.apache.isis.applib.services.i18n;

import org.apache.isis.applib.annotation.Programmatic;

public interface TranslationService {

    TranslationService IDENTITY = new TranslationService() {
        @Override
        public String translate(String context, String text) {
            return text;
        }

        @Override
        public String translate(String context, String singularText, String pluralText, int num) {
            return singularText;
        }

        @Override
        public Mode getMode() {
            return Mode.READ;
        }
    };

    /**
     * Return a translation of the text, in the locale of the &quot;current user&quot;.
     *
     * <p>
     *     The mechanism to determine the locale is implementation-specific.
     * </p>
     *
     * @param context
     * @param text
     * @return
     */
    @Programmatic
    public String translate(final String context, final String text);

    /**
     * Return a translation of either the singular or the plural text, dependent on the <tt>num</tt> parameter,
     * in the locale of the &quot;current user&quot;.
     *
     * <p>
     *     The mechanism to determine the locale is implementation-specific.
     * </p>
     *
     * @param context
     * @param singularText
     * @param pluralText
     * @param num - whether to return the translation of the singular (if =1) or of the plural (if != 1)
     * @return
     */
    @Programmatic
    public String translate(final String context, final String singularText, final String pluralText, int num);


    public enum Mode {
        READ,
        WRITE;

        public boolean isRead() {
            return this == READ;
        }
        public boolean isWrite() {
            return this == WRITE;
        }
    }

    /**
     * Whether this implementation is operating in read or in write mode.
     *
     * <p>
     *     If in read mode, then the translations are expected to be present.  In such cases, the
     *     {@link #translate(String, String) translate}
     *     {@link #translate(String, String, String, int) method}s should be <i>lazily</i> called,
     *     if only because there will (most likely) need to be a session in progress (such that the locale of the
     *     current user can be determined).
     * </p>
     *
     * <p>
     *     If in write mode, then the implementation is saving translation keys, and will
     *     always return the untranslated translation.  In such cases, the {@link #translate(String, String) translate}
     *     {@link #translate(String, String, String, int) method}s should be <i>eagerly</i> called
     *     such that all pathways are exercised..
     * </p>
     */
    @Programmatic
    Mode getMode();
}
