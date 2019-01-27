# lastkatka_bot
Helpful bot for telegram

## Build
### Linux
`./gradlew shadowJar`
### Windows
`gradlew.bat shadowJar`

## Configuration
You can set config vars in the lastkatkabot.yaml config or you can get them from system environment vars. To do this, write in yaml config
`variable: env(x)` where x is the name of system env variable.
Also you can set default value for the variable if there's no system env variable with such name: `variable: env(x:default_value)`.
For example:
`port: env(PORT:8443)`

As this bot is optimized for [heroku](https://heroku.com), don't forget to set __GRADLE_TASK__ to __shadowJar__ in heroku's config vars.
