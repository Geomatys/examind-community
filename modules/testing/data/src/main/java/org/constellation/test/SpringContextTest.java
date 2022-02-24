package org.constellation.test;

import org.constellation.test.utils.SpringTestRunner;
import org.constellation.test.utils.TestDirectoryInit;
import org.constellation.test.utils.TestEnvironment;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

/**
 * Parent class to use when creating unit tests that needs a "core" application context: with business components, but
 * without REST controllers.
 *
 * TODO: remove {@link DirtiesContext } usage. For now, it is necessary, because there's a conflict between Spring
 * runner cache, and app contexts created through 'AbstractGrizzlyServer'.
 */
@RunWith(SpringTestRunner.class)
@TestExecutionListeners( {
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        DirtiesContextBeforeModesTestExecutionListener.class
})
@ContextConfiguration(value = "classpath:/cstl/spring/test-context.xml", initializers = { TestDirectoryInit.class })
@DirtiesContext
public class SpringContextTest {

    @Autowired
    protected TestEnvironment.TestResources testResources;
}
