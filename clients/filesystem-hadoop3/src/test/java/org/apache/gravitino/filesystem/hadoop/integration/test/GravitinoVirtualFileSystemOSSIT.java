/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.gravitino.filesystem.hadoop.integration.test;

import static org.apache.gravitino.catalog.hadoop.HadoopCatalogPropertiesMetadata.FILESYSTEM_PROVIDERS;
import static org.apache.gravitino.filesystem.hadoop.GravitinoVirtualFileSystemConfiguration.FS_FILESYSTEM_PROVIDERS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.gravitino.Catalog;
import org.apache.gravitino.NameIdentifier;
import org.apache.gravitino.file.Fileset;
import org.apache.gravitino.integration.test.util.GravitinoITUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Disabled(
    "Disabled due to we don't have a real OSS account to test. If you have a GCP account,"
        + "please change the configuration(BUCKET_NAME, OSS_ACCESS_KEY, OSS_SECRET_KEY, OSS_ENDPOINT) and enable this test.")
public class GravitinoVirtualFileSystemOSSIT extends GravitinoVirtualFileSystemIT {
  private static final Logger LOG = LoggerFactory.getLogger(GravitinoVirtualFileSystemOSSIT.class);

  public static final String BUCKET_NAME = "YOUR_BUCKET";
  public static final String OSS_ACCESS_KEY = "YOUR_OSS_ACCESS_KEY";
  public static final String OSS_SECRET_KEY = "YOUR_OSS_SECRET_KEY";
  public static final String OSS_ENDPOINT = "YOUR_OSS_ENDPOINT";

  @BeforeAll
  public void startIntegrationTest() {
    // Do nothing
  }

  @BeforeAll
  public void startUp() throws Exception {
    copyBundleJarsToHadoop("aliyun-bundle");
    // Need to download jars to gravitino server
    super.startIntegrationTest();

    // This value can be by tune by the user, please change it accordingly.
    defaultBockSize = 64 * 1024 * 1024;

    metalakeName = GravitinoITUtils.genRandomName("gvfs_it_metalake");
    catalogName = GravitinoITUtils.genRandomName("catalog");
    schemaName = GravitinoITUtils.genRandomName("schema");

    Assertions.assertFalse(client.metalakeExists(metalakeName));
    metalake = client.createMetalake(metalakeName, "metalake comment", Collections.emptyMap());
    Assertions.assertTrue(client.metalakeExists(metalakeName));

    Map<String, String> properties = Maps.newHashMap();
    properties.put(FILESYSTEM_PROVIDERS, "oss");
    properties.put("gravitino.bypass.fs.oss.accessKeyId", OSS_ACCESS_KEY);
    properties.put("gravitino.bypass.fs.oss.accessKeySecret", OSS_SECRET_KEY);
    properties.put("gravitino.bypass.fs.oss.endpoint", OSS_ENDPOINT);
    properties.put(
        "gravitino.bypass.fs.oss.impl", "org.apache.hadoop.fs.aliyun.oss.AliyunOSSFileSystem");

    Catalog catalog =
        metalake.createCatalog(
            catalogName, Catalog.Type.FILESET, "hadoop", "catalog comment", properties);
    Assertions.assertTrue(metalake.catalogExists(catalogName));

    catalog.asSchemas().createSchema(schemaName, "schema comment", properties);
    Assertions.assertTrue(catalog.asSchemas().schemaExists(schemaName));

    conf.set("fs.gvfs.impl", "org.apache.gravitino.filesystem.hadoop.GravitinoVirtualFileSystem");
    conf.set("fs.AbstractFileSystem.gvfs.impl", "org.apache.gravitino.filesystem.hadoop.Gvfs");
    conf.set("fs.gvfs.impl.disable.cache", "true");
    conf.set("fs.gravitino.server.uri", serverUri);
    conf.set("fs.gravitino.client.metalake", metalakeName);

    // Pass this configuration to the real file system
    conf.set("gravitino.bypass.fs.oss.accessKeyId", OSS_ACCESS_KEY);
    conf.set("gravitino.bypass.fs.oss.accessKeySecret", OSS_SECRET_KEY);
    conf.set("gravitino.bypass.fs.oss.endpoint", OSS_ENDPOINT);
    conf.set("gravitino.bypass.fs.oss.impl", "org.apache.hadoop.fs.aliyun.oss.AliyunOSSFileSystem");

    conf.set(FS_FILESYSTEM_PROVIDERS, "oss");
  }

  @AfterAll
  public void tearDown() throws IOException {
    Catalog catalog = metalake.loadCatalog(catalogName);
    catalog.asSchemas().dropSchema(schemaName, true);
    metalake.dropCatalog(catalogName);
    client.dropMetalake(metalakeName);

    if (client != null) {
      client.close();
      client = null;
    }

    try {
      closer.close();
    } catch (Exception e) {
      LOG.error("Exception in closing CloseableGroup", e);
    }
  }

  /**
   * Remove the `gravitino.bypass` prefix from the configuration and pass it to the real file system
   * This method corresponds to the method org.apache.gravitino.filesystem.hadoop
   * .GravitinoVirtualFileSystem#getConfigMap(Configuration) in the original code.
   */
  protected Configuration convertGvfsConfigToRealFileSystemConfig(Configuration gvfsConf) {
    Configuration gcsConf = new Configuration();
    gvfsConf.forEach(
        entry -> {
          gcsConf.set(entry.getKey().replace("gravitino.bypass.", ""), entry.getValue());
        });

    return gcsConf;
  }

  protected String genStorageLocation(String fileset) {
    return String.format("oss://%s/%s", BUCKET_NAME, fileset);
  }

  @Test
  public void testGetDefaultReplications() throws IOException {
    String filesetName = GravitinoITUtils.genRandomName("test_get_default_replications");
    NameIdentifier filesetIdent = NameIdentifier.of(schemaName, filesetName);
    Catalog catalog = metalake.loadCatalog(catalogName);
    String storageLocation = genStorageLocation(filesetName);
    catalog
        .asFilesetCatalog()
        .createFileset(
            filesetIdent,
            "fileset comment",
            Fileset.Type.MANAGED,
            storageLocation,
            new HashMap<>());
    Assertions.assertTrue(catalog.asFilesetCatalog().filesetExists(filesetIdent));
    Path gvfsPath = genGvfsPath(filesetName);
    try (FileSystem gvfs = gvfsPath.getFileSystem(conf)) {
      // Here HDFS is 3, but for oss is 1.
      assertEquals(1, gvfs.getDefaultReplication(gvfsPath));
    }

    catalog.asFilesetCatalog().dropFileset(filesetIdent);
  }

  @Disabled(
      "OSS does not support append, java.io.IOException: The append operation is not supported")
  public void testAppend() throws IOException {}
}
