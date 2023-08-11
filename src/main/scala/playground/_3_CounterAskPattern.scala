package playground

import akka.actor.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object _3_CounterAskPattern extends App {

  implicit val system: ActorSystem[Counter.Protocol] = ???

  system ! Counter.Incr(1)

  implicit val timeout: Timeout = 10.seconds

  val result = system ? Counter.Get

  println(Await.result(result, 1.minute))

  system.terminate()
}


