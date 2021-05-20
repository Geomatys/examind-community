package org.constellation.admin;

import java.io.IOException;
import java.nio.file.Files;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/cstl/spring/test-context.xml")
public class ConfigurationBusinessTest {

    @Autowired
    private IConfigurationBusiness biz;

    @BeforeClass
    public static void initConfDirectory() {
        ConfigDirectory.setupTestEnvironement(ConfigurationBusinessTest.class.getSimpleName());
    }

    @AfterClass
    public static void deleteConfDirectory() {
        ConfigDirectory.shutdownTestEnvironement(ConfigurationBusinessTest.class.getSimpleName());
    }

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
