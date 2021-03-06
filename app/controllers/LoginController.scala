/**
 * Copyright (C) 2012 Kaj Magnus Lindberg (born 1979)
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

import actions.SafeActions._
import com.debiki.core._
import com.debiki.core.Prelude._
import java.{util => ju}
import play.api._
import play.api.mvc._
import play.api.mvc.BodyParsers.parse.empty


/** Logs in and out.
  */
object LoginController extends mvc.Controller {


  /** Opens a popup and a login dialog inside that popup. Useful when logging in
    * in an iframe, because it's not feasible to open modal dialogs from inside
    * iframes — only the iframe would be disabled by the modal dialog, but not
    * the rest of the page.
    */
  def showLoginPopup(mode: String, returnToUrl: String) = ExceptionAction(empty) { request =>
    Ok(views.html.login.loginPopup(
      mode = mode,
      serverAddress = s"//${request.host}",
      returnToUrl = returnToUrl)) as HTML
  }


  /** Clears login related cookies and OpenID and OpenAuth stuff.
    */
  def logout = mvc.Action(parse.empty) { request =>
    // Keep the xsrf cookie, so login dialog works:
    Ok.discardingCookies(DiscardingCookie("dwCoSid"))
  }

}
