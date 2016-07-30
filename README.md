# Greeb-Yo

A discord bot made using [Greeb](https://github.com/Conorrr/greeb-core)

## Contact
Feel free to email me at greeb@greeb.io

## Commands
### Public
Anyone can carry out these commands
`!ping` - simple check to see if the server is up

`!regions` - PMs the user a list of available regions
`!resetregion` - Removes any region that the user has assigned to them
`![REGION]` - Assigns the user to a region if they are not already assigned

### Admin Only
These commands can only be used in bot-console channel
`!createRegion [REGION-PREFIX]` - Adds a role to the region list. Role must already exist
`!deleteRegion [REGION-PREFIX]` - Removes the role from the region list. This does not unassign the role from users 
`!regionsStats` - To begin with just lists each region and how many users are assigned to each region

## How to get channel + user ids
User Settings -> Appearance -> Enable Developer Mode

Then right click a user of channel and select 'Copy ID'