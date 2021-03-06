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

import actions.PageActions._
import com.debiki.core._
import com.debiki.core.{liftweb => lw}
import com.debiki.core.Prelude._
import debiki._
import debiki.DebikiHttp._
import debiki.dao.SiteDao
import java.{util => ju, lang => jl}
import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.JsValue
import play.api.mvc.{Action => _, _}
import requests._


object Utils extends Results with http.ContentTypes {


  /**
   * Prefixes `<!DOCTYPE html>` to the reply, otherwise Internet Explorer
   * enters the terrible Quirks mode. Also sets the Content-Type header.
   */
  def OkHtml(htmlNode: xml.NodeSeq) =
    Ok(serializeHtml(htmlNode)) as HTML

  def OkHtmlBody(bodyNodes: xml.NodeSeq) =
    OkHtml(<body>{bodyNodes}</body>)

  def ForbiddenHtml(htmlNode: xml.NodeSeq) =
    Forbidden(serializeHtml(htmlNode)) as HTML

  def BadReqHtml(htmlNode: xml.NodeSeq) =
    BadRequest(serializeHtml(htmlNode)) as HTML

  /**
   * Adds doctype and serializes to html using a real HTML5 writer.
   *
   * Some pros with using a real HTML5 writer: it won't escape '"' when found
   * inside script tags (which is very annoying when you e.g. copy-paste
   * Twitter's Follow Button <script> elem).
   */
  def serializeHtml(htmlNode: xml.NodeSeq): String = {
    require(htmlNode.size == 1)
    "<!DOCTYPE html>\n"+ lw.Html5.toString(htmlNode.head)
  }


  /**
   * Prefixes the JSON string with characters that prevents the JSON
   * from being parsed as Javascript from a <script> tag.
   * This supposedly thwarts a JSON vulnerability that allows third
   * party websites to turn your JSON resource URL into JSONP
   * request under some conditions, see:
   *   "JSON Vulnerability Protection", here:
   *      http://docs.angularjs.org/api/ng.$http
   *   and:
   *     http://haacked.com/archive/2008/11/20/
   *        anatomy-of-a-subtle-json-vulnerability.aspx
   * Debiki's Javascript, and AngularJS, strips the ")]}'," prefix before
   * parsing the JSON.
   */
  def OkSafeJson(json: JsValue) =
    Ok(")]}',\n" + json.toString) as JSON


