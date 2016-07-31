import com.google.inject.matcher.Matchers
import groovy.sql.Sql
import io.greeb.core.discord.DiscordMatchers
import io.greeb.sql.LiquibaseService
import io.greeb.sql.SqlModule
import io.greeb.yo.dataServices.DataServiceModule
import io.greeb.yo.dataServices.RegionDataService
import sx.blah.discord.handle.impl.events.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IRole

import static io.greeb.core.discord.DiscordMatchers.all
import static io.greeb.core.discord.DiscordMatchers.channelMatches
import static io.greeb.core.discord.DiscordMatchers.channelNameMatches
import static io.greeb.core.discord.DiscordMatchers.combine
import static io.greeb.core.discord.DiscordMatchers.messageMatches
import static io.greeb.core.dsl.DSL.greeb

greeb {
  credentials new File('discord.token').text
  properties("properties.json")

  String mainChannelId = properties.mainChannelId
  Map<String, String> regions

  List<IRole> roles
  IGuild guild
  IChannel mainChannel

  bindings {
    module SqlModule
    module DataServiceModule
  }

  onAppStart { Sql sql, LiquibaseService liquibaseService ->
    liquibaseService.runLiquibase()
  }

  consumers {
    def regionBullets

    def generateBullets = {
      regionBullets = regions.keySet().inject("") { result, region -> result + "\n• !$region" }
    }

    def listenForRegion = { regionName ->
      messageReceived(/(?i)^!$regionName/) {
        List<IRole> currentRoles = user.getRolesForGuild(guild)

        def alreadyAssigned = currentRoles.find { regions.keySet().contains(it.name) }

        if (!alreadyAssigned) {
          def newRole = guild.getRoles().find { it.name == regionName }
          guild.editUserRoles(user, (currentRoles + newRole) as IRole[])
          client.getOrCreatePMChannel(user).sendMessage("You are now assigned to `$regionName`")
        } else {
          client.getOrCreatePMChannel(user).sendMessage("You are already assigned to $alreadyAssigned.name. If you wish to change region please use `!resetregion` if you wish to change your region use $regionBullets.")
        }
      }
    }

    messageReceived(/^!ping/) {
      respond("pong")
    }

    guildCreate(all()) { RegionDataService regionDS ->
      // currently only support for 1 guild
      regions = regionDS.all
      guild = event.guild
      roles = guild.roles
      mainChannel = guild.getChannelByID(mainChannelId)
      generateBullets()

      regions.each { regionName, regionId ->
        listenForRegion(regionName)
      }
    }

    userJoin(all()) {
      List<IRole> currentRoles = user.getRolesForGuild(guild)
      def newRole = guild.getRoleByID(properties.starterRole)

      guild.editUserRoles(user, (currentRoles + newRole) as IRole[])

      mainChannel.sendMessage("""\
        Welcome to Calm Yo' <@!$user.ID> - Feel free to make games, and get to know everyone. If you need any help, give Yo' Team a shout!

        GL & HF 😃""".stripIndent())
    }

    messageReceived(/(?i)^!regions$/) {
      def message = "Here is a list of region tags you can choose from, choose wisely! \nPlease reply to this message with:"

      client.getOrCreatePMChannel(user).sendMessage(message + regionBullets)
    }

    messageReceived(/(?i)^!resetregion/) {
      List<IRole> currentRoles = user.getRolesForGuild(guild)

      def currentRegion = currentRoles.find { regions.keySet().contains(it.name) }

      def message

      if (currentRegion) {
        guild.editUserRoles(user, (currentRoles - currentRegion) as IRole[])
        message = "Your region tag has been reset, To join a new region reply to this message with a region tag from the list below."
      } else {
        message = "You aren't assigned to any regions. To join a region reply to this message with a region tag from the list below."
      }

      client.getOrCreatePMChannel(user).sendMessage(message + regionBullets)
    }

    messageReceived(/^!createRegion ([A-Z]{2,5})$/, 'bot-console') { RegionDataService regionDs ->
      def newRole = guild.getRoles().find({ it.name == parts[1] })

      if (!newRole) {
        return respond("Role `${parts[1]}` needs to be created in discord first.")
      }

      if (regions.find { it.key == newRole.name }) {
        return respond("`$newRole.name` already exists as a region")
      }

      regionDs.insert(newRole.name, newRole.ID, user.name, user.ID)

      regions[newRole.name] = newRole.ID
      listenForRegion(newRole.name)
      generateBullets()

      respond("Region created: $newRole.name")
    }

    messageReceived(/^!deleteRegion ([A-Z]{2,5})$/, 'bot-console') { RegionDataService regionDS ->
      def regionToDelete = parts[1]
      if (!regions[regionToDelete]) {
        return respond("`${parts[1]}` is not a region")
      }

      unregister("!$regionToDelete")

      regionDS.delete(regionToDelete)

      regions.remove(regionToDelete)

      generateBullets()

      respond("Region deleted: $regionToDelete")
    }

    messageReceived(/(?i)^!regionstats$/, 'bot-console') {
      guild.getUsers().get(0).getRolesForGuild(guild)

      respond(guild.users
              .groupBy { it.getRolesForGuild(guild).find { role -> regions.containsKey(role.name) } ?: 'NONE' }
              .collect { region, users -> "• !$region - ${users.size()}" }
              .join('\n'))
    }

    messageReceived(combine(messageMatches(/(?i)^!help/), { MessageReceivedEvent e -> e.message.channel.name != 'bot-console' })) {
      respond('''\n\
        • `!ping` - check if the bot is working
        • `!regions` - get a list of regions
        • `!resetregion` - remove assigned region role'''.stripIndent())
    }

    messageReceived(/(?i)^!help/, 'bot-console') {
      respond('''\n\
        • `!ping` - check if the bot is working
        • `!regions` - get a list of regions
        • `!resetregion` - remove assigned region role
        -- admin commands (only work in bot-console)
        • `!createRegion [REGION]` - creates a new region, can be 2-5 characters
        • `!deleteRegion [REGION]` - deletes region
        • `regionstats` - lists the number of users in each region'''.stripIndent())
    }

  }
}