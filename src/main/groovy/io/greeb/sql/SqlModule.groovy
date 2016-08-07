package io.greeb.sql

import com.google.inject.AbstractModule
import groovy.sql.Sql

import javax.sql.DataSource

import static com.google.inject.Scopes.SINGLETON

public class SqlModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DataSource).toProvider(HikariProvider).in(SINGLETON)
    bind(Sql).toProvider(SqlProvider).in(SINGLETON)
    bind(LiquibaseService)
  }

}