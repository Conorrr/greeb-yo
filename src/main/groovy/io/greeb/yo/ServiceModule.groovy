package io.greeb.yo

import com.google.inject.AbstractModule
import io.greeb.yo.dataServices.HousePointDataService
import io.greeb.yo.dataServices.rss.RSSService
import io.greeb.yo.housePoints.HousePointService

class ServiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(RSSService)
    bind(HousePointDataService)
    bind(HousePointService)
  }

}
