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
import actions.ApiActions.JsonOrFormDataPostAction
import com.debiki.core._
import com.debiki.core.Prelude._
import debiki._
import debiki.DebikiHttp._
import java.{util => ju}
import play.api._
import play.api.mvc.{Action => _, _}
import requests.ApiRequest
import scala.concurrent.Future



/** Resets the password of a PasswordIdentity, in case the user forgot it.
  */
object ResetPasswordController extends mvc.Controller {

  val MaxResetPasswordEmailAgeInHours = 24


  def start = mvc.Action { request =>
    Redirect(routes.ResetPasswordController.showResetPasswordPage.url)
  }


  def showResetPasswordPage = GetAction { request =>
    Future.successful(Ok(
      views.html.resetpassword.specifyEmailAddress(xsrfToken = request.xsrfToken.value)))
  }


  def handleResetPasswordForm = JsonOrFormDataPostAction(maxBytes = 200) { request =>
    val emailAddress = request.body.getOrThrowBadReq("email")
    val anyIdentityAndUser = request.dao.loadIdtyDetailsAndUser(forEmailAddr = emailAddress)

    anyIdentityAndUser match {
      case Some((identity, user)) =>
        identity match {
          case _: PasswordIdentity =>
            Logger.info(s"Sending password reset email to: $emailAddress")
            sendResetPasswordEmailTo(user, emailAddress, request)
          case _ =>
            Logger.info(s"Sending no-password-to-reset email to: $emailAddress")
            sendNoPasswordToResetEmail(user, emailAddress, request)
        }
      case None =>
        // Don't tell the user that this account doesn't exist; that'd be a
        // security issue.
        Logger.info(s"Not sending password reset email to non-existing user: $emailAddress")
    }

    Future.successful(Redirect(
      routes.ResetPasswordController.showEmailSentPage()))
  }


  private def sendResetPasswordEmailTo(user: User, emailAddress: String, request: ApiRequest[_]) {
    val email = Email(
      EmailType.ResetPassword,
      sendTo = emailAddress,
      toUserId = Some(user.id),
      subject = "Reset Password",
      bodyHtmlText = (emailId: String) => {
        views.html.resetpassword.resetPasswordEmail(
          userName = user.displayName,
          emailId = emailId,
          siteAddress = request.host,
          expirationTimeInHours = MaxResetPasswordEmailAgeInHours).body
      })
    request.dao.saveUnsentEmail(email)
    Globals.sendEmail(email, request.dao.siteId)
  }


  private def sendNoPasswordToResetEmail(
        user: User, emailAddress: String, request: ApiRequest[_]) {
    val email = Email(
      EmailType.Notification,
      sendTo = emailAddress,
      toUserId = Some(user.id),
      subject = "There is no password to reset",
      bodyHtmlText = (emailId: String) => {
        views.html.resetpassword.noPasswordToResetEmail(
          userName = user.displayName,
          emailAddress = emailAddress,
          siteAddress = request.host).body
      })
    request.dao.saveUnsentEmail(email)
    Globals.sendEmail(email, request.dao.siteId)
  }


  def showEmailSentPage = GetAction { request =>
    Future.successful(Ok(
      views.html.resetpassword.emailSent()))
  }


  def showChooseNewPasswordPage(resetPasswordEmailId: String) = GetAction { request =>
    loginByEmailOrThrow(resetPasswordEmailId, request)
    Future.successful(Ok(
      views.html.resetpassword.chooseNewPassword(
        xsrfToken = request.xsrfToken.value,
        anyResetPasswordEmailId = resetPasswordEmailId)))
  }


  def handleNewPasswordForm(anyResetPasswordEmailId: String) =
        JsonOrFormDataPostAction(maxBytes = 200) { request =>
    val newPassword = request.body.getOrThrowBadReq("newPassword")
    val newPasswordAgain = request.body.getOrThrowBadReq("newPasswordAgain")
    if (newPassword != newPasswordAgain)
      throwBadReq("DwE2MJ0", "You specified two different passwords; please go back and try again")

    val loginGrant = loginByEmailOrThrow(anyResetPasswordEmailId, request)
    val emailIdentity = loginGrant.identity.asInstanceOf[IdentityEmailId]
    val emailAddress = emailIdentity.emailSent.map(_.sentTo) getOrElse assErr("DwE72DM0")
    val identity2 = request.dao.loadIdtyDetailsAndUser(forEmailAddr = emailAddress) match {
      case Some((id, user)) => id
      case None =>
        // The password identity was present when we started this reset-password wizard.
        throwForbidden( //logAndThrowForbidden(
          "DwE22RV6", s"Password identity gone, email address: `$emailAddress', user deleted?")
    }

    if (!identity2.isInstanceOf[PasswordIdentity]) {
      // This cannot happen because `handleResetPasswordForm` checks the identity type.
      assErr("DwE77903", "Attempting to reset password for a non-PasswordIdentity: " + identity2)
    }

    val passwordIdentity = identity2.asInstanceOf[PasswordIdentity]

    // Perhaps change to `dao.changePasswordIdentitysPassword(emailAddress, newSaltHash)`?
    request.dao.changePassword(
      passwordIdentity, newPasswordSaltHash = DbDao.saltAndHashPassword(newPassword))

    Future.successful(Ok(
      views.html.resetpassword.passwordHasBeenChanged()))
  }


  def showPasswordChangedPage = GetAction { request =>
    Future.successful(Ok(
      views.html.resetpassword.passwordHasBeenChanged()))
  }


  private def loginByEmailOrThrow(resetPasswordEmailId: String, request: ApiRequest[_])
        : LoginGrant = {
    val loginAttempt = EmailLoginAttempt(
      ip = request.ip, date = new ju.Date, emailId = resetPasswordEmailId)
    // TODO: Check email type !
    val loginGrant =
      try request.dao.saveLogin(loginAttempt)
      catch {
        case ex: DbDao.EmailNotFoundException =>
          throwForbidden("DwE7PWE7", "Email not found")
      }
    loginGrant
  }

}
