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
package com.jivesoftware.os.routing.bird.deployable;

import org.merlin.config.Config;
import org.merlin.config.defaults.BooleanDefault;
import org.merlin.config.defaults.Default;
import org.merlin.config.defaults.IntDefault;

public interface InstanceConfig extends Config {

    @Default("defaultDatacenter")
    String getDatacenter();

    @Default("defaultRack")
    String getRack();

    @Default("localhost")
    String getHost();

    @Default("localhost")
    String getRoutesHost();

    void setRoutesHost(String host);

    @IntDefault(-1)
    Integer getRoutesPort();

    void setRoutesPort(int port);

    @Default("/request/keyStorePassword")
    String getKeyStorePasswordsPath();

    void setPasswordsPath(String path);

    @Default("/session/exchangeAccessToken")
    String getSessionExchangePath();

    void setSessionExchangePath(String path);

    @Default("/session/validate")
    String getSessionValidatorPath();

    void setSessionValidatorPath(String path);

    @Default("/request/instance/publicKey")
    String getOauthValidatorPath();

    void setOauthValidatorPath(String path);

    @Default("/request/connections")
    String getRoutesPath();

    void setRoutesPath(String path);

    @Default("/connections/health")
    String getConnectionsHealth();

    void setConnectionsHealth(String path);

    @Default("-1")
    String getClusterKey();

    @Default("unspecified")
    String getClusterName();

    void setClusterName(String clusterName);

    @Default("-1")
    String getServiceKey();

    @Default("unspecified")
    String getServiceName();

    @Default("-1")
    String getReleaseGroupKey();

    @Default("unspecified")
    String getReleaseGroupName();

    @Default("unspecified")
    String getInstanceKey();

    void setInstanceKey(String instanceKey);

    @IntDefault(-1)
    Integer getInstanceName();

    void setInstanceName(Integer instanceName);

    @Default("unspecified")
    String getVersion();

    @IntDefault(10000)
    Integer getMainPort();

    @BooleanDefault(false)
    boolean getMainSslEnabled();

    @BooleanDefault(false)
    boolean getMainServiceAuthEnabled();

    @BooleanDefault(false)
    boolean getMainServiceAuthDryRun();

    @BooleanDefault(true)
    boolean getManageLoopback();

    @IntDefault(10001)
    Integer getManagePort();

    @BooleanDefault(false)
    boolean getManageSslEnabled();

    @BooleanDefault(false)
    boolean getManageServiceAuthEnabled();

    @BooleanDefault(false)
    boolean getManageServiceAuthDryRun();

    @IntDefault(1024)
    Integer getMainMaxThreads();

    @IntDefault(1024)
    Integer getManageMaxThreads();

    @IntDefault(10000)
    Integer getMainMaxQueuedRequests();

    @IntDefault(10000)
    Integer getManageMaxQueuedRequests();

}
