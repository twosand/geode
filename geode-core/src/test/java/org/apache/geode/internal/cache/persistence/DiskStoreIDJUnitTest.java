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
package org.apache.geode.internal.cache.persistence;

import static org.apache.geode.internal.Assert.assertTrue;

import org.junit.Test;

public class DiskStoreIDJUnitTest {
  @Test
  public void compareShouldReturnCorrectOrder() {
    // note: diskStoreID_1 > diskStoreID_2 if considered mostSig
    // Note: the current code will treat diskStoreID_1 == diskStoreID_3
    DiskStoreID diskStoreID_1 = new DiskStoreID(0xa870e3a0126e4b0dL, 0x87559e428bd3ad67L);
    DiskStoreID diskStoreID_2 = new DiskStoreID(0x5a42b9ec2e6e45dbL, 0x9a3c0f29623d925cL);
    DiskStoreID diskStoreID_3 = new DiskStoreID(0x5a42b9ec2e6e45dbL, 0x87559e428bd3ad67L);
    assertTrue(diskStoreID_1.compareTo(diskStoreID_2) > 0);
    assertTrue(diskStoreID_1.compareTo(diskStoreID_3) > 0);
  }
}
