package playground

import akka.actor.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._


object _1_CounterApp extends App {

  trait Protocol

  case class Incr(n: Long) extends Protocol

  case class Get(replyto: ActorRef) extends Protocol

  def behavior: Behavior[Protocol] = Behaviors.setup { ctx =>
    var value: Long = 0
    Behaviors.receiveMessage {
      case Incr(n) =>
        value += n
        Behaviors.same
      case Get(replyto) =>
        replyto ! value
        Behaviors.same
    }
  }

}
