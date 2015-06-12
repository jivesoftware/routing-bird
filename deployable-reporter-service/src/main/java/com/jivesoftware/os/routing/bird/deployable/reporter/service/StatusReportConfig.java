package com.jivesoftware.os.routing.bird.deployable.reporter.service;

import org.merlin.config.Config;
import org.merlin.config.defaults.LongDefault;

public interface StatusReportConfig extends Config {

    @LongDefault(10000L)
    public Long getAnnouceEveryNMills();
}
