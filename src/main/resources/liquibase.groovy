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
      column(name: 'url', type: 'varchar(250)')
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

  changeSet([author: 'conor', id: 'house-points']) {
    // season Table
    createTable([tableName: 'season']) {
      column(name: 'id', type: 'int') {
        constraints(nullable: false)
      }
      column(name: 'start', type: 'datetime')
      column(name: 'end', type: 'datetime')

    }
    addPrimaryKey([tableName: 'season', columnNames: 'id'])
    addAutoIncrement([tableName: 'season', columnDataType: 'int', columnName: 'id', incrementBy: '1'])

    // house Table
    createTable([tableName: 'house']) {
      column(name: 'id', type: 'int') {
        constraints(nullable: false)
      }
      column(name: 'roleId', type: 'varchar(20)')
      column(name: 'channelId', type: 'varchar(20)')

    }
    addPrimaryKey([tableName: 'house', columnNames: 'id'])
    addAutoIncrement([tableName: 'house', columnDataType: 'int', columnName: 'id', incrementBy: '1'])


    // point_audit table
    createTable([tableName: 'point_audit']) {
      column(name: 'id', type: 'int') {
        constraints(nullable: false)
      }
      column(name: 'house', type: 'int')
      column(name: 'userId', type: 'varchar(20)')
      column(name: 'season', type: 'int')
      column(name: 'date', type: 'datetime')
      column(name: 'points', type: 'int')
      column(name: 'reason', type: 'varchar(256)')

    }
    addPrimaryKey([tableName: 'point_audit', columnNames: 'id'])
    addAutoIncrement([tableName: 'point_audit', columnDataType: 'int', columnName: 'id', incrementBy: '1'])

    createIndex([tableName: 'point_audit', indexName: 'idx_house']) {
      column(name: 'house', type: 'int')
    }
    createIndex([tableName: 'point_audit', indexName: 'idx_season']) {
      column(name: 'season', type: 'int')
    }

  }

}