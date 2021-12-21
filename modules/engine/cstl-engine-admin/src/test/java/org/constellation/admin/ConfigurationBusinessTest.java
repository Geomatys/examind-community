package org.constellation.admin;

import java.io.IOException;
import java.nio.file.Files;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.test.SpringContextTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ConfigurationBusinessTest extends SpringContextTest {

    @Autowired
    private IConfigurationBusiness biz;

    @Test
    public void cannotRemoveBadDirectory() throws IOException {
        Files.createDirectories(biz.getDataIntegratedDirectory(null));
        final String[] candidates = { "", null, ".", "..", "./../.", "/" };
        for (String candidate : candidates) {
            try {
                biz.removeDataIntegratedDirectory(candidate);
                Assert.fail("We should not be able to delete a parent folder of any provider. Input provider Id was: "+candidate);
            } catch (IllegalArgumentException e) {
                // That's the expected behavior. Just check that root directory still exists.
                Assert.assertTrue(Files.exists(biz.getDataIntegratedDirectory(null)));
            }
        }
    }
}
