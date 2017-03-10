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

import com.jivesoftware.os.routing.bird.shared.HttpClientException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public interface HttpClient {

    HttpResponse get(String path, Map<String, String> headers) throws HttpClientException;

    HttpResponse delete(String path, Map<String, String> headers) throws HttpClientException;

    HttpResponse postBytes(String path, byte[] postBytes, Map<String, String> headers) throws HttpClientException;

    HttpResponse postJson(String path, String postJsonBody, Map<String, String> headers) throws HttpClientException;

    HttpStreamResponse streamingPost(String path, String postJsonBody, Map<String, String> headers) throws HttpClientException;

    HttpStreamResponse streamingPostStreamableRequest(String path, StreamableRequest streamableRequest, Map<String, String> headers) throws
        HttpClientException;

    HttpResponse postStreamableRequest(String path, StreamableRequest streamableRequest, Map<String, String> headers) throws HttpClientException;

    void close();

    public static interface StreamableRequest {

        void writeRequest(OutputStream out) throws IOException;

    }

}
