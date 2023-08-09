import config.AppConfig

object Main extends App {

  println(AppConfig.config.getString("app.name"))
  println("All ok!")

}
