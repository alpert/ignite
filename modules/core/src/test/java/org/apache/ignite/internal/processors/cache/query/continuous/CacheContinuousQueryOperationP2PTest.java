/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache.query.continuous;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import javax.cache.configuration.Factory;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryUpdatedListener;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheEntryEventSerializableFilter;
import org.apache.ignite.cache.CacheMemoryMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.TcpDiscoveryIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

import static org.apache.ignite.cache.CacheAtomicWriteOrderMode.PRIMARY;
import static org.apache.ignite.cache.CacheAtomicityMode.ATOMIC;
import static org.apache.ignite.cache.CacheAtomicityMode.TRANSACTIONAL;
import static org.apache.ignite.cache.CacheMemoryMode.ONHEAP_TIERED;
import static org.apache.ignite.cache.CacheMode.PARTITIONED;
import static org.apache.ignite.cache.CacheMode.REPLICATED;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;

/**
 *
 */
public class CacheContinuousQueryOperationP2PTest extends GridCommonAbstractTest {
    /** */
    private static TcpDiscoveryIpFinder ipFinder = new TcpDiscoveryVmIpFinder(true);

    /** */
    private static final int NODES = 5;

    /** */
    private static final int KEYS = 50;

    /** */
    private static final int VALS = 10;

    /** */
    public static final int ITERATION_CNT = 100;

    /** */
    private boolean client;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        ((TcpDiscoverySpi)cfg.getDiscoverySpi()).setIpFinder(ipFinder);

        cfg.setClientMode(client);
        cfg.setPeerClassLoadingEnabled(true);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        super.beforeTestsStarted();

        startGridsMultiThreaded(NODES - 1);

        client = true;

        startGrid(NODES - 1);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        stopAllGrids();

