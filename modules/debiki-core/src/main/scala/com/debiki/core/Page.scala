/**
 * Copyright (C) 2012-2013 Kaj Magnus Lindberg (born 1979)
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

package com.debiki.core

import java.{util => ju}
import Prelude._



trait HasPageMeta {
  self: {
    def meta: PageMeta
    def ancestorIdsParentFirst: List[PageId]
  } =>

  require(meta.parentPageId == ancestorIdsParentFirst.headOption,
    o"""Parent page id and ancestor ids mismatch, parent: ${meta.parentPageId}, ancestors:
      ${ancestorIdsParentFirst} [DwE0FBY8]""")

  def id = meta.pageId
  def role = meta.pageRole
  def parentPageId = meta.parentPageId

  // Useful if referring to instance as e.g. "pathAndMeta", without "page" in the name.
  def pageId = meta.pageId
}



trait HasPagePath {
  self: { def path: PagePath } =>

  def anyId = path.pageId
  @deprecated("now", "use `siteId` instead")
  def tenantId = path.tenantId
  def siteId = path.tenantId
  def folder = path.folder
  def slug = path.pageSlug
  def idShownInUrl = path.showId
}



object Page {

  def apply(pathAndMeta: PagePathAndMeta, parts: PageParts): Page = Page(
    meta = pathAndMeta.meta,
    path = pathAndMeta.path,
    ancestorIdsParentFirst = pathAndMeta.ancestorIdsParentFirst,
    parts = parts)

  def newPage(
        pageRole: PageRole,
        path: PagePath,
        parts: PageParts,
        publishDirectly: Boolean = false,
        author: User,
        url: Option[String] = None): Page = {
    val partsInclAuthor = parts + author
    val meta = PageMeta.forNewPage(
      pageRole,
      author,
      parts = partsInclAuthor,
      creationDati = parts.oldestDati getOrElse new ju.Date,
      publishDirectly = publishDirectly,
      url = url)
    Page(meta, path, ancestorIdsParentFirst = Nil, partsInclAuthor)
  }

  def newEmptyPage(pageRole: PageRole, path: PagePath, author: User) =
    newPage(pageRole, path, PageParts(guid = "?"), author = author)

  def isOkayId(id: String): Boolean =
    id forall { char =>
      def isLower = 'a' <= char && char <= 'z'
      def isUpper = 'A' <= char && char <= 'Z'
      def isDigit = '0' <= char && char <= '9'
      isDigit || isLower || isUpper || char == '_'
    }
}


/** A Page can be a blog post, a forum topic, a forum topic list, a Wiki page,
  * a Wiki main page, or a site's homepage — for example.
  *
  * @param meta Meta info on the page, e.g. creation date and author user id.
  * @param path Where the page is located: site id + URL path to the page.
  * @param parts Page contents: title, body and comments.
  */
case class Page(
  meta: PageMeta,
  path: PagePath,
  ancestorIdsParentFirst: List[PageId],
  parts: PageParts) extends HasPageMeta with HasPagePath {

  requireMetaMatchesPaths(this)
  require(meta.pageId == parts.id)

  def hasIdAssigned = id != "?"

  def copyWithNewId(newId: String) =
    copy(
      meta = meta.copy(pageId = newId),
      path = path.copy(pageId = Some(newId)),
      parts = parts.copy(guid = newId))

  def copyWithNewSiteId(newSiteId: String) =
    copy(path = path.copy(tenantId = newSiteId))

  def copyWithNewAncestors(newAncestorIdsParentFirst: List[PageId]): Page =
    copy(
      meta = meta.copy(parentPageId = newAncestorIdsParentFirst.headOption),
      ancestorIdsParentFirst = newAncestorIdsParentFirst)

  def withoutPath = PageNoPath(parts, ancestorIdsParentFirst, meta)
}


/** A page that does not know what it contains (the `parts` fields is absent).
  */
case class PagePathAndMeta(
  path: PagePath,
  ancestorIdsParentFirst: List[PageId],
  meta: PageMeta)
  extends HasPagePath with HasPageMeta {

  requireMetaMatchesPaths(this)
}



