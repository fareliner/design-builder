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

package io.fares.maven.plugins.design.builder.flattener;

import static java.text.MessageFormat.format;

import io.fares.maven.plugins.utils.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.artifact.Artifact;

/**
 * A {@link DependencyResource} represents a resource within a maven
 * {@link Dependency}. As an extension of {@link Dependency}, this class also
 * contains the resource location url within the actual {@link Dependency}
 * itself.
 * <p>
 * Example:
 * <p>
 * <code>io.fares.some:bucket:jar::1.0.2!/META-INF/catalog.xml</code> <br>
 * </p>
 * will reflect the dependency
 * <p>
 * <code>
 * &lt;dependency&gt;<br>
 * &nbsp;&nbsp;&lt;groupId&gt;io.fares.some&lt;/groupId&gt;<br>
 * &nbsp;&nbsp;&lt;artifactId&gt;bucket&lt;/artifactId&gt;<br>
 * &nbsp;&nbsp;&lt;version&gt;1.0.2&lt;/version&gt;<br>
 * &nbsp;&nbsp;&lt;type&gt;jar&lt;/type&gt;<br>
 * &lt;/dependency&gt;
 * </code>
 * </p>
 * and the resource within this dependency located at
 * <code>/META-INF/catalog.xml</code>
 */
public class DependencyResource extends Dependency {

  private static final long serialVersionUID = -7680130645800522100L;
  private String resource;

  public DependencyResource() {
    setScope(Artifact.SCOPE_RUNTIME);
  }

  public static DependencyResource valueOf(String value) throws IllegalArgumentException {

    final String resourceDelimiter = "!/";
    final int resourceDelimiterPosition = value.indexOf(resourceDelimiter);

    final String dependencyPart;
    final String resource;
    if (resourceDelimiterPosition == -1) {
      dependencyPart = value;
      resource = "";
    } else {
      dependencyPart = value.substring(0, resourceDelimiterPosition);
      resource = value.substring(resourceDelimiterPosition + resourceDelimiter.length());
    }

    final String[] dependencyParts = StringUtils.split(dependencyPart, ':', true);

    if (dependencyParts.length < 2) {
      throw new IllegalArgumentException(
        format("Error parsing dependency descriptor [{0}], both groupId and artifactId must be specified.",
          dependencyPart));
    }

    if (dependencyParts.length > 5) {
      throw new IllegalArgumentException(
        format("Error parsing dependency descriptor [{0}], it contains too many parts.", dependencyPart));
    }

    final String groupId = dependencyParts[0];
    final String artifactId = dependencyParts[1];
    final String version;

    final String type;

    if (dependencyParts.length > 2) {
      type = (dependencyParts[2] == null || dependencyParts[2].length() == 0) ? null : dependencyParts[2];
    } else {
      type = null;
    }

    final String classifier;

    if (dependencyParts.length > 3) {
      classifier = (dependencyParts[3] == null || dependencyParts[3].length() == 0) ? null : dependencyParts[3];
    } else {
      classifier = null;
    }

    if (dependencyParts.length > 4) {
      version = (dependencyParts[4] == null || dependencyParts[4].length() == 0) ? null : dependencyParts[4];
    } else {
      version = null;
    }

    final DependencyResource dependencyResource = new DependencyResource();

    dependencyResource.setGroupId(groupId);
    dependencyResource.setArtifactId(artifactId);
    if (version != null) {
      dependencyResource.setVersion(version);
    }
    if (type != null) {
      dependencyResource.setType(type);
    }
    if (classifier != null) {
      dependencyResource.setClassifier(classifier);
    }
    if (resource != null) {
      dependencyResource.setResource(resource);
    }
    return dependencyResource;
  }

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public String toString() {
    return format("Dependency[groupId={0}, artifactId={1}, version={2}, type={4}, classifier={5}, resource={6}]",
      getGroupId(), getArtifactId(), getVersion(), getType(), getClassifier(), getResource());
  }

}
