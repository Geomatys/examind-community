/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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

package org.constellation.dto.thesaurus;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public final class SearchMode {

    public static final int NO_WILD_CHAR        = 0;

    public static final int SUFFIX_REGEX        = 1;

    public static final int PREFIX_REGEX        = 2;

    public static final int PREFIX_SUFFIX_REGEX = 3;

    public static final int AUTO_SEARCH         = 4;

    private SearchMode() {}
}
