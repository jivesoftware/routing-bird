package com.jivesoftware.os.routing.bird.http.client;

import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;

/**
 *
 * @author jonathan.colt
 */
public interface ClientHealthProvider {

    ClientHealth get(ConnectionDescriptor connectionDescriptor);

    interface ClientHealth {

        void attempt(String family);

        void success(String family, long latencyMillis);

        void markedDead();

        void connectivityError();

        void fatalError(Exception x);

        void stillDead();

    }

}
