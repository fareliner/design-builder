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

import org.codehaus.plexus.component.annotations.Requirement;

abstract class CatalogEntries {

  private SystemSuffixEntry systemSuffix;

  private RewriteSystemEntry rewriteSystem;

  private SystemEntry system;

  private UriEntry uri;

  @Requirement(hint = "public")
  private PublicEntry publicOption;

  public SystemSuffixEntry getSystemSuffix() {
    return systemSuffix;
  }

  public void setSystemSuffix(SystemSuffixEntry systemSuffix) {
    this.systemSuffix = systemSuffix;
  }

  public RewriteSystemEntry getRewriteSystem() {
    return rewriteSystem;
  }

  public void setRewriteSystem(RewriteSystemEntry rewriteSystem) {
    this.rewriteSystem = rewriteSystem;
  }

  public SystemEntry getSystem() {
    return system;
  }

  public void setSystem(SystemEntry system) {
    this.system = system;
  }

  public UriEntry getUri() {
    return uri;
  }

  public void setUri(UriEntry uri) {
    this.uri = uri;
  }

  public PublicEntry getPublic() {
    return publicOption;
  }

  public void setPublic(PublicEntry publicOption) {
    this.publicOption = publicOption;
  }

}
