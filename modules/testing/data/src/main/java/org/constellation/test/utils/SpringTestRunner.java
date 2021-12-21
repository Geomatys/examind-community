/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.test.utils;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SpringTestRunner extends SpringJUnit4ClassRunner {

    public SpringTestRunner(final Class klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected List<FrameworkMethod> computeTestMethods() {
        return copySortByOrder(super.computeTestMethods());
    }

    static List<FrameworkMethod> copySortByOrder(final List<FrameworkMethod> testMethods) {
        final ArrayList<FrameworkMethod> defCopy = new ArrayList<>(testMethods);
        Collections.sort(defCopy, Comparator.comparing(SpringTestRunner::orderOrHighCardinality));
        return defCopy;
    }

    private static int orderOrHighCardinality(FrameworkMethod method) {
        final Order methodOrder = method.getAnnotation(Order.class);
        if (methodOrder == null) return Integer.MAX_VALUE;
        else return methodOrder.order();
    }
}

