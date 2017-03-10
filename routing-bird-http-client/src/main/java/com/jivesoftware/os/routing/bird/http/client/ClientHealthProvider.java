package com.jivesoftware.os.routing.bird.http.client;

import com.jivesoftware.os.routing.bird.shared.ClientHealth;
import com.jivesoftware.os.routing.bird.shared.ConnectionDescriptor;

/**
 *
 * @author jonathan.colt
 */
public interface ClientHealthProvider {

    ClientHealth get(ConnectionDescriptor connectionDescriptor);

}
