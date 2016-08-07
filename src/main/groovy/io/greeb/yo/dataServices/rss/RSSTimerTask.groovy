package io.greeb.yo.dataServices.rss

import sx.blah.discord.handle.obj.IChannel

class RSSTimerTask extends TimerTask {

  IChannel channel
  RSSDataService rssDataService

  RSSTimerTask(RSSDataService rssDataService, IChannel channel) {
    this.rssDataService = rssDataService
    this.channel = channel
  }

  @Override
  void run() {
    Map<Integer, String> feeds = rssDataService.allFeeds

    feeds.each { feedId, url ->
      List<RSSArticle> articles = RSSUtil.download(url)

      List<RSSArticle> newArticles = findNewArticles(articles)

      addArticlesToDb(newArticles, feedId)
      postArticles(newArticles)
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
