/*
 * Copyright (c) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.client.googleapis.services;

import com.google.api.client.googleapis.MethodOverride;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpMethod;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpClient;
import com.google.api.client.http.json.RemoteRequest;
import com.google.api.client.http.json.RemoteRequestInitializer;
import com.google.api.client.json.JsonFactory;

import java.io.IOException;

/**
 * Google API client.
 *
 * @since 1.6
 * @author Ravi Mistry
 */
public class GoogleClient extends JsonHttpClient {

  /** {@link MethodOverride} to ensure that methods get overridden correctly. */
  private final MethodOverride methodOverride = new MethodOverride();

  /**
   * Construct the {@link GoogleClient}.
   *
   * @param transport The transport to use for requests
   * @param remoteRequestInitializer The initializer to use when creating an {@link RemoteRequest}
   *        or {@code null} for none
   * @param httpRequestInitializer The initializer to use when creating an {@link HttpRequest} or
   *        {@code null} for none
   * @param jsonFactory A factory for creating JSON parsers and serializers
   * @param baseUrl The base URL of the service. Must end with a "/"
   * @param applicationName The application name to be sent in the User-Agent header of requests or
   *        {@code null} for none
   */
  protected GoogleClient(
      HttpTransport transport,
      RemoteRequestInitializer remoteRequestInitializer,
      HttpRequestInitializer httpRequestInitializer,
      JsonFactory jsonFactory,
      String baseUrl,
      String applicationName) {
    super(transport, remoteRequestInitializer, httpRequestInitializer, jsonFactory, baseUrl,
        applicationName);
  }

  /**
   * Create an {@link HttpRequest} suitable for use against this service.
   *
   * @param method HTTP Method type
   * @param uriTemplate URI template for the path relative to the base URL. Must not start with
   *        a "/"
   * @param remoteRequest Remote Request type
   * @return newly created {@link HttpRequest}
   */
  @Override
  protected HttpRequest buildHttpRequest(
      HttpMethod method, String uriTemplate, RemoteRequest remoteRequest) throws IOException {
    HttpRequest httpRequest = super.buildHttpRequest(method, uriTemplate, remoteRequest);
    methodOverride.intercept(httpRequest);
    return httpRequest;
  }

  /**
   * Returns an instance of a new builder.
   *
   * @param transport The transport to use for requests
   * @param jsonFactory A factory for creating JSON parsers and serializers
   * @param baseUrl The base URL of the service. Must end with a "/"
   */
  public static Builder builder(
      HttpTransport transport, JsonFactory jsonFactory, GenericUrl baseUrl) {
    return new Builder(transport, jsonFactory, baseUrl);
  }

  /**
   * Builder for {@link GoogleClient}.
   *
   * <p>
   * Implementation is not thread-safe.
   * </p>
   *
   * @since 1.6
   */
  public static class Builder extends JsonHttpClient.Builder {

    /**
     * Returns an instance of a new builder.
     *
     * @param transport The transport to use for requests
     * @param jsonFactory A factory for creating JSON parsers and serializers
     * @param baseUrl The base URL of the service. Must end with a "/"
     */
    protected Builder(HttpTransport transport, JsonFactory jsonFactory, GenericUrl baseUrl) {
      super(transport, jsonFactory, baseUrl);
    }

    /** Builds a new instance of {@link GoogleClient}. */
    @Override
    public GoogleClient build() {
      return new GoogleClient(
          getTransport(),
          getRemoteRequestInitializer(),
          getHttpRequestInitializer(),
          getJsonFactory(),
          getBaseUrl().build(),
          getApplicationName());
    }
  }
}
