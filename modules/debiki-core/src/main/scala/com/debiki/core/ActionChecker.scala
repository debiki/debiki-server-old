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

package com.debiki.core

import com.debiki.core.{PostActionPayload => PAP}
import com.debiki.core.Prelude._


object ActionChecker {


  /** Throws an exception if any of the `actions` aren't allowed, given `page`.
    */
  def checkActions(page: PageNoPath, actions: List[RawPostAction[_]]) {
    var pageAndNewActions = page
    for (action <- actions) {
      checkAction(page, action)
      pageAndNewActions = pageAndNewActions + action
    }
  }


  def checkAction(page: PageNoPath, action: RawPostAction[_]) {
    action.payload match {
      case PAP.VoteLike =>
        // One may not like ones own post.
        val post = page.parts.getPost_!(action.postId)
        if (post.userId == action.userId) {
          throw DbDao.LikesOwnPostException
        }
      case _ =>
        // Cold add many kinds of tests, later.
    }
  }

}

