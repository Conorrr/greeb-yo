package io.greeb.yo.dataServices

import com.google.inject.Inject
import groovy.sql.Sql
import io.greeb.yo.housePoints.House
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IRole

class HousePointDataService {

  private final Sql sql

  @Inject
  public HousePointDataService(Sql sql) {
    this.sql = sql
  }

  // get all houses
  public List<House> getAllHouses(IGuild guild) {
    sql.rows("""SELECT house.id, house.roleId, house.channelId, IFNULL((select sum(points) from point_audit where point_audit.season = season.id and point_audit.house = house.id),0) as points
    FROM season
    INNER JOIN house
    WHERE season.start < now()
    AND season.end > now()""").collect({
      int id = (int) it.get('id')
      IRole role = guild.getRoleByID((String) it.get('roleid'))
      IChannel channel = null // todo
      int points = (int) it.getProperty('points')
      new House(id, role, channel, points)
    })
  }

  // add points

  // remove points

  // get points for houses

  // create season

  // end season
}