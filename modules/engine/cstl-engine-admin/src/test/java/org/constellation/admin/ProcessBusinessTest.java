/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import org.apache.sis.util.logging.Logging;
import org.constellation.business.IProcessBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.process.Task;
import org.constellation.dto.process.TaskParameter;
import org.constellation.exception.ConstellationException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author guilhem
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/cstl/spring/test-context.xml")
public class ProcessBusinessTest {

    private static final DateFormat TASK_DATE = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    @Autowired
    private IProcessBusiness processBusiness;

    @BeforeClass
    public static void initTestDir() {
        ConfigDirectory.setupTestEnvironement("ProcessBusinessTest");
        final IProcessBusiness dbus = SpringHelper.getBean(IProcessBusiness.class);
        if (dbus != null) {
            dbus.deleteAllTaskParameter();
        }
    }

    @AfterClass
    public static void tearDown() {
        //try {
            final IProcessBusiness dbus = SpringHelper.getBean(IProcessBusiness.class);
            if (dbus != null) {
                dbus.deleteAllTaskParameter();
            }
            ConfigDirectory.shutdownTestEnvironement("ProcessBusinessTest");
        /*} catch (ConstellationException ex) {
            Logging.getLogger("org.constellation.admin").log(Level.SEVERE, null, ex);
        }*/
    }

    @Test
    public void taskParameterExecutionTest() throws Exception {
        TaskParameter tp = new TaskParameter();
        tp.setDate(System.currentTimeMillis());
        tp.setName("add test");
        tp.setProcessAuthority("geotoolkit");
        tp.setProcessCode("math:add");
        tp.setInputs("{\"first\":[1],\"second\":[2]}");
        tp.setOwner(1);
        Integer tpid = processBusiness.addTaskParameter(tp);

        Assert.assertNotNull(tpid);

        // verify that the task parameter is well saved
        List<TaskParameter> results = processBusiness.findTaskParameterByNameAndProcess("add test", "geotoolkit", "math:add");
        Assert.assertEquals(1, results.size());

        tp = results.get(0);

        // test execution
        final String title = tp.getName()+" "+TASK_DATE.format(new Date());
        processBusiness.executeTaskParameter(tp, title, 1);

        // wait for execution
        Thread.sleep(2000);

        List<Task> tasks = processBusiness.listTaskHistory(tpid, 0, 10);
        Assert.assertEquals(1, tasks.size());

        Task task = tasks.get(0);
        Assert.assertEquals("{\"result\":[3.0]}", task.getTaskOutput());
    }

    @Ignore
    public void taskParameterscheduleTest() throws Exception {
        TaskParameter tp = new TaskParameter();
        tp.setDate(System.currentTimeMillis());
        tp.setName("add test schedule");
        tp.setProcessAuthority("geotoolkit");
        tp.setProcessCode("math:add");
        tp.setInputs("{\"first\":[2],\"second\":[2]}");
        tp.setOwner(1);
        tp.setTriggerType("CRON");
        tp.setTrigger("{\"cron\":\"* * * * * ? *\",\"endDate\":" + (System.currentTimeMillis() + 10000) + "}");

        Integer tpid = processBusiness.addTaskParameter(tp);

        Assert.assertNotNull(tpid);

        Assert.assertNotNull(tpid);

        // verify that the task parameter is well saved
        List<TaskParameter> results = processBusiness.findTaskParameterByNameAndProcess("add test", "geotoolkit", "math:add");
        Assert.assertEquals(1, results.size());

        tp = results.get(0);

        // test scheduled execution
        final String title = tp.getName()+" "+TASK_DATE.format(new Date());
        processBusiness.scheduleTaskParameter(tp, title, tpid, true);

        List<Task> tasks = new ArrayList<>();
        while (tasks.size() < 9 ) {
            tasks = processBusiness.listTaskHistory(tpid, 0, 10);
            Thread.sleep(1000);
        }
    }
}
