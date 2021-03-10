package org.constellation.admin;

import java.io.IOException;
import java.nio.file.Files;
import org.constellation.business.IConfigurationBusiness;
import org.junit.Assert;
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

    @Test
    public void cannotRemoveBadDirectory() throws IOException {
        Files.createDirectories(biz.getDataIntegratedDirectory(null));
        final String[] candidates = { "", null, ".", "..", "./../.", "/" };
        for (String candidate : candidates) {
            try {
                biz.removeDataIntegratedDirectory(candidate);
                Assert.fail("We should not be able to delete a parent folder of any provider. Input provider Id was: "+candidate);
            } catch (IllegalArgumentException e) {
                System.out.printf("%n%n ERRROR MESSAGE: %n%n %s %n%n", e.getMessage());
                // That's the expected behavior. Just check that root directory still exists.
                Assert.assertTrue(Files.exists(biz.getDataIntegratedDirectory(null)));
            }
        }
    }
}
