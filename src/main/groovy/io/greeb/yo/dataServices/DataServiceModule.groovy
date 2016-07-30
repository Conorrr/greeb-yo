package io.greeb.yo.dataServices

import com.google.inject.AbstractModule

class DataServiceModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(RegionDataService)
  }
}
