/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.api.rest;

/**
 * List of known error or message codes.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class I18nCodes {

    /**
     * Used for internal unexpected server exceptions.
     */
    public static final String API_MSG_SERVER_ERROR = "api.msg.error.serverError";

    /**
     * User and roles message codes.
     */
    public interface User {
        /** When requested user is not available. */
        public static final String NOT_FOUND = "api.msg.error.user.notFound";
        /** When action failed if the specified user is the last administrator. */
        public static final String LAST_ADMIN = "api.msg.error.user.lastAdmin";
    }

    /**
     * Styles message codes.
     */
    public interface Style {
        /** When requested user is not available. */
        public static final String NOT_FOUND = "api.msg.error.style.notFound";
        /** When trying to create a style with an id which already exist. */
        public static final String ALREADY_EXIST = "api.msg.error.style.alreadyExist";

        //INTERNAL
        /** Cannot get interpolation palette because the function of colormap is unrecognized. */
        public static final String NOT_COLORMAP = "api.msg.error.internal.style.notColorMap";
        public static final String RULE_NOT_FOUND = "api.msg.error.internal.style.rule.notFound";
        /** When a parameter is not valid */
        public static final String INVALID_ARGUMENT = "api.msg.error.internal.style.invalidArgument";

    }

}
