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

package org.apache.geode.internal.cache.partitioned;

import org.apache.geode.DataSerializer;
import org.apache.geode.distributed.internal.ClusterDistributionManager;
import org.apache.geode.distributed.internal.DistributionMessage;
import org.apache.geode.internal.DataSerializableFixedID;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PartitionedRegionFunctionStreamingAbortMessage extends DistributionMessage {
  private int processorId;

  public PartitionedRegionFunctionStreamingAbortMessage() {}

  public PartitionedRegionFunctionStreamingAbortMessage(int processorId) {
    this.processorId = processorId;
  }


  @Override
  public int getDSFID() {
    return DataSerializableFixedID.PR_FUNCTION_STREAMING_ABORT_MESSAGE;
  }

  @Override
  public int getProcessorType() {
    return ClusterDistributionManager.STANDARD_EXECUTOR;
  }

  @Override
  protected void process(ClusterDistributionManager dm) {
    PartitionedRegionFunctionStreamingContext.removeProcessorId(getSender(), processorId);
  }

  @Override
  public void toData(DataOutput out) throws IOException {
    super.toData(out);
    DataSerializer.writePrimitiveInt(processorId, out);
  }

  @Override
  public void fromData(DataInput in) throws IOException, ClassNotFoundException {
    super.fromData(in);
    this.processorId = DataSerializer.readPrimitiveInt(in);

  }
}