  /**
   * Prefixes `<?xml version=...>` to the post data.
   */
  def OkXml(xmlNode: xml.NodeSeq, contentType: String = "text/xml") =
    Ok("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+ xmlNode) as contentType


  def renderOrRedirect(pageReq: PageRequest[_]): Result = {
    if (isAjax(pageReq.request)) {
      val pageHtml = pageReq.dao.renderTemplate(pageReq)
      Ok(pageHtml) as HTML
    } else {
      val queryString =
         queryStringAndHashToView(pageReq.pageRoot, pageReq.oldPageVersion)
      Redirect(pageReq.pagePath.value + queryString)
    }
  }


  def formHtml(pageReq: PageRequest[_]) =
    HtmlForms(pageReq.xsrfToken.value, pageReq.pageRoot, pageReq.permsOnPage)


  def queryStringAndHashToView(pageRoot: AnyPageRoot, pageVersion: Option[ju.Date],
        actionId: Option[ActionId] = None, forceQuery: Boolean = false)
        : String = {
    var params = List[String]()
    if (pageVersion.isDefined) params ::= s"version=${toIso8601T(pageVersion.get)}"

    pageRoot match {
      case None =>
        ???
      case DefaultPageRoot =>
         if (params.nonEmpty)
           params ::= "?view"
      case Some(commentId) =>
        params ::= "?view=" + commentId
    }

    var queryString = params.mkString("&")
    if (queryString.isEmpty && forceQuery) queryString = "?"
    val hash = actionId.map("#post-"+ _) getOrElse ""
    queryString + hash
  }


  // COULD move to new object debiki.Utils?
  def isPublicArticlePage(pagePath: PagePath): Boolean =
    !isPrivatePage(pagePath) && !pagePath.isFolderOrIndexPage


  def isPrivatePage(pagePath: PagePath): Boolean =
    pagePath.isConfigPage || pagePath.isHiddenPage


  /**
   * Loads a user from the database.
   * Verifies that the loaded id match the id encoded in the session identifier,
   * and throws a LoginNotFoundException on mismatch (happens e.g. if
   * I've connected the server to another backend, or access many backends
   * via the same hostname but different ports).
   */
  def loadUserOrThrow(sid: SidStatus, dao: SiteDao): Option[User] = {
    val user = sid.userId match {
      case None => None
      case Some(userId) =>
        dao.loadUser(userId) match {
          case Some(user) =>
            if (Some(user.id) == sid.userId) {
              // Fine.
              Some(user)
            }
            else {
              // Sometimes I access different databases via different ports,
              // but from the same host name. Browsers, however, usually ignore
              // port numbers when sending cookies. So they sometimes send
              // the wrong login-id and user-id to the server.
              Logger.warn(
                s"DAO loaded wrong user, session: $sid, role: $user [error DwE9kD4]")
              throw LoginNotFoundException(dao.siteId, userId)
            }
          case None =>
            // This might happen 1) if the server connected to a new database
            // (e.g. a standby where the login entry hasn't yet been
            // created), or 2) during testing, when I sometimes manually
            // delete stuff from the database (including login entries).
            Logger.warn("DAO did not load user [error DwE01521ku35]")
            throw LoginNotFoundException(dao.siteId, userId)
        }
    }
    user
  }


  case class LoginNotFoundException(tenantId: String, userId: String)
     extends Exception("No user with id: "+ userId +", site id: "+ tenantId)


  def parseIntOrThrowBadReq(text: String, errorCode: String = "DwE50BK7"): Int = {
    try {
      text.toInt
    }
    catch {
      case ex: NumberFormatException =>
        throwBadReq(s"Not an integer: ``$text''", errorCode)
    }
  }


  object ValidationImplicits {

    implicit def queryStringToValueGetter(
        queryString: Map[String, Seq[String]]) =
      new FormInpReader(queryString)

    implicit def pageReqToFormInpReader(pageReq: PagePostRequest) =
      new FormInpReader(pageReq.request.body)

    implicit def seqToSeqChecker[A](seq: Seq[A]) =
      new SeqChecker[A](seq)

    implicit def textToTextChecker(text: String) =
      new TextChecker(text)

    /**
     * Adds rich methods like `getEmptyAsNone` to a PagePostRequest.
     */
    class FormInpReader(val body: Map[String, Seq[String]]) {

      def getFirst(param: String): Option[String] =
        body.get(param).map(_.head)

      def getOrThrowBadReq(param: String): String =
        body.get(param).map(_.head) getOrElse throwBadReq(
          "DwE03Jk5", "Parameter missing: "+ param)

      def getEmptyAsNone(param: String): Option[String] =
        body.get(param).map(_.head) match {
          case None => None
          case Some("") => None
          case s: Some[_] => s
        }

      def getNoneAsEmpty(param: String): String =
        body.get(param).map(_.head) match {
          case None => ""
          case Some(s) => s
        }

      def getLong(param: String): Option[Long] =
        getFirst(param) map { value =>
          try { value.toLong }
          catch {
            case ex: jl.NumberFormatException =>
              throwBadReq("DwE4XK71", s"Param `$param' is not an Long, it is: `$value'")
          }
        }

      def getInt(param: String): Option[Int] =
        getFirst(param) map { value =>
          try { value.toInt }
          catch {
            case ex: jl.NumberFormatException =>
              throwBadReq("DwE4XK71", s"Param `$param' is not an Int, it is: `$value'")
          }
        }

      def getBool(param: String): Option[Boolean] =
        getFirst(param).map(_ == "t")

      def getBoolOrFalse(param: String): Boolean =
        getBool(param) getOrElse false

      def getBoolOrTrue(param: String): Boolean =
        getBool(param) getOrElse true

      def listSkipEmpty(param: String): Seq[String] = {
        body.get(param) match {
          case None => Nil
          case Some(values) => values.filterNot(_ isEmpty)
        }
      }
    }

    /**
     * Pimps class Seq with som form input validation helpers.
     */
    class SeqChecker[A](val seq: Seq[A]) {
      def ifEmpty(block: => Unit): Seq[A] = {
        if (seq isEmpty) block
        seq
      }
    }

    /**
     * Pimps class String with som form input validation helpers.
     */
    class TextChecker(val text: String) {
      def ifNotOneOf(chars: String, block: => Unit): String = {
        if (!(chars contains text)) block
        text
      }
    }
  }


  /** Groups a list of (PageId, Action) by page id, so it becomes a Map[PageId, Seq[Action]].
    */
  implicit class ActionsByPageIdGrouper[A](pageIdsAndActions: Seq[(PageId, RawPostAction[A])]) {
    def groupedByPageId: Map[PageId, Seq[RawPostAction[A]]] =
      pageIdsAndActions groupBy (_._1) mapValues {
        pageIdsAndActions: Seq[(PageId, RawPostAction[A])] =>
          pageIdsAndActions.map(_._2)
      }
  }


  @deprecated("Use `groupedByPageId` instead, see controllers.Pin", since = "now")
  def parsePageActionIds[A](
        pageActionIds: List[Map[String, String]])(fn: (ActionId) => A)
        : Map[String, List[A]] = {

    val pagesAndThings: List[(String, A)] = pageActionIds map { pageActionId =>
      val pageId = pageActionId("pageId")
      val actionIdStr = pageActionId("actionId")
      val actionId = parseIntOrThrowBadReq(actionIdStr, "DwE77BH3")
      pageId -> fn(actionId)
    }

    val thingsByPageId: Map[String, List[A]] =
      pagesAndThings groupBy (_._1) mapValues { somePageIdsThings: List[(String, A)] =>
        somePageIdsThings.map(_._2)
    }

    thingsByPageId
  }


  def parsePathRanges(baseFolder: String, queryString: Map[String, Seq[String]],
        urlParamPrefix: String = "in"): PathRanges = {

    import Utils.ValidationImplicits._

    def makeListOfParamValues(paramName: String): List[String] = {
      val pathsString = queryString.getEmptyAsNone(paramName) getOrElse {
        return Nil
      }
      val pathsListNoPrefix = pathsString.split(",").toList
      pathsListNoPrefix map { path =>
        val isAbsolute = path.startsWith("/")
        if (isAbsolute) path else baseFolder + path
      }
    }

    var folderPathsList = List[String]()
    var treePathsList = List[String]()
    var pageIdsList = List[String]()
    val forWholeTree = queryString.getFirst(urlParamPrefix +"-tree")
    val forCurFolder = queryString.getFirst(urlParamPrefix +"-folder")

    if (forWholeTree isDefined) {
      // Include everything in the tree designated by basePath.
      // Need consider no other parameters (parent paths like "/../" are not
      // supported).
      treePathsList = List(baseFolder)
    } else {
      treePathsList = makeListOfParamValues(urlParamPrefix +"-trees")
      folderPathsList = makeListOfParamValues(urlParamPrefix +"-folders")
      pageIdsList = queryString.getEmptyAsNone("for-pages")
         .map(_.split(",").toList) getOrElse Nil
      if (forCurFolder isDefined) {
        folderPathsList ::= baseFolder
      }
    }

    // List folder contents, by default.
    if (folderPathsList.isEmpty && treePathsList.isEmpty && pageIdsList.isEmpty)
      folderPathsList ::= baseFolder

    PathRanges(folders = folderPathsList, trees = treePathsList,
        pageIds = pageIdsList)
  }

}

