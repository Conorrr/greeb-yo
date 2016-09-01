import groovy.sql.Sql
import io.greeb.sql.LiquibaseService
import io.greeb.sql.SqlModule
import io.greeb.yo.dataServices.BanWordDataService
import io.greeb.yo.dataServices.DataServiceModule
import io.greeb.yo.dataServices.RegionDataService
import io.greeb.yo.dataServices.rss.RSSDataService
import io.greeb.yo.dataServices.rss.RSSService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sx.blah.discord.handle.impl.events.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.UserJoinEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IRole
import sx.blah.discord.handle.obj.IUser

import static io.greeb.core.discord.DiscordMatchers.*
import static io.greeb.core.dsl.DSL.greeb

greeb {
  Logger LOGGER = LoggerFactory.getLogger("io.greeb.yo")

  credentials new File('discord.token').text.trim()
  properties("properties.json")

  String mainChannelId = properties.mainChannelId
  String consoleChannelId = properties.consoleChannelId
  String gamingNewsChannelId = properties.gamingNewsChannelId
  List<String> autoBanNames = properties.autoBanNames

  Map<String, Map<String, String>> pokeTeams = properties.pokemon

  Map<String, String> regions
  List<String> banWords

  List<IRole> roles
  IGuild guild
  IChannel mainChannel
  IChannel consoleChannel
  IChannel gamingNewsChannel

  def hasRole = { IUser user, String roleId ->
    guild.getRolesForUser(user).any { it.ID == roleId }
  }

  def isAdmin = { MessageReceivedEvent event ->
    hasRole(event.message.author, properties.botAdminRoleID)
  }

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
      LOGGER.info(message)
    }

    def listenForRegion = { String regionName ->
      messageReceived(/(?i)^!$regionName$/) {
        List<IRole> currentRoles = user.getRolesForGuild(guild)

        def alreadyAssigned = currentRoles.find { regions.keySet().contains(it.name) }

        if (!alreadyAssigned) {
          def newRole = guild.getRoles().find { it.name == regionName.toUpperCase() }
          guild.editUserRoles(user, (currentRoles + newRole) as IRole[])
          client.getOrCreatePMChannel(user).sendMessage("You are now assigned to `$regionName`")
          console("<@!$user.ID> joined region $newRole.name")
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
        client.getOrCreatePMChannel(user).sendMessage("your message `$content` has removed from <#$message.channel.ID>. If you think this is a mistake please contact Yo' Support")

        // post a line to console
        console("message `$content` by <@$user.ID> removed from <#$message.channel.ID>")
      }
    }

    messageReceived(/^!ping/) {
      respond("pong")
    }

    guildCreate(all()) { RegionDataService regionDS, BanWordDataService banWordDs, RSSService rssService ->
      // currently only support for 1 guild
      regions = regionDS.all
      banWords = banWordDs.all

      guild = event.guild
      roles = guild.roles
      mainChannel = guild.getChannelByID(mainChannelId)
      consoleChannel = guild.getChannelByID(consoleChannelId)
      gamingNewsChannel = guild.getChannelByID(gamingNewsChannelId)

      rssService.start(gamingNewsChannel)

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
        Welcome to Calm Yo' <@!$user.ID> - Make yourself at home! Feel free to make games, set your region (!regions), check the latest bot commands (<#211829950163058688>) and imporantly give our <#195522076730195968> a quick read.
        HF 😃""".stripIndent())

      console("<@!106136360892514304>: <@!$user.ID> has joined")
    }

    userJoin { UserJoinEvent event ->
      autoBanNames.any { it.equalsIgnoreCase(event.user.name) }
    } {
      guild.banUser(user)
      console("<@!$user.ID> has been auto-banned because their username is on the autoban list")
    }

    // delete any file uploaded with extension .exe
    messageReceived { MessageReceivedEvent event ->
      event.message.attachments.any({ it.filename.contains('.exe') })
    } {
      message.delete()
      console("<@!$user.ID> tried to upload an .exe file, the file has been deleted")
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
      console("<@!$user.ID> reset their region")
    }

    messageReceived(/(?i)^!createRegion ([A-Z]{2,5})$/, 'bot-console', isAdmin) { RegionDataService regionDs ->
      def newRegionName = parts[1].toUpperCase()
      def newRole = guild.getRoles().find({ it.name == newRegionName })

      if (!newRole) {
        newRole = guild.createRole()
        newRole.changeName(newRegionName)
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

    messageReceived(/(?i)^!deleteRegion ([A-Z]{2,5})$/, 'bot-console', isAdmin) { RegionDataService regionDS ->
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
        • `!resetregion` - remove assigned region role
        • `!joinTeam(Instinct/Mystic/Valor)` - join the appropriate Pokemon Go channel
        * `!LFG` - get the looking for game role
        * `!NLFG` - remove the looking for game flag'''.stripIndent())
    }

    messageReceived(/(?i)^!help/, 'bot-console') {
      respond('''\n\
        • `!ping` - check if the bot is working
        • `!regions` - get a list of regions
        • `!resetregion` - remove assigned region role
        • `!joinTeam(Instinct/Mystic/Valor)` - join the appropriate Pokemon Go channel
        * `!LFG` - get the looking for game role
        * `!NLFG` - remove the looking for game flag
        -- admin commands (only work in bot-console)
        • `!createRegion [REGION]` - * creates a new region, can be 2-5 characters
        • `!deleteRegion [REGION]` - * deletes region
        • `regionstats` - lists the number of users in each region
        • `!banWords` - lists all words on the block list
        • `!addBanWord [WORD]` - adds word to block list. If that word appears in any message the message will be deleted
        • `!removeBanWord [WORD]` - removes word from block list
        • `!addFeed [FEED URL]` - * adds a new feed to RSS (currently only supports a small subset of feed formats)
        • `!listFeeds` - Lists all current feeds and their IDs
        • `!removeFeed [FEED ID]` - * removes an RSS feed

        * Admin role only'''.stripIndent())
    }

    messageReceived(/(?i)^!addBanWord [a-z]*$/, 'bot-console') { BanWordDataService banWordDs ->
      String banWord = parts[1]

      listenForBanWord(banWord)

      banWordDs.insert(banWord, user.ID, user.name)
      respond("banword `$banWord` added to list")
    }

    messageReceived(/(?i)^!removeBanWord [a-z]*$/, 'bot-console') { BanWordDataService banWordDs ->
      String banWord = parts[1]

      unregister(banWord)

      banWordDs.delete(banWord)
      respond("banword `$banWord` removed from list")
    }

    messageReceived(/(?i)^!banWords/, 'bot-console') { BanWordDataService banWordDs ->
      def banWordList = banWordDs.all.collect { "• $it" }.join('\n')
      respond('\n' + banWordList)
    }

    messageReceived(/(?i)^!addFeed https?:\/\/.*$/, 'bot-console', isAdmin) { RSSDataService rssDS ->
      String feedurl = parts[1]

      rssDS.addFeed(feedurl, user.ID, user.name)

      respond("Added feed: `$feedurl`")
    }

    messageReceived(/(?i)^!listFeeds$/, 'bot-console') { RSSDataService rssDS ->
      def feeds = rssDS.getAllFeeds()

      def response = '\n' + feeds.collect { "${it.key}.${it.value}" }.join('\n')

      respond(response)
    }

    messageReceived(/(?i)^!removeFeed \d+$/, 'bot-console', isAdmin) { RSSDataService rssDS ->
      Integer feedId = parts[1].toInteger()

      rssDS.delete(feedId)

      respond("Removed feed: `$feedId`")
    }

    pokeTeams.each { teamName, teamSettings ->
      messageReceived(/(?i)!joinTeam$teamName$/) {
        if (properties.pokemon.collect { (String) it.value.roleId }.any { hasRole(user, it) }) {
          return client.getOrCreatePMChannel(user).sendMessage('You are already a member of a pokemon team')
        }

        def newRole = guild.getRoleByID(teamSettings.roleId)

        List<IRole> currentRoles = user.getRolesForGuild(guild)
        guild.editUserRoles(user, (currentRoles + newRole) as IRole[])

        guild.getChannelByID(teamSettings.channelId).
                sendMessage("Team <@&${newRole.ID}>, You have a new Team Member! Welcome ${user.mention()}")
        console("<@!$user.ID> joined poketeam $teamName")
      }
    }

    messageReceived(/(?i)!lfg/) {
      List<IRole> currentRoles = user.getRolesForGuild(guild)

      IRole currentRegion = currentRoles.find { regions.keySet().contains(it.name) }

      if (!currentRegion) {
        return client.getOrCreatePMChannel(user).sendMessage('''\n
            Before using the Looking For Games feature, you\'ll need to set your region. :)
            Reply with `!regions` for a list of possible regions'''.stripIndent())
      }

      IRole lfgRole = guild.roles.find { it.name == "LFG - $currentRegion.name".toString() }
      if (lfgRole) {
        guild.editUserRoles(user, (currentRoles + lfgRole) as IRole[])
      } else {
        console("<@!106136360892514304>: `LFG - $currentRegion.name` role doesn't exist")
      }
    }

    messageReceived(/(?i)!nlfg/) {
      List<IRole> currentRoles = user.getRolesForGuild(guild)
      IRole lfgRole = currentRoles.find { it.name.contains('LFG') }
      if (lfgRole) {
        guild.editUserRoles(user, (currentRoles - lfgRole) as IRole[])
      }
    }

    discordDisconnected { true } {
      LOGGER.error('Stopping application because of disconnect with discord. Supervisor should bring it back.')
      System.exit(1)
    }

  }
}