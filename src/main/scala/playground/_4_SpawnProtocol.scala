package playground
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Props, SpawnProtocol}
import akka.util.Timeout

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
object _4_SpawnProtocol extends App {

  implicit val system: ActorSystem[SpawnProtocol.Command] =
    ActorSystem(SpawnProtocol(), "root")
  implicit val timeout: Timeout = 10.seconds

  val counterF: Future[ActorRef[Counter.Protocol]] =
    system.ask(Spawn(Counter.behavior, "counter", Props.empty, _))

  // foreach

  implicit val ec: ExecutionContext = system.executionContext
  counterF.foreach { counter =>
    counter ! Counter.Incr(1)
    val result: Future[Long] = counter ? Counter.Get
    println(Await.result(result, 1.minute))
    system.terminate()
  }


}
