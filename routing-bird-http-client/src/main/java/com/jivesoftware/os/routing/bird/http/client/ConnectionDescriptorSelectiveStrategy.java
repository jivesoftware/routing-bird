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

import com.jivesoftware.os.upena.routing.shared.ConnectionDescriptor;
import com.jivesoftware.os.upena.routing.shared.HostPort;
import com.jivesoftware.os.upena.routing.shared.NextClientStrategy;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class ConnectionDescriptorSelectiveStrategy implements NextClientStrategy {

    private final HostPort[] orderHostPorts;
    private final AtomicReference<DescriptorsReference> lastDescriptorsReference = new AtomicReference<>();

    public ConnectionDescriptorSelectiveStrategy(HostPort[] orderHostPorts) {
        this.orderHostPorts = orderHostPorts;
    }

    @Override
    public int[] getClients(ConnectionDescriptor[] connectionDescriptors) {
        DescriptorsReference descriptorsReference = lastDescriptorsReference.get();
        if (descriptorsReference != null) {
            if (descriptorsReference.connectionDescriptors == connectionDescriptors) {
                return descriptorsReference.indexes;
            }
        }

        int[] indexes = new int[orderHostPorts.length];
        Arrays.fill(indexes, -1);
        for (int i = 0; i < orderHostPorts.length; i++) {
            for (int j = 0; j < connectionDescriptors.length; j++) {
                if (orderHostPorts[i].equals(connectionDescriptors[j].getHostPort())) {
                    indexes[i] = j;
                    break;
                }
            }
        }
        lastDescriptorsReference.set(new DescriptorsReference(connectionDescriptors, indexes));

        return indexes;
    }

    @Override
    public void usedClientAtIndex(int index) {
    }

    private static class DescriptorsReference {

        private final ConnectionDescriptor[] connectionDescriptors;
        private final int[] indexes;

        public DescriptorsReference(ConnectionDescriptor[] connectionDescriptors, int[] indexes) {
            this.connectionDescriptors = connectionDescriptors;
            this.indexes = indexes;
        }
    }
}
