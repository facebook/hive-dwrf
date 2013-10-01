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

package org.apache.hadoop.hive.ql.io.slice;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public final class SizeOf
{
    public static final byte SIZE_OF_BYTE = 1;
    public static final byte SIZE_OF_SHORT = 2;
    public static final byte SIZE_OF_INT = 4;
    public static final byte SIZE_OF_LONG = 8;
    public static final byte SIZE_OF_FLOAT = 4;
    public static final byte SIZE_OF_DOUBLE = 8;

    private static final Unsafe UNSAFE;
    static {
        try {
            // fetch theUnsafe object
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
            if (UNSAFE == null) {
                throw new RuntimeException("Unsafe access not available");
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final int ARRAY_BOOLEAN_BASE_OFFSET = UNSAFE.arrayBaseOffset(boolean[].class);
    public static final int ARRAY_BOOLEAN_INDEX_SCALE = UNSAFE.arrayIndexScale(boolean[].class);
    public static final int ARRAY_BYTE_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
    public static final int ARRAY_BYTE_INDEX_SCALE = UNSAFE.arrayIndexScale(byte[].class);
    public static final int ARRAY_SHORT_BASE_OFFSET = UNSAFE.arrayBaseOffset(short[].class);
    public static final int ARRAY_SHORT_INDEX_SCALE = UNSAFE.arrayIndexScale(short[].class);
    public static final int ARRAY_INT_BASE_OFFSET = UNSAFE.arrayBaseOffset(int[].class);
    public static final int ARRAY_INT_INDEX_SCALE = UNSAFE.arrayIndexScale(int[].class);
    public static final int ARRAY_LONG_BASE_OFFSET = UNSAFE.arrayBaseOffset(long[].class);
    public static final int ARRAY_LONG_INDEX_SCALE = UNSAFE.arrayIndexScale(long[].class);
    public static final int ARRAY_FLOAT_BASE_OFFSET = UNSAFE.arrayBaseOffset(float[].class);
    public static final int ARRAY_FLOAT_INDEX_SCALE = UNSAFE.arrayIndexScale(float[].class);
    public static final int ARRAY_DOUBLE_BASE_OFFSET = UNSAFE.arrayBaseOffset(double[].class);
    public static final int ARRAY_DOUBLE_INDEX_SCALE = UNSAFE.arrayIndexScale(double[].class);
    public static final int ARRAY_OBJECT_BASE_OFFSET = UNSAFE.arrayBaseOffset(Object[].class);
    public static final int ARRAY_OBJECT_INDEX_SCALE = UNSAFE.arrayIndexScale(Object[].class);

    public static long sizeOf(boolean[] array)
    {
        if (array == null) {
            return 0;
        }

        return ARRAY_BOOLEAN_BASE_OFFSET + ((long) ARRAY_BOOLEAN_INDEX_SCALE * array.length);
    }

    public static long sizeOf(byte[] array)
    {
        if (array == null) {
            return 0;
        }
        return ARRAY_BYTE_BASE_OFFSET + ((long) ARRAY_BYTE_INDEX_SCALE * array.length);
    }

    public static long sizeOf(short[] array)
    {
        if (array == null) {
            return 0;
        }
        return ARRAY_SHORT_BASE_OFFSET + ((long) ARRAY_SHORT_INDEX_SCALE * array.length);
    }

    public static long sizeOf(int[] array)
    {
        if (array == null) {
            return 0;
        }
        return ARRAY_INT_BASE_OFFSET + ((long) ARRAY_INT_INDEX_SCALE * array.length);
    }

    public static long sizeOf(long[] array)
    {
        if (array == null) {
            return 0;
        }
        return ARRAY_LONG_BASE_OFFSET + ((long) ARRAY_LONG_INDEX_SCALE * array.length);
    }

    public static long sizeOf(float[] array)
    {
        if (array == null) {
            return 0;
        }
        return ARRAY_FLOAT_BASE_OFFSET + ((long) ARRAY_FLOAT_INDEX_SCALE * array.length);
    }

    public static long sizeOf(double[] array)
    {
        if (array == null) {
            return 0;
        }
        return ARRAY_DOUBLE_BASE_OFFSET + ((long) ARRAY_DOUBLE_INDEX_SCALE * array.length);
    }

    public static long sizeOf(Object[] array)
    {
        if (array == null) {
            return 0;
        }
        return ARRAY_OBJECT_BASE_OFFSET + ((long) ARRAY_OBJECT_INDEX_SCALE * array.length);
    }

    private SizeOf()
    {
    }
}
