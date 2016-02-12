/*
 * Copyright 2016 The Johns Hopkins University Applied Physics Laboratory LLC
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.jhuapl.dorset.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import edu.jhuapl.dorset.http.HttpClient;

/**
 * Agent wrapper for remote web services that implement the agent API
 *
 */
public class RemoteAgent extends AgentBase {
    private static final String REQUEST_ENDPOINT = "request";
    private static final String PING_ENDPOINT = "ping";
    private static final String PING_RESPONSE = "pong";

    private final Logger logger = LoggerFactory.getLogger(RemoteAgent.class);

    private String urlBase;
    private String requestUrl;
    private String pingUrl;
    private HttpClient client;
    private Gson gson;

    /**
     * Create a remote agent object
     * @param urlBase the base URL for the web services
     * @param client the http client
     */
    public RemoteAgent(String urlBase, HttpClient client) {
        // make sure end of url has slash
        this.urlBase = urlBase.replaceAll("/$", "") + "/";
        this.client = client;
        setUrls();
        gson = new Gson();
    }

    @Override
    public AgentResponse process(AgentRequest request) {
        AgentResponse response = new AgentResponse(AgentMessages.NO_RESPONSE);
        String json = gson.toJson(request);
        String resp = client.post(requestUrl, json, HttpClient.APPLICATION_JSON);
        if (resp != null) {
            try {
                response = gson.fromJson(resp, AgentResponse.class);
                // the gson deserialization code is very permissive so we verify
                if (response.getStatusCode() == AgentMessages.SUCCESS
                                && response.getText() == null) {
                    response.setStatusCode(AgentMessages.INVALID_RESPONSE);
                    logger.warn("Invalid json for request: " + resp);
                }
            } catch (JsonSyntaxException e) {
                response = new AgentResponse(AgentMessages.INVALID_RESPONSE);
                logger.warn("Invalid json for request: " + resp);
            }
        }
        return response;
    }

    /**
     * Is the remote agent available?
     * @return boolean
     */
    public boolean ping() {
        String resp = client.get(pingUrl);
        if (resp != null) {
            try {
                String text = gson.fromJson(resp, String.class);
                return text.equals(PING_RESPONSE);
            } catch (JsonSyntaxException e) {
                logger.warn("Invalid json for ping response: " + resp);
            }
        }
        return false;
    }

    /**
     * Get the URL base
     * @return base URL
     */
    public String getUrlBase() {
        return urlBase;
    }

    private void setUrls() {
        requestUrl = urlBase + REQUEST_ENDPOINT;
        pingUrl = urlBase + PING_ENDPOINT;
    }
}