package org.apache.geode.cache.lucene.test;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class SomeDomainTestObject implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String strField;
  private final int intField;
  private final long longField;
  private final float floatField;
  private final double doubleField;
  private final Date dateField;

  public SomeDomainTestObject(String strField, int intField, long longField, float floatField,
      double doubleField, Date dateField) {
    this.strField = strField;
    this.intField = intField;
    this.longField = longField;
    this.floatField = floatField;
    this.doubleField = doubleField;
    this.dateField = dateField;
  }

  private SomeDomainTestObject(Builder builder) {
    strField = builder.strField;
    intField = builder.intField;
    longField = builder.longField;
    floatField = builder.floatField;
    doubleField = builder.doubleField;
    dateField = builder.dateField;
  }

  public String getStrField() {
    return strField;
  }

  public int getIntField() {
    return intField;
  }

  public long getLongField() {
    return longField;
  }

  public float getFloatField() {
    return floatField;
  }

  public Date getDateField() {
    return dateField;
  }

  public double getDoubleField() {
    return doubleField;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("SomeDomain{");
    sb.append("strField='").append(strField).append('\'');
    sb.append(", height=").append(intField);
    sb.append(", dateField=").append(dateField);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SomeDomainTestObject that = (SomeDomainTestObject) o;
    return intField == that.intField &&
        longField == that.longField &&
        Float.compare(that.floatField, floatField) == 0 &&
        Double.compare(that.doubleField, doubleField) == 0 &&
        Objects.equals(strField, that.strField) &&
        Objects.equals(dateField, that.dateField);
  }

  @Override
  public int hashCode() {

    return Objects.hash(strField, intField, longField, floatField, doubleField, dateField);
  }

  public static final class Builder {
    private String strField;
    private int intField;
    private long longField;
    private float floatField;
    private double doubleField;
    private Date dateField;

    private Builder() {}

    public Builder withStrField(String val) {
      strField = val;
      return this;
    }

    public Builder withIntField(int val) {
      intField = val;
      return this;
    }

    public Builder withLongField(long val) {
      longField = val;
      return this;
    }

    public Builder withFloatField(float val) {
      floatField = val;
      return this;
    }

    public Builder withDoubleField(double val) {
      doubleField = val;
      return this;
    }

    public Builder withDateField(Date val) {
      dateField = val;
      return this;
    }

    public SomeDomainTestObject build() {
      return new SomeDomainTestObject(this);
    }
  }
}
