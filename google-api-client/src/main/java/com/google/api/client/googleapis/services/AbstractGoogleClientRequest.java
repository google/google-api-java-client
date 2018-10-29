/*
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

import com.google.api.client.googleapis.GoogleUtils;
import com.google.api.client.googleapis.MethodOverride;
import com.google.api.client.googleapis.batch.BatchCallback;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.EmptyContent;
import com.google.api.client.http.GZipEncoding;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpMethods;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpResponseInterceptor;
import com.google.api.client.http.UriTemplate;
import com.google.api.client.util.GenericData;
import com.google.api.client.util.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract Google client request for a {@link AbstractGoogleClient}.
 *
 * <p>
 * Implementation is not thread-safe.
 * </p>
 *
 * @param <T> type of the response
 *
 * @since 1.12
 * @author Yaniv Inbar
 */
public abstract class AbstractGoogleClientRequest<T> extends GenericData {

  /**
   * User agent suffix for all requests.
   *
   * @since 1.20
   */
  public static final String USER_AGENT_SUFFIX = "Google-API-Java-Client";

  private static final String API_VERSION_HEADER = "X-Goog-Api-Client";

  /** Google client. */
  private final AbstractGoogleClient abstractGoogleClient;

  /** HTTP method. */
  private final String requestMethod;

  /** URI template for the path relative to the base URL. */
  private final String uriTemplate;

  /** HTTP content or {@code null} for none. */
  private final HttpContent httpContent;

  /** HTTP headers used for the Google client request. */
  private HttpHeaders requestHeaders = new HttpHeaders();

  /** HTTP headers of the last response or {@code null} before request has been executed. */
  private HttpHeaders lastResponseHeaders;

  /** Status code of the last response or {@code -1} before request has been executed. */
  private int lastStatusCode = -1;

  /** Status message of the last response or {@code null} before request has been executed. */
  private String lastStatusMessage;

  /** Whether to disable GZip compression of HTTP content. */
  private boolean disableGZipContent;

  /** Response class to parse into. */
  private Class<T> responseClass;

  /** Media HTTP uploader or {@code null} for none. */
  private MediaHttpUploader uploader;

  /** Media HTTP downloader or {@code null} for none. */
  private MediaHttpDownloader downloader;

  /**
   * @param abstractGoogleClient Google client
   * @param requestMethod HTTP Method
   * @param uriTemplate URI template for the path relative to the base URL. If it starts with a "/"
   *        the base path from the base URL will be stripped out. The URI template can also be a
   *        full URL. URI template expansion is done using
   *        {@link UriTemplate#expand(String, String, Object, boolean)}
   * @param httpContent HTTP content or {@code null} for none
   * @param responseClass response class to parse into
   */
  protected AbstractGoogleClientRequest(AbstractGoogleClient abstractGoogleClient,
      String requestMethod, String uriTemplate, HttpContent httpContent, Class<T> responseClass) {
    this.responseClass = Preconditions.checkNotNull(responseClass);
    this.abstractGoogleClient = Preconditions.checkNotNull(abstractGoogleClient);
    this.requestMethod = Preconditions.checkNotNull(requestMethod);
    this.uriTemplate = Preconditions.checkNotNull(uriTemplate);
    this.httpContent = httpContent;
    // application name
    String applicationName = abstractGoogleClient.getApplicationName();
    if (applicationName != null) {
      requestHeaders.setUserAgent(applicationName + " " + USER_AGENT_SUFFIX);
    } else {
      requestHeaders.setUserAgent(USER_AGENT_SUFFIX);
    }
    // Set the header for the Api Client version (Java and OS version)
    requestHeaders.set(API_VERSION_HEADER, ApiClientVersion.build(abstractGoogleClient));
  }

  /**
   * Internal class to help build the X-Goog-Api-Client header. This header identifies the
   * API Client version and environment.
   *
   * See <a href="https://cloud.google.com/apis/docs/system-parameters"></a>
   *
   */
  private static class ApiClientVersion {
    private static final String JAVA_VERSION = getJavaVersion();
    private static final String OS_NAME = formatName(System.getProperty("os.name"));
    private static final String OS_VERSION = formatSemver(System.getProperty("os.version"));
    private static final String HEADER_TEMPLATE = buildHeaderTemplate();

