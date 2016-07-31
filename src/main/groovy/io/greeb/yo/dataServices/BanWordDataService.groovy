package io.greeb.yo.dataServices

import com.google.inject.Inject
import groovy.sql.Sql

class BanWordDataService {

  private final Sql sql

  @Inject
  public BanWordDataService(Sql sql) {
    this.sql = sql
  }

  List<String> getAll() {
    sql.rows("select word from banword").collect { it.get('word').toString() }
  }

  void insert(String word, String createdBy, String createdByName) {
    sql.executeInsert("insert into banword (word, createdBy, createdByName) values ($word, $createdBy, $createdByName)")
  }

  void delete(String word) {
    sql.execute("delete from banword where word = $word")
  }
}
