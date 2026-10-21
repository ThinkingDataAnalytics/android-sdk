/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.core.receiver;

import android.net.Network;
import android.net.NetworkCapabilities;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for {@link TDNetWorkObservable} concurrent modification safety.
 *
 * <p>Historically the observer list was a plain {@link java.util.ArrayList}: notifying on the
 * network callback thread while another thread called {@code addNetWorkObserver} /
 * {@code removeNetWorkObserver} (or an observer mutated the list from inside its callback)
 * threw {@link java.util.ConcurrentModificationException}.
 */
public class TDNetWorkObservableTest {

    private static final int TYPE_ON_CHANGE = 4;

    private TDNetWorkObservable observable;

    @Before
    public void setUp() throws Exception {
        // reset the singleton so each test gets an isolated observable
        Field instance = TDNetWorkObservable.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        // null context skips receiver registration, safe on JVM unit tests
        observable = TDNetWorkObservable.getInstance(null);
    }

    private void notifyOnChange() throws Exception {
        Method m = TDNetWorkObservable.class.getDeclaredMethod(
                "notifyObservers", int.class, Network.class, NetworkCapabilities.class);
        m.setAccessible(true);
        m.invoke(observable, TYPE_ON_CHANGE, null, null);
    }

    private TDNetWorkObserver countingObserver(final AtomicInteger calls) {
        return new TDNetWorkObserver() {
            @Override
            public void onAvailable(Network network) {
            }

            @Override
            public void onLost(Network network) {
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            }

            @Override
            public void onChange() {
                calls.incrementAndGet();
            }
        };
    }

    @Test
    public void observerMutatingListInsideCallback_doesNotThrow() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final boolean[] added = {false};
        observable.addNetWorkObserver(new TDNetWorkObserver() {
            @Override
            public void onAvailable(Network network) {
            }

            @Override
            public void onLost(Network network) {
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            }

            @Override
            public void onChange() {
                calls.incrementAndGet();
                if (!added[0]) {
                    added[0] = true;
                    // reentrant add during iteration: old ArrayList implementation threw
                    // ConcurrentModificationException here
                    observable.addNetWorkObserver(countingObserver(new AtomicInteger()));
                }
            }
        });

        notifyOnChange();

        assertEquals(1, calls.get());
        assertEquals(2, observerCount());
    }

    @Test
    public void concurrentAddRemoveWhileNotifying_neverThrows() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<Throwable> failure = new AtomicReference<>();

        // baseline observers that stay registered for the whole run
        for (int i = 0; i < 4; i++) {
            observable.addNetWorkObserver(countingObserver(calls));
        }

        final int threads = 6;
        final int iterations = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            final int threadIndex = t;
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    ready.countDown();
                    try {
                        start.await();
                        for (int i = 0; i < iterations; i++) {
                            if (threadIndex == 0) {
                                notifyOnChange();
                            } else {
                                TDNetWorkObserver o = countingObserver(new AtomicInteger());
                                observable.addNetWorkObserver(o);
                                observable.removeNetWorkObserver(o);
                            }
                        }
                    } catch (Throwable e) {
                        failure.compareAndSet(null, e);
                    } finally {
                        done.countDown();
                    }
                }
            });
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertNull("concurrent notification threw: " + failure.get(), failure.get());
        // only the 4 baseline observers were registered for the whole run, so every
        // notification must have delivered exactly 4 callbacks
        assertEquals(4 * iterations, calls.get());
    }

    private int observerCount() throws Exception {
        Field f = TDNetWorkObservable.class.getDeclaredField("observers");
        f.setAccessible(true);
        return ((List<?>) f.get(observable)).size();
    }
}
