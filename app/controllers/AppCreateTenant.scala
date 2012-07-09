/**
 * Copyright (c) 2012 Kaj Magnus Lindberg (born 1979)
 */

package controllers

import com.debiki.v0._
import debiki._
import debiki.DebikiHttp._
import java.{util => ju}
import play.api._
import play.api.mvc.{Action => _, _}
import play.api.Play.current
import Actions._
import Prelude._
import Utils.ValidationImplicits._


object AppCreateTenant extends mvc.Controller {


  val (newTenantParentDomain, newTenantPort) = {
    def getString = Play.configuration.getString(_: String, validValues = None)
    val domain = getString("new-tenant.parent-domain") getOrElse "localhost"
    val port = getString("new-tenant.port") getOrElse ""
    (domain, port)
  }

  val _secretNewTenantSalt = "a0kr3Qi8BgwIWF"  // hardcoded, for now

  val _newTenantPasswordHashLength = 12


  def showLoginForm() = ExceptionActionNoBody { request =>
    Ok(views.html.createTenant(doWhat = "showLoginForm"))
  }


  def handleLoginForm() = ExceptionAction(
        BodyParsers.parse.urlFormEncoded(maxLength = 1000)) {
      implicit request =>

    AppAuthOpenid.asyncLogin(
      returnToUrl = routes.AppCreateTenant.showCreateWebsiteForm.absoluteURL())
  }


  def showCreateWebsiteForm() = CheckSidActionNoBody {
        (sidOk, xsrfOk, request) =>

    if (sidOk.roleId isEmpty)
      throwRedirect(
        routes.AppCreateTenant.showLoginForm().absoluteURL()(request))

    val curTenantId = AppAuth.lookupTenantByHost(request.host)
    val ipAddr = request.remoteAddress
    _throwIfMayNotCreateTenant(curTenantId, ipAddr, sidOk.roleId)

    Ok(views.html.createTenant(doWhat = "showCreateWebsiteForm",
      xsrfToken = xsrfOk.value))
  }


  def handleCreateWebsiteForm() = CheckSidAction(
        BodyParsers.parse.urlFormEncoded(maxLength = 100)) {
        (sidOk, xsrfOk, request) =>

    def redirectBackToLoginPage(errorCode: String, errorMessage: String) {
      throwForbidden(errorCode, errorMessage) // for now
    }

    if (sidOk.roleId isEmpty) redirectBackToLoginPage(
      "DwE013k586", "Cannot create website: Not logged in")

    val curTenantId = AppAuth.lookupTenantByHost(request.host)
    val ipAddr = request.remoteAddress
    val dao = Debiki.tenantDao(curTenantId, ip = ipAddr, roleId = sidOk.roleId)

    val (identity, user) = {
      val loginId = sidOk.loginId.getOrElse(assErr("DwE01955"))
      dao.loadIdtyAndUser(loginId) match {
        case Some((identity, user)) => (identity, user)
        case None =>
          runErr("DwE01j920", "Cannot create website: Bad login ID: "+ loginId)
      }
    }

    if (!user.isAuthenticated) redirectBackToLoginPage(
      "DwE01B7", "Cannot create website: User not authenticated. "+
      "Please do not login as guest")

    if (user.email isEmpty) redirectBackToLoginPage(
      "DwE56Yr5", "Cannot create website: User's email address unknown. " +
      "Please use an account that has an email address")

    // SHOULD consume IP quota — but not tenant quota!? — when generating
    // new tenant ID. Don't consume tenant quota because the tenant
    // would be www.debiki.com?

    _throwIfMayNotCreateTenant(curTenantId, ipAddr, roleIdOpt = sidOk.roleId)

    val newTenantId = "abc123" // dao.createNewTenant(curTenantId, user.id)

    Redirect(
      routes.AppCreateTenant.tenantCreated(newTenantId).absoluteURL()(request))
  }


  def tenantCreated(newTenantId: String) = CheckSidActionNoBody {
        (sidOk, xsrfOk, request) =>
    Ok(views.html.createTenant(doWhat = "welcomeOwner",
      manageWebsiteUrl = AppManageTenant.urlToManage(newTenantId)(request)))
  }


  private def _generatePassword(newTenantId: String, ipAddr: String): String = {
    (new ju.Date).getTime +"."+
       (math.random * Int.MaxValue).toInt +"."+
       newTenantId +"."+
       ipAddr
  }


  private def _signPassword(password: String): String = {
    val saltedHash = hashSha1Base64UrlSafe(password +"."+ _secretNewTenantSalt)
       .take(_newTenantPasswordHashLength)
    password +"."+ saltedHash
  }


  private def _throwIfBadPassword(password: String, tenantId: String,
        ip: String) {
    val passwordUnsigned = password.dropRightWhile(_ != '.').dropRight(1)
    val passwordCorrectlySigned = _signPassword(passwordUnsigned)
    if (password != passwordCorrectlySigned)
      throwForbidden("DwE021kR5", "Bad one-time-password")
  }


  private def _throwIfMayNotCreateTenant(
        curTenantId: String, ipAddr: String, roleIdOpt: Option[String]) {
    // Perhaps like so:
    // Unless logged in:
    //   If > 10 tenants already created from ipAddr, deny.
    // If logged in:
    //   If > 100 tenants already created from ipAddr, deny.
    //   If > 10 tenants already created by the current role, deny.
  }


  private def _throwIfMayNotLogin(oneTimePassword: String) {
    // Unless magic token signed correctly by server, deny.
  }

}


// vim: fdm=marker et ts=2 sw=2 tw=80 fo=tcqwn list

