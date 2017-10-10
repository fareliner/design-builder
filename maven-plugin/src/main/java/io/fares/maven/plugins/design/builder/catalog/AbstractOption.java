/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.fares.maven.plugins.design.builder.catalog;

public abstract class AbstractOption {

  /**
   * Will prepend the value of this property to the schema file name using the forward slash separator.
   * <p>
   * Example:
   * <pre>
   * {@code
   * <system>
   *   <uriPrefix>classpath:/META-INF/types</uriPrefix>
   * </system>
   * }
   * </pre>
   * will generate a system catalog entry like this:
   * <pre>
   * {@code
   * <system systemId="urn:types:GlobalDataTypes.xsd" uri="classpath:/META-INF/types/GlobalDataTypes.xsd"/>
   * }
   * </pre>
   */
  private String uriPrefix;

  /**
   * Use to override the scheme separator that will be used as the separator between the entityId (e.g. publicId) and the
   * schema file name if the {@link #appendSchemaFile} property is set to true.
   */
  private String appendSeparator;

  /**
   * This property will drive how the entityId (e.g. publicId) value is generated. If true the name of the schema will be appended
   * to the entityId using either the separator in {@link #appendSeparator} if specified or a calculated value that best suites
   * scheme of the entityId.
   */
  private boolean appendSchemaFile = false;

  public String getUriPrefix() {
    return uriPrefix;
  }

  public void setUriPrefix(String uriPrefix) {
    this.uriPrefix = uriPrefix;
  }

  public String getAppendSeparator() {
    return appendSeparator;
  }

  public void setAppendSeparator(String appendSeparator) {
    this.appendSeparator = appendSeparator;
  }

  public boolean isAppendSchemaFile() {
    return appendSchemaFile;
  }

  public void setAppendSchemaFile(boolean append) {
    this.appendSchemaFile = append;
  }

  abstract String getEntityId();

  abstract String getDefaultEntityId();


}
