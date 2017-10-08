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

/**
 * The system configuration option can be used to generate a system catalog entry.
 * <p>
 * Example:
 * <pre>
 * {@code
 * <public>
 *   <publicId>scheme:something</publicId>
 *   <appendSeparator>|</appendSeparator>
 *   <appendSchemaFile>true</appendSchemaFile>
 * </public>
 * }
 * </pre>
 * <p>
 * The resulting public entry would return the following.
 * <p/>
 * <pre>
 * {@code
 * <public publicId="scheme:something|DataTypes.xsd" uri="DataTypes.xsd"/>
 * }
 * </pre>
 * <p>
 * By default the schema targetNamespace is used as the {@link #publicId} prefix. The id prefix can be overridden by the
 * {@link #publicId} property. If the targetNamespace of the resource is a urn, the {@link #appendSeparator} will automatically
 * default to <code>:</code> else it will be <code>/</code>.
 * If the schema has no targetNamespace (e.g. chameleon schema), the publicId will simply be the name of the resource as provided.
 *
 * @see <a href="https://www.oasis-open.org/committees/download.php/14809/xml-catalogs.html#s.public">XML Catalogs OASIS Standard - public Element</a>
 */
public class PublicEntry {

  static final char DEFAULT_SEPARATOR = '/';

  /**
   * The value used as the publicId. If null, the schema <code>targetNamespace</code> is used. Should the schema be a chameleon schema,
   * the mojo fill fail given it is impossible to create a publicId if no concrete value is provided.
   */
  private String publicId;

  /**
   * This character is appended to the {@link #publicId} if the {@link #appendSchemaFile} property is true.
   */
  private char appendSeparator = DEFAULT_SEPARATOR;

  /**
   * When this setting is true, the file name of the resource is appended to the {@link #publicId} element
   * using the {@link #appendSeparator}.
   */
  private boolean appendSchemaFile = false;

  public String getPublicId() {
    return publicId;
  }

  public void setPublicId(String publicId) {
    this.publicId = publicId;
  }

  public char getAppendSeparator() {
    return appendSeparator;
  }

  public void setAppendSeparator(char separator) {
    this.appendSeparator = separator;
  }

  public boolean isAppendSchemaFile() {
    return appendSchemaFile;
  }

  public void setAppendSchemaFile(boolean append) {
    this.appendSchemaFile = append;
  }

}
