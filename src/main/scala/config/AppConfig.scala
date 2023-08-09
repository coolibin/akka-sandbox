package config

import com.typesafe.config.{Config, ConfigFactory}

object AppConfig {
  lazy val config: Config = ConfigFactory.load()

}
