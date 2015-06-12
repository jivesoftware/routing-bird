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
import com.jivesoftware.os.routing.bird.shared.NextClientStrategy;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinStrategy implements NextClientStrategy {

    private final AtomicInteger lastIndexUsed = new AtomicInteger(0);

    @Override
    public int[] getClients(ConnectionDescriptor[] connectionDescriptors) {
        int last = lastIndexUsed.get();
        int len = connectionDescriptors.length;
        int[] indexes = new int[len];
        for (int i = 0; i < len; i++) {
            indexes[i] = (last + i + 1) % len;
        }
        return indexes;
    }

    @Override
    public void usedClientAtIndex(int index) {
        lastIndexUsed.set(index);
    }
}
