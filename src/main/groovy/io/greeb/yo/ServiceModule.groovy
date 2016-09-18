package io.greeb.yo.dataServices

import com.google.inject.AbstractModule
import io.greeb.yo.dataServices.rss.RSSService

class ServiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(RSSService)
  }

}
