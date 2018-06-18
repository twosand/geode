package org.apache.geode.internal.cache.tier.sockets;

import static com.sun.tools.doclint.Entity.part;
import static org.apache.geode.distributed.ConfigurationProperties.MAX_WAIT_TIME_RECONNECT;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.DataSerializer;
import org.apache.geode.cache.EntryNotFoundException;
import org.apache.geode.cache.Operation;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.internal.HeapDataOutputStream;
import org.apache.geode.internal.Version;
import org.apache.geode.internal.cache.entries.AbstractRegionEntry;
import org.apache.geode.internal.cache.tier.CachedRegionHelper;
import org.apache.geode.pdx.PdxInstance;
import org.apache.geode.pdx.PdxInstanceJUnitTest;
import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxSerializable;
import org.apache.geode.pdx.PdxWriter;
import org.apache.geode.test.dunit.AsyncInvocation;
import org.apache.geode.test.dunit.cache.internal.JUnit4CacheTestCase;
import org.apache.geode.test.dunit.rules.ClientVM;
import org.apache.geode.test.dunit.rules.ClusterStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;
import org.apache.geode.test.junit.rules.ServerStarterRule;

public class HeapDataOutputStreamDUnitTest {

  @Rule
  public ClusterStartupRule cluster = new ClusterStartupRule();

  @Test
  public void sendIncompleteMessage() throws Exception{
    MemberVM locator = cluster.startLocatorVM(0);
    MemberVM server = cluster.startServerVM(1,
        x -> x.withPDXReadSerialized()
            .withConnectionToLocator(locator.getPort())
            .withProperty(MAX_WAIT_TIME_RECONNECT, "4000")
            .withRegion(RegionShortcut.REPLICATE, "regionA"));


    int locatorPort = locator.getPort();
    ClientVM client = cluster.startClientVM(2, new Properties(),
        (Serializable & Consumer<ClientCacheFactory>) x -> x.addPoolLocator("localhost", locatorPort).setPoolSubscriptionEnabled(true));

    AsyncInvocation asyncInvocation = client.invokeAsync(() -> {
      ClientCache cache = ClusterStartupRule.getClientCache();
      Region<Object, Object> regionA = cache.createClientRegionFactory(ClientRegionShortcut.PROXY).create("regionA");
      while(true) {
        try {
          regionA.destroy("A");
        } catch (EntryNotFoundException e) {
          //System.out.println("destroy: " + e.getMessage());
        }
      }
    });

    Thread thread = new Thread(() -> {
      while (true) {
        System.out.println("force disconnect");
        server.forceDisconnectMember();
        try {
          Thread.sleep(8000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    });

    thread.start();

    asyncInvocation.await();
    thread.join();
  }

  //  @Test
//  public void name() throws IOException, ClassNotFoundException {
//    Object key = "myKey";
//    Object regionPath = "myRegionPath";
//    String expectedOldValueString = "veryveryveryveryveryveryveryveryveryveryveryverylongvalue";
//    for(int i = 0; i < 6; i++)
//    {
//      expectedOldValueString += expectedOldValueString;
//    }
//
//    String valueString = expectedOldValueString;
//
//    Object expectedOldValue = new ValueHolder(expectedOldValueString) {
//      public void toData(PdxWriter writer) {
//        writer.writeString("value", valueString);
//      }
//    };
//
//    Operation op = Operation.DESTROY;
//
//    HeapDataOutputStream heapDataOutputStream = new MutableHeapDataOutputStream(Version.CURRENT);
//
//    Message msg = new Message(5, Version.CURRENT);
//
//    msg.addObjPart(key);
//    msg.addObjPart(regionPath);
//    msg.addObjPart(op);
//    msg.addObjPart(expectedOldValue);
//
//    msg.setComms(mock(Socket.class), mock(InputStream.class), heapDataOutputStream,
//        ByteBuffer.allocate(1024), mock(MessageStats.class));
//
//
//    msg.send(false);
//
//    InputStream inputStream = heapDataOutputStream.getInputStream();
//
//    Message receivedMsg = new Message(2, Version.CURRENT);
//    receivedMsg.setComms(mock(Socket.class), inputStream, mock(OutputStream.class),
//        ByteBuffer.allocate(1024), mock(MessageStats.class));
//
//    ServerConnection serverConnection = mock(ServerConnection.class);
//    when(serverConnection.getCachedRegionHelper()).thenReturn(mock(CachedRegionHelper.class));
//    receivedMsg.receive(serverConnection, -1, mock(Semaphore.class), mock(Semaphore.class));
//
//    assertEquals(msg.getNumberOfParts(), receivedMsg.getNumberOfParts());
//
//    Part expectedOldValuePart = receivedMsg.getPart(3);
//
//    byte[] serializedForm = expectedOldValuePart.getSerializedForm();
//    serializedForm[0] = -128;
//
//    AbstractRegionEntry.checkPdxEquals((PdxInstance)expectedOldValuePart.getObject(),
//        (PdxInstance)expectedOldValuePart.getObject(), server.getCache());
//  }
}
