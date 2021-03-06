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

import com.debiki.core.{PostActionPayload => PAP}
import java.{util => ju}
import RawPostAction.{copyCreatePost, copyApprovePost}
import Prelude._


/**
 * Constructs a page, with a body text (id = Page.BodyId),
 * and an Edit of that text,
 * and an EditApp of that Edit,
 * and a Delete of that Edit, or the EditApp,
 * or a ReviewPostAction & approval of the EditApp,
 * or a ReviewPostAction & rejection of the EditApp.
 */
trait PageTestValues {

  val datiBeforeFirstAction = new ju.Date(0)

  val textInitially = "initial-body-text"
  val textAfterFirstEdit = "body-text-after-first-edit"

  val bodySkeleton =
    RawPostAction(id = PageParts.BodyId, postId = PageParts.BodyId,
      userIdData = UserIdData.newTest(userId = "?"), creationDati = new ju.Date(1000),
      payload = PostActionPayload.CreatePost(
        parentPostId = None,
        text = textInitially,
        approval = None))

  val bodySkeletonAutoApproved =
    copyCreatePost(bodySkeleton, approval = Some(Approval.WellBehavedUser))

  val bodySkeletonPrelApproved =
    copyCreatePost(bodySkeleton, approval = Some(Approval.Preliminary))

  val bodyApprovalSkeleton = RawPostAction.toApprovePost(
    11, postId = bodySkeleton.id, UserIdData.newTest(userId = "?"),
        ctime = new ju.Date(11000), approval = Approval.AuthoritativeUser)

  val bodyDeletionSkeleton = bodyApprovalSkeleton.copy(
    payload = PAP.DeletePost(clearFlags = false))

  val editSkeleton =
    RawPostAction.toEditPost(
        id = 12, postId = bodySkeleton.id, ctime = new ju.Date(12000),
        UserIdData.newTest(userId = "?"),
        text = makePatch(from = textInitially, to = textAfterFirstEdit),
        approval = None, autoApplied = false)

  def deletionOfEdit =
    RawPostAction(id = 13, postId = editSkeleton.postId,
      userIdData = UserIdData.newTest(userId = "?"), creationDati = new ju.Date(13000),
      payload = PAP.Delete(editSkeleton.id))

  val editAppSkeleton =
    RawPostAction[PAP.EditApp](id = 14, creationDati = new ju.Date(14000),
      payload = PAP.EditApp(editSkeleton.id, approval = None),
      postId = editSkeleton.postId, userIdData = UserIdData.newTest(userId = "?"))

  val deletionOfEditApp =
    RawPostAction(id = 15, postId = editAppSkeleton.postId,
        userIdData = UserIdData.newTest(userId = "?"), creationDati = new ju.Date(15000),
        payload = PAP.Delete(editSkeleton.id))

  val approvalOfEditApp = RawPostAction.toApprovePost(id = 16, postId = editAppSkeleton.postId,
        userIdData = UserIdData.newTest(userId = "?"), ctime = new ju.Date(16000),
        approval = Approval.AuthoritativeUser)

  val rejectionOfEditApp = RawPostAction.toRejectEdits(id = 16, postId = editAppSkeleton.postId,
        UserIdData.newTest(userId = "?"), createdAt = new ju.Date(16000),
        deleteEdits = false)

  /* val ratingOfBody = Rating(17, postId = bodySkeleton.id,
    userIdData = UserIdData.newTest(userId = "?"), ctime = new ju.Date(17000), tags = Nil)
    */

  /*
  val flagOfBody = RawPostAction(18, new ju.Date(18000),
    PAP.Flag(tyype = FlagType.Spam, reason = ""), postId = bodySkeleton.id,
    UserIdData.newTest(userId = "?"))
    */


  case class PageWithEditApplied(
    page: PageParts, edit: RawPostAction[PAP.EditPost], applDate: ju.Date)

  val EmptyPage = PageParts("a")

  def makePageWithEditApplied(autoApplied: Boolean): PageWithEditApplied = {
    val (edit, editApplDati) =
      if (autoApplied)
        (RawPostAction.copyEditPost(editSkeleton, autoApplied = Some(true)), editSkeleton.ctime)
      else
        (editSkeleton, editAppSkeleton.ctime)
    var page = EmptyPage + bodySkeletonAutoApproved + edit
    if (!autoApplied) page = page + editAppSkeleton
    PageWithEditApplied(page, edit, editApplDati)
  }

  lazy val PageWithEditManuallyAppliedNotApproved =
    EmptyPage + bodySkeletonAutoApproved + editSkeleton + editAppSkeleton

  lazy val PageWithEditManuallyAppliedAndExplApproved =
    EmptyPage + bodySkeletonAutoApproved + editSkeleton +
       editAppSkeleton + approvalOfEditApp

  lazy val PageWithEditManuallyAppliedAndAutoApproved =
    EmptyPage + bodySkeletonAutoApproved + editSkeleton +
       RawPostAction.copyApplyEdit(editAppSkeleton, approval = Some(Approval.WellBehavedUser))

  lazy val PageWithEditManuallyAppliedAndPrelApproved =
    EmptyPage + bodySkeletonAutoApproved + editSkeleton +
      RawPostAction.copyApplyEdit(editAppSkeleton, approval = Some(Approval.Preliminary))

  lazy val PageWithEditManuallyAppliedAndPrelApprovedThenRejected =
    PageWithEditManuallyAppliedAndPrelApproved + rejectionOfEditApp

  lazy val PageWithEditManuallyAppliedAndRejected =
    EmptyPage + bodySkeletonAutoApproved + editSkeleton +
     editAppSkeleton + rejectionOfEditApp

  lazy val PageWithEditManuallyAppliedNothingApproved =
    EmptyPage + bodySkeleton + editSkeleton + editAppSkeleton

  val datiAfterLastAction = new ju.Date(20000)
}


object PageTestValues extends PageTestValues

