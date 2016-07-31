import static org.liquibase.groovy.delegate.ScriptDelegate.databaseChangeLog

databaseChangeLog {

  changeSet([author: 'conor', id: 'create-region-table']) {
    createTable([tableName: 'region']) {
      column(name: 'name', type: 'varchar(10)'){
        constraints(nullable: false)
      }
      column(name: 'id', type: 'varchar(20)'){
        constraints(nullable: false)
      }
      column(name: 'createdBy', type: 'varchar(20)')
      column(name: 'createdByName', type: 'varchar(20)')
    }

    addPrimaryKey([tableName: 'region', columnNames: 'name'])
  }

  changeSet([author: 'conor', id: 'create-banword-table']) {
    createTable([tableName: 'banword']) {
      column(name: 'word', type: 'varchar(10)'){
        constraints(nullable: false)
      }
      column(name: 'createdBy', type: 'varchar(20)')
      column(name: 'createdByName', type: 'varchar(20)')
    }

    addPrimaryKey([tableName: 'banword', columnNames: 'word'])
  }

}