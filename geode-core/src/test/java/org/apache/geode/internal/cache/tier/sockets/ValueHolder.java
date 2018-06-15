package org.apache.geode.internal.cache.tier.sockets;

import org.apache.geode.pdx.PdxInstance;
import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxSerializable;
import org.apache.geode.pdx.PdxWriter;

public abstract class ValueHolder implements PdxSerializable {
  public String value;

  public ValueHolder() {
    value = "Test";
  }

  public ValueHolder (String input) {
    value = input;
  }

  public String getValue(){
    return value;
  }

  public void setValue(String value){
    this.value = value;
  }

  @Override
  public void fromData(PdxReader reader) {
    value = reader.readString("value");
  }
}
