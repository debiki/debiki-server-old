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

package actions

import actions.SafeActions.{SessionAction, SessionRequest}
import com.debiki.core._
import com.debiki.core.Prelude._
import debiki._
import debiki.DebikiHttp._
import java.{util => ju}
import play.api._
import play.api.libs.json.JsValue
import play.api.mvc._
import requests._
import scala.concurrent.Future
import controllers.Utils


/** Play Framework Actions for requests to Debiki's HTTP API.
  *
  * Use PageRequest instead iff the request concerns one specific page only.
  */
object ApiActions {


  def AsyncGetAction(f: GetRequest => Future[Result]): mvc.Action[Unit] =
    PlainApiAction.async(BodyParsers.parse.empty)(f)

  def GetAction(f: GetRequest => Result) =
    PlainApiAction(BodyParsers.parse.empty)(f)


  def AdminGetAction(f: GetRequest => Result) =
    PlainApiActionAdminOnly(BodyParsers.parse.empty)(f)


  def JsonOrFormDataPostAction
        (maxBytes: Int)
        (f: ApiRequest[JsonOrFormDataBody] => Result) =
    PlainApiAction(
      JsonOrFormDataBody.parser(maxBytes = maxBytes))(f)


  def AsyncJsonOrFormDataPostAction
        (maxBytes: Int)
        (f: ApiRequest[JsonOrFormDataBody] => Future[Result]): mvc.Action[JsonOrFormDataBody] =
    PlainApiAction.async(
      JsonOrFormDataBody.parser(maxBytes = maxBytes))(f)

  /**
   * @deprecated Use ApiRequest[JsonOrFormDataBody] instead
   */
  def PostFormDataAction
        (maxUrlEncFormBytes: Int)
        (f: FormDataPostRequest => Result) =
    PlainApiAction(
      BodyParsers.parse.urlFormEncoded(maxLength = maxUrlEncFormBytes))(f)


  /**
   * If the JSON data is rather complex and cannot be represented as form-data,
   * then you cannot use JsonOrFormDataPostAction, and that's when you should
   * use this function.
   */
  def PostJsonAction
        (maxLength: Int)
        (f: JsonPostRequest => Result) =
    PlainApiAction(
      BodyParsers.parse.json(maxLength = maxLength))(f)


  def AdminPostJsonAction
        (maxLength: Int)
        (f: JsonPostRequest => Result) =
    PlainApiActionAdminOnly(
      BodyParsers.parse.json(maxLength = maxLength))(f)


  private val PlainApiAction = PlainApiActionImpl(adminOnly = false)
  private val PlainApiActionAdminOnly = PlainApiActionImpl(adminOnly = true)


  private def PlainApiActionImpl(adminOnly: Boolean) = new ActionBuilder[ApiRequest] {

    override def composeAction[A](action: Action[A]) = {
      SessionAction.async(action.parser) { request: Request[A] =>
        action(request)
      }
    }

    override def invokeBlock[A](
        genericRequest: Request[A], block: ApiRequest[A] => Future[Result]) = {

      // We've wrapped PlainApiActionImpl in a SessionAction which only provides SessionRequest:s.
      val request = genericRequest.asInstanceOf[SessionRequest[A]]

      val tenantId = DebikiHttp.lookupTenantIdOrThrow(request, Globals.systemDao)

      val dao = Globals.siteDao(siteId = tenantId,
         ip = realOrFakeIpOf(request), request.sidStatus.roleId)

      val user = Utils.loadUserOrThrow(request.sidStatus, dao)

      if (adminOnly && user.map(_.isAdmin) != Some(true))
        throwForbidden("DwE1GfK7", "Please login as admin")

      val apiRequest = ApiRequest[A](
        request.sidStatus, request.xsrfOk, request.browserId, user, dao, request)

      val result = block(apiRequest)
      result
    }
  }
}

