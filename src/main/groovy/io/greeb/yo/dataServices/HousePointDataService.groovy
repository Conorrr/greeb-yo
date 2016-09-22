package io.greeb.yo.dataServices

import com.google.inject.Inject
import groovy.sql.Sql
import io.greeb.yo.housePoints.House
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IRole
import sx.blah.discord.handle.obj.IUser

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
      IRole role = guild.getRoleByID((String) it.get('roleId'))
      IChannel channel = guild.getChannelByID((String) it.get('channelId'))
      int points = (int) it.getProperty('points')
      new House(id, role, channel, points)
    })
  }

  void honour(House house, int points, String reason, IUser awardee) {
    sql.executeInsert("""INSERT INTO point_audit (house, userId, season, date, points, reason)
    values ($house.id, $awardee.ID, (SELECT id FROM season WHERE start < now() AND end > now()), now(), $points, $reason)""")
  }

  int getHonourForUser(String userId) {
    (int) sql.firstRow("""SELECT IFNULL(SUM(points), 0) as points
      FROM point_audit
      WHERE season = (SELECT season.id from SEASON where season.start < now() AND season.end > now())
      AND userid = $userId""").get('points')
  }
}
