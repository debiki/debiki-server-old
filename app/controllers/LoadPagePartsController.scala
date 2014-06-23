/**
 * Copyright (C) 2013 Kaj Magnus Lindberg (born 1979)
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
import com.debiki.core._
import controllers.Utils.OkSafeJson
import debiki._
import debiki.DebikiHttp._
import java.{util => ju}
import play.api._
import scala.concurrent.Future
import Prelude._
import BrowserPagePatcher.TreePatchSpec



/** Loads parts of a page, e.g. a single comment, a tree or a thread.
  *
  * Use PageViewer to view a complete page however — it loads more stuff,
  * e.g. permissions and user specific data for the page.
  */
object LoadPagePartsController extends mvc.Controller {


  def loadTrees =
    loadTreesImpl { (page, postIds) =>
      postIds.map(TreePatchSpec(_, wholeTree = true))
    }


  def loadPosts = PostJsonAction(maxLength = 5000) { apiReq =>
    SECURITY // What about access control?! Page ids generally unknown however, but
    // should really fix anyway.
    // Ensure I fix `loadThreads` at the same time.

    val pageActionIds = apiReq.body.as[List[Map[String, String]]]
    val postIdsByPage: Map[PageId, List[PostId]] =
      Utils.parsePageActionIds(pageActionIds)(identity)

    var postIdsAndPages = Vector[(Seq[PostId], PageParts)]()

    postIdsByPage map { case (pageId, postIds) =>
      val page = apiReq.dao.loadPageParts(pageId) getOrElse throwNotFound(
        "DwE30EU5", s"Page not found, id: `$pageId'; could not load all posts")
      postIdsAndPages :+= (postIds, page)
    }

    Future.successful(OkSafeJson(
      BrowserPagePatcher(apiReq).jsonForPosts(postIdsAndPages)))
  }


  def loadReplies =
    loadTreesImpl { (page, postIds) =>
      val posts = postIds map { postId => page.getPost_!(postId) }
      val patchSpecs = posts.foldLeft(Nil: List[TreePatchSpec]) { (specs, post) =>
        post.replies.map(reply => TreePatchSpec(reply.id, wholeTree = true)) ::: specs
      }
      patchSpecs
    }


  /** For each post id, loads all posts from the Original Post down to post id,
    * and also loads the tree starting at the post id.
    *
    * This is useful if a user navigates to http://server/-pageId/#post-X
    * but post-X isn't loaded when the page is loaded, because there are very
    * many posts, so only the most interesting ones are loaded.
    * Then the browse makes a request to loadThreadsAndTrees for post id X,
    * and the server replies with everything the browser needs to show post X,
    * including some replies to post X.
    */
  def loadThreadsAndTrees = loadTreesImpl { (page, postIds) =>
    val treePatchSpecs = postIds.map(TreePatchSpec(_, wholeTree = true))
    var threadPatchSpecs: List[TreePatchSpec] = Nil
    postIds foreach { postId =>
      // `startPost` is included in `treeSpatchSpecs` already; ignore it here.
      val startPost = page.getPost(postId)
      var nextParent = startPost.flatMap(_.parentPost)
      while (nextParent.isDefined) {
        threadPatchSpecs ::= TreePatchSpec(nextParent.get.id, wholeTree = false)
        nextParent = nextParent.get.parentPost
      }
    }
    treePatchSpecs ::: threadPatchSpecs
  }


  private def loadTreesImpl(loadWhatFn: (PageParts, List[PostId]) => List[TreePatchSpec]) =
      PostJsonAction(maxLength = 5000) { apiReq =>

    SECURITY // What about access control?! Page ids generally unknown however, but
    // should really fix anyway.
    // Ensure I fix `loadPosts` at the same time.

    val pageActionIds = apiReq.body.as[List[Map[String, String]]]

    val postIdsByPage: Map[PageId, List[PostId]] =
      Utils.parsePageActionIds(pageActionIds)(identity)

    var pagesAndPatchSpecs = List[(PageParts, List[TreePatchSpec])]()

    postIdsByPage foreach { case (pageId, postIds) =>
      val page = apiReq.dao.loadPageParts(pageId) getOrElse throwNotFound(
        "DwE80Bw2", s"Page not found, id: `$pageId'; could not do all changes")
      val postIdsToLoad = loadWhatFn(page, postIds)
      pagesAndPatchSpecs ::= (page, postIdsToLoad)
    }

    Future.successful(OkSafeJson(
      BrowserPagePatcher(apiReq).jsonForTrees(pagesAndPatchSpecs)))
  }

}

