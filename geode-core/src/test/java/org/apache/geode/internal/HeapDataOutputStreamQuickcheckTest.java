package org.apache.geode.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;

import java.nio.ByteBuffer;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import org.apache.geode.internal.tcp.ByteBufferInputStream;
import org.apache.geode.test.junit.categories.UnitTest;

@Category(UnitTest.class)
@RunWith(JUnitQuickcheck.class)
public class HeapDataOutputStreamQuickcheckTest {
  @Property
  public void roundTripStringSerialization(String s) throws Exception {
    assumeThat(s, new IsNot<>(new IsEqual<String>("")));
    HeapDataOutputStream heapDataOutputStream = new HeapDataOutputStream(Version.CURRENT);
    heapDataOutputStream.writeUTF(s);
    ByteBuffer byteBuffer = heapDataOutputStream.toByteBuffer();

    ByteBufferInputStream inputStream = new ByteBufferInputStream(byteBuffer);
    String decoded = inputStream.readUTF();

    assertEquals(s, decoded);
  }
}
