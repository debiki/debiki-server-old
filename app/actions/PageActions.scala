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

import com.debiki.core._
import com.debiki.core.Prelude._
import controllers.Utils
import debiki._
import debiki.DebikiHttp._
import debiki.dao.SiteDao
import java.{util => ju}
import play.api._
import play.api.mvc.{Action => _, _}
import requests._
import scala.concurrent.Future


/**
 * Actions for requests that concern a single page.
 *
 * These actions attempt to redirect any incorrect, but almost correct,
 * request to the correct page path. And load per page permissions, and
 * construct and hand over a PageRequest to the action implementation.
 */
object PageActions {


  /**
   * Set `maySetCookies` to false if the response might be cached by
   * proxy servers, e.g. static JS and CSS. (Otherwise silly serveres
   * might serve the same cached XSRF cookie to everyone.)
   */
  def PageGetAction
        (pathIn: PagePath, pageMustExist: Boolean = true, fixPath: Boolean = true,
         maySetCookies: Boolean = true)
        (f: PageGetRequest => Future[SimpleResult]) =
    PageReqAction(BodyParsers.parse.empty)(
      pathIn, pageMustExist, fixPath = fixPath, maySetCookies = maySetCookies)(f)


  def FolderGetAction
        (pathIn: PagePath)
        (f: PageGetRequest => Future[SimpleResult]) =
    FolderReqAction(BodyParsers.parse.empty)(pathIn)(f)


  /**
   * Supports form data only.
   * @deprecated
   */
  def PagePostAction
        (maxUrlEncFormBytes: Int)
        (pathIn: PagePath, pageMustExist: Boolean = true, fixPath: Boolean = true)
        (f: PagePostRequest => Future[SimpleResult]) =
    PageReqAction(
      BodyParsers.parse.urlFormEncoded(maxLength = maxUrlEncFormBytes))(
      pathIn, pageMustExist, fixPath = fixPath)(f)


  /**
   * Works with both form data, and JSON (if the JSON is a map,
   * optionally with array values).
   * COULD replace all PagePostAction with PagePostAction2 and then rename
   * PagePostAction2 to PagePostAction.
   */
  def PagePostAction2
        (maxBytes: Int)
        (pathIn: PagePath, pageMustExist: Boolean = true, fixPath: Boolean = true)
        (f: PagePostRequest2 => Future[SimpleResult]) =
    PageReqAction(
      JsonOrFormDataBody.parser(maxBytes = maxBytes))(
      pathIn, pageMustExist, fixPath = fixPath)(f)


  def PageReqAction[A]
        (parser: BodyParser[A])
        (pathIn: PagePath, pageMustExist: Boolean, fixPath: Boolean,
         maySetCookies: Boolean = true)
        (f: PageRequest[A] => Future[SimpleResult])
        = CheckPathAction[A](parser)(
            pathIn, maySetCookies = maySetCookies, fixPath = fixPath) {
      (sidStatus, xsrfOk, browserId, pathOkOpt, dao, request) =>

    if (pathOkOpt.isEmpty && pageMustExist)
      throwNotFound("DwE0404", "Page not found")

    val tenantId = pathIn.tenantId
    val pagePath = pathOkOpt.getOrElse(pathIn)
    val (identity, user) = Utils.loadIdentityAndUserOrThrow(sidStatus, dao)
    val pageExists = pathOkOpt.isDefined

    val anyPageMeta = pagePath.pageId.flatMap(dao.loadPageMeta(_))
    if (pageExists && anyPageMeta.isEmpty)
      throwNotFound("DwE2WEb8", s"No page meta found, page id: ${pagePath.pageId.get}")

    // Load permissions.
    val permsReq = PermsOnPageQuery(
      tenantId = tenantId,
      ip = realOrFakeIpOf(request),
      loginId = sidStatus.loginId,
      identity = identity,
      user = user,
      pagePath = pagePath,
      pageMeta = anyPageMeta)

    val permsOnPage = dao.loadPermsOnPage(permsReq)
    if (!permsOnPage.accessPage)
      throwForbidden("DwE403DNI0", "You are not allowed to access that page.")

    // Construct the actual request.
    val pageReq = PageRequest[A](
      sid = sidStatus,
      xsrfToken = xsrfOk,
      browserId = browserId,
      identity = identity,
      user = user,
      pageExists = pageExists,
      pagePath = pagePath,
      pageMeta = anyPageMeta,
      permsOnPage = permsOnPage,
      dao = dao,
      request = request)()

    val result = f(pageReq)
    result
  }


