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

import play.api.Play
import play.api.Play.current
import play.api.libs.json.JsValue


package object requests {


  /** A request with no post data.
    */
  type GetRequest = ApiRequest[Unit]


  /** A request with form data.
    * @deprecated Use ApiRequest[JsonOrFormDataBody] instead.
    */
  type FormDataPostRequest = ApiRequest[Map[String, Seq[String]]]


  type JsonPostRequest = ApiRequest[JsValue]


  /**
   * A PageRequest with no post data.
   */
  type PageGetRequest = PageRequest[Unit]


  /**
   * A PageRequest with post data.
   */
  @deprecated
  type PagePostRequest = PageRequest[Map[String, Seq[String]]]


  /** In prod mode: Always the real ip address of the client.
    * Otherwise: Any 'fakeIp' query string parameter value, or any 'dwCoFakeIp' cookie.
    * (If 'fakeIp' is specified, actions.SafeActions.scala copies the value to
    * the dwCoFakeIp cookie.)
    */
  def realOrFakeIpOf(request: play.api.mvc.Request[_]): String = {
    val isProd = Play.maybeApplication.map(_.mode == play.api.Mode.Prod) == Some(true)
    if (isProd)
      request.remoteAddress
    else
      request.queryString.get("fakeIp").flatMap(_.headOption).orElse(
        request.cookies.get("dwCoFakeIp").map(_.value)) getOrElse request.remoteAddress
  }

}
