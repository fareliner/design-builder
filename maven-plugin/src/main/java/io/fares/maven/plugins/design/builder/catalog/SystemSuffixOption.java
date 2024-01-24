/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.fares.maven.plugins.design.builder.catalog;

/**
 * Select output to be of type system suffix
 * <pre>
 * {@code
 * <systemSuffix>
 *   <pathOffset>1</pathOffset>
 * </systemSuffix>
 * </pre>
 * }
 */
public class SystemSuffixOption {

  private int pathOffset = 0;

  /**
   * Specifies how much offset the schema file should have to its parent directory.
   * This offset will be used to populate the systemIdSuffix path depth.
   */
  public int getPathOffset() {
    return pathOffset;
  }

  /**
   * Set the offset of the systemIdSuffix in the catalog relative to the schema file. Lets say we have a
   * schema located in folder ./test/DataTypes.xsd and we generate the catalog file into the same location.
   * <pre>
   * {@code
   * <catalog>
   *   <systemSuffix>
   *     <pathOffset>1</pathOffset>
   *   </systemSuffix>
   * </catalog>
   * }
   * </pre>
   * <p>
   * The resulting systemSuffix entry would return the following where the relative path between the 2 is offset by 1 level.
   * <p/>
   * <pre>
   * {@code
   * <systemSuffix systemIdSuffix="test/DataTypes.xsd" uri="DataTypes.xsd"/>
   * }
   * </pre>
   *
   * @param pathOffset the offset to consider in calculating paths between catalog and schema file
   */
  public void setPathOffset(int pathOffset) {
    this.pathOffset = pathOffset;
  }

}
