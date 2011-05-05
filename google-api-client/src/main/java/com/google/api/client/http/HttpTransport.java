/*
 * Copyright (c) 2010 Google Inc.
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

package com.google.api.client.http;

import com.google.api.client.util.ArrayMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Thread-safe abstract HTTP transport.
 *
 * <p>
 * The recommended concrete implementation HTTP transport library to use depends on what environment
 * you are running in:
 * </p>
 * <ul>
 * <li>Google App Engine: use {@code com.google.api.client.appengine.UrlFetchTransport}.
 * <ul>
 * <li>{@code com.google.api.client.apache.ApacheHttpTransport} doesn't work on App Engine because
 * the Apache HTTP Client opens its own sockets (though in theory there are ways to hack it to work
 * on App Engine that might work).</li>
 * <li>{@code com.google.api.client.javanet.NetHttpTransport} is discouraged due to a bug in the App
 * Engine SDK itself in how it parses HTTP headers in the response.</li>
 * </ul>
 * </li>
 * <li>Android:
 * <ul>
 * <li>Starting with SDK 2.3, strongly recommended to use {@code
 * com.google.api.client.javanet.NetHttpTransport}. Their Apache HTTP Client implementation is not
 * as well maintained.</li>
 * <li>For SDK 2.2 and earlier, use {@code com.google.api.client.apache.ApacheHttpTransport}. {@code
 * com.google.api.client.javanet.NetHttpTransport} is not recommended due to some bugs in the
 * Android SDK implementation of HttpURLConnection.</li>
 * </ul>
 * </li>
 * <li>Other Java environments
 * <ul>
 * <li>{@code com.google.api.client.javanet.NetHttpTransport} is based on the HttpURLConnection
 * built into the Java SDK, so it is normally the preferred choice.</li>
 * <li>{@code com.google.api.client.apache.ApacheHttpTransport} is a good choice for users of the
 * Apache HTTP Client, especially if you need some of the configuration options available in that
 * library.</li>
 * </ul>
 * </li>
 * </ul>
 * <p>
 * Upgrade warning: users of prior version 1.3 can continue to use the deprecated fields and methods
 * in this class. However, that invalidates the thread safety claims on this class, thus making this
 * class potentially unsafe to share between threads.
 * </p>
 *
 * @since 1.0
 * @author Yaniv Inbar
 */
public abstract class HttpTransport {

  static final Logger LOGGER = Logger.getLogger(HttpTransport.class.getName());

  /**
   * Default HTTP headers. These transport default headers are put into a request's headers when its
   * build method is called.
   *
   * @deprecated (scheduled to be removed in 1.5) Use {@link HttpRequest#headers} in an
   *             {@link HttpRequestInitializer}
   */
  @Deprecated
  public HttpHeaders defaultHeaders = new HttpHeaders();

  /** Map from content type to HTTP parser. */
  @Deprecated
  final ArrayMap<String, HttpParser> contentTypeToParserMap = ArrayMap.create();

  /**
   * HTTP request execute intercepters. The intercepters will be invoked in the order of the
   * {@link List#iterator()}.
   *
   * @deprecated (scheduled to be removed in 1.5) Use {@link HttpRequest#interceptor}
   */
  @Deprecated
  public List<HttpExecuteIntercepter> intercepters = new ArrayList<HttpExecuteIntercepter>(1);

  /**
   * Returns a new instance of an HTTP request factory based on this HTTP transport.
   *
   * @return new instance of an HTTP request factory
   * @since 1.4
   */
  public final HttpRequestFactory createRequestFactory() {
    return createRequestFactory(null);
  }

  /**
   * Returns a new instance of an HTTP request factory based on this HTTP transport with the given
   * HTTP request initializer.
   *
   * @param initializer HTTP request initializer or {@code null} for none
   * @return new instance of an HTTP request factory
   * @since 1.4
   */
  public final HttpRequestFactory createRequestFactory(HttpRequestInitializer initializer) {
    return new HttpRequestFactory(this, initializer);
  }

  /**
   * Adds an HTTP response content parser.
   * <p>
   * If there is already a previous parser defined for this new parser (as defined by
   * {@link #getParser(String)} then the previous parser will be removed.
   *
   * @deprecated (scheduled to be removed in 1.5) Use {@link HttpRequest#addParser(HttpParser)} in
   *             an {@link HttpRequestInitializer}
   */
  @Deprecated
  public final void addParser(HttpParser parser) {
    String contentType = getNormalizedContentType(parser.getContentType());
    contentTypeToParserMap.put(contentType, parser);
  }

  /**
   * Returns the HTTP response content parser to use for the given content type or {@code null} if
   * none is defined.
   *
   * @param contentType content type or {@code null} for {@code null} result
   * @deprecated (scheduled to be removed in 1.5) Use {@link HttpRequest#getParser(String)} in an
   *             {@link HttpRequestInitializer}
   */
  @Deprecated
  public final HttpParser getParser(String contentType) {
    if (contentType == null) {
      return null;
    }
    contentType = getNormalizedContentType(contentType);
    return contentTypeToParserMap.get(contentType);
  }

  @Deprecated
  private String getNormalizedContentType(String contentType) {
    int semicolon = contentType.indexOf(';');
    return semicolon == -1 ? contentType : contentType.substring(0, semicolon);
  }