    private static String build(AbstractGoogleClient client) {
      // TODO(chingor): add the API version from the generated client
      return String.format(
          HEADER_TEMPLATE,
          formatName(client.getClass().getSimpleName()),
          formatSemver(GoogleUtils.VERSION)
      );
    }

    private static String getJavaVersion() {
      String version = System.getProperty("java.version");
      // Java 9 doesn't report a semver here: instead it's something like 9-Debian+0-x-y
      if (version.startsWith("9")) {
        return "9.0.0";
      } else {
        return formatSemver(version);
      }
    }

    private static String formatName(String name) {
      // Only lowercase letters, digits, and "-" are allowed
      return name.toLowerCase().replaceAll("[^\\w\\d\\-]", "-");
    }

    private static String formatSemver(String version) {
      if (version == null) {
        return null;
      }

      // Take only the semver version: x.y.z-a_b_c -> x.y.z
      Matcher m = Pattern.compile("(\\d+\\.\\d+\\.\\d+).*").matcher(version);
      if (m.find()) {
        return m.group(1);
      } else {
        return version;
      }
    }

    private static String buildHeaderTemplate() {
      StringBuilder sb = new StringBuilder("java/");
      sb.append(JAVA_VERSION);
      sb.append(" http-google-%s/%s");
      if (OS_NAME != null && OS_VERSION != null) {
        sb.append(" ");
        sb.append(OS_NAME);
        sb.append("/");
        sb.append(OS_VERSION);
      }
      return sb.toString();
    }
  }

  /** Returns whether to disable GZip compression of HTTP content. */
  public final boolean getDisableGZipContent() {
    return disableGZipContent;
  }

  /**
   * Sets whether to disable GZip compression of HTTP content.
   *
   * <p>
   * By default it is {@code false}.
   * </p>
   *
   * <p>
   * Overriding is only supported for the purpose of calling the super implementation and changing
   * the return type, but nothing else.
   * </p>
   */
  public AbstractGoogleClientRequest<T> setDisableGZipContent(boolean disableGZipContent) {
    this.disableGZipContent = disableGZipContent;
    return this;
  }

  /** Returns the HTTP method. */
  public final String getRequestMethod() {
    return requestMethod;
  }

  /** Returns the URI template for the path relative to the base URL. */
  public final String getUriTemplate() {
    return uriTemplate;
  }

  /** Returns the HTTP content or {@code null} for none. */
  public final HttpContent getHttpContent() {
    return httpContent;
  }

  /**
   * Returns the Google client.
   *
   * <p>
   * Overriding is only supported for the purpose of calling the super implementation and changing
   * the return type, but nothing else.
   * </p>
   */
  public AbstractGoogleClient getAbstractGoogleClient() {
    return abstractGoogleClient;
  }

  /** Returns the HTTP headers used for the Google client request. */
  public final HttpHeaders getRequestHeaders() {
    return requestHeaders;
  }

  /**
   * Sets the HTTP headers used for the Google client request.
   *
   * <p>
   * These headers are set on the request after {@link #buildHttpRequest} is called, this means that
   * {@link HttpRequestInitializer#initialize} is called first.
   * </p>
   *
   * <p>
   * Overriding is only supported for the purpose of calling the super implementation and changing
   * the return type, but nothing else.
   * </p>
   */
  public AbstractGoogleClientRequest<T> setRequestHeaders(HttpHeaders headers) {
    this.requestHeaders = headers;
    return this;
  }

  /**
   * Returns the HTTP headers of the last response or {@code null} before request has been executed.
   */
  public final HttpHeaders getLastResponseHeaders() {
    return lastResponseHeaders;
  }

  /**
   * Returns the status code of the last response or {@code -1} before request has been executed.
   */
  public final int getLastStatusCode() {
    return lastStatusCode;
  }

  /**
   * Returns the status message of the last response or {@code null} before request has been
   * executed.
   */
  public final String getLastStatusMessage() {
    return lastStatusMessage;
  }

  /** Returns the response class to parse into. */
  public final Class<T> getResponseClass() {
    return responseClass;
  }

