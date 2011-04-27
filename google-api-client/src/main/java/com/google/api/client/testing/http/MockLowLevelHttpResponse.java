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

package com.google.api.client.testing.http;

import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.util.Strings;
import com.google.common.collect.Lists;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Mock for {@link LowLevelHttpResponse}.
 *
 * @author Yaniv Inbar
 * @since 1.3
 */
public class MockLowLevelHttpResponse extends LowLevelHttpResponse {

  /** Input stream content of HTTP response or {@code null} by default. */
  public InputStream content;

  /** Content type of HTTP response or {@code null} by default. */
  public String contentType;

  /** Status code of HTTP response or {@code 200} by default. */
  public int statusCode = 200;

  /** List of header names of HTTP response (empty array list by default). */
  public ArrayList<String> headerNames = Lists.newArrayList();

  /** List of header values of HTTP response (empty array list by default). */
  public ArrayList<String> headerValues = Lists.newArrayList();

  /**
   * Adds a header to the response.
   *
   * @param name header name
   * @param value header value
   */
  public void addHeader(String name, String value) {
    headerNames.add(name);
    headerValues.add(value);
  }

  /**
   * Sets the response content to the given content string.
   *
   * @param stringContent content string
   */
  public void setContent(String stringContent) {
    content = new ByteArrayInputStream(Strings.toBytesUtf8(stringContent));
  }

  @Override
  public InputStream getContent() throws IOException {
    return content;
  }

  @Override
  public String getContentEncoding() {
    return null;
  }

  @Override
  public long getContentLength() {
    return 0;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public int getHeaderCount() {
    return headerNames.size();
  }

  @Override
  public String getHeaderName(int index) {
    return headerNames.get(index);
  }

  @Override
  public String getHeaderValue(int index) {
    return headerValues.get(index);
  }

  @Override
  public String getReasonPhrase() {
    return null;
  }

  @Override
  public int getStatusCode() {
    return statusCode;
  }

  @Override
  public String getStatusLine() {
    return null;
  }

}
