/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.http.scaladsl.server

import java.util.concurrent.{ CountDownLatch, TimeUnit }
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.impl.util.WithLogCapturing
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, Uri }
import akka.stream.Materializer
import akka.stream.scaladsl.{ Sink, Source }
import akka.stream.testkit.Utils.assertAllStagesStopped
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import akka.event.LogSource

abstract class DontLeakActorsOnFailingConnectionSpecs(poolImplementation: String)
  extends AnyWordSpecLike with Matchers with BeforeAndAfterAll with WithLogCapturing {

  val config = ConfigFactory.parseString(s"""
    akka {
      # disable logs (very noisy tests - 100 expected errors)
      loglevel = DEBUG
      loggers = ["akka.http.impl.util.SilenceAllTestEventListener"]

      http.host-connection-pool.pool-implementation = $poolImplementation

      http.host-connection-pool.base-connection-backoff = 0 ms
    }""").withFallback(ConfigFactory.load())
  implicit val system: ActorSystem = ActorSystem("DontLeakActorsOnFailingConnectionSpecs-" + poolImplementation, config)
  implicit val materializer: Materializer = Materializer.createMaterializer(system)

  val log = Logging(system, getClass)(LogSource.fromClass)

  "Http.superPool" should {

    "not leak connection Actors when hitting non-existing endpoint" in {
      assertAllStagesStopped {
        val reqsCount = 100
        val clientFlow = Http().superPool[Int]()
        // host that will reply, important because if it is a host not replying it will
        // take too long to fail
        val host = "127.0.0.1"
        val port = 86 // (Micro Focus Cobol) unlikely to be used port in the "system ports" range
        val source = Source(1 to reqsCount)
          .map(i => HttpRequest(uri = Uri(s"http://$host:$port/test/$i")) -> i)

        val countDown = new CountDownLatch(reqsCount)
        val sink = Sink.foreach[(Try[HttpResponse], Int)] {
          case (resp, id) =>
            countDown.countDown()
            handleResponse(resp, id)
        }

        val running = source.via(clientFlow).runWith(sink)
        countDown.await(10, TimeUnit.SECONDS) should be(true)
        Await.result(running, 10.seconds)
      }
    }
  }

  private def handleResponse(httpResp: Try[HttpResponse], id: Int): Unit = {
    httpResp match {
      case Success(httpRes) =>
        system.log.error(s"$id: OK: (${httpRes.status.intValue}")
        httpRes.entity.dataBytes.runWith(Sink.ignore)

      case Failure(ex) =>
        system.log.debug(s"$id: FAIL $ex") // this is what we expect
    }
  }

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)
}

class LegacyPoolDontLeakActorsOnFailingConnectionSpecs extends DontLeakActorsOnFailingConnectionSpecs("legacy")
class NewPoolDontLeakActorsOnFailingConnectionSpecs extends DontLeakActorsOnFailingConnectionSpecs("new")