/** Helper function that checks that page meta and page path IDs matches. */
object requireMetaMatchesPaths {
  def apply(page: {
    def meta: PageMeta
    def path: PagePath
    def ancestorIdsParentFirst: List[PageId]
  }) {
    if (page.path.pageId.isDefined) require(page.meta.pageId == page.path.pageId.get)
    else require(page.meta.pageId == "?")

    require(page.ancestorIdsParentFirst.headOption == page.meta.parentPageId,
      o"""meta.parentPageId != ancestorIdsParentFirst.head:
    ${page.meta.parentPageId} and ${page.ancestorIdsParentFirst}, page id: ${page.meta.pageId}""")
  }
}



/** A page that does not know where it's located (it doesn't know its URL or ancestor ids).
  */
case class PageNoPath(parts: PageParts, ancestorIdsParentFirst: List[PageId], meta: PageMeta)
  extends HasPageMeta {

  def +(user: User): PageNoPath =
    copy(parts = parts + user)

  def +(actionDto: RawPostAction[_]): PageNoPath =
    copy(parts = parts + actionDto)

}



object PageMeta {

  def forNewPage(
        pageRole: PageRole,
        author: User,
        parts: PageParts,
        creationDati: ju.Date = new ju.Date,
        parentPageId: Option[String] = None,
        url: Option[String] = None,
        publishDirectly: Boolean = false) =
    PageMeta(
      pageId = parts.pageId,
      pageRole = pageRole,
      creationDati = creationDati,
      modDati = creationDati,
      pubDati = if (publishDirectly) Some(creationDati) else None,
      parentPageId = parentPageId,
      embeddingPageUrl = url,
      pageExists = false,
      cachedTitle = parts.maybeUnapprovedTitleText,
      cachedAuthorDispName = author.displayName,
      cachedAuthorUserId = author.id,
      cachedNumPosters = parts.numPosters,
      cachedNumActions = parts.actionCount,
      cachedNumLikes = parts.numLikes,
      cachedNumWrongs = parts.numWrongs,
      cachedNumPostsToReview = parts.numPostsToReview,
      cachedNumPostsDeleted = parts.numPostsDeleted,
      cachedNumRepliesVisible = parts.numRepliesVisible,
      cachedLastVisiblePostDati = parts.lastVisiblePostDati,
      cachedNumChildPages = 0)

  def forChangedPage(originalMeta: PageMeta, changedPage: PageParts): PageMeta = {
    require(changedPage.id == originalMeta.pageId)

    // Re the page modification dati: Sometimes an empty page is created,
    // and then, later on, a title (for example) is added to the page. But this
    // title might have an older creation time than the page itself, if the
    // title was created in-memory before the page. Then use the page's
    // modification time, not the title's, so we won't accidentally set the
    // page modification time to *before* its creation time.
    val modifiedAt = new ju.Date(math.max(
      changedPage.modificationDati.map(_.getTime) getOrElse 0: Long,
      originalMeta.modDati.getTime))

    val authorUserId = originalMeta.cachedAuthorUserId orIfEmpty {
      changedPage.body.map(_.userId) getOrElse ""
    }

    val authorDispName = originalMeta.cachedAuthorDispName orIfEmpty {
      changedPage.body.flatMap(post => post.user.map(_.displayName)) getOrElse ""
    }

    originalMeta.copy(
      cachedTitle = changedPage.approvedTitleText,
      modDati = modifiedAt,
      cachedAuthorDispName = authorDispName,
      cachedAuthorUserId = authorUserId,
      cachedNumPosters = changedPage.numPosters,
      cachedNumActions = changedPage.actionCount,
      cachedNumLikes = changedPage.numLikes,
      cachedNumWrongs = changedPage.numWrongs,
      cachedNumPostsDeleted = changedPage.numPostsDeleted,
      cachedNumRepliesVisible = changedPage.numRepliesVisible,
      cachedNumPostsToReview = changedPage.numPostsToReview,
      cachedLastVisiblePostDati = changedPage.lastVisiblePostDati)
    // (cachedNumChildPages is updated elsewhere — when a child page is created.)
  }

}


/** @param pageId
  * @param pageRole
  * @param creationDati
  * @param modDati
  * @param pubDati
  * @param sgfntModDati
  * @param parentPageId
  * @param embeddingPageUrl The canonical URL to the page, useful when linking to the page.
  *            Currently only needed and used for embedded comments, and then it
  *            is the URL of the embedding page.
  * @param pageExists
  * @param cachedTitle
  * @param cachedAuthorDispName
  * @param cachedAuthorUserId
  * @param cachedNumPosters
  * @param cachedNumActions
  * @param cachedNumLikes
  * @param cachedNumWrongs
  * @param cachedNumPostsDeleted
  * @param cachedNumRepliesVisible
  * @param cachedNumPostsToReview
  * @param cachedNumChildPages
  * @param cachedLastVisiblePostDati
  */
