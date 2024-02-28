/*
 * Copyright 2024 Datastrato Pvt Ltd.
 * This software is licensed under the Apache License version 2.
 */

package com.datastrato.gravitino.storage.relational.utils;

import com.datastrato.gravitino.Catalog;
import com.datastrato.gravitino.Namespace;
import com.datastrato.gravitino.json.JsonUtils;
import com.datastrato.gravitino.meta.AuditInfo;
import com.datastrato.gravitino.meta.BaseMetalake;
import com.datastrato.gravitino.meta.CatalogEntity;
import com.datastrato.gravitino.meta.SchemaVersion;
import com.datastrato.gravitino.storage.relational.po.CatalogPO;
import com.datastrato.gravitino.storage.relational.po.MetalakePO;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** POConverters is a utility class to convert PO to Base and vice versa. */
public class POConverters {

  private POConverters() {}

  /**
   * Initialize MetalakePO
   *
   * @param baseMetalake BaseMetalake object
   * @return MetalakePO object with version initialized
   */
  public static MetalakePO initializeMetalakePOWithVersion(BaseMetalake baseMetalake) {
    try {
      return new MetalakePO.Builder()
          .withMetalakeId(baseMetalake.id())
          .withMetalakeName(baseMetalake.name())
          .withMetalakeComment(baseMetalake.comment())
          .withProperties(JsonUtils.anyFieldMapper().writeValueAsString(baseMetalake.properties()))
          .withAuditInfo(JsonUtils.anyFieldMapper().writeValueAsString(baseMetalake.auditInfo()))
          .withSchemaVersion(
              JsonUtils.anyFieldMapper().writeValueAsString(baseMetalake.getVersion()))
          .withCurrentVersion(1L)
          .withLastVersion(1L)
          .withDeletedAt(0L)
          .build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize json object:", e);
    }
  }

  /**
   * Update MetalakePO version
   *
   * @param oldMetalakePO the old MetalakePO object
   * @param newMetalake the new BaseMetalake object
   * @return MetalakePO object with updated version
   */
  public static MetalakePO updateMetalakePOWithVersion(
      MetalakePO oldMetalakePO, BaseMetalake newMetalake) {
    Long lastVersion = oldMetalakePO.getLastVersion();
    // Will set the version to the last version + 1 when having some fields need be multiple version
    Long nextVersion = lastVersion;
    try {
      return new MetalakePO.Builder()
          .withMetalakeId(newMetalake.id())
          .withMetalakeName(newMetalake.name())
          .withMetalakeComment(newMetalake.comment())
          .withProperties(JsonUtils.anyFieldMapper().writeValueAsString(newMetalake.properties()))
          .withAuditInfo(JsonUtils.anyFieldMapper().writeValueAsString(newMetalake.auditInfo()))
          .withSchemaVersion(
              JsonUtils.anyFieldMapper().writeValueAsString(newMetalake.getVersion()))
          .withCurrentVersion(nextVersion)
          .withLastVersion(nextVersion)
          .withDeletedAt(0L)
          .build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize json object:", e);
    }
  }

  /**
   * Convert {@link MetalakePO} to {@link BaseMetalake}
   *
   * @param metalakePO MetalakePO object
   * @return BaseMetalake object from MetalakePO object
   */
  public static BaseMetalake fromMetalakePO(MetalakePO metalakePO) {
    try {
      return new BaseMetalake.Builder()
          .withId(metalakePO.getMetalakeId())
          .withName(metalakePO.getMetalakeName())
          .withComment(metalakePO.getMetalakeComment())
          .withProperties(
              JsonUtils.anyFieldMapper().readValue(metalakePO.getProperties(), Map.class))
          .withAuditInfo(
              JsonUtils.anyFieldMapper().readValue(metalakePO.getAuditInfo(), AuditInfo.class))
          .withVersion(
              JsonUtils.anyFieldMapper()
                  .readValue(metalakePO.getSchemaVersion(), SchemaVersion.class))
          .build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to deserialize json object:", e);
    }
  }

