package io.greeb.sql

import com.google.inject.Inject
import com.google.inject.Provider
import groovy.sql.Sql

import javax.sql.DataSource

class SqlProvider implements Provider<Sql> {

  private final DataSource ds

  @Inject
  public SqlProvider(DataSource ds) {
    this.ds = ds
  }

  @Override
  public Sql get() {
    return new Sql(ds)
  }

}
