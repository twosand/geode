package org.apache.geode.internal.cache.tier.sockets;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.pholser.junit.quickcheck.From;
import org.apache.logging.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.apache.geode.DataSerializable;
import org.apache.geode.DataSerializer;
import org.apache.geode.SerializationException;
import org.apache.geode.cache.Operation;
import org.apache.geode.distributed.DistributedSystem;
import org.apache.geode.distributed.internal.DistributionManager;
import org.apache.geode.distributed.internal.InternalDistributedSystem;
import org.apache.geode.internal.Assert;
import org.apache.geode.internal.DSCODE;
import org.apache.geode.internal.DataSerializableFixedID;
import org.apache.geode.internal.HeapDataOutputStream;
import org.apache.geode.internal.InternalDataSerializer;
import org.apache.geode.internal.InternalInstantiator;
import org.apache.geode.internal.Sendable;
import org.apache.geode.internal.Version;
import org.apache.geode.internal.cache.GemFireCacheImpl;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.cache.entries.AbstractRegionEntry;
import org.apache.geode.internal.cache.tier.CachedRegionHelper;
import org.apache.geode.internal.cache.tier.sockets.testing.OperationGenerator;
import org.apache.geode.internal.i18n.LocalizedStrings;
import org.apache.geode.internal.logging.LogService;
import org.apache.geode.internal.logging.log4j.LogMarker;
import org.apache.geode.internal.util.BlobHelper;
import org.apache.geode.pdx.PdxInstance;
import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxSerializable;
import org.apache.geode.pdx.PdxWriter;
import org.apache.geode.pdx.internal.PdxInstanceEnum;
import org.apache.geode.pdx.internal.TypeRegistry;
import org.apache.geode.test.dunit.rules.ClientVM;
import org.apache.geode.test.dunit.rules.ClusterStartupRule;
import org.apache.geode.test.junit.categories.DistributedTest;
import org.apache.geode.test.junit.categories.UnitTest;
import org.apache.geode.test.junit.runners.CategoryWithParameterizedRunnerFactory;

@Category(DistributedTest.class)
// @RunWith(JUnitQuickcheck.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(CategoryWithParameterizedRunnerFactory.class)
public class MessageSerializationQuickcheckTest {
//  @Rule
//  ClusterStartupRule clusterStartupRule = new ClusterStartupRule();

  @Parameterized.Parameter
  public Operation thisTestOp;

  @Parameterized.Parameters(name = "{index}: {0}")
  public static List<Operation> getAllPublicStaticFinalOperationFields()
      throws IllegalAccessException {
    Field[] declaredFields = Operation.class.getDeclaredFields();
    List<Operation> opTypeFields = new ArrayList<>();

    // Since we want to be able to identify any type of operation and new ones may be added over
    // time
    // we use this logic to parse the fields of the Operation method which should contain a public
    // static final instance of every operation type.
    for (Field field : declaredFields) {
      if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
          && java.lang.reflect.Modifier.isFinal(field.getModifiers())
          && java.lang.reflect.Modifier.isPublic(field.getModifiers())) {

        if (field.getType() == Operation.class) {
          Operation op = (Operation) field.get(null);
          opTypeFields.add(op);
        }
      }
    }

