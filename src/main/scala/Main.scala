//import akka.actor.ActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl._
import akka.util.Timeout
import config.AppConfig
import playground.Counter

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object Main extends App {

  implicit val system: ActorSystem[Counter.Protocol] = ???

  system ! Counter.Incr(1)

  implicit val timeout: Timeout = 10.seconds
  val result = system ? Counter.Get
  println(Await.result(result, 1.minutes))
  system.terminate()
}
