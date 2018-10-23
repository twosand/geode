package org.apache.geode.internal.cache.partitioned;

import org.apache.geode.DataSerializer;
import org.apache.geode.distributed.internal.ClusterDistributionManager;
import org.apache.geode.distributed.internal.DistributionMessage;
import org.apache.geode.internal.DataSerializableFixedID;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class PartitionedRegionFunctionStreamingAbortMessage extends DistributionMessage {
    private int processorId;
    public PartitionedRegionFunctionStreamingAbortMessage() {
    }

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
        PartitionedRegionFunctionStreamingContext.removeProcessorId(processorId);
    }

    @Override
    public void toData(DataOutput out) throws IOException {
        super.toData(out);
        DataSerializer.writePrimitiveInt(processorId,out);
    }

    @Override
    public void fromData(DataInput in) throws IOException, ClassNotFoundException {
        super.fromData(in);
        this.processorId= DataSerializer.readPrimitiveInt(in);

    }
}
