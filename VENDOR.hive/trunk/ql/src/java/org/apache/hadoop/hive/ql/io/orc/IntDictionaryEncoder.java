/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hive.ql.io.orc;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import java.io.IOException;
import java.io.OutputStream;

class IntDictionaryEncoder extends DictionaryEncoder {

  private long newKey;
  private int numElements = 0;
  private final int numBytes;
  private final boolean useVInts;

  protected final DynamicLongArray keys = new DynamicLongArray();
  protected final Long2IntOpenHashMap dictionary = new Long2IntOpenHashMap();

  public IntDictionaryEncoder(int numBytes, boolean useVInts) {
    super();
    this.numBytes = numBytes;
    this.useVInts = useVInts;
  }

  public IntDictionaryEncoder(boolean sortKeys, int numBytes, boolean useVInts) {
    super(sortKeys);
    this.numBytes = numBytes;
    this.useVInts = useVInts;
  }

  public long getValue(int position) {
    return keys.get(position);
  }

  /**
   *
   */
  public class LongPositionComparator implements IntComparator {
    @Override
    public int compare(Integer pos, Integer cmpPos) {
      return this.compare(pos.intValue(), cmpPos.intValue());
    }

    @Override
    public int compare(int pos, int cmpPos) {
      return compareValue(keys.get(pos), keys.get(cmpPos));
    }
  }

  public void visitDictionary(Visitor<Long> visitor, IntDictionaryEncoderVisitorContext context) throws IOException {
      int[] keysArray = null;
      if (sortKeys) {
        keysArray = new int[numElements];
        for (int idx = 0; idx < numElements; idx++) {
          keysArray[idx] = idx;
        }
        IntArrays.quickSort(keysArray, new LongPositionComparator());
      }
      for (int pos = 0; pos < numElements; pos++) {
        context.setOriginalPosition(keysArray == null? pos : keysArray[pos]);
        visitor.visit(context);
      }
      keysArray = null;
  }

  public void visit(Visitor<Long> visitor) throws IOException {
    visitDictionary(visitor, new IntDictionaryEncoderVisitorContext());
  }

  @Override
  public void clear() {
    keys.clear();
    dictionary.clear();
    numElements = 0;
  }

  private int compareValue (long k, long cmpKey) {
    if (k > cmpKey) {
      return 1;
    } else if (k < cmpKey) {
      return -1;
    }
    return 0;
  }

  @Override
  protected int compareValue(int position) {
    long cmpKey = keys.get(position);
    return compareValue(newKey, cmpKey);
  }

  public int add (long value) {
    newKey = value;
    if (dictionary.containsKey(value)) {
      return dictionary.get(value);
    } else {
      int valRow = numElements;
      numElements++;
      dictionary.put(value, valRow);
      keys.add(newKey);
      return valRow;
    }
  }

  public class IntDictionaryEncoderVisitorContext implements VisitorContext<Long> {

    int originalPosition;
    public void setOriginalPosition(int pos) {
      originalPosition = pos;
    }
    public int getOriginalPosition() {
      return originalPosition;
    }

    public Long getKey() {
      return keys.get(originalPosition);
    }

    public void writeBytes(OutputStream outputStream) throws IOException {
      long cur = keys.get(originalPosition);
      SerializationUtils.writeIntegerType(outputStream, cur, numBytes, true, useVInts);
    }

    // TODO: this should be different
    public int getLength() {
      return 8;
    }
  }

  public long getByteSize() {

    // Long2IntOpenHashMap stores per element:
    // key in long[] (8 bytes)
    // value in int[] (4 bytes)
    // whether each bucket was used or not in boolean [] (1 byte
    long posSizes = (8 + 4 + 1) * numElements;

    return keys.getSizeInBytes() + posSizes;
  }

  /**
   * Get the number of elements in the set.
   */
  @Override
  public int size() {
    return numElements;
  }

}

