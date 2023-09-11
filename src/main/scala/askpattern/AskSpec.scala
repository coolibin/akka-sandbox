package askpattern

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import askpattern.AskSpec.{AuthFailure, AuthManager, Authenticate, RegisterUser}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)


  "An authenticator" should {
    import AuthManager._
    "fail to authenticate a non-registred user" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! Authenticate("daniel", "rtjvm")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }

    "fail to authenticate if invalid password" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! RegisterUser("daniel", "rtjvm")
      authManager ! Authenticate("daniel", "iloveakka")
      expectMsg(AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
    }
  }


}

object AskSpec {

  case class Read(key: String)

  case class Write(key: String, value: String)


  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read a value at the key $key")
        sender() ! kv.get(key) // Option[String]
      case Write(key, value) =>
        log.info(s"Writing the value $value for the key $key")
        context.become(online(kv + (key -> value)))
    }
  }

  case class RegisterUser(username: String, password: String)

  case class Authenticate(username: String, password: String)

  case class AuthFailure(message: String)

  case object AuthSuccess

  class AuthManager extends Actor with ActorLogging {
    import AuthManager._
    private val authDb = context.actorOf(Props[KVActor])

    implicit val timeout: Timeout = Timeout(1.seconds)
    implicit val executionContext: ExecutionContext = context.dispatcher

    override def receive: Receive = {

      case RegisterUser(username, password) =>
        authDb ! Write(username, password)

      case Authenticate(username, password) =>
        handleAuthentication(username, password)
    }

    def handleAuthentication(username: String, password: String) = {
      val originalSender = sender()

      val future = authDb ? Read(username)
      future.onComplete {
        // NEVER CALL METHODS ON THE ACTOR INSTANCE OR ACCESS MUTABLE STATE IN .onComplete()!
        // avoid closing over the actor instance or mutable state
        case Success(None) =>
          originalSender ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(dbPassword) =>
          if (dbPassword == password) originalSender ! AuthSuccess
          else originalSender ! AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
        case Failure(_) =>
          originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM)
      }
    }
  }

  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "password not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "password incorrect"
    val AUTH_FAILURE_SYSTEM = "system error"
  }

}
