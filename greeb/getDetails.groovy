import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IRole

import static io.greeb.core.discord.DiscordMatchers.all
import static io.greeb.core.dsl.DSL.greeb

greeb {
  credentials "MjAzNzk0MzU2MzI5NTEyOTYw.CmvR0Q.wJpaCL1mc17scDAuv7MgMPZFSBU"

  consumers {

    guildCreate(all()) {
      // currently only support for 1 guild
      event.guild.roles.each {println "$it.ID:$it.name"}
    }

  }
}