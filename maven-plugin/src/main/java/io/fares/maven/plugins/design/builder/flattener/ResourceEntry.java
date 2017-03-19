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

import java.net.URL;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.FileSet;

/**
 * A {@link ResourceEntry} can represent a standard maven {@link FileSet}, a
 * common {@link URL} or a reference to a resource within a maven
 * {@link Dependency}.
 */
public class ResourceEntry {

  /**
   * Example:
   * <p>
   * <code>
   * &lt;dependencyResource&gt;<br>
   * &nbsp;&nbsp;&lt;groupId&gt;io.fares.maven.plugins.test.unit&lt;/
   * groupId&gt;<br>
   * &nbsp;&nbsp;&lt;artifactId&gt;jar-1&lt;/artifactId&gt;<br>
   * &nbsp;&nbsp;&lt;resource&gt;META-INF/catalog.xml&lt;/resource&gt;<br>
   * &lt;/dependencyResource&gt;<br>
   * </code>
   */
  private DependencyResource dependencyResource;

  /**
   * Example:
   * <p>
   * <code>
   * &lt;fileset&gt;<br>
   * &nbsp;&nbsp;&lt;directory&gt;${project.outputDirectory}/META-INF/catalog.xml/&lt;/directory&gt;<br>
   * &nbsp;&nbsp;&lt;includes&gt;<br>
   * &nbsp;&nbsp;&nbsp;&nbsp;&lt;include&gt;**\/*-catalog.xml&lt;/include&gt;<br>
   * &nbsp;&nbsp;&lt;/includes&gt;<br>
   * &lt;/fileset&gt;<br>
   * </code>
   */
  private FileSet fileset;

  /**
   * Example:
   * <p>
   * <code>
   * &lt;url&gt;classpath:META-INF/catalog.xml&lt;/url&gt;<br>
   * </code>
   */

  private String url;

  public FileSet getFileset() {
    return fileset;
  }

  public void setFileset(FileSet fileset) {
    this.fileset = fileset;
  }


  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public DependencyResource getDependencyResource() {
    return dependencyResource;
  }

  public void setDependencyResource(DependencyResource dependencyResource) {
    this.dependencyResource = dependencyResource;
  }

  @Override
  public String toString() {
    if (getFileset() != null) {
      return getFileset().toString();
    } else if (getUrl() != null) {
      return "URL { " + getUrl().toString() + "}";
    } else if (getDependencyResource() != null) {
      return getDependencyResource().toString();
    } else {
      return "Empty resource entry {}";
    }
  }

}
