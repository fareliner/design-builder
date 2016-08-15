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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import org.apache.maven.artifact.Artifact;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;

import javax.inject.Named;

@Named
@Component(role = ArtifactDepdenencyResolver.class)
public class ArtifactDepdenencyResolver {

    Logger log = LoggerFactory.getLogger(ArtifactDepdenencyResolver.class);

    @Requirement
    private RepositorySystem system;

    @Requirement
    private RepositorySystemSession session;

    public ArtifactDepdenencyResolver() {
    }

    public ArtifactDepdenencyResolver(RepositorySystem system, RepositorySystemSession session) {
        this.system = system;
        this.session = session;
    }

    public List<URL> scan(Set<Artifact> artifacts, List<RemoteRepository> remoteRepositories) throws DependencyResolutionException, MalformedURLException {
        List<URL> result = new ArrayList<>();
        CollectRequest collectRequest = new CollectRequest(convertArtifactsToDependencies(artifacts), null, remoteRepositories);
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, new ScopeDependencyFilter());
        List<ArtifactResult> artifactResults = system.resolveDependencies(session, dependencyRequest).getArtifactResults();
        for (ArtifactResult artifactResult : artifactResults) {
            result.add(artifactResult.getArtifact().getFile().toURI().toURL());
        }
        return result;
    }

    private List<org.eclipse.aether.graph.Dependency> convertArtifactsToDependencies(Set<Artifact> artifacts) {
        List<org.eclipse.aether.graph.Dependency> deps = new ArrayList<>(artifacts == null ? 0 : artifacts.size());
        for (org.apache.maven.artifact.Artifact a : artifacts) {
            org.eclipse.aether.artifact.Artifact artifact = new DefaultArtifact(a.getGroupId(), a.getArtifactId(), a.getClassifier(), a.getType(), a.getVersion());
            deps.add(new org.eclipse.aether.graph.Dependency(artifact, JavaScopes.COMPILE));
        }
        return deps;
    }


}
