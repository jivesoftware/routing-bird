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
import com.jivesoftware.os.routing.bird.shared.NextClientStrategy;
import com.jivesoftware.os.routing.bird.shared.TenantRoutingClient;

public class TenantRoutingHttpClient<T> implements TenantAwareHttpClient<T> {

    private final TenantRoutingClient<T, HttpClient, HttpClientException> tenantRoutingClient;

    public TenantRoutingHttpClient(TenantRoutingClient<T, HttpClient, HttpClientException> tenantRoutingClient) {
        this.tenantRoutingClient = tenantRoutingClient;
    }

    @Override
    public HttpResponse call(T tenant,
        NextClientStrategy strategy,
        ClientCall<HttpClient, HttpResponse, HttpClientException> clientCall)
        throws HttpClientException {
        return tenantRoutingClient.tenantAwareCall(tenant, strategy, clientCall);
    }
}
