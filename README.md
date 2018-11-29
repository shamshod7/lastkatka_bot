# lastkatka_bot
Helpful bot for telegram

## Build
### Linux
`./gradlew shadowJar`
### Windows
`gradlew.bat shadowJar`

## Config vars
- mainAdmin - usually bot's owner
- lastvegan - id of the group where you play [VeganWars](https://t.me/veganwarsbot)
- tourgroup - id of the group where you only play VeganWars tournament
- tourgroupname - username of the tourgroup
- tourchannel - username of the tournament **channel**

You can set config vars in the yaml config or you can get them from system environment vars. To do this, write in yaml config
`variable: env(x)` where x is the name of system env variable.
Also you can set default value for the variable if there's no system env variable with such name: `variable: env(x:default_value)`.
For example:
`port: env(PORT:8443)`

As this bot is optimized for [heroku](https://heroku.com), don't forget to set __GRADLE_TASK__ to __shadowJar__ in heroku's config vars.
