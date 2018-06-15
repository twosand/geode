package org.apache.geode.internal.cache.tier.sockets.testing;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import org.apache.geode.cache.Operation;

public class OperationGenerator extends Generator<Operation> {
  private static int counter = 0;

  // private RandOp randOp;

  public OperationGenerator() {
    super(Operation.class);
  }

  // public void configure(RandOp randOp) {
  // this.randOp = randOp;
  // }

  @Override
  public Operation generate(SourceOfRandomness r, GenerationStatus status) {
    Field[] declaredFields = Operation.class.getDeclaredFields();
    List<Field> opTypeFields = new ArrayList<Field>();

    // Since we want to be able to identify any type of operation and new ones may be added over
    // time
    // we use this logic to parse the fields of the Operation method which should contain a public
    // static final instance of every operation type.
    for (Field field : declaredFields) {
      if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
          && java.lang.reflect.Modifier.isFinal(field.getModifiers())
          && java.lang.reflect.Modifier.isPublic(field.getModifiers())) {

        if (field.getType() == Operation.class) {
          opTypeFields.add(field);
        }
      }
    }

    int opIndex = counter;
    counter = (counter + 1) % opTypeFields.size();

    try {
      return (Operation) opTypeFields.get(opIndex).get(null);
    } catch (Exception e) {
      throw new RuntimeException("Exception in OperationGenerator: ", e.getCause());
    }
  }

  // @Target({PARAMETER, FIELD, ANNOTATION_TYPE, TYPE_USE})
  // @Retention(RUNTIME)
  // @GeneratorConfiguration
  // public @interface RandOp {
  // // ...
  // }
}
