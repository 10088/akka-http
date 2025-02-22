/*
 * Copyright (C) 2018-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package docs.http.scaladsl.server

// format: OFF

//#source-quote
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TestKitFragmentSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  val routeFragment =
//#fragment
      pathEnd {
        get {
          complete {
            "Fragments of imagination"
          }
        }
      }
//#fragment

  // Synthetic route to enable pathEnd testing
  val testRoute = {
    pathPrefix("test") {
      routeFragment
    }
  }

  "The service" should {
    "return a greeting for GET requests" in {
      // tests:
      Get("/test") ~> testRoute ~> check {
        responseAs[String] shouldEqual "Fragments of imagination"
      }
    }

    "return a MethodNotAllowed error for PUT requests to the root path" in {
      // tests:
      Put("/test") ~> Route.seal(testRoute) ~> check {
        status shouldEqual StatusCodes.MethodNotAllowed
      }
    }
  }
}
//#source-quote

class TestKitTimeoutSpec extends AnyWordSpec with ScalatestRouteTest {
  //#timeout-setting
  import scala.concurrent.duration._
  import akka.http.scaladsl.testkit.RouteTestTimeout
  import akka.testkit.TestDuration

  implicit val timeout: RouteTestTimeout = RouteTestTimeout(5.seconds.dilated)
  //#timeout-setting
}
