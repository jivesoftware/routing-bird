package com.jivesoftware.os.routing.bird.shared;

/**
 *
 */
public interface IndexedClientStrategy {

    int[] getClients(ConnectionDescriptor[] connectionDescriptors);

    void usedClientAtIndex(int index);
}