  /**
   * Convert list of {@link MetalakePO} to list of {@link BaseMetalake}
   *
   * @param metalakePOS list of MetalakePO objects
   * @return list of BaseMetalake objects from list of MetalakePO objects
   */
  public static List<BaseMetalake> fromMetalakePOs(List<MetalakePO> metalakePOS) {
    return metalakePOS.stream().map(POConverters::fromMetalakePO).collect(Collectors.toList());
  }

  /**
   * Initialize CatalogPO
   *
   * @param catalogEntity CatalogEntity object
   * @return CatalogPO object with version initialized
   */
  public static CatalogPO initializeCatalogPOWithVersion(
      CatalogEntity catalogEntity, Long metalakeId) {
    try {
      return new CatalogPO.Builder()
          .withCatalogId(catalogEntity.id())
          .withCatalogName(catalogEntity.name())
          .withMetalakeId(metalakeId)
          .withType(catalogEntity.getType().name())
          .withProvider(catalogEntity.getProvider())
          .withCatalogComment(catalogEntity.getComment())
          .withProperties(
              JsonUtils.anyFieldMapper().writeValueAsString(catalogEntity.getProperties()))
          .withAuditInfo(JsonUtils.anyFieldMapper().writeValueAsString(catalogEntity.auditInfo()))
          .withCurrentVersion(1L)
          .withLastVersion(1L)
          .withDeletedAt(0L)
          .build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize json object:", e);
    }
  }

  /**
   * Update CatalogPO version
   *
   * @param oldCatalogPO the old CatalogPO object
   * @param newCatalog the new CatalogEntity object
   * @return CatalogPO object with updated version
   */
  public static CatalogPO updateCatalogPOWithVersion(
      CatalogPO oldCatalogPO, CatalogEntity newCatalog, Long metalakeId) {
    Long lastVersion = oldCatalogPO.getLastVersion();
    // Will set the version to the last version + 1 when having some fields need be multiple version
    Long nextVersion = lastVersion;
    try {
      return new CatalogPO.Builder()
          .withCatalogId(newCatalog.id())
          .withCatalogName(newCatalog.name())
          .withMetalakeId(metalakeId)
          .withType(newCatalog.getType().name())
          .withProvider(newCatalog.getProvider())
          .withCatalogComment(newCatalog.getComment())
          .withProperties(JsonUtils.anyFieldMapper().writeValueAsString(newCatalog.getProperties()))
          .withAuditInfo(JsonUtils.anyFieldMapper().writeValueAsString(newCatalog.auditInfo()))
          .withCurrentVersion(nextVersion)
          .withLastVersion(nextVersion)
          .withDeletedAt(0L)
          .build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize json object:", e);
    }
  }

  /**
   * Convert {@link CatalogPO} to {@link CatalogEntity}
   *
   * @param catalogPO CatalogPO object to be converted
   * @param namespace Namespace object to be associated with the catalog
   * @return CatalogEntity object from CatalogPO object
   */
  public static CatalogEntity fromCatalogPO(CatalogPO catalogPO, Namespace namespace) {
    try {
      return CatalogEntity.builder()
          .withId(catalogPO.getCatalogId())
          .withName(catalogPO.getCatalogName())
          .withNamespace(namespace)
          .withType(Catalog.Type.valueOf(catalogPO.getType()))
          .withProvider(catalogPO.getProvider())
          .withComment(catalogPO.getCatalogComment())
          .withProperties(
              JsonUtils.anyFieldMapper().readValue(catalogPO.getProperties(), Map.class))
          .withAuditInfo(
              JsonUtils.anyFieldMapper().readValue(catalogPO.getAuditInfo(), AuditInfo.class))
          .build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to deserialize json object:", e);
    }
  }

  /**
   * Convert list of {@link MetalakePO} to list of {@link BaseMetalake}
   *
   * @param catalogPOS list of MetalakePO objects
   * @param namespace Namespace object to be associated with the metalake
   * @return list of BaseMetalake objects from list of MetalakePO objects
   */
  public static List<CatalogEntity> fromCatalogPOs(
      List<CatalogPO> catalogPOS, Namespace namespace) {
    return catalogPOS.stream()
        .map(catalogPO -> POConverters.fromCatalogPO(catalogPO, namespace))
        .collect(Collectors.toList());
  }
}