case class PageMeta(
  pageId: String,
  pageRole: PageRole,
  creationDati: ju.Date,
  modDati: ju.Date,
  pubDati: Option[ju.Date] = None,
  sgfntModDati: Option[ju.Date] = None,
  parentPageId: Option[String] = None,
  embeddingPageUrl: Option[String],
  pageExists: Boolean = true,
  cachedTitle: Option[String] = None,
  cachedAuthorDispName: String,
  cachedAuthorUserId: String,
  cachedNumPosters: Int = 0,
  cachedNumActions: Int = 0,
  cachedNumLikes: Int = 0,
  cachedNumWrongs: Int = 0,
  cachedNumPostsDeleted: Int = 0,
  cachedNumRepliesVisible: Int = 0,
  cachedNumPostsToReview: Int = 0,
  cachedNumChildPages: Int = 0,
  cachedLastVisiblePostDati: Option[ju.Date] = None) {

  def status: PageStatus =
    if (pubDati.isDefined) PageStatus.Published
    else PageStatus.Draft

}



sealed abstract class PageRole {

  /** True if this page is e.g. a blog or a forum — they can have child pages
    * (namely blog posts, forum topics).
    */
  def isSection: Boolean = false
}


object PageRole {

  case object HomePage extends PageRole

  case object WebPage extends PageRole

  case object Code extends PageRole

  case object SpecialContent extends PageRole

  case object EmbeddedComments extends PageRole

  case object Blog extends PageRole {
    override def isSection = true
  }

  case object BlogPost extends PageRole

  case object Forum extends PageRole {
    override def isSection = true
  }

  case object ForumCategory extends PageRole {
    override val isSection = true
  }

  case object ForumTopic extends PageRole

  case object WikiMainPage extends PageRole {
    override def isSection = true
  }

  case object WikiPage extends PageRole


  // Hmm, regrettably this breaks should I rename any case object.
  // Perhaps use a match ... case list instead?
  private val _PageRoleLookup = Vector(
    HomePage, WebPage, EmbeddedComments, Blog, BlogPost,
    Forum, ForumCategory, ForumTopic,
    WikiMainPage, WikiPage,
    Code).map(x => (x, x.toString))

  def parse(pageRoleString: String): PageRole =
    _PageRoleLookup.find(_._2 == pageRoleString).map(_._1).getOrElse(
      illArgErr("DwE930rR3", s"Bad page role: `$pageRoleString'"))

}



/**
 * The page status, see debiki-for-developers.txt #9vG5I.
 */
sealed abstract class PageStatus
object PageStatus {
  // COULD rename to PrivateDraft, becaus ... other pages with limited
  // visibility might be considered Drafts (e.g. pages submitted for review).
  case object Draft extends PageStatus
  //COULD rename to Normal, because access control rules might result in
  // it effectively being non-pulbished.
  case object Published extends PageStatus

  case object Deleted extends PageStatus
  val All = List(Draft, Published, Deleted)

  def parse(text: String): PageStatus = text match {
    case "Draft" => Draft
    case "Published" => Published
    case "Deleted" => Deleted
    case x => illArgErr("DwE3WJH7", s"Bad page status: `$x'")
  }
}



/** How to sort pages, and where to start listing them, e.g. if fetching additional
  * pages after the user has scrolled down to the end of a page list.
  */
sealed abstract class PageOrderOffset

object PageOrderOffset {
  case object Any extends PageOrderOffset
  case object ByPath extends PageOrderOffset
  case object ByPublTime extends PageOrderOffset
  case class ByBumpTime(offset: Option[ju.Date]) extends PageOrderOffset
  case class ByLikesAndBumpTime(offset: Option[(Int, ju.Date)]) extends PageOrderOffset
}



case class PagePostId(pageId: PageId, postId: PostId) {
  def toList: List[AnyRef] = List(pageId, postId.asInstanceOf[AnyRef])
}


case class Category(categoryName: String, pageId: String, subCategories: Seq[Category])

