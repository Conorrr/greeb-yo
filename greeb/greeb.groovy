import com.google.inject.matcher.Matchers
import groovy.json.JsonOutput
import groovy.sql.Sql
import io.greeb.core.discord.DiscordMatchers
import io.greeb.sql.LiquibaseService
import io.greeb.sql.SqlModule
import io.greeb.yo.dataServices.BanWordDataService
import io.greeb.yo.dataServices.DataServiceModule
import io.greeb.yo.dataServices.RegionDataService
import sx.blah.discord.handle.impl.events.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IRole

import static io.greeb.core.discord.DiscordMatchers.all
import static io.greeb.core.discord.DiscordMatchers.channelNameMatches
import static io.greeb.core.discord.DiscordMatchers.combine
import static io.greeb.core.discord.DiscordMatchers.messageMatches
import static io.greeb.core.discord.DiscordMatchers.not
import static io.greeb.core.discord.DiscordMatchers.privateChat
import static io.greeb.core.dsl.DSL.greeb

greeb {
  credentials new File('discord.token').text
  properties("properties.json")

  String mainChannelId = properties.mainChannelId
  String consoleChannelId = properties.consoleChannelId

  Map<String, String> regions
  List<String> banWords

  List<IRole> roles
  IGuild guild
  IChannel mainChannel
  IChannel consoleChannel

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

    def console = { String message ->
      consoleChannel.sendMessage(message)
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

    def listenForBanWord = { banWord ->
      messageReceived(combine(not(privateChat()), not(channelNameMatches('bot-console')), messageMatches(/(?i)(^|\s)$banWord(\s|$)/))) {
        // delete message
        message.delete()

        // pm the user
        client.getOrCreatePMChannel(user).sendMessage("your message `$content` has removed from <#$message.channel.ID>. If you think this is a mistake please contact the mods")

        // post a line to console
        console("message `$content` by <@$user.ID> removed from <#$message.channel.ID>")
      }
    }

    messageReceived(/^!ping/) {
      respond("pong")
    }

    guildCreate(all()) { RegionDataService regionDS, BanWordDataService banWordDs ->
      // currently only support for 1 guild
      regions = regionDS.all
      banWords = banWordDs.all

      guild = event.guild
      roles = guild.roles
      mainChannel = guild.getChannelByID(mainChannelId)
      consoleChannel = guild.getChannelByID(consoleChannelId)

      generateBullets()

      regions.each { regionName, regionId ->
        listenForRegion(regionName)
      }

      banWords.each { banWord -> listenForBanWord(banWord) }
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

    // add banword
    messageReceived(/(?i)^!addBanWord [a-z]*$/, 'bot-console') { BanWordDataService banWordDs ->
      String banWord = parts[1]

      listenForBanWord(banWord)

      banWordDs.insert(banWord, user.ID, user.name)
      respond("banword `$banWord` added to list")
    }

    // remove banword
    messageReceived(/(?i)^!removeBanWord [a-z]*$/, 'bot-console') { BanWordDataService banWordDs ->
      String banWord = parts[1]

      unregister(banWord)

      banWordDs.delete(banWord)
      respond("banword `$banWord` removed from list")
    }

    // list banwords
    messageReceived(/(?i)^!banWords/, 'bot-console') { BanWordDataService banWordDs ->
      def banWordList = banWordDs.all.collect { "• $it" }.join('\n')
      respond('\n' + banWordList)
    }

  }
}