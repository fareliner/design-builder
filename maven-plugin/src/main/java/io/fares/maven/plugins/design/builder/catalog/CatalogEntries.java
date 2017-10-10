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

abstract class CatalogEntries {

  private SystemSuffixOption systemSuffix;

  private RewriteSystemOption rewriteSystem;

  private SystemOption system;

  private UriOption uri;

  private PublicOption publicOption;

  public SystemSuffixOption getSystemSuffix() {
    return systemSuffix;
  }

  public void setSystemSuffix(SystemSuffixOption systemSuffix) {
    this.systemSuffix = systemSuffix;
  }

  public RewriteSystemOption getRewriteSystem() {
    return rewriteSystem;
  }

  public void setRewriteSystem(RewriteSystemOption rewriteSystem) {
    this.rewriteSystem = rewriteSystem;
  }

  public SystemOption getSystem() {
    return system;
  }

  public void setSystem(SystemOption system) {
    this.system = system;
  }

  public UriOption getUri() {
    return uri;
  }

  public void setUri(UriOption uri) {
    this.uri = uri;
  }

  public PublicOption getPublic() {
    return publicOption;
  }

  public void setPublic(PublicOption publicOption) {
    this.publicOption = publicOption;
  }

}
