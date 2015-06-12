package com.jivesoftware.os.routing.bird.shared;

/**
 *
 */
public interface NextClientStrategy {

    int[] getClients(ConnectionDescriptor[] connectionDescriptors);

    void usedClientAtIndex(int index);
}
