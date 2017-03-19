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

package io.fares.maven.plugins.design.builder.scanner;

import javax.inject.Named;
import java.io.File;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import io.fares.maven.plugins.design.builder.flattener.ResourceEntry;
import io.fares.maven.plugins.design.builder.flattener.ResourceEntryDependencyResolver;

@Named("catalogFileScannerFactory")
@Component(role = CatalogFileScannerFactory.class)
public class CatalogFileScannerFactory {

  @Requirement
  ResourceEntryDependencyResolver resourceResolver;

  public CatalogFileScanner newInstance(MavenProject project, RepositorySystemSession repositorySystemSession, List<RemoteRepository> remoteRepositories, List<Resource> resources, ResourceEntry[] catalogs, File sourceDirectory, Set<String> includes, Set<String> excludes) throws DependencyResolutionRequiredException {
    CatalogFileScanner scanner = new CatalogFileScanner();
    scanner.setCompileClasspathElements(project.getCompileClasspathElements());
    scanner.setRepositorySystemSession(repositorySystemSession);
    scanner.setRemoteRepositories(remoteRepositories);
    scanner.setResourceEntryResolver(resourceResolver);
    scanner.setResources(resources);
    scanner.setCatalogs(catalogs);
    scanner.setSourceDirectory(sourceDirectory);
    scanner.setIncludes(includes);
    scanner.setExcludes(excludes);
    return scanner;
  }

}