    return opTypeFields;
  }

  // @Property(trials = 2000)

  @Test
  public void invalidPartHeaderByte() throws Exception {
    GemFireCacheImpl gfCache = mock(GemFireCacheImpl.class);
    InternalDistributedSystem system = mock(InternalDistributedSystem.class);
    //when(gfCache.hasPool()).thenReturn(true);
    when(gfCache.getPdxPersistent()).thenReturn(false);
    when(gfCache.getInternalDistributedSystem()).thenReturn(system);
    when(system.isLoner()).thenReturn(true);

    TypeRegistry tr = new TypeRegistry(gfCache, false);
    when(gfCache.getPdxRegistry()).thenReturn(tr);
    InternalCache cache = mock(InternalCache.class);
    when(cache.getPdxRegistry()).thenReturn(mock(TypeRegistry.class));

    Object key = "myKey";
    Object regionPath = "myRegionPath";
    String expectedOldValueString = "veryveryveryveryveryveryveryveryveryveryveryverylongvalue";
    for(int i = 0; i < 6; i++)
    {
      expectedOldValueString += expectedOldValueString;
    }

    //Object expectedOldValue = new ValueHolder2(expectedOldValueString);
    Object expectedOldValue = expectedOldValueString;
    Operation op = thisTestOp;

    HeapDataOutputStream heapDataOutputStream = new MutableHeapDataOutputStream(Version.CURRENT);

    Message msg = new Message(5, Version.CURRENT);

    msg.addObjPart(key);
    msg.addObjPart(regionPath);
    msg.addObjPart(op);
    msg.addObjPart(expectedOldValue);

    Part actualOldValuePart = msg.getPart(3);

    msg.setComms(mock(Socket.class), mock(InputStream.class), heapDataOutputStream,
        ByteBuffer.allocate(1024), mock(MessageStats.class));


    msg.send(false);

    //((MutableHeapDataOutputStream)heapDataOutputStream).BreakChunks();

    InputStream inputStream = heapDataOutputStream.getInputStream();

    Message receivedMsg = new Message(2, Version.CURRENT);
    receivedMsg.setComms(mock(Socket.class), inputStream, mock(OutputStream.class),
        ByteBuffer.allocate(1024), mock(MessageStats.class));

    ServerConnection serverConnection = mock(ServerConnection.class);
    when(serverConnection.getCachedRegionHelper()).thenReturn(mock(CachedRegionHelper.class));
    receivedMsg.receive(serverConnection, -1, mock(Semaphore.class), mock(Semaphore.class));

    assertEquals(msg.getNumberOfParts(), receivedMsg.getNumberOfParts());

    Part expectedOldValuePart = receivedMsg.getPart(3);

    AbstractRegionEntry.checkPdxEquals((PdxInstance) expectedOldValuePart.getObject(),
        actualOldValuePart.getObject(), cache);
  }

  @Test
  public void example() throws Exception {
    roundTripMessageSerialization("myKey", "/regionPath", "exampledValue", thisTestOp);
  }

  public void roundTripMessageSerialization(String key, String regionPath, String expectedOldValue,
      // @OperationGenerator.RandOp
      @From(OperationGenerator.class) Operation op) throws Exception {

    System.out.println(key);
    System.out.println(regionPath);
    System.out.println(expectedOldValue);
    System.out.println(op);
    HeapDataOutputStream heapDataOutputStream = new HeapDataOutputStream(Version.CURRENT);

    Message msg = new Message(5, Version.CURRENT);

    msg.addStringOrObjPart(key);
    msg.addStringPart(regionPath);
    msg.addObjPart(op);
    msg.addObjPart(expectedOldValue);

    msg.setComms(mock(Socket.class), mock(InputStream.class), heapDataOutputStream,
        ByteBuffer.allocate(1024), mock(MessageStats.class));

    msg.send(false);

    InputStream inputStream = heapDataOutputStream.getInputStream();

    Message receivedMsg = new Message(2, Version.CURRENT);
    receivedMsg.setComms(mock(Socket.class), inputStream, mock(OutputStream.class),
        ByteBuffer.allocate(1024), mock(MessageStats.class));

    ServerConnection serverConnection = mock(ServerConnection.class);
    when(serverConnection.getCachedRegionHelper()).thenReturn(mock(CachedRegionHelper.class));
    receivedMsg.receive(serverConnection, -1, mock(Semaphore.class), mock(Semaphore.class));

    assertEquals(msg.getNumberOfParts(), receivedMsg.getNumberOfParts());

    Part keyPart = receivedMsg.getPart(0);
    Part regionNamePart = receivedMsg.getPart(1);
    Part opPart = receivedMsg.getPart(2);
    Part expectedOldValuePart = receivedMsg.getPart(3);

    assertEquals(key, keyPart.getStringOrObject());
    assertEquals(regionPath, regionNamePart.getString());
    assertEquals(op, opPart.getObject());
    assertEquals(expectedOldValue, expectedOldValuePart.getObject());
  }

}

