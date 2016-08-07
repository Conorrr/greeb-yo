package io.greeb.yo.dataServices.rss

import com.google.inject.Inject
import sx.blah.discord.handle.obj.IChannel

class RSSService {

  @Inject
  RSSDataService rssDataService

  Timer timer = new Timer()
  boolean isRunning = false

  public boolean start(IChannel channel) {
    if (isRunning) {
      return false
    }

    isRunning = true

    RSSTimerTask task = new RSSTimerTask(rssDataService, channel)

    timer.scheduleAtFixedRate(task, 0, 60 * 1000)

    return true
  }

  public void stop() {
    timer.cancel()
  }

}
