/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal.cache.eviction;

import static java.lang.Math.abs;
import static org.apache.geode.distributed.ConfigurationProperties.OFF_HEAP_MEMORY_SIZE;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.junit.experimental.categories.Category;

import org.apache.geode.cache.CacheException;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache30.CacheSerializableRunnable;
import org.apache.geode.distributed.DistributedSystem;
import org.apache.geode.internal.cache.GemFireCacheImpl;
import org.apache.geode.internal.cache.LocalRegion;
import org.apache.geode.internal.cache.OffHeapTestUtil;
import org.apache.geode.internal.cache.control.InternalResourceManager.ResourceType;
import org.apache.geode.test.awaitility.GeodeAwaitility;
import org.apache.geode.test.dunit.Assert;
import org.apache.geode.test.dunit.Invoke;
import org.apache.geode.test.dunit.LogWriterUtils;
import org.apache.geode.test.dunit.SerializableRunnable;
import org.apache.geode.test.dunit.VM;
import org.apache.geode.test.dunit.WaitCriterion;
import org.apache.geode.test.junit.categories.EvictionTest;

/**
 * Performs eviction dunit tests for off-heap memory.
 */
@Category({EvictionTest.class})
public class OffHeapEvictionDUnitTest extends EvictionDUnitTest {

  @Override
  public final void preTearDownAssertions() throws Exception {
    SerializableRunnable checkOrphans = new SerializableRunnable() {

      @Override
      public void run() {
        if (hasCache()) {
          OffHeapTestUtil.checkOrphans(getCache());
        }
      }
    };
    Invoke.invokeInEveryVM(checkOrphans);
    checkOrphans.run();
  }

  @Override
  public Properties getDistributedSystemProperties() {
    Properties properties = super.getDistributedSystemProperties();
    properties.setProperty(OFF_HEAP_MEMORY_SIZE, "200m");

    return properties;
  }

  @Override
  public void createCache() {
    try {
      Properties props = new Properties();
      DistributedSystem ds = getSystem(props);
      assertNotNull(ds);
      ds.disconnect();
      ds = getSystem(getDistributedSystemProperties());
      cache = CacheFactory.create(ds);
      assertNotNull(cache);
      LogWriterUtils.getLogWriter().info("cache= " + cache);
      LogWriterUtils.getLogWriter().info("cache closed= " + cache.isClosed());
      cache.getResourceManager().setEvictionOffHeapPercentage(85);
      ((GemFireCacheImpl) cache).getInternalResourceManager().getOffHeapMonitor()
          .stopMonitoring(true);
      LogWriterUtils.getLogWriter()
          .info("eviction= " + cache.getResourceManager().getEvictionOffHeapPercentage());
      LogWriterUtils.getLogWriter()
          .info("critical= " + cache.getResourceManager().getCriticalOffHeapPercentage());
    } catch (Exception e) {
      Assert.fail("Failed while creating the cache", e);
    }
  }

  @Override
  public void raiseFakeNotification(VM vm, final String prName, final int noOfExpectedEvictions) {
    vm.invoke(new CacheSerializableRunnable("fakeNotification") {
      @Override
      public void run2() throws CacheException {
        final LocalRegion region = (LocalRegion) cache.getRegion(prName);
        getEvictor().setTestAbortAfterLoopCount(1);

        ((GemFireCacheImpl) cache).getInternalResourceManager().getOffHeapMonitor()
            .updateStateAndSendEvent(188743680);

        WaitCriterion wc = new WaitCriterion() {
          public boolean done() {
            // we have a primary
            final long currentEvictions = region.getTotalEvictions();
            if (abs(currentEvictions - noOfExpectedEvictions) <= 1) { // Margin of error is 1
              return true;
            } else if (currentEvictions > noOfExpectedEvictions) {
              fail(description());
            }
            return false;
          }

          public String description() {
            return "expected " + noOfExpectedEvictions + " evictions, but got "
                + region.getTotalEvictions();
          }
        };
        GeodeAwaitility.await().untilAsserted(wc);
      }
    });
  }

  @Override
  public boolean getOffHeapEnabled() {
    return true;
  }

  @Override
  public HeapEvictor getEvictor() {
    return ((GemFireCacheImpl) cache).getOffHeapEvictor();
  }

  @Override
  public ResourceType getResourceType() {
    return ResourceType.OFFHEAP_MEMORY;
  }

  @Override
  public void testDummyInlineNCentralizedEviction() {
    super.testDummyInlineNCentralizedEviction();
  }
}
