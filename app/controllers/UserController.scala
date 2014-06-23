/**
 * Copyright (C) 2014 Kaj Magnus Lindberg (born 1979)
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
import collection.mutable
import com.debiki.core._
import com.debiki.core.Prelude._
import debiki._
import java.{util => ju}
import play.api.mvc
import play.api.libs.json._
import play.api.mvc.{Action => _, _}
import requests.GetRequest
import scala.concurrent.Future
import Utils.OkSafeJson
import Utils.ValidationImplicits._
import DebikiHttp.{throwBadReq, throwNotFound}



/** Handles requests related to users (guests, roles, groups).
 */
object UserController extends mvc.Controller {


  def viewUserPage() = GetAction { request =>
    // For now, always use the default theme.
    val htmlStr = debiki.TemplateRenderer.renderThemeTemplate(
      theme = TemplateRenderer.DefaultThemeFullName,
      template = "users",
      arguments = Seq(SiteTpi(request)))
    Future.successful(
      Ok(htmlStr) as HTML)
  }


  def loadUserInfo(userId: String) = GetAction { request =>
    val userInfo = request.dao.loadUserInfoAndStats(userId) getOrElse throwNotFound(
      "DwE512WR8", s"User not found, id: $userId")
    val json = Json.obj("userInfo" -> userInfoToJson(userInfo))
    Future.successful(
      OkSafeJson(json))
  }


  def listUserActions(userId: String) = GetAction { request =>
    val actionInfos: Seq[UserActionInfo] = request.dao.listUserActions(userId)
    val json = Json.obj("actions" -> actionInfos.map(actionToJson(_)))
    Future.successful(
      OkSafeJson(json))
  }


  private def userInfoToJson(userInfo: UserInfoAndStats): JsObject = {
    Json.obj(
      "userId" -> userInfo.info.id,
      "displayName" -> userInfo.info.displayName,
      "isAdmin" -> userInfo.info.isAdmin,
      "isModerator" -> false, // userInfo.info.isModerator,
      "numPages" -> userInfo.stats.numPages,
      "numPosts" -> userInfo.stats.numPosts,
      "numReplies" -> userInfo.stats.numReplies,
      "numLikesGiven" -> userInfo.stats.numLikesGiven,
      "numLikesReceived" -> userInfo.stats.numLikesReceived,
      "numWrongsGiven" -> userInfo.stats.numWrongsGiven,
      "numWrongsReceived" -> userInfo.stats.numWrongsReceived,
      "numOffTopicsGiven" -> userInfo.stats.numOffTopicsGiven,
      "numOffTopicsReceived" -> userInfo.stats.numOffTopicsReceived)

    /* Discourse also includes:
      "avatar_template": ...
      "badge_count" : 0,
      "bio_cooked" : "<p>Hi <strong>everybody</strong>! </p>",
      "bio_excerpt" : "Hi everybody!",
      "bio_raw" : "\nHi **everybody**! ",
      "can_edit" : false,
      "can_edit_email" : false,
      "can_edit_name" : false,
      "can_edit_username" : false,
      "can_send_private_message_to_user" : true,
      "created_at" : "2013-02-17T15:09:06.675-05:00",
       group membership info
      "featured_user_badge_ids" : [  ],
      "invited_by" : null,
      "last_posted_at" : "2014-05-10T02:47:06.860-04:00",
      "last_seen_at" : "2014-05-10T03:42:16.842-04:00",
      "profile_background" : "/uploads/default/4870/f95c8f5b0817f799.jpg",
      "stats" : [ { "action_type" : 4,
              "count" : 5,
              "id" : null
            },
            { "action_type" : 5,
              "count" : 217,
              "id" : null
            },
            ... 11 stats
          ],
        "title" : "designerator",
        "trust_level" : 2,
        "username" : "awesomerobot",
        "website" : "http://"
      },
      "user_badges" : [ ]
     */
  }


  private def actionToJson(actionInfo: UserActionInfo): JsObject = {
    Json.obj(
      "pageUrl" -> s"/-${actionInfo.pageId}", // redirects to the page
      "pageTitle" -> JsString(actionInfo.pageTitle),
      "postId" -> JsNumber(actionInfo.postId),
      "actionId" -> JsNumber(actionInfo.actionId),
      "actingUserId" -> JsString(actionInfo.actingUserId),
      "actingUserDisplayName" -> JsString(actionInfo.actingUserDisplayName),
      "targetUserId" -> JsString(actionInfo.targetUserId),
      "targetUserDisplayName" -> JsString(actionInfo.targetUserDisplayName),
      "createdAtEpoch" -> JsNumber(actionInfo.createdAt.getTime),
      "excerpt" -> JsString(actionInfo.postExcerpt),
      "repliedToPostId" -> actionInfo.repliedToPostId.map(JsNumber(_)),
      "editedPostId" -> actionInfo.editedPostId.map(JsNumber(_)),
      "votedLike" -> JsBoolean(actionInfo.votedLike),
      "votedWrong" -> JsBoolean(actionInfo.votedWrong),
      "votedOffTopic" -> JsBoolean(actionInfo.votedOffTopic))
    /* Discourse also includes:
      - usernames
      - the user that wrote the relevant post (avatar, display name, username, id)
      - action type (instead of votedLike, repliedTo...)
      - avatars: "//www.gravatar.com/avatar/....png?s={size}&r=pg&d=identicon",
      - deleted : false,
      - edit_reason : null,
      - hidden : false,
      - moderator_action : false,
     */
  }

}

