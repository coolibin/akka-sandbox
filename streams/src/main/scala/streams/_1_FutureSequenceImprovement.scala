package streams

import akka.actor._
import akka.stream.scaladsl._
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object _1_FutureSequenceImprovement extends App {

  private val log = LoggerFactory.getLogger("logger")
  implicit val actorSystem = ActorSystem("x")
  implicit val ec = scala.concurrent.ExecutionContext.global

  // To replace Future.sequence()
  def futureStreaming[A, B](input: List[A])(func: A => Future[B])(parallelism: Int): Future[Seq[B]] = {
    val builder = Source(input).mapAsync[B](parallelism)(func)
    builder.runWith(Sink.seq) // execution starts on "runWith" !
  }

  def func(x: Int): Future[Int] = {
    log.info(s"Initiating process for ${x}")
    Future{
      log.info(s"Running: $x -> ${x+100}")
      Thread.sleep(2000)
      x + 100
    }
  }

  val input: List[Int] = List.range(1, 100)
  val output: Future[Seq[Int]] = futureStreaming(input)(func)(10)

  val procs = output.map { xs =>
    xs.foreach(x =>  println(x.toString))
  }

  log.info("Started all processes!")

  Await.result(procs, 60.seconds)
  actorSystem.terminate()
}
