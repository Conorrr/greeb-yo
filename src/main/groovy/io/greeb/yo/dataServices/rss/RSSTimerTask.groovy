package io.greeb.yo.dataServices.rss

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sx.blah.discord.handle.obj.IChannel

class RSSTimerTask extends TimerTask {
  Logger LOGGER = LoggerFactory.getLogger(RSSTimerTask)

  IChannel channel
  RSSDataService rssDataService

  RSSTimerTask(RSSDataService rssDataService, IChannel channel) {
    this.rssDataService = rssDataService
    this.channel = channel
  }

  @Override
  void run() {
    try {
      LOGGER.info('Feed update check')
      Map<Integer, String> feeds = rssDataService.allFeeds

      feeds.each { feedId, url ->
        LOGGER.info("downloading articles from $url")
        List<RSSArticle> articles = RSSUtil.download(url)
        List<RSSArticle> newArticles = findNewArticles(articles)

        LOGGER.debug("New articles: ${newArticles.join(',')}")

        addArticlesToDb(newArticles, feedId)
        LOGGER.info("Posting ${newArticles.size()} articles")
        postArticles(newArticles)
      }
    } catch(Exception e){
      LOGGER.error('Error running RSS feed', e)
    }
  }

  private addArticlesToDb(List<RSSArticle> newArticles, Integer feedId) {
    newArticles.each { rssDataService.addArticle(it.id, feedId, it.date) }
  }

  private void postArticles(List<RSSArticle> newArticles) {
    newArticles.reverse().collect { it.toString() }.each(channel.&sendMessage)
  }

  private List<RSSArticle> findNewArticles(List<RSSArticle> allArticles) {
    allArticles.findAll { !rssDataService.alreadyPosted(it.id) }
  }

}