class MutableHeapDataOutputStream extends HeapDataOutputStream {

  public void BreakChunks() {
    while (this.chunks != null && this.chunks.size() > 1) {
      this.chunks.removeLast();
    }
  }

  public MutableHeapDataOutputStream(Version streamVersion) {
    super(streamVersion); }
}

class InternalDataSerializerWithSpecifiedCache extends InternalDataSerializer{

  public InternalDistributedSystem system;
  private static final Logger logger = LogService.getLogger();

  @Override
  public Class[] getSupportedClasses() {
    // illegal for a customer to return null but we can do it since we never register
    // this serializer.
    return null;
  }

  @Override
  public boolean toData(Object o, DataOutput out) throws IOException {

    throw new IOException("Unexpected call to toData(Object, DataOutput) in InternalDataSerializerWIthSpecificCache");
  }

  @Override
  public Object fromData(DataInput in) throws IOException, ClassNotFoundException {

    throw new IOException("Unexpected call to fromData(DataOutput) in InternalDataSerializerWIthSpecificCache");
  }

  @Override
  public int getId() {
    // illegal for a customer to use but since our WellKnownDS is never registered
    // with this id it gives us one to use
    return 0;
  }

  public static void basicWriteObject(Object o, DataOutput out, boolean ensurePdxCompatibility, GemFireCacheImpl gfCache)
      throws IOException {
    checkOut(out);

    final boolean isDebugEnabled_SERIALIZER = logger.isTraceEnabled(LogMarker.SERIALIZER_VERBOSE);
    if (isDebugEnabled_SERIALIZER) {
      logger.trace(LogMarker.SERIALIZER_VERBOSE, "basicWriteObject: {}", o);
    }

    // Handle special objects first
    if (o == null) {
      out.writeByte(DSCODE.NULL.toByte());

    } else if (o instanceof PdxSerializable) {
      writePdx(out, gfCache, o, null);
    } else {
      if (logger.isTraceEnabled(LogMarker.SERIALIZER_ANNOUNCE_TYPE_WRITTEN_VERBOSE)) {
        logger.trace(LogMarker.SERIALIZER_ANNOUNCE_TYPE_WRITTEN_VERBOSE,
            "DataSerializer Serializing an instance of {}", o.getClass().getName());
      }

      writeSerializableObject(o, out);
    }
  }
}

class BlobHelperWithSpecifiedCache extends BlobHelper {

  public static void serializeTo(Object obj, HeapDataOutputStream hdos, GemFireCacheImpl gfCache) throws IOException {
    InternalDataSerializerWithSpecifiedCache.basicWriteObject(obj, hdos, false, gfCache);
  }
}

class MessageWithSpecifiedCache extends Message {

  private GemFireCacheImpl gfCache = null;

  MessageWithSpecifiedCache(int numberOfParts, Version destVersion, GemFireCacheImpl inputGfCache) {
    super(numberOfParts, destVersion);
    this.version = destVersion;
    this.gfCache = inputGfCache;
  }

  public void addObjPart(Object o) {
    if (o == null || o instanceof byte[]) {
      addRawPart((byte[]) o, false);
    } else {
      serializeAndAddPart(o);
    }
  }

  private void serializeAndAddPart(Object o) {
    Version v = this.version;
    if (this.version.equals(Version.CURRENT)) {
      v = null;
    }

    // do NOT close the HeapDataOutputStream
    HeapDataOutputStream hdos = new HeapDataOutputStream(this.chunkSize, v);
    try {
      if(o instanceof PdxSerializable) {
        BlobHelperWithSpecifiedCache.serializeTo(o, hdos, gfCache);
      } else {
        BlobHelper.serializeTo(o, hdos);
      }
    } catch (IOException ex) {
      throw new SerializationException("failed serializing object", ex);
    }
    this.messageModified = true;
    Part part = this.partsList[this.currentPart];
    part.setPartState(hdos, true);
    this.currentPart++;
  }
}