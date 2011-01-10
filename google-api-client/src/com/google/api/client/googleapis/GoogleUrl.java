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

package com.google.api.client.googleapis;

import com.google.api.client.escape.CharEscapers;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.api.client.util.DataUtil;
import com.google.api.client.util.Key;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic Google URL providing for some common query parameters used in Google API's such as the
 * {@link #alt} and {@link #fields} parameters.
 *
 * @since 1.0
 * @author Yaniv Inbar
 */
public class GoogleUrl extends GenericUrl {

  /** Whether to pretty print the output. */
  @Key
  public Boolean prettyprint;

  /** Alternate wire format. */
  @Key
  public String alt;

  /** Partial fields mask. */
  @Key
  public String fields;

  public GoogleUrl() {
  }

  /**
   * @param encodedUrl encoded URL, including any existing query parameters that should be parsed
   */
  public GoogleUrl(String encodedUrl) {
    super(encodedUrl);
  }

  @Override
  public GoogleUrl clone() {
    return (GoogleUrl) super.clone();
  }

  /**
   * @param encodedServerUrl encoded URL of the server
   * @param pathTemplate path template
   * @param parameters an object with parameters designated by Key annotations
   * @throws IllegalArgumentException if a requested element in the pathTemplate is not
   *   in the parameters
   *
   * @since 1.3
   */
  public GoogleUrl(String encodedUrl, String pathTemplate, Object parameters)
      throws IllegalArgumentException {
    super(encodedUrl);

    HashMap<String, String> requestMap = new HashMap<String, String>();
    for (Map.Entry<String, Object> entry : DataUtil.mapOf(parameters).entrySet()) {
      Object value = entry.getValue();
      if (value != null) {
        requestMap.put(entry.getKey(), value.toString());
      }
    }
    appendRawPath(expandUriTemplates(pathTemplate, requestMap));
    // all other parameters are assumed to be query parameters
    putAll(requestMap);
  }

  /**
   * Expands templates in a URI.
   *
   * @param pathUri Uri component.  It may contain one or more sequences of the form "{name}",
   *   where "name" must be a key in variableMap
   * @param variableMap map of request variable names to values.  Any names which are found in
   *   pathUri are removed from the map during processing
   * @return The expanded template
   * @throws IllegalArgumentException if a requested element in the pathUri is not in the
   *   variableMap
   * @since 1.3
   */
  protected static String expandUriTemplates(
      String pathUri, HashMap<String, String> variableMap)
      throws IllegalArgumentException {
    StringBuilder pathBuf = new StringBuilder();
    int cur = 0;
    int length = pathUri.length();
    while (cur < length) {
      int next = pathUri.indexOf('{', cur);
      if (next == -1) {
        pathBuf.append(pathUri.substring(cur));
        break;
      }
      pathBuf.append(pathUri.substring(cur, next));
      int close = pathUri.indexOf('}', next + 2);
      String varName = pathUri.substring(next + 1, close);
      cur = close + 1;
      String value = variableMap.remove(varName);
      Preconditions.checkArgument(value != null,
                                  "missing required path parameter: %s",
                                  varName);
      pathBuf.append(CharEscapers.escapeUriPath(value));
    }
    return pathBuf.toString();
  }
}