  /** Returns the media HTTP Uploader or {@code null} for none. */
  public final MediaHttpUploader getMediaHttpUploader() {
    return uploader;
  }

  /**
   * Initializes the media HTTP uploader based on the media content.
   *
   * @param mediaContent media content
   */
  protected final void initializeMediaUpload(AbstractInputStreamContent mediaContent) {
    HttpRequestFactory requestFactory = abstractGoogleClient.getRequestFactory();
    this.uploader = new MediaHttpUploader(
        mediaContent, requestFactory.getTransport(), requestFactory.getInitializer());
    this.uploader.setInitiationRequestMethod(requestMethod);
    if (httpContent != null) {
      this.uploader.setMetadata(httpContent);
    }
  }

  /** Returns the media HTTP downloader or {@code null} for none. */
  public final MediaHttpDownloader getMediaHttpDownloader() {
    return downloader;
  }

  /** Initializes the media HTTP downloader. */
  protected final void initializeMediaDownload() {
    HttpRequestFactory requestFactory = abstractGoogleClient.getRequestFactory();
    this.downloader =
        new MediaHttpDownloader(requestFactory.getTransport(), requestFactory.getInitializer());
  }

  /**
   * Creates a new instance of {@link GenericUrl} suitable for use against this service.
   *
   * <p>
   * Subclasses may override by calling the super implementation.
   * </p>
   *
   * @return newly created {@link GenericUrl}
   */
  public GenericUrl buildHttpRequestUrl() {
    return new GenericUrl(
        UriTemplate.expand(abstractGoogleClient.getBaseUrl(), uriTemplate, this, true));
  }

  /**
   * Create a request suitable for use against this service.
   *
   * <p>
   * Subclasses may override by calling the super implementation.
   * </p>
   */
  public HttpRequest buildHttpRequest() throws IOException {
    return buildHttpRequest(false);
  }

  /**
   * Create a request suitable for use against this service, but using HEAD instead of GET.
   *
   * <p>
   * Only supported when the original request method is GET.
   * </p>
   *
   * <p>
   * Subclasses may override by calling the super implementation.
   * </p>
   */
  protected HttpRequest buildHttpRequestUsingHead() throws IOException {
    return buildHttpRequest(true);
  }

  /** Create a request suitable for use against this service. */
  private HttpRequest buildHttpRequest(boolean usingHead) throws IOException {
    Preconditions.checkArgument(uploader == null);
    Preconditions.checkArgument(!usingHead || requestMethod.equals(HttpMethods.GET));
    String requestMethodToUse = usingHead ? HttpMethods.HEAD : requestMethod;
    final HttpRequest httpRequest = getAbstractGoogleClient()
        .getRequestFactory().buildRequest(requestMethodToUse, buildHttpRequestUrl(), httpContent);
    new MethodOverride().intercept(httpRequest);
    httpRequest.setParser(getAbstractGoogleClient().getObjectParser());
    // custom methods may use POST with no content but require a Content-Length header
    if (httpContent == null && (requestMethod.equals(HttpMethods.POST)
        || requestMethod.equals(HttpMethods.PUT) || requestMethod.equals(HttpMethods.PATCH))) {
      httpRequest.setContent(new EmptyContent());
    }
    httpRequest.getHeaders().putAll(requestHeaders);
    if (!disableGZipContent) {
      httpRequest.setEncoding(new GZipEncoding());
    }
    final HttpResponseInterceptor responseInterceptor = httpRequest.getResponseInterceptor();
    httpRequest.setResponseInterceptor(new HttpResponseInterceptor() {

      public void interceptResponse(HttpResponse response) throws IOException {
        if (responseInterceptor != null) {
          responseInterceptor.interceptResponse(response);
        }
        if (!response.isSuccessStatusCode() && httpRequest.getThrowExceptionOnExecuteError()) {
          throw newExceptionOnError(response);
        }
      }
    });
    return httpRequest;
  }

