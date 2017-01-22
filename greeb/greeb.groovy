import groovy.sql.Sql
import io.greeb.sql.LiquibaseService
import io.greeb.sql.SqlModule
import io.greeb.yo.dataServices.BanWordDataService
import io.greeb.yo.dataServices.DataServiceModule
import io.greeb.yo.dataServices.RegionDataService
import io.greeb.yo.dataServices.rss.RSSDataService
import io.greeb.yo.dataServices.rss.RSSService
import io.greeb.yo.housePoints.House
import io.greeb.yo.housePoints.HousePointService
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

  boolean welcomeMessageEnabled = true

  def hasRole = { IUser user, String roleId ->
    guild.getRolesForUser(user).any { it.ID == roleId }
  }

  def or = { Closure<Boolean> c1, Closure<Boolean> c2 ->
    return { MessageReceivedEvent event ->
      return c1.call(event) || c2.call(event)
    }
  }

  def isAdmin = { MessageReceivedEvent event ->
    hasRole(event.message.author, properties.botAdminRoleID)
  }

  def isSupport = { MessageReceivedEvent event ->
    hasRole(event.message.author, properties.supportRoleID)
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
          client.getOrCreatePMChannel(user).sendMessage(
                  "You are already assigned to $alreadyAssigned.name. If you wish to change region please use `!resetregion` if you wish to change your region use $regionBullets.")
        }
      }
    }

    def listenForBanWord = { banWord ->
      messageReceived(combine(not(privateChat()), not(channelNameMatches('bot-console')),
              messageMatches(/(?i)(^|\s)$banWord(\s|$)/))) {
        // delete message
        message.delete()
        // pm the user
        client.getOrCreatePMChannel(user).sendMessage(
                "your message `$content` has removed from <#$message.channel.ID>. If you think this is a mistake please contact Yo' Support")

        // post a line to console
        console("<@&272444639389417483> message `$content` by <@$user.ID> removed from <#$message.channel.ID>")
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

      if (welcomeMessageEnabled) {
        mainChannel.sendMessage("""\
        Sup, <@!$user.ID>
        <#250322357585969153> to Filthy Casuals. Please set your <#272057946135986176>, check <#195522076730195968>, create games, make friends and have fun. If you get stuck ask FUC buddies for help :whale: :sweat_drops:
        """.stripIndent())
        console("<@&272444639389417483>: <@!$user.ID> has joined")
      }
    }

    userJoin { UserJoinEvent event ->
      autoBanNames.any { it.equalsIgnoreCase(event.user.name) }
    } {
      guild.banUser(user)
      console("<@!$user.ID> has been auto-banned because their username is on the autoban list")
    }

    // delete any file uploaded with extension .exe
    // temporary removed until it's fixed
//    messageReceived { MessageReceivedEvent event ->
//      event.message.attachments.any({ it.filename.contains('.exe') })
//    } {
//      message.delete()
//      console("<@!$user.ID> tried to upload an .exe file, the file has been deleted")
//    }

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
        message =
                "Your region tag has been reset, To join a new region reply to this message with a region tag from the list below."
      } else {
        message =
                "You aren't assigned to any regions. To join a region reply to this message with a region tag from the list below."
      }

      client.getOrCreatePMChannel(user).sendMessage(message + regionBullets)
      console("<@!$user.ID> reset their region")
    }

    messageReceived(/!disableWelcomeMessage/, 'bot-console', isAdmin) {
      welcomeMessageEnabled = false
      console('welcome messagee disabled')
    }

    messageReceived(/!enableWelcomeMessage/, 'bot-console', isAdmin) {
      welcomeMessageEnabled = true
      console('welcome messagee enabled')
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

    messageReceived(
            combine(messageMatches(/(?i)^!help/), { MessageReceivedEvent e -> e.message.channel.name != 'bot-console' })) {
      respond('''\n\
        • `!ping` - check if the bot is working
        • `!regions` - get a list of regions
        • `!resetregion` - remove assigned region role
        • `!joinTeam(Instinct/Mystic/Valor)` - join the appropriate Pokemon Go channel
        * `!LFG` - get the looking for game role
        * `!NLFG` - remove the looking for game flag
        • `!houseStats` - Gives the number of points each house has
        • `!mypoints` - Tells you how many points you have earned this season'''.stripIndent())
    }

    messageReceived(/(?i)^!help/, 'bot-console') {
      respond('''\n\
        • `!ping` - check if the bot is working
        • `!regions` - get a list of regions
        • `!resetregion` - remove assigned region role
        • `!joinTeam(Instinct/Mystic/Valor)` - join the appropriate Pokemon Go channel
        * `!LFG` - get the looking for game role
        * `!NLFG` - remove the looking for game flag
        • `!houseStats` - Gives the number of points each house has
        • `!mypoints` - Tells you how many points you have earned this season
        -- admin commands (only work in bot-console)
        • `!createRegion [REGION]` - * creates a new region, can be 2-5 characters
        • `!deleteRegion [REGION]` - * deletes region
        • `!regionstats` - lists the number of users in each region
        • `!banWords` - lists all words on the block list
        • `!addBanWord [WORD]` - adds word to block list. If that word appears in any message the message will be deleted
        • `!removeBanWord [WORD]` - removes word from block list
        • `!addFeed [FEED URL]` - * adds a new feed to RSS (currently only supports a small subset of feed formats)
        • `!listFeeds` - Lists all current feeds and their IDs
        • `!removeFeed [FEED ID]` - * removes an RSS feed
        • `!enableWelcomeMessage` - * stops users being sent welcome messages, also stops house welcome messages
        • `!disableWelcomeMessage` - * re-enables welcome messages
        • `!houseMemberCount` - Gives the total number of members in each house
        • `!assignUnhoused` - * Assigns any users without a house to a house
        • `!honour @USER [POINTS] [REASON]` - Points will be awarded to the users house, can only be done by support
        • `!dishonour @USER [POINTS] [REASON]` - Points will be deducted to the users house, can only be done by support
        • `!purge` - Removes all users without a region from the guild

        * Admin role only'''.stripIndent())
    }

    messageReceived(/(?i)^!purge$/, 'bot-console', isAdmin) {
      // find users with no region
      def toRemove = guild.users.findAll {u -> !(u.getRolesForGuild(guild).collect({r->r.ID}).intersect(regions.values()))}.findAll({u -> u.name != 'greeb'})

      respond("${toRemove.size()} users with no region found, removing them...")

      toRemove.each { removeUser ->
        guild.kickUser(removeUser)
        sleep(2000)
      }

      respond("All users with no region have been removed.")
    }

    messageReceived(/(?i)^!addBanWord .*$/, 'bot-console') { BanWordDataService banWordDs ->
      String banWord = parts[1..parts.size()-1].join(' ')

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
      println "$user.name leaving ${lfgRole?.name}"
      if (lfgRole != null) {
        println 'removing role'
        guild.editUserRoles(user, (currentRoles - lfgRole) as IRole[])
      }
    }

    discordDisconnected { true } {
      LOGGER.error('Stopping application because of disconnect with discord. Supervisor should bring it back.')
      System.exit(1)
    }

    // ======= House Points ========
    guildCreate(all()) { HousePointService housePointService ->
      housePointService.initiate(guild)
    }

    messageReceived(/(?i)^!houseMemberCount/, 'bot-console') { HousePointService housePointService ->
      def message = housePointService.houseRoleUsers.collect { k, v -> "${k.role.name} - ${v.size()}" }.join('\n')

      respond("$message\nunassigned - ${housePointService.unassignedUsers.size()}")
    }

    messageReceived(/(?i)^!houseStats/) { HousePointService housePointService ->
      respond(housePointService.houses.collect { house -> "${house.role.name} - ${house.points}" }.join('\n'))
    }

    messageReceived(combine(messageMatches(/(?i)^!honou?r <@\d+> ?\d{1,5} .{5,}/), channelNameMatches('bot-console'), or(isSupport, isAdmin))) {
      HousePointService housePointService ->
        def receiver = guild.getUserByID(parts[1][2..-2])
        def points = parts[2].toInteger()
        def reason = parts[3..-1].join(' ')

        House house = housePointService.honour(receiver, points, reason)
        if (house) {
          console("${user.mention()} has awarded ${receiver.mention()}(${house.channel.mention()}) $points points because they're `$reason`")
          house.channel.sendMessage("Contrats $house.role.name, ${receiver.mention()} has been Honoured because they're $reason and earned your House $points Points. Your House now has $house.points points.")
        } else {
          console("${receiver.mention()} is not a member of any house and therefore can't receive honour")
        }
    }

    messageReceived(combine(messageMatches(/(?i)^!dishonou?r <@\d+> ?\d{1,5} .{5,}/), channelNameMatches('bot-console'), or(isSupport, isAdmin))) {
      HousePointService housePointService ->
        def receiver = guild.getUserByID(parts[1][2..-2])
        def points = parts[2].toInteger()
        def reason = parts[3..-1].join(' ')

        House house = housePointService.honour(receiver, -points, reason)
        if (house) {
          console("${user.mention()} has removed ${receiver.mention()}(${house.channel.mention()}) $points points because `$reason`")
          house.channel.sendMessage("$house.role.name, ${receiver.mention()} has broken House Rules and has been Dishonoured. $points have been deducted for your house. " +
                  "Redemption is always possible, help your Housemate out, promote a welcoming enviroment, play games and have fun to earn those points back.")
        } else {
          console("${receiver.mention()} is not a member of any house and therefore can't receive dishonour")
        }
    }

    messageReceived(/(?i)!myPoints/) { HousePointService housePointService ->
      Integer honour = housePointService.getHonour(user)
      House house = housePointService.getHouse(user)

      if (house) {
        client.getOrCreatePMChannel(user).sendMessage("This season you have earned your house $honour points. Your house(${house.channel.mention()}) currently has $house.points points")
      } else {
        client.getOrCreatePMChannel(user).sendMessage("You are not currently a member of a house message Yo' Support and they will randomly assign you to one")
      }
    }

    userLeave(all()) { HousePointService housePointService ->
      println "$event.user.name($event.user.ID) left"
      housePointService.userLeft(event.user)
    }

    messageReceived(/(?i)^!assignUnhoused/, 'bot-console') { HousePointService housePointService ->
      def unassignedUsers = housePointService.unassignedUsers

      respond("unassigned users ${housePointService.unassignedUsers.size()}")

      unassignedUsers.collect().each { unassignedUser ->
        housePointService.addUser(unassignedUser)
        sleep(2000)
      }

      console("finished assigning users to houses")
    }

    userJoin(all()) { HousePointService housePointService ->
      House newHouse = housePointService.addUser(user)
      if (welcomeMessageEnabled) {
        newHouse.channel.sendMessage("${newHouse.channel.mention()} - Please give a warm welcome your very new House member <@!$user.ID>!")
      }
    }


    messageReceived(combine(messageMatches(/^!message ([0-9]+)/), privateChat(), { MessageReceivedEvent event -> event.message.author.ID == '140266692155539456'})) {
      def channel = parts[1]
      def message = parts[2..parts.length-1].join(' ')

      client.getChannelByID(channel).sendMessage(message)
    }

  }
}