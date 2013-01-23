/**
 * Copyright (c) 2012 Kaj Magnus Lindberg (born 1979)
 */

package test.e2e

import org.scalatest.{Suite, Suites, BeforeAndAfterAll}
import play.api.{test => pt}
import pt.Helpers.testServerPort
import com.debiki.v0._
import com.debiki.v0.Prelude._


/**
 * Starts a Debiki server and Google Chrome and empties the database.
 */
trait ChromeSuiteMixin extends BeforeAndAfterAll {
  self: Suite =>

  lazy val testServer = pt.TestServer(testServerPort, pt.FakeApplication())

  override def beforeAll() {
    ChromeDriverFactory.start()
    testServer.start()
    debiki.Debiki.SystemDao.emptyDatabase()
  }


  override def afterAll() {
    testServer.stop()
    ChromeDriverFactory.stop()
  }

}


/**
 * Runs all end to end tests. Empties the database and restarts the browser
 * before each specification.
 */
class EndToEndSuite extends Suites(
  new CreateSiteSpec {},
  new AdminDashboardSpec {},
  new AnonLoginSpec {})
  with ChromeSuiteMixin
