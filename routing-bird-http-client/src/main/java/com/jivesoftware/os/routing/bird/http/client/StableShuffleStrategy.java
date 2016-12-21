/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.routing.bird.http.client;

import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.HostPort;
import com.jivesoftware.os.routing.bird.shared.NextClientStrategy;
import java.util.Arrays;
import java.util.Random;

import static org.apache.commons.lang.math.RandomUtils.nextLong;

public class StableShuffleStrategy implements NextClientStrategy {

    private final long seed;

    public StableShuffleStrategy(long seed) {
        this.seed = seed;
    }

    @Override
    public int[] getClients(ConnectionDescriptor[] connectionDescriptors) {
        Random random = new Random(seed);
        long[] hash = new long[connectionDescriptors.length];
        int[] indexes = new int[connectionDescriptors.length];
        for (int i = 0; i < hash.length; i++) {
            hash[i] = random.nextLong();
            indexes[i] = i;
        }
        mirrorSort(hash, indexes);
        return indexes;
    }

    @Override
    public void usedClientAtIndex(int index) {
    }

    private static void mirrorSort(long[] _sort, int[] _keys) {
        sortLI(_sort, _keys, 0, _sort.length);
    }

    private static void sortLI(long[] x, int[] keys, int off, int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++) {
                for (int j = i; j > off && x[j - 1] > x[j]; j--) {
                    swapLI(x, keys, j, j - 1);
                }
            }
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1);       // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3L(x, l, l + s, l + 2 * s);
                m = med3L(x, m - s, m, m + s);
                n = med3L(x, n - 2 * s, n - s, n);
            }
            m = med3L(x, l, m, n); // Mid-size, med of 3
        }
        double v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && x[b] <= v) {
                if (x[b] == v) {
                    swapLI(x, keys, a++, b);
                }
                b++;
            }
            while (c >= b && x[c] >= v) {
                if (x[c] == v) {
                    swapLI(x, keys, c, d--);
                }
                c--;
            }
            if (b > c) {
                break;
            }
            swapLI(x, keys, b++, c--);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecswapLI(x, keys, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswapLI(x, keys, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1) {
            sortLI(x, keys, off, s);
        }
        if ((s = d - c) > 1) {
            sortLI(x, keys, n - s, s);
        }
    }

    /**
     * Swaps x[a] with x[b].
     */
    private static void swapLI(long x[], int[] keys, int a, int b) {
        long t = x[a];
        x[a] = x[b];
        x[b] = t;

        int l = keys[a];
        keys[a] = keys[b];
        keys[b] = l;

    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswapLI(long x[], int[] keys, int a, int b, int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swapLI(x, keys, a, b);
        }
    }

    /**
     * Returns the index of the median of the three indexed doubles.
     */
    private static int med3L(long x[], int a, int b, int c) {
        return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a) : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }

}
