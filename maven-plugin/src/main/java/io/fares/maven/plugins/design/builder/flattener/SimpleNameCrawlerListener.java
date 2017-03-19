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

import jlibs.core.io.FileUtil;
import jlibs.core.lang.StringUtil;
import jlibs.xml.sax.crawl.CrawlerListener;
import jlibs.xml.sax.crawl.DefaultCrawlerListener;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.StringTokenizer;

public class SimpleNameCrawlerListener implements CrawlerListener {

  /**
   * Setting this to false will result in every file
   */
  protected boolean overrideExistingFile = false;

  protected File dir;

  public SimpleNameCrawlerListener(File dir) {
    this.dir = dir;
  }

  public SimpleNameCrawlerListener(File dir, boolean overrideExistingFile) {
    this.dir = dir;
    this.overrideExistingFile = overrideExistingFile;
  }

  @Override
  public boolean doCrawl(URL url) {
    return true;
  }

  @Override
  public File toFile(URL url, String extension) {
    /**
     * FIXME this will blow up when a reference url from an app server that
     * is something like:
     * <code>http://services.acme.com/MyService?WSDL&type=XSD&file=schema:f333485f-56bf-4fe6-b33a-8e7744f7b5ab</code>
     */

    URI uri = URI.create(url.toExternalForm());
    String fileName = suggestFile(uri, extension);

    if (overrideExistingFile)
      return new File(dir, fileName);
    else
      return FileUtil.findFreeFile(new File(dir, fileName));

  }

  private String suggestFile(URI uri, String extension) {

    String path = uri.toASCIIString();

    if (path.endsWith(extension)) {
      return suggestDirFile(path, extension);
    } else {
      return suggestGeneratedUri(path, extension);
    }

  }

  /**
   * Works out the file system path woithout mocking the name up like the
   * {@link DefaultCrawlerListener} does.
   *
   * @param path
   * @param extension
   * @return
   */
  private String suggestDirFile(String path, String extension) {

    String tokens[] = StringUtil.getTokens(path, "/", true);
    String file = tokens[tokens.length - 1];
    int dot = file.lastIndexOf(".");

    if (dot == -1)
      return file + '.' + extension;
    else
      return file.substring(0, dot) + '.' + extension;

  }

  /**
   * Works out the OSB generated paths ...
   *
   * @param path
   * @param extension
   * @return
   */
  String suggestGeneratedUri(String path, String extension) {
    String tokens[] = StringUtil.getTokens(path, "/", true);

    String parts[] = StringUtil.getTokens(tokens[tokens.length - 1], "?",
      true);

    // String wsdlName = parts[0];

    String attrib[] = StringUtil.getTokens(parts[parts.length - 1], "&",
      true);

    if (attrib.length == 3) {
      String typeAttrib = attrib[1];
      String fileAttrib = attrib[2];
      String type = typeAttrib.substring(typeAttrib.indexOf("=") + 1);
      String file = fileAttrib.substring(fileAttrib.indexOf("=") + 1);
      return file.replace(':', '_') + '.' + type.toLowerCase();
    } else if (attrib.length == 1
      && (attrib[0].startsWith("wsdl=") || attrib[0]
      .startsWith("xsd="))) {
      String[] toks = splitMe(attrib[0]);
      String file = parts[0];
      return file.replace(':', '_') + '-' + toks[1] + '.' + toks[0];
    } else if (attrib.length == 1 && parts.length == 2
      && parts[1].equalsIgnoreCase("wsdl")) {
      return parts[0] + "." + parts[1];
    } else if (attrib.length == 1 && parts.length == 1
      && parts[0].endsWith(".wsdl")) {
      return parts[0];
    } else {
      throw new RuntimeException("Cannot generated a file name for ["
        + path + "]");
    }

  }

  private String[] splitMe(String token) {

    StringTokenizer st = new StringTokenizer(token, "=");
    st.countTokens();

    String[] args = new String[st.countTokens()];

    int i = 0;
    while (st.hasMoreElements()) {
      args[i++] = st.nextToken();
    }

    return args;

  }

}