  /**
   * Sends the metadata request to the server and returns the raw metadata {@link HttpResponse}.
   *
   * <p>
   * Callers are responsible for disconnecting the HTTP response by calling
   * {@link HttpResponse#disconnect}. Example usage:
   * </p>
   *
   * <pre>
     HttpResponse response = request.executeUnparsed();
     try {
       // process response..
     } finally {
       response.disconnect();
     }
   * </pre>
   *
   * <p>
   * Subclasses may override by calling the super implementation.
   * </p>
   *
   * @return the {@link HttpResponse}
   */
  public HttpResponse executeUnparsed() throws IOException {
    return executeUnparsed(false);
  }

  /**
   * Sends the media request to the server and returns the raw media {@link HttpResponse}.
   *
   * <p>
   * Callers are responsible for disconnecting the HTTP response by calling
   * {@link HttpResponse#disconnect}. Example usage:
   * </p>
   *
   * <pre>
     HttpResponse response = request.executeMedia();
     try {
       // process response..
     } finally {
       response.disconnect();
     }
   * </pre>
   *
   * <p>
   * Subclasses may override by calling the super implementation.
   * </p>
   *
   * @return the {@link HttpResponse}
   */
  protected HttpResponse executeMedia() throws IOException {
    set("alt", "media");
    return executeUnparsed();
  }

  /**
   * Sends the metadata request using HEAD to the server and returns the raw metadata
   * {@link HttpResponse} for the response headers.
   *
   * <p>
   * Only supported when the original request method is GET. The response content is assumed to be
   * empty and ignored. Calls {@link HttpResponse#ignore()} so there is no need to disconnect the
   * response. Example usage:
   * </p>
   *
   * <pre>
     HttpResponse response = request.executeUsingHead();
     // look at response.getHeaders()
   * </pre>
   *
   * <p>
   * Subclasses may override by calling the super implementation.
   * </p>
   *
   * @return the {@link HttpResponse}
   */
  protected HttpResponse executeUsingHead() throws IOException {
    Preconditions.checkArgument(uploader == null);
    HttpResponse response = executeUnparsed(true);
    response.ignore();
    return response;
  }

  /**
   * Sends the metadata request using the given request method to the server and returns the raw
   * metadata {@link HttpResponse}.
   */
  private HttpResponse executeUnparsed(boolean usingHead) throws IOException {
    HttpResponse response;
    if (uploader == null) {
      // normal request (not upload)
      response = buildHttpRequest(usingHead).execute();
    } else {
      // upload request
      GenericUrl httpRequestUrl = buildHttpRequestUrl();
      HttpRequest httpRequest = getAbstractGoogleClient()
          .getRequestFactory().buildRequest(requestMethod, httpRequestUrl, httpContent);
      boolean throwExceptionOnExecuteError = httpRequest.getThrowExceptionOnExecuteError();

      response = uploader.setInitiationHeaders(requestHeaders)
          .setDisableGZipContent(disableGZipContent).upload(httpRequestUrl);
      response.getRequest().setParser(getAbstractGoogleClient().getObjectParser());
      // process any error
      if (throwExceptionOnExecuteError && !response.isSuccessStatusCode()) {
        throw newExceptionOnError(response);
      }
    }
    // process response
    lastResponseHeaders = response.getHeaders();
    lastStatusCode = response.getStatusCode();
    lastStatusMessage = response.getStatusMessage();
    return response;
  }

  /**
   * Returns the exception to throw on an HTTP error response as defined by
   * {@link HttpResponse#isSuccessStatusCode()}.
   *
   * <p>
   * It is guaranteed that {@link HttpResponse#isSuccessStatusCode()} is {@code false}. Default
   * implementation is to call {@link HttpResponseException#HttpResponseException(HttpResponse)},
   * but subclasses may override.
   * </p>
   *
   * @param response HTTP response
   * @return exception to throw
   */
  protected IOException newExceptionOnError(HttpResponse response) {
    return new HttpResponseException(response);
  }

  /**
   * Sends the metadata request to the server and returns the parsed metadata response.
   *
   * <p>
   * Subclasses may override by calling the super implementation.
   * </p>
   *
   * @return parsed HTTP response
   */
  public T execute() throws IOException {
    return executeUnparsed().parseAs(responseClass);
  }