  /**
   * Builds a request without specifying the HTTP method.
   *
   * @return new HTTP request
   * @deprecated (scheduled to be made package private in 1.5) Use
   *             {@link HttpRequestFactory#buildRequest(HttpMethod, GenericUrl, HttpContent)}
   */
  @Deprecated
  public final HttpRequest buildRequest() {
    return new HttpRequest(this, null);
  }

  /**
   * Builds a {@code DELETE} request.
   *
   * @deprecated (scheduled to be removed in 1.5) Use
   *             {@link HttpRequestFactory#buildDeleteRequest(GenericUrl)}
   */
  @Deprecated
  public final HttpRequest buildDeleteRequest() {
    return new HttpRequest(this, HttpMethod.DELETE);
  }

  /**
   * Builds a {@code GET} request.
   *
   * @deprecated (scheduled to be removed in 1.5) Use
   *             {@link HttpRequestFactory#buildGetRequest(GenericUrl)}
   */
  @Deprecated
  public final HttpRequest buildGetRequest() {
    return new HttpRequest(this, HttpMethod.GET);
  }

  /**
   * Builds a {@code POST} request.
   *
   * @deprecated (scheduled to be removed in 1.5) Use
   *             {@link HttpRequestFactory#buildPostRequest(GenericUrl, HttpContent)}
   */
  @Deprecated
  public final HttpRequest buildPostRequest() {
    return new HttpRequest(this, HttpMethod.POST);
  }

  /**
   * Builds a {@code PUT} request.
   *
   * @deprecated (scheduled to be removed in 1.5) Use
   *             {@link HttpRequestFactory#buildPutRequest(GenericUrl, HttpContent)}
   */
  @Deprecated
  public final HttpRequest buildPutRequest() {
    return new HttpRequest(this, HttpMethod.PUT);
  }

  /**
   * Builds a {@code PATCH} request.
   *
   * @deprecated (scheduled to be removed in 1.5) Use
   *             {@link HttpRequestFactory#buildPatchRequest(GenericUrl, HttpContent)}
   */
  @Deprecated
  public final HttpRequest buildPatchRequest() {
    return new HttpRequest(this, HttpMethod.PATCH);
  }

  /**
   * Builds a {@code HEAD} request.
   *
   * @deprecated (scheduled to be removed in 1.5) Use
   *             {@link HttpRequestFactory#buildHeadRequest(GenericUrl)}
   */
  @Deprecated
  public final HttpRequest buildHeadRequest() {
    return new HttpRequest(this, HttpMethod.HEAD);
  }

  /**
   * Removes HTTP request execute intercepters of the given class or subclasses.
   *
   * @param intercepterClass intercepter class
   * @deprecated (scheduled to be removed in 1.5) Use {@link HttpRequest#interceptor}
   */
  @Deprecated
  public final void removeIntercepters(Class<?> intercepterClass) {
    Iterator<HttpExecuteIntercepter> iterable = intercepters.iterator();
    while (iterable.hasNext()) {
      HttpExecuteIntercepter intercepter = iterable.next();
      if (intercepterClass.isAssignableFrom(intercepter.getClass())) {
        iterable.remove();
      }
    }
  }

  /**
   * Returns whether this HTTP transport implementation supports the {@code HEAD} request method.
   * <p>
   * Default implementation returns {@code false}.
   * </p>
   *
   * @since 1.3
   */
  public boolean supportsHead() {
    return false;
  }

  /**
   * Returns whether this HTTP transport implementation supports the {@code PATCH} request method.
   * <p>
   * Default implementation returns {@code false}.
   * </p>
   *
   * @since 1.3
   */
  public boolean supportsPatch() {
    return false;
  }

  /**
   * Builds a {@code DELETE} request.
   *
   * @param url URL
   * @throws IOException I/O exception
   * @since 1.3
   */
  protected abstract LowLevelHttpRequest buildDeleteRequest(String url) throws IOException;

  /**
   * Builds a {@code GET} request.
   *
   * @param url URL
   * @throws IOException I/O exception
   * @since 1.3
   */
  protected abstract LowLevelHttpRequest buildGetRequest(String url) throws IOException;

  /**
   * Builds a {@code HEAD} request. Won't be called if {@link #supportsHead()} returns {@code false}
   * .
   * <p>
   * Default implementation throws an {@link UnsupportedOperationException}.
   *
   * @param url URL
   * @throws IOException I/O exception
   * @since 1.3
   */
  protected LowLevelHttpRequest buildHeadRequest(String url) throws IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * Builds a {@code PATCH} request. Won't be called if {@link #supportsPatch()} returns {@code
   * false}.
   * <p>
   * Default implementation throws an {@link UnsupportedOperationException}.
   *
   * @param url URL
   * @throws IOException I/O exception
   * @since 1.3
   */
  protected LowLevelHttpRequest buildPatchRequest(String url) throws IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * Builds a {@code POST} request.
   *
   * @param url URL
   * @throws IOException I/O exception
   * @since 1.3
   */
  protected abstract LowLevelHttpRequest buildPostRequest(String url) throws IOException;

  /**
   * Builds a {@code PUT} request.
   *
   * @param url URL
   * @throws IOException I/O exception
   * @since 1.3
   */
  protected abstract LowLevelHttpRequest buildPutRequest(String url) throws IOException;

  /**
   * Default implementation does nothing, but subclasses may override to possibly release allocated
   * system resources or close connections.
   *
   * @throws IOException I/O exception
   * @since 1.4
   */
  public void shutdown() throws IOException {
  }
}
