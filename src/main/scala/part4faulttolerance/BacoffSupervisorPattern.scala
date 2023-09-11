package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{Backoff, BackoffOpts, BackoffSupervisor}

import java.io.File
import scala.concurrent.duration.DurationInt
import scala.io.Source

object BacoffSupervisorPattern extends App {

  case object ReadFile

  class FileBasedPersistentActor extends Actor with ActorLogging {

    var datasource: Source = null

    override def preStart(): Unit = {
      log.info("Persistent actor starting")
    }

    override def postStop(): Unit = {
      log.warning("Persistent actor has stopped")
    }

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      log.warning("Persistent actor restarting")
    }

    override def receive: Receive = {
      case ReadFile =>
        if (datasource == null)
          datasource = Source.fromFile(new File("src/main/resources/testfiles/important_data.txt"))
        log.info("I've just read some important data:" + datasource.getLines().toList)
    }

  }

  val system = ActorSystem("BackoffSupervisorDemo")
//  val simpleActor = system.actorOf(Props[FileBasedPersistentActor], "simpleActor")
//  simpleActor ! ReadFile

  val simpleSupervisorProps = BackoffSupervisor.props(
    Backoff.onFailure(
      Props[FileBasedPersistentActor],
      "simpleBackoffActor",
      3.seconds, // then 6, 12, 24
      30.seconds, // a cap
      0.2
    )
  )

//  val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")
//  simpleBackoffSupervisor ! ReadFile

  /**
   * simpleSupervisor
   *   - child called simpleBackoffActor(props of type FileBasedPersistentActor)
   *   - supervision strategy is the default one (restarting on everything)
   *     - first attempt after 3 seconds
   *     - next attempt is 2x the previous attempt
   * simpleSupervisor receives any message and forwards it to its child
   *
   */

  val stopSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[FileBasedPersistentActor],
      "stopBackoffActor",
      3.seconds,
      30.seconds,
      0.2
    ).withSupervisorStrategy(
      OneForOneStrategy() {
        case _ => akka.actor.SupervisorStrategy.Stop
      }
    )
  )

//  val simpleStopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
//  simpleStopSupervisor ! ReadFile

  class EagerFBPActor extends FileBasedPersistentActor {
    override def preStart(): Unit = {
      log.info("Eager actor starting")
      datasource = Source.fromFile(new File("src/main/resources/testfiles/important_data.txt"))
    }
  }

  //val eagerActor = system.actorOf(Props[EagerFBPActore])
  //ActorInitializationException => Stop

  val repeatedSupervisorProps = BackoffSupervisor.props(
    Backoff.onStop(
      Props[EagerFBPActor],
      "eagerActor",
      1.second,
      30.seconds,
      0.1
    )
  )
  val repeatedSupervisor = system.actorOf(repeatedSupervisorProps, "eagerSupervisor")

  /**
   * eagerSupervisor
   *   - child eagerActor
   *     - will die on start with ActorInitializationException
   *     - will trigger the supervision strategy => STOP eagerActor
   *   - backoff will kick in after 1 second, 2, 4, 8, 16
   *   if resource is available again - it will proceed normally
   */

}
