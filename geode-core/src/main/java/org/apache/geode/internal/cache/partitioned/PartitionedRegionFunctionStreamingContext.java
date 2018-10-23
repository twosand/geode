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

public class PartitionedRegionFunctionStreamingContext {
    public static short msgId= DataSerializableFixedID.REQUEST_SYNC_MESSAGE+1;
    private static Set<Integer> runningProcessorIds=new ConcurrentSkipListSet<>();
    public static void addProcessorId(int processorId){
        System.err.println("Add "+runningProcessorIds+" >>> "+processorId);
        runningProcessorIds.add(processorId);
    }
    public static void removeProcessorId(int processorId){
        System.err.println("Remove "+runningProcessorIds+" >>> "+processorId);
        runningProcessorIds.remove(processorId);
    }

    /***
     *
     * @param processorId
     * @return true: processor closed
     */
    public static boolean processorClosed(int processorId){
        System.err.println("Check "+runningProcessorIds+" >>> "+processorId);
        return !runningProcessorIds.contains(processorId);
    }
}
