package org.constellation.provider.datastore;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.storage.ConcurrentReadException;
import org.apache.sis.storage.ConcurrentWriteException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.util.collection.BackingStoreException;
import org.geotoolkit.storage.memory.InMemoryFeatureSet;
import org.geotoolkit.storage.memory.InMemoryStore;
import org.junit.Assume;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SafeAccessTest {

    /**
     * When creating multiple read sessions, ensure that they can be temporarily promoted with write permission in order
     * to load handle properly.
     */
    @Test
    public void promotion_to_exclusive_lock_works() throws Exception {
        final FeatureTypeBuilder builder = new FeatureTypeBuilder().setName("Test");
        builder.addAttribute(String.class).setName("text");
        final FeatureType type = builder.build();
        final Feature f1 = type.newInstance();
        f1.setPropertyValue("text", "t1");
        final Feature f2 = type.newInstance();
        f2.setPropertyValue("text", "t2");
        final SafeAccess access = new SafeAccess("Test", () -> {
            final InMemoryStore store = new InMemoryStore();
            try {
                store.add(new InMemoryFeatureSet(type, Arrays.asList(f1, f2)));
            } catch (DataStoreException e) {
                throw new BackingStoreException(e);
            }
            return store;
        });

        final int nbThreads = 8;
        final ExecutorService service = Executors.newFixedThreadPool(nbThreads);
        try {

            final Future<DataStoreHandle> handle1 = service.submit(() -> {
                try (SafeAccess.Session r = access.read()) {
                    // Do not do that in real life. We only do it in test to check instance synchronisation
                    return r.handle();
                }
            });

            final Future<DataStoreHandle> handle2 = service.submit(() -> {
                try (SafeAccess.Session r = access.read()) {
                    // Do not do that in real life. We only do it in test to check instance synchronisation
                    return r.handle();
                }
            });

            assertTrue("Fetched dataStore handles should be the same instance", handle1.get() == handle2.get());

            // Repeat, but with more tasks, and force them starting approximately at the same time
            final Object trigger = new Object();
            final AtomicInteger launchedTasks = new AtomicInteger(0);
            final List<Future<DataStoreHandle>> tasks = IntStream.range(0, nbThreads)
                    .mapToObj(i -> service.submit(() -> {
                        try (SafeAccess.Session r = access.read()) {
                            synchronized (trigger) {
                                launchedTasks.incrementAndGet();
                                trigger.wait();
                            }
                            return r.handle();
                        }
                    }))
                    .collect(Collectors.toList());

            synchronized (trigger) {
                // First, wait for all tasks to start and park
                int trials = 0;
                while (trials++ < 10 && launchedTasks.get() < nbThreads) {
                    trigger.wait(2);
                }
                Assume.assumeTrue(launchedTasks.get() == nbThreads);
                // Then, trigger restart to compete for handle acquisition
                trigger.notifyAll();
            };

            service.shutdown();
            // Ensure no deadlock is going on
            service.awaitTermination(2, TimeUnit.SECONDS);

            final DataStoreHandle handle = tasks.get(0).get();
            for (Future<DataStoreHandle> task : tasks) assertTrue("All handles should refer to the same instance", handle == task.get());

            // Ensure content has not been altered
            FeatureSet fs = (FeatureSet) handle.store.findResource("Test");
            assertEquals(type, fs.getType());
            try (Stream<Feature> features = fs.features(false)) {
                final Set<Feature> set = features.collect(Collectors.toSet());
                assertEquals(2, set.size());
                assertTrue(set.contains(f1));
                assertTrue(set.contains(f2));
            }
        } finally {
            service.shutdownNow();
        }
    }

    /**
     * When a write session is active, only it can be used. No other session can be used.
     */
    @Test
    public void ensure_write_sessions_are_exclusive() throws Exception {
        final SafeAccess access = new SafeAccess("Test", InMemoryStore::new, 10, TimeUnit.MILLISECONDS);
        final ExecutorService service = Executors.newFixedThreadPool(1);

        final Object deadlock = new Object();
        final AtomicBoolean writeLockIsReady = new AtomicBoolean(false);
        service.submit(() -> {
            // DO NOT call w.handle(), because it would break the entire purpose of this test.
            try(SafeAccess.Session w = access.write()) {
                synchronized (deadlock) {
                    writeLockIsReady.set(true);
                    deadlock.wait();
                }
            }
            return null;
        });

        synchronized (deadlock) {
            int trials = 0;
            while (!writeLockIsReady.get() && trials++ < 10) {
                deadlock.wait(2);
            }
        }

        Assume.assumeTrue(writeLockIsReady.get());

        try (SafeAccess.Session r = access.read()) {

        } catch (RuntimeException e) {
            // Expected behavior: cannot acquire read session when a write one is already on-going
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof ConcurrentWriteException);
            // Closing should be no-op, not give errors.
            assertTrue(e.getSuppressed().length == 0);
        }

        try (SafeAccess.Session w2 = access.write()) {

        } catch (RuntimeException e) {
            // Expected behavior: cannot acquire another write session when a write one is already on-going
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof ConcurrentReadException);
            // Closing should be no-op, not give errors.
            assertTrue(e.getSuppressed().length == 0);
        }

        synchronized (deadlock) { deadlock.notifyAll(); }

        service.shutdown();
        if (!service.awaitTermination(50, TimeUnit.MILLISECONDS)) {
            fail("A session failed to free itself");
        }
    }

    /**
     * Ensure that while read sessions are active, no write operation can occur, neither by promoting read session nor
     * by creating a new write session.
     */
    @Test
    public void cannot_acquire_write_session_when_a_read_session_is_active() throws Throwable {
        // Initially, test phases require two participants: test thread and thread of the session to keep alive.
        final Phaser workflowSync = new Phaser(2);
        final SafeAccess access = new SafeAccess("Test", InMemoryStore::new, 10, TimeUnit.MILLISECONDS);
        final ExecutorService service = Executors.newFixedThreadPool(2);

        service.submit(() -> {
            try (SafeAccess.Session r = access.read()) {
                workflowSync.awaitAdvanceInterruptibly(workflowSync.arrive(), 1, TimeUnit.SECONDS);
                workflowSync.awaitAdvanceInterruptibly(workflowSync.arrive(), 1, TimeUnit.SECONDS);
                return null;
            }
        });

        // Phase 1 over: Read session initialized
        workflowSync.awaitAdvanceInterruptibly(workflowSync.arrive(), 1, TimeUnit.SECONDS);

        // Try creating a write session should fail
        workflowSync.register();
        final Future writeAcquisition = service.submit(() -> {
            try (SafeAccess.Session w = access.write()) {} finally { workflowSync.arrive(); }
        });

        workflowSync.register();
        final Future readPromotion = service.submit(() -> {
            try (SafeAccess.Session r = access.read()) {
                return r.handle(); // should try to acquire exclusive lock, but fail to do it.
            } finally {
                workflowSync.arrive();
            }
        });

        // Terminate phase 2: all tests passed
        workflowSync.arrive();

        service.shutdown();
        if (!service.awaitTermination(1, TimeUnit.SECONDS)) {
            fail("A session failed to free itself");
        }

        try {
            writeAcquisition.get();
            fail("Exclusive locking should have failed");
        } catch (ExecutionException e) {
            // Expected behavior: cannot acquire another write session when a write one is already on-going
            assertNotNull(e.getCause());
            assertTrue(e.getCause().getCause() instanceof ConcurrentReadException);
            // Closing should be no-op, not give errors.
            assertTrue(e.getSuppressed().length == 0);
        }

        try {
            readPromotion.get();
            fail("Exclusive locking should have failed");
        } catch (ExecutionException e) {
            // Expected behavior: cannot acquire another write session when a write one is already on-going
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof ConcurrentReadException);
            // Closing should be no-op, not give errors.
            assertTrue(e.getSuppressed().length == 0);
        }
    }
}