  // For now. (COULD create a FolderRequest, later.)
  def FolderReqAction[A]
        (parser: BodyParser[A])
        (pathIn: PagePath)
        (f: PageRequest[A] => Future[SimpleResult])
    = SafeActions.CheckSidAction[A](parser, maySetCookies = true) {
        (sidStatus, xsrfOk, browserId, request) =>

    if (!pathIn.isFolderOrIndexPage)
      throwBadReq("DwE903XH3", s"Call on folders only, not pages: ${request.uri}")

    val dao = Globals.siteDao(siteId = pathIn.tenantId,
      ip = realOrFakeIpOf(request), sidStatus.roleId)

    val (identity, user) = Utils.loadIdentityAndUserOrThrow(sidStatus, dao)

    // Load permissions.
    val permsReq = PermsOnPageQuery(
      tenantId = pathIn.tenantId,
      ip = realOrFakeIpOf(request),
      loginId = sidStatus.loginId,
      identity = identity,
      user = user,
      pagePath = pathIn,
      pageMeta = None)

    val permsOnPage = dao.loadPermsOnPage(permsReq)
    if (!permsOnPage.accessPage)
      throwForbidden("DwE67BY2", "You are not allowed to access that page.")

    // Construct the actual request. COULD create and use a FolderRequest instead.
    val pageReq = PageRequest[A](
      sid = sidStatus,
      xsrfToken = xsrfOk,
      browserId = browserId,
      identity = identity,
      user = user,
      pageExists = false,
      pagePath = pathIn,
      pageMeta = None,
      permsOnPage = permsOnPage,
      dao = dao,
      request = request)()

    val result = f(pageReq)
    result
  }


  /**
   * Attempts to redirect almost correct requests to the correct path,
   * e.g. adds/removes an absent or superfluous trailing slash
   * or looks up a page id and finds out that the page
   * has been moved.
   */
  def CheckPathActionNoBody
        (pathIn: PagePath)
        (f: (SidStatus, XsrfOk, Option[BrowserId], Option[PagePath], SiteDao,
           Request[Option[Any]]) => Future[SimpleResult]) =
    CheckPathAction(BodyParsers.parse.empty)(pathIn)(f)


  def CheckPathAction[A]
        (parser: BodyParser[A])
        (pathIn: PagePath, maySetCookies: Boolean = true, fixPath: Boolean = true)
        (f: (SidStatus, XsrfOk, Option[BrowserId], Option[PagePath], SiteDao, Request[A]) =>
           Future[SimpleResult]) =
    SafeActions.CheckSidAction[A](parser, maySetCookies = maySetCookies) {
        (sidStatus, xsrfOk, browserId, request) =>
      val dao = Globals.siteDao(siteId = pathIn.tenantId,
         ip = realOrFakeIpOf(request), sidStatus.roleId)
      dao.checkPagePath(pathIn) match {
        case Some(correct: PagePath) =>
          if (correct.value == pathIn.value) {
            f(sidStatus, xsrfOk, browserId, Some(correct), dao, request)
          } else if (!fixPath) {
            f(sidStatus, xsrfOk, browserId, None, dao, request)
          } else {
            Future.successful(
              Results.MovedPermanently(correct.value))
          }
        case None => f(sidStatus, xsrfOk, browserId, None, dao, request)
      }
    }

}

