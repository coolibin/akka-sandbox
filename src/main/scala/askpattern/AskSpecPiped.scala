package askpattern

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import askpattern.AskSpecPiped.PipedAuthManager

// Step 1 - import ask pattern
import akka.pattern.{ask, pipe}

import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import askpattern.AskSpecPiped.{AuthFailure, AuthManager, Authenticate, RegisterUser}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class AskSpecPiped extends TestKit(ActorSystem("AskSpecPiped"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)


  "An authenticator" should {
    import AskSpecPiped.AuthManager._
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

    "successfully authenticate a registered user" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! RegisterUser("daniel", "rtjvm")
      authManager ! Authenticate("daniel", "rtjvm")
      expectMsg(AskSpecPiped.AuthSuccess)
    }
  }

  "A piped authenticator" should {
    import AskSpecPiped.AuthManager._

    "fail to authenticate a non-registred user" in {
      val authManager = system.actorOf(Props[PipedAuthManager])
      authManager ! Authenticate("daniel", "rtjvm")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }

    "fail to authenticate if invalid password" in {
      val authManager = system.actorOf(Props[PipedAuthManager])
      authManager ! RegisterUser("daniel", "rtjvm")
      authManager ! Authenticate("daniel", "iloveakka")
      expectMsg(AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
    }

    "successfully authenticate a registered user" in {
      val authManager = system.actorOf(Props[PipedAuthManager])
      authManager ! RegisterUser("daniel", "rtjvm")
      authManager ! Authenticate("daniel", "rtjvm")
      expectMsg(AskSpecPiped.AuthSuccess)
    }
  }


}

object AskSpecPiped {

  case class Read(key: String)

  case class Write(key: String, value: String)


  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read a value at the key $key in(${kv.toString})")
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
    import AskSpecPiped.AuthManager._
    protected val authDb = context.actorOf(Props[KVActor])

    // Step 2 - logistics
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
        case Success(Some(dbPassword)) =>
          log.info(s"verifying password: $password with db: $dbPassword")

          if (dbPassword == password) originalSender ! AskSpecPiped.AuthSuccess
          else originalSender ! AskSpecPiped.AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
        case Failure(_) =>
          originalSender ! AskSpecPiped.AuthFailure(AUTH_FAILURE_SYSTEM)
      }
    }
  }


  ///////////////////////////////////////////////////////////

  class PipedAuthManager extends AuthManager {
    import AskSpecPiped.AuthManager._
    override def handleAuthentication(username: String, password: String): Unit = {

      // Step 3 - ask the actor
      val future = authDb ? Read(username) // Future[Any]

      // Step 4 - process the future until you get the responses you will send back
      val passwordFuture = future.mapTo[Option[String]] // Future[Option[String]]

      val responseFuture = passwordFuture.map {
        case None => AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(dbPassword) =>
          if (dbPassword == password) AuthSuccess
          else AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
      } // Future[Any] - will be completed with the response I will send back

      // Step 5 - pipe the resulting future to the actor you want to send the results to
      /*
      When the future completes, send the response to the actor ref in the arg list.
       */
      responseFuture.pipeTo(sender())
    }
  }


  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "password not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "password incorrect"
    val AUTH_FAILURE_SYSTEM = "system error"
  }
}