  /**
   * Sends the metadata request to the server and returns the metadata content input stream of
   * {@link HttpResponse}.
   *
   * <p>
   * Callers are responsible for closing the input stream after it is processed. Example sample:
   * </p>
   *
   * <pre>
     InputStream is = request.executeAsInputStream();
     try {
       // Process input stream..
     } finally {
       is.close();
     }
   * </pre>
   *
   * <p>
   * Subclasses may override by calling the super implementation.
   * </p>
   *
   * @return input stream of the response content
   */
  public InputStream executeAsInputStream() throws IOException {
    return executeUnparsed().getContent();
  }

  /**
   * Sends the media request to the server and returns the media content input stream of
   * {@link HttpResponse}.
   *
   * <p>
   * Callers are responsible for closing the input stream after it is processed. Example sample:
   * </p>
   *
   * <pre>
     InputStream is = request.executeMediaAsInputStream();
     try {
       // Process input stream..
     } finally {
       is.close();
     }
   * </pre>
   *
   * <p>
   * Subclasses may override by calling the super implementation.
   * </p>
   *
   * @return input stream of the response content
   */
  protected InputStream executeMediaAsInputStream() throws IOException {
    return executeMedia().getContent();
  }

  /**
   * Sends the metadata request to the server and writes the metadata content input stream of
   * {@link HttpResponse} into the given destination output stream.
   *
   * <p>
   * This method closes the content of the HTTP response from {@link HttpResponse#getContent()}.
   * </p>
   *
   * <p>
   * Subclasses may override by calling the super implementation.
   * </p>
   *
   * @param outputStream destination output stream
   */
  public void executeAndDownloadTo(OutputStream outputStream) throws IOException {
    executeUnparsed().download(outputStream);
  }

  /**
   * Sends the media request to the server and writes the media content input stream of
   * {@link HttpResponse} into the given destination output stream.
   *
   * <p>
   * This method closes the content of the HTTP response from {@link HttpResponse#getContent()}.
   * </p>
   *
   * <p>
   * Subclasses may override by calling the super implementation.
   * </p>
   *
   * @param outputStream destination output stream
   */
  protected void executeMediaAndDownloadTo(OutputStream outputStream) throws IOException {
    if (downloader == null) {
      executeMedia().download(outputStream);
    } else {
      downloader.download(buildHttpRequestUrl(), requestHeaders, outputStream);
    }
  }

  /**
   * Queues the request into the specified batch request container using the specified error class.
   *
   * <p>
   * Batched requests are then executed when {@link BatchRequest#execute()} is called.
   * </p>
   *
   * @param batchRequest batch request container
   * @param errorClass data class the unsuccessful response will be parsed into or
   *        {@code Void.class} to ignore the content
   * @param callback batch callback
   */
  public final <E> void queue(
      BatchRequest batchRequest, Class<E> errorClass, BatchCallback<T, E> callback)
      throws IOException {
    Preconditions.checkArgument(uploader == null, "Batching media requests is not supported");
    batchRequest.queue(buildHttpRequest(), getResponseClass(), errorClass, callback);
  }

  // @SuppressWarnings was added here because this is generic class.
  // see: http://stackoverflow.com/questions/4169806/java-casting-object-to-a-generic-type and
  // http://www.angelikalanger.com/GenericsFAQ/FAQSections/TechnicalDetails.html#Type%20Erasure
  // for more details
  @SuppressWarnings("unchecked")
  @Override
  public AbstractGoogleClientRequest<T> set(String fieldName, Object value) {
    return (AbstractGoogleClientRequest<T>) super.set(fieldName, value);
  }

  /**
   * Ensures that the specified required parameter is not null or
   * {@link AbstractGoogleClient#getSuppressRequiredParameterChecks()} is true.
   *
   * @param value the value of the required parameter
   * @param name the name of the required parameter
   * @throws IllegalArgumentException if the specified required parameter is null and
   *         {@link AbstractGoogleClient#getSuppressRequiredParameterChecks()} is false
   * @since 1.14
   */
  protected final void checkRequiredParameter(Object value, String name) {
    Preconditions.checkArgument(
        abstractGoogleClient.getSuppressRequiredParameterChecks() || value != null,
        "Required parameter %s must be specified", name);
  }
}
