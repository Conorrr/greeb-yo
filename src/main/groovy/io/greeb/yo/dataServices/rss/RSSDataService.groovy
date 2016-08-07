package io.greeb.yo.dataServices.rss

import com.google.inject.Inject
import groovy.sql.Sql

class RSSDataService {

  private final Sql sql

  @Inject
  public RSSDataService(Sql sql) {
    this.sql = sql
  }

  Map<Integer, String> getAllFeeds() {
    sql.rows("select id, url from RSS_FEED").collectEntries { [it.get('id'), it.get('url')] }
  }

  void addFeed(String url, String createdBy, String createdByName) {
    sql.executeInsert("insert into RSS_FEED (url, createdBy, createdByName) values ($url, $createdBy, $createdByName)")
  }

  void addArticle(String articleId, Integer feedId, Date date) {
    sql.executeInsert("insert into RSS_HISTORY (id, feed, created) values ($articleId, $feedId, ${date.toTimestamp()})")
  }

  void delete(Integer feedId) {
    sql.execute("delete from RSS_FEED where id = $feedId")
  }

  Boolean alreadyPosted(String articleId) {
    sql.firstRow("select true from RSS_HISTORY where id = $articleId")?: false
  }

}
