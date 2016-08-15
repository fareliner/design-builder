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

package io.fares.maven.plugins.test.ruse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.Assert.*;

import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Rule;
import org.junit.Test;

public class FingCatalogTest {

    @Rule
    public TestResources resources = new TestResources("src/test/resources/unit", "target/ut/");


    @Test
    public void testURLJar() throws IOException, URISyntaxException {

        URL repo = getClass().getResource("/");
        URI uri = repo.toURI();

        URI catJar = uri
                .resolve("../../src/test/resources/unit/test-dependency-resolver-config/lib/io/fares/maven/plugins/test/unit/jar-1/1.0.0/jar-1-1.0.0.jar");

        URL url = new URL("jar:" + catJar.toString() + "!/catalog.xml");

        URLConnection cn = url.openConnection();

        InputStream is = url.openStream();

        assertNotNull("Need to connect succesfully", is);

        System.out.println(url.toExternalForm());

    }
}
