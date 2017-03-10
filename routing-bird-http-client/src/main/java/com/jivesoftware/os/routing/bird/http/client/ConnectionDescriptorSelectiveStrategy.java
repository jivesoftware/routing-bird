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

import com.jivesoftware.os.routing.bird.shared.ClientCall;
import com.jivesoftware.os.routing.bird.shared.ClientHealth;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;
import com.jivesoftware.os.routing.bird.shared.HostPort;
import com.jivesoftware.os.routing.bird.shared.HttpClientException;
import com.jivesoftware.os.routing.bird.shared.IndexedClientStrategy;
import com.jivesoftware.os.routing.bird.shared.NextClientStrategy;
import com.jivesoftware.os.routing.bird.shared.ReturnFirstNonFailure;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ConnectionDescriptorSelectiveStrategy implements NextClientStrategy, IndexedClientStrategy {

    private final HostPort[] orderHostPorts;
    private final AtomicReference<DescriptorsReference> lastDescriptorsReference = new AtomicReference<>();
    private final ReturnFirstNonFailure returnFirstNonFailure = new ReturnFirstNonFailure();

    public ConnectionDescriptorSelectiveStrategy(HostPort[] orderHostPorts) {
        this.orderHostPorts = orderHostPorts;
    }

    @Override
    public <C, R> R call(String family,
        ClientCall<C, R, HttpClientException> httpCall,
        ConnectionDescriptor[] connectionDescriptors,
        long connectionDescriptorsVersion,
        C[] clients,
        ClientHealth[] clientHealths,
        int deadAfterNErrors,
        long checkDeadEveryNMillis,
        AtomicInteger[] clientsErrors,
        AtomicLong[] clientsDeathTimestamp) throws HttpClientException {
        return returnFirstNonFailure.call(this,
            family,
            httpCall,
            connectionDescriptors,
            connectionDescriptorsVersion,
            clients,
            clientHealths,
            deadAfterNErrors,
            checkDeadEveryNMillis,
            clientsErrors,
            clientsDeathTimestamp);
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

    @Override
    public String toString() {
        return "ConnectionDescriptorSelectiveStrategy{"
            + "orderHostPorts=" + Arrays.toString(orderHostPorts)
            + ", lastDescriptorsReference=" + lastDescriptorsReference
            + '}';
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
