import static ch.qos.logback.classic.Level.INFO

appender("Console-Appender", ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  }
}

//logger("io.greeb.yo.dataServices.rss", DEBUG)

root(INFO, ["Console-Appender"])
