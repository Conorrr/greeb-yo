package io.greeb.yo.dataServices.rss

import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader

class RSSUtil {

  static SyndFeedInput input = new SyndFeedInput()

  static List<RSSArticle> download(String rssUrl) {
    SyndFeed feed = input.build(new XmlReader(rssUrl.toURL()))

    return extractArticles(feed)
  }

  static List<RSSArticle> extractArticles(SyndFeed feed) {
    return feed.entries.collect { entry ->
      String id = entry.link
      String title = entry.title
      String url = entry.link
      Date date = entry.publishedDate

      new RSSArticle(id, title, url, date)
    }
  }
}
