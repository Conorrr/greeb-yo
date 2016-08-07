import static org.liquibase.groovy.delegate.ScriptDelegate.databaseChangeLog

databaseChangeLog {

  changeSet([author: 'conor', id: 'create-region-table']) {
    createTable([tableName: 'region']) {
      column(name: 'name', type: 'varchar(10)') {
        constraints(nullable: false)
      }
      column(name: 'id', type: 'varchar(20)') {
        constraints(nullable: false)
      }
      column(name: 'createdBy', type: 'varchar(20)')
      column(name: 'createdByName', type: 'varchar(20)')
    }

    addPrimaryKey([tableName: 'region', columnNames: 'name'])
  }

  changeSet([author: 'conor', id: 'create-banword-table']) {
    createTable([tableName: 'banword']) {
      column(name: 'word', type: 'varchar(10)') {
        constraints(nullable: false)
      }
      column(name: 'createdBy', type: 'varchar(20)')
      column(name: 'createdByName', type: 'varchar(20)')
    }

    addPrimaryKey([tableName: 'banword', columnNames: 'word'])
  }

  changeSet([author: 'conor', id: 'create-rss-feed-tables']) {
    createTable([tableName: 'RSS_FEED']) {
      column(name: 'id', type: 'int') {
        constraints(nullable: false)
      }
      column(name: 'url', type:'varchar(250)')
      column(name: 'createdBy', type: 'varchar(20)')
      column(name: 'createdByName', type: 'varchar(20)')
    }

    addPrimaryKey([tableName: 'RSS_FEED', columnNames: 'id'])

    addAutoIncrement([tableName: 'RSS_FEED', columnDataType: 'int', columnName: 'id', incrementBy: '1'])

    createTable([tableName: 'RSS_HISTORY']) {
      column(name: 'id', type: 'varchar(200)') {
        constraints(nullable: false)
      }
      column(name: 'feed', type: 'int')
      column(name: 'created', type: 'datetime')
    }

    addPrimaryKey([tableName: 'RSS_HISTORY', columnNames: 'id'])
  }


}