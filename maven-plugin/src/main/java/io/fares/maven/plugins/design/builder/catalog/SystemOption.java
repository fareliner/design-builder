/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
 * <system>
 *   <systemId>urn:something</systemId>
 *   <appendSchemaFile>true</appendSchemaFile>
 *   <appendSeparator>:</appendSeparator>
 *   <uriPrefix>classpath:/META-INF/types</uriPrefix>
 * </system>
 * }
 * </pre>
 * <p>
 * By default the schema targetNamespace is used as the {@link #systemId} prefix, but this can be overridden by setting the systemId.
 * If the schema has no targetNamespace (e.g. chameleon schema), the {@link #defaultSystemId} can be used to set a default value.
 *
 * @see <a href="https://www.oasis-open.org/committees/download.php/14809/xml-catalogs.html#s.system">XML Catalogs OASIS Standard - system Element</a>
 */
public class SystemOption extends AbstractSystemIdOption {

  /**
   * Set a default systemId for schema files that do not have a targetNamespace, such as chameleon schema.
   */
  private String defaultSystemId;


  public String getDefaultSystemId() {
    return defaultSystemId;
  }

  public void setDefaultSystemId(String defaultSystemId) {
    this.defaultSystemId = defaultSystemId;
  }

  @Override
  String getDefaultEntityId() {
    return getDefaultSystemId();
  }

}
