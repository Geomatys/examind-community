package org.constellation.test;

import org.constellation.test.utils.SpringTestRunner;
import org.constellation.test.utils.TestDirectoryInit;
import org.constellation.test.utils.TestEnvironment;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

@RunWith(SpringTestRunner.class)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class})
@ContextConfiguration(value = "classpath:/cstl/spring/test-context.xml", initializers = { TestDirectoryInit.class })
public class SpringContextTest {

    @Autowired
    protected TestEnvironment.TestResources testResources;
}
