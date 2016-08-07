package io.greeb.yo.dataServices.rss

import groovy.util.slurpersupport.GPathResult

class RSSUtil {

  static List<RSSArticle> download(String rssUrl) {
    GPathResult rss = new XmlSlurper().parseText(rssUrl.toURL().text)

    return extractArticles(rss)
  }

  static List<RSSArticle> extractArticles(GPathResult rss) {
    return rss.entry.collect { entry ->
      String id = entry.id
      String title = entry.title
      String url = entry.link.find { it.attributes()['type'] == 'text/html' }.attributes()['href']
      Date date = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", entry.published.toString())

      new RSSArticle(id, title, url, date)
    }
  }
}
