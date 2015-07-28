package com.jivesoftware.os.routing.bird.shared;

import java.util.List;

/**
 *
 * @author jonathan.colt
 */
public class InstanceConnectionHealth {

    public String instanceId;
    public List<ConnectionHealth> connectionHealths;

    public InstanceConnectionHealth() {
    }

    public InstanceConnectionHealth(String instanceId, List<ConnectionHealth> connectionHealths) {
        this.instanceId = instanceId;
        this.connectionHealths = connectionHealths;
    }

}
