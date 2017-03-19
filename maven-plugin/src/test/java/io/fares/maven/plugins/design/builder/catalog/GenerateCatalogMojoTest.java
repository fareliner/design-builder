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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.project.MavenProject;

public class GenerateCatalogMojoTest {

  protected final Logger log = LoggerFactory.getLogger(GenerateCatalogMojoTest.class);

  @Rule
  public MojoRule rule = new MojoRule();

  @Rule
  public TestResources resources = new TestResources("src/test/resources/unit", "target/ut/");

  @Test
  public void testGenerateCatalogBasic() throws Exception {
    File baseDir = resources.getBasedir("generate-catalog-basic-test");
    MavenProject project = rule.readMavenProject(baseDir);
    GenerateCatalogMojo mojo = (GenerateCatalogMojo) rule.lookupConfiguredMojo(project, "catalog");
    mojo.execute();
  }

  @Test
  public void testGenerateCatalogLevel() throws Exception {
    File baseDir = resources.getBasedir("generate-catalog-level-test");
    MavenProject project = rule.readMavenProject(baseDir);
    GenerateCatalogMojo mojo = (GenerateCatalogMojo) rule.lookupConfiguredMojo(project, "catalog");
    mojo.execute();
  }

  @Test
  public void testGenerateCatalogZeroLevel() throws Exception {
    File baseDir = resources.getBasedir("generate-catalog-0-level-test");
    MavenProject project = rule.readMavenProject(baseDir);
    GenerateCatalogMojo mojo = (GenerateCatalogMojo) rule.lookupConfiguredMojo(project, "catalog");
    mojo.setTargetCatalogFile(new File(mojo.getSourceDirectory(), "lvl-01/catalog.xcat"));
    mojo.setSystemIdPathOffset(1);
    mojo.execute();
  }

  @Test
  public void testGenerateTestDataTypes() throws Exception {
    File baseDir = resources.getBasedir("test-data-types");
    MavenProject project = rule.readMavenProject(baseDir);
    GenerateCatalogMojo mojo = (GenerateCatalogMojo) rule.lookupConfiguredMojo(project, "catalog");
    mojo.execute();
  }

  @Test
  public void testMultiExecution() throws Exception {
    File baseDir = resources.getBasedir("test-execution-config");
    MavenProject project = rule.readMavenProject(baseDir);
    GenerateCatalogMojo mojo = (GenerateCatalogMojo) rule.lookupConfiguredMojo(project, "catalog");
    mojo.execute();
  }

}
