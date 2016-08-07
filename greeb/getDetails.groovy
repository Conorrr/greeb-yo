import static io.greeb.core.discord.DiscordMatchers.all
import static io.greeb.core.dsl.DSL.greeb

greeb {
  credentials new File('discord.token').text

  consumers {

    guildCreate(all()) {
      // currently only support for 1 guild
      event.guild.roles.each {println "$it.ID:$it.name"}
    }

  }
}
