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
package com.jivesoftware.os.routing.bird.hello.routing.bird.service;

import com.jivesoftware.os.routing.bird.http.client.TenantAwareHttpClient;
import org.merlin.config.Config;
import org.merlin.config.defaults.StringDefault;

public class HelloRoutingBirdServiceInitializer {

    public interface HelloRoutingBirdServiceConfig extends Config {

        @StringDefault("Hello World")
        public String getGreeting();
    }

    public HelloRoutingBirdService initialize(HelloRoutingBirdServiceConfig config,
            TenantAwareHttpClient<String> client) {
        return new HelloRoutingBirdService(config.getGreeting(), client);
    }
}