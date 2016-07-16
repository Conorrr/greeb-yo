package io.greeb.sql

import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.name.Named
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

import javax.sql.DataSource

class HikariProvider implements Provider<DataSource> {

  @Inject
  @Named("db.username")
  private String dbUsername

  @Inject
  @Named("db.password")
  private String dbPassword

  @Inject
  @Named("db.location")
  private String dbLocation

  @Override
  public DataSource get() {
    HikariConfig config = new HikariConfig()

    config.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource")
    config.setConnectionTestQuery("VALUES 1")
    config.addDataSourceProperty("URL", "jdbc:h2:$dbLocation")
    config.addDataSourceProperty("user", dbUsername)
    config.addDataSourceProperty("password", dbPassword)

    return new HikariDataSource(config)
  }
}
