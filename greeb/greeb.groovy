import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IRole

import static io.greeb.core.dsl.DSL.greeb

greeb {
  credentials new File('discord.token').text

  def mainChannelName = 'allchat'
  def welcomeChannelName = 'welcome'
  def regions = ["NA", "EUW", "EUNE", "LAN", "LAS", "BR", "OCR"]

  List<IRole> roles
  IGuild guild
  IChannel mainChannel
  IChannel welcomeChannel

  consumers {

    guildCreate {
      // currently only support for 1 guild
      guild = event.client.guilds[0]
      roles = guild.roles
      mainChannel = guild.channels.find { it.name == mainChannelName }
      welcomeChannel = guild.channels.find { it.name == welcomeChannelName }
    }

    userJoin {
      def user = event.user
      mainChannel.sendMessage("""\
        Hey <@!$user.ID>
        Welcome to Keep Calm.

        Please check the <#$welcomeChannel.ID> page first of all. !NA, !EUW, !EUNE, !LAN, !LAS, !BR or !OCR and the bot will assign you a region tag.
        GL & HF 😃""".stripIndent())
    }

    regions.each { region ->
      messageReceived(/^!$region/, mainChannelName) {
        def user = event.message.author
        List<IRole> currentRoles = user.getRolesForGuild(guild)

        def alreadyAssigned = currentRoles.find { regions.contains(it.name) }

        if (!alreadyAssigned) {
          def newRole = guild.getRoles().find { it.name == region }
          guild.editUserRoles(user, (currentRoles + newRole) as IRole[])
          respond("<@!${user.ID}> you are now assigned to `$region`")
        } else {
          respond("<@!$user.ID> you are already assigned to $alreadyAssigned.name. If you wish to change region please use `!resetregion` if you wish to change your region use !NA, !EUW, !EUNE, !LAN, !LAS, !BR or !OCR.")
        }

        void // work around for a greeb bug
      }
    }

    messageReceived(/^!resetregion/) {
      def user = event.message.author
      List<IRole> currentRoles = user.getRolesForGuild(guild)

      def currentRegion = currentRoles.find { regions.contains(it.name) }

      if (currentRegion) {
        guild.editUserRoles(user, (currentRoles - currentRegion) as IRole[])
        respond("<@!$user.ID> you are now unassigned to $currentRegion.name. If you wish to set a region use !NA, !EUW, !EUNE, !LAN, !LAS, !BR or !OCR.")
      } else {
        respond("<@!$user.ID> you aren't assigned to any regions. If you wish to set a region use !NA, !EUW, !EUNE, !LAN, !LAS, !BR or !OCR.")
      }
    }

  }
}