package io.greeb.yo.dataServices.rss

class RSSArticle {

  final String id

  final String title

  final String url

  final Date date

  RSSArticle(String id, String title, String url, Date date) {
    this.id = id
    this.title = title
    this.url = url
    this.date = date
  }

  @Override
  String toString() {
    return "$title \n $url"
  }
}
