package playground

import akka.actor.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

object _2_CounterApp extends App {

  trait Protocol

  case class Incr(n: Long) extends Protocol

  case class Get(replyto: ActorRef) extends Protocol

  private def counter(value: Long): Behavior[Protocol] = {
    Behaviors.receiveMessage {
      case Incr(n) =>
        counter(value + n)
      case Get(replyto) =>
        replyto ! value
        Behaviors.same
    }
  }

  def behavior: Behavior[Protocol] = counter(0)

  val system = ActorSystem(behavior, "counter")
  system ! Incr(1)
  system.terminate()
}


