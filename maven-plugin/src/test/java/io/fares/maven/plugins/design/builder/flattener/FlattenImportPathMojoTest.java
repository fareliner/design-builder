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

import io.fares.maven.plugins.utils.AetherUtil;
import org.apache.maven.execution.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.*;


public class FlattenImportPathMojoTest {

  protected final Logger log = LoggerFactory
    .getLogger(FlattenImportPathMojoTest.class);

  @Rule
  public MojoRule rule = new MojoRule();

  @Rule
  public TestResources resources = new TestResources("src/test/resources/unit", "target/ut/");

  /**
   * @throws Exception if any
   */
  //@Ignore("Needs more work to make the mocking behave.")
  @Test
  public void testDependencyResolver() throws Exception {


    File baseDir = resources.getBasedir("test-dependency-resolver-config");
    File pomFile = new File(baseDir, "pom.xml");

    PlexusContainer container = rule.getContainer();

    // see if we can get localrepo manager from system and add to session
    DefaultRepositorySystemSession systemSession = new DefaultRepositorySystemSession();
    LocalRepository lr = new LocalRepository(new File(baseDir, "repo"));
    DefaultRepositorySystem system = container.lookup(DefaultRepositorySystem.class);
    LocalRepositoryManager lrm = system.newLocalRepositoryManager(systemSession, lr);
    systemSession.setLocalRepositoryManager(lrm);
    systemSession.setProxySelector(AetherUtil.newProxySelector(baseDir));

    MavenExecutionRequest request = new DefaultMavenExecutionRequest();

    request.setBaseDirectory(baseDir);
    request.setLocalRepositoryPath(new File(baseDir, ".m2"));
    request.setLocalRepository(new StubArtifactRepository(new File(baseDir, ".m2").getCanonicalPath()));

    ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
    configuration.setResolveDependencies(true);
    configuration.setRepositorySession(systemSession);

    ProjectBuilder projectBuilder = rule.lookup(ProjectBuilder.class);
    ProjectBuildingResult projectBuildingResult = projectBuilder.build(pomFile, configuration);

    MavenProject project = projectBuildingResult.getProject();
    MavenExecutionResult result = new DefaultMavenExecutionResult();
    MavenSession session = new MavenSession(container, systemSession, request, result);
    session.setCurrentProject(project);
    session.setProjects(Arrays.asList(project));

    MojoExecution exec = rule.newMojoExecution("flatten");
    FlattenImportPathMojo mojo = (FlattenImportPathMojo) rule.lookupConfiguredMojo(session, exec);

    // set sources dir
    rule.setVariableValueToObject(mojo, "sourceDirectory", new File(baseDir, "src"));
    rule.setVariableValueToObject(mojo, "outputDirectory", new File(baseDir, "target"));

    mojo.execute();

    assertNotNull(pomFile);
    assertTrue(pomFile.exists());

  }

}