        super.afterTestsStopped();
    }

    /**
     * @throws Exception If failed.
     */
    public void testAtomicClient() throws Exception {
        CacheConfiguration<Object, Object> ccfg = cacheConfiguration(PARTITIONED,
            1,
            ATOMIC,
            ONHEAP_TIERED
        );

        testContinuousQuery(ccfg, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testAtomic() throws Exception {
        CacheConfiguration<Object, Object> ccfg = cacheConfiguration(PARTITIONED,
            1,
            ATOMIC,
            ONHEAP_TIERED
        );

        testContinuousQuery(ccfg, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testAtomicReplicated() throws Exception {
        CacheConfiguration<Object, Object> ccfg = cacheConfiguration(REPLICATED,
            0,
            ATOMIC,
            ONHEAP_TIERED
        );

        testContinuousQuery(ccfg, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testAtomicReplicatedClient() throws Exception {
        CacheConfiguration<Object, Object> ccfg = cacheConfiguration(REPLICATED,
            0,
            ATOMIC,
            ONHEAP_TIERED
        );

        testContinuousQuery(ccfg, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testTx() throws Exception {
        CacheConfiguration<Object, Object> ccfg = cacheConfiguration(PARTITIONED,
            1,
            TRANSACTIONAL,
            ONHEAP_TIERED
        );

        testContinuousQuery(ccfg, false);
    }
    /**
     * @throws Exception If failed.
     */
    public void testTxClient() throws Exception {
        CacheConfiguration<Object, Object> ccfg = cacheConfiguration(PARTITIONED,
            1,
            TRANSACTIONAL,
            ONHEAP_TIERED
        );

        testContinuousQuery(ccfg, true);
    }

    /**
     * @throws Exception If failed.
     */
    public void testTxReplicated() throws Exception {
        CacheConfiguration<Object, Object> ccfg = cacheConfiguration(REPLICATED,
            0,
            TRANSACTIONAL,
            ONHEAP_TIERED
        );

        testContinuousQuery(ccfg, false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testTxReplicatedClient() throws Exception {
        CacheConfiguration<Object, Object> ccfg = cacheConfiguration(REPLICATED,
            0,
            TRANSACTIONAL,
            ONHEAP_TIERED
        );

        testContinuousQuery(ccfg, true);
    }

    /**
     * @param ccfg Cache configuration.
     * @param isClient Client.
     * @throws Exception If failed.
     */
    protected void testContinuousQuery(CacheConfiguration<Object, Object> ccfg, boolean isClient)
        throws Exception {
        ignite(0).createCache(ccfg);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        QueryCursor<?> cur = null;

        final Class<Factory<CacheEntryEventFilter>> evtFilterFactory =
            (Class<Factory<CacheEntryEventFilter>>)getExternalClassLoader().
                loadClass("org.apache.ignite.tests.p2p.CacheDeploymentEntryEventFilterFactory");

        final CountDownLatch latch = new CountDownLatch(10);

        ContinuousQuery<Integer, Integer> qry = new ContinuousQuery<>();

        CacheEntryUpdatedListener<Integer, Integer> localLsnr = new CacheEntryUpdatedListener<Integer, Integer>() {
            @Override public void onUpdated(Iterable<CacheEntryEvent<? extends Integer,
                ? extends Integer>> evts) throws CacheEntryListenerException {
                for (CacheEntryEvent<? extends Integer, ? extends Integer> evt : evts)
                    latch.countDown();
            }
        };

        MutableCacheEntryListenerConfiguration<Integer, Integer> lsnrCfg =
            new MutableCacheEntryListenerConfiguration<>(
                new FactoryBuilder.SingletonFactory<>(localLsnr),
                (Factory<? extends CacheEntryEventFilter<? super Integer, ? super Integer>>)
                    (Object)evtFilterFactory.newInstance(),
                true,
                true
            );

        qry.setLocalListener(localLsnr);

        qry.setRemoteFilterFactory(
            (Factory<? extends CacheEntryEventFilter<Integer, Integer>>)(Object)evtFilterFactory.newInstance());

        IgniteCache<Integer, Integer> cache = null;

        try {
            if (isClient)
                cache = grid(NODES - 1).cache(ccfg.getName());
            else
                cache = grid(rnd.nextInt(NODES - 1)).cache(ccfg.getName());

            //cur = cache.query(qry);

            cache.registerCacheEntryListener(lsnrCfg);

            for (int i = 0; i < 10; i++)
                cache.put(i, i);

            assertTrue(latch.await(3, TimeUnit.SECONDS));
        }
        finally {
            if (cur != null)
                cur.close();

            if (cache != null)
                cache.deregisterCacheEntryListener(lsnrCfg);
        }
    }

    /**
     *
     * @param cacheMode Cache mode.
     * @param backups Number of backups.
     * @param atomicityMode Cache atomicity mode.
     * @param memoryMode Cache memory mode.
     * @return Cache configuration.
     */
    private CacheConfiguration<Object, Object> cacheConfiguration(
        CacheMode cacheMode,
        int backups,
        CacheAtomicityMode atomicityMode,
        CacheMemoryMode memoryMode) {
        CacheConfiguration<Object, Object> ccfg = new CacheConfiguration<>();

        ccfg.setAtomicityMode(atomicityMode);
        ccfg.setCacheMode(cacheMode);
        ccfg.setMemoryMode(memoryMode);
        ccfg.setWriteSynchronizationMode(FULL_SYNC);
        ccfg.setAtomicWriteOrderMode(PRIMARY);

        if (cacheMode == PARTITIONED)
            ccfg.setBackups(backups);

        return ccfg;
    }

    /**
     *
     */
    public static class QueryTestKey implements Serializable, Comparable {
        /** */
        private final Integer key;

        /**
         * @param key Key.
         */
        public QueryTestKey(Integer key) {
            this.key = key;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null || getClass() != o.getClass())
                return false;

            QueryTestKey that = (QueryTestKey)o;

            return key.equals(that.key);
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return key.hashCode();
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(QueryTestKey.class, this);
        }

        /** {@inheritDoc} */
        @Override public int compareTo(Object o) {
            return key - ((QueryTestKey)o).key;
        }
    }

    /**
     *
     */
    public static class QueryTestValue implements Serializable {
        /** */
        protected final Integer val1;

        /** */
        protected final String val2;

        /**
         * @param val Value.
         */
        public QueryTestValue(Integer val) {
            this.val1 = val;
            this.val2 = String.valueOf(val);
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null || getClass() != o.getClass())
                return false;

            QueryTestValue that = (QueryTestValue) o;

            return val1.equals(that.val1) && val2.equals(that.val2);
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            int res = val1.hashCode();

            res = 31 * res + val2.hashCode();

            return res;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(QueryTestValue.class, this);
        }
    }
}
