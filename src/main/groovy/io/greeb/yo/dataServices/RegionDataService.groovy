package io.greeb.yo.dataServices

import com.google.inject.Inject
import groovy.sql.Sql

class RegionDataService {

  private final Sql sql

  @Inject
  public RegionDataService(Sql sql) {
    this.sql = sql
  }

  Map<String, String> getAll() {
    sql.rows("select name, id from region").collectEntries { [it.get('name'), it.get('id')] }
  }

  void insert(String name, String id, String createdBy, String createdByName) {
    sql.executeInsert("insert into region (name, id, createdBy, createdByName) values ($name, $id, $createdBy, $createdByName)")
  }

  void delete(String name) {
    sql.execute("delete from region where name = $name")
  }
}
