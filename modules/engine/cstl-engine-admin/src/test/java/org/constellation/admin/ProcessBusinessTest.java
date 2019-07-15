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
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.constellation.business.IProcessBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.process.Task;
import org.constellation.dto.process.TaskParameter;
import org.constellation.exception.ConstellationException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
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

    private static final Logger LOGGER = Logging.getLogger("org.constellation.admin");

    @Autowired
    private IProcessBusiness processBusiness;

    @BeforeClass
    public static void initTestDir() {
        ConfigDirectory.setupTestEnvironement("ProcessBusinessTest");
    }

    @AfterClass
    public static void tearDown() {
        try {
            final IProcessBusiness dbus = SpringHelper.getBean(IProcessBusiness.class);
            if (dbus != null) {
                dbus.deleteAllTaskParameter();
            }
            ConfigDirectory.shutdownTestEnvironement("ProcessBusinessTest");
        } catch (ConstellationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void taskParameterExecutionTest() throws Exception {
        processBusiness.deleteAllTaskParameter();

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

    @Test
    public void taskParameterscheduleTest() throws Exception {
        processBusiness.deleteAllTaskParameter();

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

        // verify that the task parameter is well saved
        List<TaskParameter> results = processBusiness.findTaskParameterByNameAndProcess("add test schedule", "geotoolkit", "math:add");
        Assert.assertEquals(1, results.size());

        tp = results.get(0);

        // test scheduled execution
        final String title = tp.getName()+" "+TASK_DATE.format(new Date());
        processBusiness.scheduleTaskParameter(tp, title, 1, true);

        List<Task> tasks = new ArrayList<>();
        int i = 0;
        while (tasks.size() < 9 ) {
            tasks = processBusiness.listTaskHistory(tpid, 0, 10);
            Thread.sleep(1000);
            i++;
            if (i > 20) {
                Assert.fail("multiple scheduled execution every second fail");
            }
        }

        processBusiness.deleteTaskParameter(tpid);
    }

    @Test
    public void taskParameterCancelTest() throws Exception {
        processBusiness.deleteAllTaskParameter();

        TaskParameter tp = new TaskParameter();
        tp.setDate(System.currentTimeMillis());
        tp.setName("progress test");
        tp.setProcessAuthority("test-admin");
        tp.setProcessCode("progress.test");
        tp.setInputs("{}");
        tp.setOwner(1);
        Integer tpid = processBusiness.addTaskParameter(tp);

        Assert.assertNotNull(tpid);

        // verify that the task parameter is well saved
        List<TaskParameter> results = processBusiness.findTaskParameterByNameAndProcess("progress test", "test-admin", "progress.test");
        Assert.assertEquals(1, results.size());

        tp = results.get(0);

        // test execution
        final String title = tp.getName()+" "+TASK_DATE.format(new Date());
        processBusiness.executeTaskParameter(tp, title, 1);

        // wait for execution
        Thread.sleep(1000);

        List<Task> tasks = processBusiness.listRunningTasks(tpid, 0, 10);
        Assert.assertEquals(1, tasks.size());

        Task task = tasks.get(0);

        processBusiness.cancelTask(task.getIdentifier());

        // wait for cancel
        Thread.sleep(2000);

        tasks = processBusiness.listRunningTasks(tpid, 0, 10);
        Assert.assertEquals(0, tasks.size());

        processBusiness.deleteTaskParameter(tpid);

    }

    @Test
    public void taskParameterscheduleCancelTest() throws Exception {
        processBusiness.deleteAllTaskParameter();

        TaskParameter tp = new TaskParameter();
        tp.setDate(System.currentTimeMillis());
        tp.setName("progress test scheduled");
        tp.setProcessAuthority("test-admin");
        tp.setProcessCode("progress.test");
        tp.setInputs("{}");
        tp.setOwner(1);
        tp.setTriggerType("CRON");
        tp.setTrigger("{\"cron\":\"0 * * ? * *\",\"endDate\":" + (System.currentTimeMillis() + 240000) + "}");

        Integer tpid = processBusiness.addTaskParameter(tp);

        Assert.assertNotNull(tpid);

        Assert.assertNotNull(tpid);

        // verify that the task parameter is well saved
        List<TaskParameter> results = processBusiness.findTaskParameterByNameAndProcess("progress test scheduled", "test-admin", "progress.test");
        Assert.assertEquals(1, results.size());

        tp = results.get(0);

        // test scheduled execution every minute
        final String title = tp.getName()+" "+TASK_DATE.format(new Date());
        processBusiness.scheduleTaskParameter(tp, title, 1, true);

        LOGGER.info("waiting for scheduled execution to start (maximum one minute)");
        // wait for execution to start at second 00 of the next minute
        List<Task> tasks = new ArrayList<>();
        boolean isRunning = false;
        int i = 0;
        while (!isRunning) {
            Thread.sleep(1000);
            tasks = processBusiness.listRunningTasks(tpid, 0, 10);
            isRunning = tasks.size() > 0;
            i++;
            if (i > 60) {
                Assert.fail("multiple scheduled execution every minute fail");
            }
        }

        // cancel the task
        Task task = tasks.get(0);

        processBusiness.cancelTask(task.getIdentifier());

        // wait for cancel
        Thread.sleep(2000);

        tasks =  processBusiness.listTaskHistory(tpid, 0, 10);
        Assert.assertEquals(1, tasks.size());

        task = tasks.get(0);

        Assert.assertEquals("FAILED", task.getState());


        // verify that the process is still scheduler, and will still be launched
        LOGGER.info("waiting for next scheduled execution to start  (maximum one minute)");
        isRunning = false;
        i = 0;
        while (!isRunning) {
            Thread.sleep(1000);
            tasks = processBusiness.listRunningTasks(tpid, 0, 10);
            isRunning = tasks.size() > 0;
            i++;
            if (i > 60) {
                Assert.fail("multiple scheduled execution every minute fail");
            }
        }
        processBusiness.deleteTaskParameter(tpid);
    }

    @Test
    public void taskParametersUncheduleTest() throws Exception {
        processBusiness.deleteAllTaskParameter();

        TaskParameter tp = new TaskParameter();
        tp.setDate(System.currentTimeMillis());
        tp.setName("add test schedule remove");
        tp.setProcessAuthority("geotoolkit");
        tp.setProcessCode("math:add");
        tp.setInputs("{\"first\":[2],\"second\":[2]}");
        tp.setOwner(1);
        tp.setTriggerType("CRON");
        tp.setTrigger("{\"cron\":\"* * * * * ? *\",\"endDate\":" + (System.currentTimeMillis() + 30000) + "}");

        Integer tpid = processBusiness.addTaskParameter(tp);

        Assert.assertNotNull(tpid);

        // verify that the task parameter is well saved
        List<TaskParameter> results = processBusiness.findTaskParameterByNameAndProcess("add test schedule remove", "geotoolkit", "math:add");
        Assert.assertEquals(1, results.size());

        tp = results.get(0);

        // test scheduled execution
        final String title = tp.getName()+" "+TASK_DATE.format(new Date());
        processBusiness.scheduleTaskParameter(tp, title, 1, true);

        List<Task> tasks = new ArrayList<>();
        int i = 0;
        while (tasks.size() < 9 ) {
            tasks = processBusiness.listTaskHistory(tpid, 0, 30);
            Thread.sleep(1000);
            i++;
            if (i > 20) {
                Assert.fail("multiple scheduled execution every second fail");
            }
        }

        // unschedule job
        processBusiness.stopScheduleTaskParameter(tp.getId());

        // wait for unscheduling
        Thread.sleep(1000);

        // try to assert that there is no more execution
        tasks = processBusiness.listTaskHistory(tpid, 0, 30);
        Thread.sleep(1000);
        List<Task> tasks2 = processBusiness.listTaskHistory(tpid, 0, 30);

        Assert.assertEquals(tasks.size(), tasks2.size());

        processBusiness.deleteTaskParameter(tpid);
    }

    @Test
    public void taskParametersRemoveScheduledTest() throws Exception {
        processBusiness.deleteAllTaskParameter();

        TaskParameter tp = new TaskParameter();
        tp.setDate(System.currentTimeMillis());
        tp.setName("add test schedule remove");
        tp.setProcessAuthority("geotoolkit");
        tp.setProcessCode("math:add");
        tp.setInputs("{\"first\":[2],\"second\":[2]}");
        tp.setOwner(1);
        tp.setTriggerType("CRON");
        tp.setTrigger("{\"cron\":\"* * * * * ? *\",\"endDate\":" + (System.currentTimeMillis() + 30000) + "}");

        Integer tpid = processBusiness.addTaskParameter(tp);

        Assert.assertNotNull(tpid);

        // verify that the task parameter is well saved
        List<TaskParameter> results = processBusiness.findTaskParameterByNameAndProcess("add test schedule remove", "geotoolkit", "math:add");
        Assert.assertEquals(1, results.size());

        tp = results.get(0);

        // test scheduled execution
        final String title = tp.getName()+" "+TASK_DATE.format(new Date());
        processBusiness.scheduleTaskParameter(tp, title, 1, true);

        List<Task> tasks = new ArrayList<>();
        int i = 0;
        while (tasks.size() < 9 ) {
            tasks = processBusiness.listTaskHistory(tpid, 0, 30);
            Thread.sleep(1000);
            i++;
            if (i > 20) {
                Assert.fail("multiple scheduled execution every second fail");
            }
        }

        // remove task parameter
        processBusiness.deleteTaskParameter(tpid);

        // wait for unscheduling
        Thread.sleep(1000);

        // try to assert that there is no more execution
        tasks = processBusiness.listTaskHistory(tpid, 0, 30);
        Thread.sleep(1000);
        List<Task> tasks2 = processBusiness.listTaskHistory(tpid, 0, 30);

        Assert.assertEquals(tasks.size(), tasks2.size());
    }
}
