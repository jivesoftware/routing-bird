package com.jivesoftware.os.routing.bird.shared;

import java.util.List;

/**
 *
 * @author jonathan.colt
 */
public class InstanceThrowns {

    public String instanceId;
    public List<InstanceThrown> throwns;

    public InstanceThrowns() {
    }

    public InstanceThrowns(String instanceId, List<InstanceThrown> throwns) {
        this.instanceId = instanceId;
        this.throwns = throwns;
    }

}
