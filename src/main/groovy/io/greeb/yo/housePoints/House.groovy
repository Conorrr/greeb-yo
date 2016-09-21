package io.greeb.yo.housePoints

import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IRole

class House {

  House(int id, IRole role, IChannel channel, int points) {
    this.id = id
    this.role = role
    this.channel = channel
    this.points = points
  }
  int id

  IRole role

  IChannel channel

  int points

}
