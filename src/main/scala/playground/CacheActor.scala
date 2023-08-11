package playground

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object CacheActor {
  trait Protocol

  case class Get(replyto: ActorRef[String]) extends Protocol

  case class RefreshAndGet(replyto: ActorRef[String]) extends Protocol

  case class Refresh(value: String) extends Protocol

  case class RefreshFailed(ex: Throwable) extends Protocol

  def behavior(source: => Future[String]): Behavior[Protocol] = {
    Behaviors.setup { ctx =>

      var current = "undefined"

      Behaviors.receiveMessage {
        case Get(replyto) =>
          replyto ! current
          Behaviors.same

        case RefreshAndGet(replyto) =>
          import ctx.executionContext
          source.onComplete {
            // var current is not accessible from here,
            // so using the message to update the value
            case Success(v) =>
              ctx.self ! Refresh(v)
              replyto ! v
            case Failure(exception) =>
              RefreshFailed(exception)
          }

          // shorter alternative
          //          ctx.pipeToSelf(source) {
          //            case Success(v) => Refresh(v)
          //            case Failure(ex) => RefreshFailed(ex)
          //          }

          Behaviors.same
        case Refresh(value) =>
          current = value
          Behaviors.same
      }
    }
  }

  def behaviorWithPostpone(source: => Future[String]): Behavior[Protocol] =
    Behaviors.setup { ctx =>
      var current = "undefined"
      Behaviors.withStash(capacity = 1000) { stash =>

        def waiting: Behavior[Protocol] = Behaviors.receiveMessage {
          case Refresh(value) =>
            current = value
            stash.unstashAll(working)
          case RefreshFailed(ex) =>
            stash.unstashAll(working)
          case other =>
            stash.stash(other) // keep in mind it can overflow
            Behaviors.same
        }

        def working: Behavior[Protocol] =
          Behaviors.receiveMessagePartial {
            case Get(replyto) =>
              replyto ! current
              Behaviors.same
            case RefreshAndGet(replyto) =>
              ctx.pipeToSelf(source) {
                case Success(v) =>
                  Refresh(v)
                case Failure(ex) =>
                  RefreshFailed(ex)
              }
              waiting
          }

        working
      }
    }

//  def behaviorWithTimers = {
//    Behaviors.withTimers[Int] { timers =>
//      timers.startSingleTimer(key = Tick, msg = Tick, 1 minute)
//
//    }
//  }
}
