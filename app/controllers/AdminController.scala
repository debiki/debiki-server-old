/**
 * Copyright (C) 2013-2014 Kaj Magnus Lindberg (born 1979)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package controllers

import actions.ApiActions._
import actions.PageActions._
import com.debiki.core._
import com.debiki.core.Prelude._
import controllers.Utils.OkSafeJson
import debiki._
import java.{util => ju, io => jio}
import play.api._
import play.api.mvc.{Action => _, _}
import play.api.Play.current
import requests.PageRequest
import scala.concurrent.Future
import DebikiHttp._
import Utils.ValidationImplicits._
import Utils.{OkHtml, OkXml}



/** Shows the administrator, the moderator and the designer pages.
  */
object AdminController extends mvc.Controller {


  def viewAdminPage() = GetAction { apiReq =>
    if (apiReq.user.map(_.isAdmin) != Some(true)) {
      Future.successful(Ok(views.html.login.loginPage(
        xsrfToken = apiReq.xsrfToken.value,
        returnToUrl = apiReq.uri, title = "Login", message = Some(
          "Login as administrator to access this page."))))
    }
    else {
      val adminPageBody = views.html.adminPage(
        hostname = apiReq.host,
        minMaxJs = TemplateProgrammingInterface.minMaxJs,
        minMaxCss = TemplateProgrammingInterface.minMaxCss).body
      Future.successful(Ok(adminPageBody) as HTML withCookies
        mvc.Cookie(
          DebikiSecurity.XsrfCookieName, apiReq.xsrfToken.value,
          httpOnly = false))
    }
  }


  // Remove later. (Dupl code, but I'm going to remove it anyway)
  def viewAdminPageOld() = GetAction { apiReq =>
    if (apiReq.user.map(_.isAdmin) != Some(true))
      Future.successful(Ok(
        views.html.login.loginPage(xsrfToken = apiReq.xsrfToken.value,
          returnToUrl = apiReq.uri, title = "Login", message = Some(
            "Login as administrator to access this page."))))
    else
      Future.successful(Ok(
        views.html.adminPageOld(apiReq.host).body) as HTML withCookies
        mvc.Cookie(
          DebikiSecurity.XsrfCookieName, apiReq.xsrfToken.value,
          httpOnly = false))
  }

}
