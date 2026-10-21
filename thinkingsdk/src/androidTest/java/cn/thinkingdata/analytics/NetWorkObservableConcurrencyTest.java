/*
 * Copyright (C) 2024 ThinkingData
 */
package cn.thinkingdata.analytics;

import android.content.Context;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import cn.thinkingdata.core.receiver.TDNetWorkObservable;
import cn.thinkingdata.core.receiver.TDNetWorkObserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented regression tests for {@link TDNetWorkObservable} on a real Android runtime.
 *
 * <p>Covers the crash reported against SDK 3.6.0: notifying network observers on the
 * connectivity callback thread while other threads register / unregister observers threw
 * {@link java.util.ConcurrentModificationException}.
 */
@RunWith(AndroidJUnit4.class)
public class NetWorkObservableConcurrencyTest {

    private static final int TYPE_ON_CHANGE = 4;

    private Context context;
    private TDNetWorkObservable observable;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        Field instance = TDNetWorkObservable.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        // real context exercises the network receiver / callback registration path
        observable = TDNetWorkObservable.getInstance(context);
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
    public void concurrentAddRemoveWhileNotifying_onDevice_neverThrows() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<Throwable> failure = new AtomicReference<>();

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
        assertTrue(done.await(60, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertNull("concurrent notification threw: " + failure.get(), failure.get());
        assertEquals(4 * iterations, calls.get());
    }
}
