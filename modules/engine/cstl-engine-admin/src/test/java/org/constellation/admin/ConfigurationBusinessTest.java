/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2023 Geomatys.
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
package org.constellation.admin;

import java.io.IOException;
import java.nio.file.Files;
import org.junit.Assert;
import org.junit.Test;

public class ConfigurationBusinessTest extends AbstractBusinessTest {

    @Test
    public void cannotRemoveBadDirectory() throws IOException {
        Files.createDirectories(configBusiness.getDataIntegratedDirectory(null, true));
        final String[] candidates = { "", null, ".", "..", "./../.", "/" };
        for (String candidate : candidates) {
            try {
                configBusiness.removeDataIntegratedDirectory(candidate);
                Assert.fail("We should not be able to delete a parent folder of any provider. Input provider Id was: "+candidate);
            } catch (IllegalArgumentException e) {
                // That's the expected behavior. Just check that root directory still exists.
                Assert.assertTrue(Files.exists(configBusiness.getDataIntegratedDirectory(null, false)));
            }
        }
    }
}
