package io.greeb.yo.housePoints

import com.google.inject.Inject
import com.google.inject.Singleton
import io.greeb.yo.dataServices.HousePointDataService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IRole
import sx.blah.discord.handle.obj.IUser

@Singleton
class HousePointService {

  Logger LOGGER = LoggerFactory.getLogger(HousePointService)

  @Inject
  HousePointDataService dataService

  List<House> houses
  Map<House, List<IUser>> houseRoleUsers
  List<IUser> unassignedUsers = []
  IGuild guild

  void initiate(IGuild guild) {
    this.guild = guild
    houses = dataService.getAllHouses(guild)

    houseRoleUsers = houses.collectEntries({ [(it): []] })

    def users = guild.users
    users.each {
      def currentRoles = it.getRolesForGuild(guild)
      def assignedHouse = houses.find({ currentRoles.contains(it.role) })
      if (assignedHouse) {
        houseRoleUsers[assignedHouse] << it
      } else if (!it.bot) {
        unassignedUsers << it
      }
    }
  }

  // add user to house (user) return house
  House addUser(IUser user) {
    House newHouse = houseRoleUsers.min { it.value.size() }.key

    def newRoles = guild.getRolesForUser(user) + newHouse.role

    houseRoleUsers[newHouse] << user
    guild.editUserRoles(user, newRoles as IRole[])

    unassignedUsers.remove(user)

    newHouse
  }

  /**
   * Assigns points to a house (can be negative)
   * returns null if the user does not belong to a house
   */
  House honour(IUser user, int points, String reason) {
    House house = houseRoleUsers.find { k, v -> v.contains(user) }?.key
    if (house == null) {
      return null
    }

    dataService.honour(house, points, reason, user)

    house.points += points

    return house
  }

  int getHonour(IUser user) {
    dataService.getHonourForUser(user.ID)
  }

  House getHouse(IUser user) {
    houseRoleUsers.find { k, v -> v.contains(user) }?.key
  }

  void userLeft(IUser user) {
    House house = getHouse(user)
    if (house) {
      houseRoleUsers[house].remove(user)
    }
  }
}
