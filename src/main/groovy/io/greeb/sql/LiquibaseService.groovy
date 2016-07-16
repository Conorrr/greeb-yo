package io.greeb.sql

import com.google.inject.Inject
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.Database
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor

import javax.sql.DataSource

class LiquibaseService {

  @Inject
  DataSource dataSource

  public void runLiquibase() {
    Database database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(dataSource.connection))

    Liquibase liquibase = new Liquibase("liquibase.groovy", new ClassLoaderResourceAccessor(), database)

    liquibase.update((Contexts) null)
  }

}
