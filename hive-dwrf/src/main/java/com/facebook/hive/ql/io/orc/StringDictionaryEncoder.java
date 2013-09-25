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
package com.facebook.hive.ql.io.orc;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.hadoop.io.Text;

/**
 * A fast, memory efficient implementation of dictionary encoding stores strings. The strings are stored as UTF-8 bytes
 * and an offset/length for each entry.
 */
class StringDictionaryEncoder extends DictionaryEncoder {
  private final DynamicByteArray byteArray = new DynamicByteArray();
  private final DynamicIntArray keySizes = new DynamicIntArray();
  private Text newKey;

  private final TextCompressedOpenHashSet htDictionary = new TextCompressedOpenHashSet(new TextCompressedHashStrategy());

  private int numElements = 0;

  private class TextCompressed {
    private int offset = -1;
    private int length = -1;
    private int hashcode = -1;
    private TextCompressed(int hash) {
      hashcode = hash;
    }
  }

  public class TextPositionComparator implements IntComparator {
   @Override
   public int compare (Integer k1, Integer k2) {
     return this.compare(k1.intValue(), k2.intValue());
   }

	 @Override
	 public int compare (int k1, int k2) {
		 int k1Offset = keySizes.get(k1);
		 int k1Length = getEnd(k1) - k1Offset;

		 int k2Offset = keySizes.get(k2);
		 int k2Length = getEnd(k2) - k2Offset;

		 return byteArray.compare(k1Offset, k1Length, k2Offset, k2Length);
	 }
  }

  public class TextCompressedHashStrategy implements Hash.Strategy<TextCompressed> {
    @Override
    public boolean equals(TextCompressed obj1, TextCompressed other) {
      return obj1.hashcode == other.hashcode && equalsValue(obj1.offset, obj1.length);
    }

    @Override
    public int hashCode(TextCompressed arg0) {
      return arg0.hashcode;
    }
  }

  public class TextCompressedOpenHashSet extends ObjectOpenCustomHashSet<TextCompressed> {
    private TextCompressedOpenHashSet(Hash.Strategy<TextCompressed> strategy) {
      super(strategy);
    }
  }

  public StringDictionaryEncoder() {
    super();
  }

  public StringDictionaryEncoder(boolean sortKeys) {
    super(sortKeys);
  }

  public int add(Text value) {
    newKey = value;
    int len = newKey.getLength();
    TextCompressed curKeyCompressed = new TextCompressed(newKey.hashCode());
    if (!htDictionary.add(curKeyCompressed)) {
      return htDictionary.get(curKeyCompressed).offset;
    } else {
      // update count of hashset keys
      int valRow = numElements;
      numElements += 1;
      // set current key offset and hashcode
      curKeyCompressed.offset = valRow;
      curKeyCompressed.length = len;
      keySizes.add(byteArray.add(newKey.getBytes(), 0, len));
      return valRow;
    }
  }

  private int getEnd(int pos) {
    if (pos + 1 == keySizes.size()) {
      return byteArray.size();
    }

    return keySizes.get(pos + 1);
  }

  @Override
  protected int compareValue(int position) {
    int start = keySizes.get(position);
    int end = getEnd(position);
    return byteArray.compare(newKey.getBytes(), 0, newKey.getLength(),
        start, end - start);
  }

  protected boolean equalsValue(int position, int length) {
    int start = keySizes.get(position);
    return byteArray.equals(newKey.getBytes(), 0, newKey.getLength(), start, length);
  }

  private class VisitorContextImpl implements VisitorContext<Text> {
    private int originalPosition;
    private int start;
    private int end;
    private final Text text = new Text();

    public void setOriginalPosition(int pos) {
      originalPosition = pos;
      start = keySizes.get(originalPosition);
      end = getEnd(pos);
    }

    public int getOriginalPosition() {
      return originalPosition;
    }

    public Text getKey() {
      byteArray.setText(text, start, getLength());
      return text;
    }

    public void writeBytes(OutputStream out) throws IOException {
        byteArray.write(out, start, getLength());
    }

    public int getLength() {
      return end - start;
    }

  }

  private void visitDictionary(Visitor<Text> visitor, VisitorContextImpl context
                      ) throws IOException {
      int[] keysArray = null;
      if (sortKeys) {
        keysArray = new int[numElements];
        for (int idx = 0; idx < numElements; idx++) {
          keysArray[idx] = idx;
        }
        IntArrays.quickSort(keysArray, new TextPositionComparator());
      }

      for (int pos = 0; pos < numElements; pos++) {
        context.setOriginalPosition(keysArray == null? pos : keysArray[pos]);
        visitor.visit(context);
      }
      keysArray = null;
  }

  /**
   * Visit all of the nodes in the tree in sorted order.
   * @param visitor the action to be applied to each ndoe
   * @throws IOException
   */
  public void visit(Visitor<Text> visitor) throws IOException {
    visitDictionary(visitor, new VisitorContextImpl());
  }

  public void getText(Text result, int originalPosition) {
    int start = keySizes.get(originalPosition);
    int end = getEnd(originalPosition);
    byteArray.setText(result, start, end - start);
  }

  /**
   * Reset the table to empty.
   */
  @Override
  public void clear() {
    byteArray.clear();
    keySizes.clear();
    htDictionary.clear();
    numElements = 0;
  }

  /**
   * Get the size of the character data in the table.
   * @return the bytes used by the table
   */
  public int getCharacterSize() {
    return byteArray.size();
  }

  /**
   * Calculate the approximate size in memory.
   * @return the number of bytes used in storing the tree.
   */
  public long getSizeInBytes() {
    // one for dictionary keys
    long refSizes = (htDictionary.size() * 4);

    // 2 int fields per element (TextCompressed object)
    long textCompressedSizes = numElements * 4 * 2;

    // bytes in the characters
    // size of the int array storing the offsets
    long totalSize =  getCharacterSize() + keySizes.getSizeInBytes();
    totalSize += refSizes;
    return totalSize;
  }

  /**
   * Get the number of elements in the set.
   */
  @Override
  public int size() {
    return numElements;
  }

}