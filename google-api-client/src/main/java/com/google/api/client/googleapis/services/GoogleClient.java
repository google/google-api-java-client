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
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpMethod;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpClient;
import com.google.api.client.http.json.JsonHttpRequest;
import com.google.api.client.http.json.JsonHttpRequestInitializer;
import com.google.api.client.json.JsonFactory;

import java.io.IOException;

/**
 * Google API client.
 *
 * @since 1.6
 * @author Ravi Mistry
 */
public class GoogleClient extends JsonHttpClient {

  /**
   * Constructor with required parameters.
   *
   * <p>
   * Use {@link #builder(HttpTransport, JsonFactory, GenericUrl)} if you need to specify any of the
   * optional parameters.
   * </p>
   *
   * @param transport The transport to use for requests
   * @param jsonFactory A factory for creating JSON parsers and serializers
   * @param baseUrl The base URL of the service. Must end with a "/"
   */
  public GoogleClient(HttpTransport transport, JsonFactory jsonFactory, String baseUrl) {
    super(transport, jsonFactory, baseUrl);
  }

  /**
   * Construct the {@link GoogleClient}.
   *
   * @param transport The transport to use for requests
   * @param jsonHttpRequestInitializer The initializer to use when creating an
   *        {@link JsonHttpRequest} or {@code null} for none
   * @param httpRequestInitializer The initializer to use when creating an {@link HttpRequest} or
   *        {@code null} for none
   * @param jsonFactory A factory for creating JSON parsers and serializers
   * @param baseUrl The base URL of the service. Must end with a "/"
   * @param applicationName The application name to be sent in the User-Agent header of requests or
   *        {@code null} for none
   */
  protected GoogleClient(HttpTransport transport,
      JsonHttpRequestInitializer jsonHttpRequestInitializer,
      HttpRequestInitializer httpRequestInitializer, JsonFactory jsonFactory, String baseUrl,
      String applicationName) {
    super(transport, jsonHttpRequestInitializer, httpRequestInitializer, jsonFactory, baseUrl,
        applicationName);
  }

  /**
   * Create an {@link HttpRequest} suitable for use against this service.
   *
   * @param method HTTP Method type
   * @param url The complete URL of the service where requests should be sent. It includes the base
   *        path along with the URI template
   * @param body A POJO that can be serialized into JSON or {@code null} for none
   * @return newly created {@link HttpRequest}
   */
  @Override
  protected HttpRequest buildHttpRequest(HttpMethod method, GenericUrl url, Object body)
      throws IOException {
    HttpRequest httpRequest = super.buildHttpRequest(method, url, body);
    new MethodOverride().intercept(httpRequest);
    // Google servers will fail to process a POST/PUT/PATCH unless the Content-Length header >= 1
    httpRequest.setAllowEmptyContent(false);
    if (body != null) {
      httpRequest.setEnableGZipContent(true);
    }
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

  @Override
  protected HttpResponse executeUnparsed(HttpMethod method, GenericUrl url, Object body)
      throws IOException {
    HttpRequest request = buildHttpRequest(method, url, body);
    return GoogleJsonResponseException.execute(getJsonFactory(), request);
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
      return new GoogleClient(getTransport(), getJsonHttpRequestInitializer(),
          getHttpRequestInitializer(), getJsonFactory(), getBaseUrl().build(),
          getApplicationName());
    }
  }
}
