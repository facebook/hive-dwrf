//  Copyright (c) 2013, Facebook, Inc.  All rights reserved.

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
package com.facebook.hive.orc;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;

import com.facebook.hive.orc.OrcProto.RowIndexEntry;

/**
 * A reader that reads a sequence of integers.
 * */
public class RunLengthIntegerReader {
  private final InStream input;
  private final boolean signed;
  private final int numBytes;
  private final boolean useVInts;
  private final long[] literals =
    new long[RunLengthConstants.MAX_LITERAL_SIZE];
  private int numLiterals = 0;
  private int delta = 0;
  private int used = 0;
  private boolean repeat = false;
  private int[] indeces;

  public RunLengthIntegerReader(InStream input, boolean signed, int numBytes)
      throws IOException {
    this.input = input;
    this.signed = signed;
    this.numBytes = numBytes;
    this.useVInts = input.useVInts();
  }

  private void readValues() throws IOException {
    int control = input.read();
    if (control == -1) {
      throw new EOFException("Read past end of RLE integer from " + input);
    } else if (control < 0x80) {
      numLiterals = control + RunLengthConstants.MIN_REPEAT_SIZE;
      used = 0;
      repeat = true;
      delta = input.read();
      if (delta == -1) {
        throw new EOFException("End of stream in RLE Integer from " + input);
      }
      // convert from 0 to 255 to -128 to 127 by converting to a signed byte
      delta = (byte) (0 + delta);
      literals[0] = SerializationUtils.readIntegerType(input, numBytes, signed, useVInts);
    } else {
      repeat = false;
      numLiterals = 0x100 - control;
      used = 0;
      for(int i=0; i < numLiterals; ++i) {
        literals[i] = SerializationUtils.readIntegerType(input, numBytes, signed, useVInts);
      }
    }
  }

  boolean hasNext() throws IOException {
    return used != numLiterals || input.available() > 0;
  }

  public long next() throws IOException {
    long result;
    if (used == numLiterals) {
      readValues();
    }
    if (repeat) {
      result = literals[0] + (used++) * delta;
    } else {
      result = literals[used++];
    }
    return result;
  }

  public void seek(int index) throws IOException {
    input.seek(index);
    int consumed = (int) indeces[index];
    if (consumed != 0) {
      // a loop is required for cases where we break the run into two parts
      while (consumed > 0) {
        readValues();
        used = consumed;
        consumed -= numLiterals;
      }
    } else {
      used = 0;
      numLiterals = 0;
    }
  }

  /**
   * Read in the number of values consumed at each index entry and store it,
   * also call loadIndeces on child stream and return the index of the next
   * streams indexes.
   */
  public int loadIndeces(List<RowIndexEntry> rowIndexEntries, int startIndex) {
    int updatedStartIndex = input.loadIndeces(rowIndexEntries, startIndex);

    int numIndeces = rowIndexEntries.size();
    indeces = new int[numIndeces + 1];
    int i = 0;
    for (RowIndexEntry rowIndexEntry : rowIndexEntries) {
      indeces[i] = (int) rowIndexEntry.getPositions(updatedStartIndex);
      i++;
    }
    return updatedStartIndex + 1;
  }

  public void skip(long numValues) throws IOException {
    while (numValues > 0) {
      if (used == numLiterals) {
        readValues();
      }
      long consume = Math.min(numValues, numLiterals - used);
      used += consume;
      numValues -= consume;
    }
  }

  public void close() throws IOException {
    input.close();
  }
}
