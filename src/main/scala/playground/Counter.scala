package playground

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object Counter {
  trait Protocol

  case class Incr(n: Long) extends Protocol

  case class Get(replyto: ActorRef[Long]) extends Protocol

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
}
