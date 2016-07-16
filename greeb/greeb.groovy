import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IRole

import static io.greeb.core.discord.DiscordMatchers.all
import static io.greeb.core.dsl.DSL.greeb

greeb {
  credentials new File('discord.token').text
  properties("properties.json")

  String mainChannelId = properties.mainChannelId
  def regions = properties.regions

  List<IRole> roles
  IGuild guild
  IChannel mainChannel

  consumers {

    // HELPER METHODS  //
    def regionBullets = regions.inject("") { result, region -> result + "\n• !$region" }
    /////////////////////

    messageReceived(/^!ping/) {
      respond("pong")
    }

    guildCreate(all()) {
      // currently only support for 1 guild
      guild = event.guild
      roles = guild.roles
      mainChannel = guild.getChannelByID(mainChannelId)
    }

    userJoin(all()) {
      List<IRole> currentRoles = user.getRolesForGuild(guild)
      def newRole = guild.getRoleByID(properties.starterRole)

      guild.editUserRoles(user, (currentRoles + newRole) as IRole[])

      mainChannel.sendMessage("""\
        Welcome to Calm Yo' <@!$user.ID>  - Feel free to make games, and get to know everyone. If you need any help, give Yo' Team a shout!

        GL & HF 😃""".stripIndent())
    }

    messageReceived(/(?i)^!regions/) {
      def message = "Here is a list of region tags you can choose from, choose wisely! \nPlease reply to this message with:"

      client.getOrCreatePMChannel(user).sendMessage(message + regionBullets)
    }

    messageReceived(/^!resetregion/) {
      List<IRole> currentRoles = user.getRolesForGuild(guild)

      def currentRegion = currentRoles.find { regions.contains(it.name) }

      def message

      if (currentRegion) {
        guild.editUserRoles(user, (currentRoles - currentRegion) as IRole[])
        message = "Your region tag has been reset, To join a new region reply to this message with a region tag from the list below."
      } else {
        message = "You aren't assigned to any regions. To join a region reply to this message with a region tag from the list below."
      }

      client.getOrCreatePMChannel(user).sendMessage(message + regionBullets)
    }

    regions.each { region ->
      messageReceived(/^!$region/) {
        List<IRole> currentRoles = user.getRolesForGuild(guild)

        def alreadyAssigned = currentRoles.find { regions.contains(it.name) }

        if (!alreadyAssigned) {
          def newRole = guild.getRoles().find { it.name == region }
          guild.editUserRoles(user, (currentRoles + newRole) as IRole[])
          client.getOrCreatePMChannel(user).sendMessage("You are now assigned to `$region`")
        } else {
          client.getOrCreatePMChannel(user).sendMessage("You are already assigned to $alreadyAssigned.name. If you wish to change region please use `!resetregion` if you wish to change your region use $regionBullets.")
        }
      }
    }

  }
}