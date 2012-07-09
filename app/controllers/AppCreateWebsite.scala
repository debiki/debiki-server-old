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


object AppCreateWebsite extends mvc.Controller {


  /**
   * The one and only host from which it's allowed to create new websites.
   * Example: www.debiki.net — and www.debiki.com has to redirect to
   * www.debiki.net, when a new website is to be created.
   *
   * Background: www.debiki.net is used *only* for website management.
   * So if people login to www.debiki.com in order to participate in a
   * discussion, and leaves their computer, and Mallory comes and takes
   * it, Mallory won't be able to steal any domain ownership.
   * — It's supposedly much more common that people login to www.debiki.com
   * in order to ask something e.g. in a support forum, than that they
   * login to www.debiki.net to manage their websites (change ownership
   * or ban admins — essentially never ever happens).
   */
  val websiteCreationHost: Option[String] = {
    val host = Play.configuration.getString("create-website.host")
    assErrIf(Play.isProd && host.isEmpty,
      "DwE01kr55", "No create-website.host specified")
    host
  }


  def showLoginForm() = ExceptionActionNoBody { request =>
    Ok(views.html.createWebsite(doWhat = "showLoginForm"))
  }


  def handleLoginForm() = ExceptionAction(
        BodyParsers.parse.urlFormEncoded(maxLength = 1000)) {
      implicit request =>

    AppAuthOpenid.asyncLogin(
      returnToUrl = routes.AppCreateWebsite.showCreateWebsiteForm.absoluteURL())
  }


  def showCreateWebsiteForm() = CheckSidActionNoBody {
        (sidOk, xsrfOk, request) =>

    if (websiteCreationHost.nonEmpty &&
        websiteCreationHost != Some(request.host))
      throwForbidden("DwE32kJ5", "You may not create a website via this host")

    if (sidOk.roleId isEmpty) {
      // Not logged in. Should always happens the first time, because
      // we redirect from www.debiki.com to www.debiki.net.
      throwRedirect(
        routes.AppCreateWebsite.showLoginForm().absoluteURL()(request))
    }

    val dao = _tenantDao(request, sidOk.roleId)
    _throwIfMayNotCreateWebsite(dao, loginId = sidOk.loginId,
       roleId = sidOk.roleId)

    Ok(views.html.createWebsite(doWhat = "showCreateWebsiteForm",
      xsrfToken = xsrfOk.value))
  }


  def handleCreateWebsiteForm() = CheckSidAction(
        BodyParsers.parse.urlFormEncoded(maxLength = 100)) {
        (sidOk, xsrfOk, request) =>

    if (sidOk.roleId isEmpty)
      throwForbidden("DwE013k586", "Cannot create website: Not logged in")

    val dao = _tenantDao(request, sidOk.roleId)
    _throwIfMayNotCreateWebsite(dao, loginId = sidOk.loginId,
       roleId = sidOk.roleId)

    // SHOULD consume IP quota — but not tenant quota!? — when generating
    // new tenant ID. Don't consume tenant quota because the tenant
    // would be www.debiki.com?
    val newTenantId = "abc123" // dao.createTenant(curTenantId, user.id)

    Redirect(routes.AppCreateWebsite.websiteCreated(newTenantId)
       .absoluteURL()(request))
  }


  def websiteCreated(newTenantId: String) = CheckSidActionNoBody {
        (sidOk, xsrfOk, request) =>
    Ok(views.html.createWebsite(doWhat = "welcomeOwner",
      manageWebsiteUrl = AppManageWebsites.urlToManage(newTenantId)(request)))
  }


  private def _tenantDao(request: mvc.Request[_], roleId: Option[String])
        : TenantDao = {
    val curTenantId = AppAuth.lookupTenantByHost(request.host)
    val ipAddr = request.remoteAddress
    Debiki.tenantDao(curTenantId, ipAddr, roleId)
  }


  private def _throwIfMayNotCreateWebsite(curTenantDao: TenantDao,
        loginId: Option[String], roleId: Option[String]) {

    val (identity, user) =
      curTenantDao.loadIdtyAndUser(loginId.getOrElse(assErr("DwE58JB3")))
        match {
          case Some((identity, user)) => (identity, user)
          case None =>
            runErr("DwE01j920", "Cannot create website: Bad login ID: "+ loginId)
        }

    if (!user.isAuthenticated) _throwRedirectToLoginPage(
      "DwE01B7", "Cannot create website: User not authenticated. "+
         "Please login again, but not as guest")

    if (user.email isEmpty) _throwRedirectToLoginPage(
      "DwE56Yr5", "Cannot create website: User's email address unknown. " +
         "Please use an account that has an email address")

    assErrIf(roleId != Some(user.id), "DwE01kJ43")

    // SHOULD also test something like this:
    // Unless logged in:
    //   If > 10 tenants already created from ipAddr, deny.
    // If logged in:
    //   If > 100 tenants already created from ipAddr, deny.
    //   If > 10 tenants already created by the current role, deny.
  }


  private def _throwRedirectToLoginPage(errorCode: String,
        errorMessage: String) {
    throwForbidden(errorCode, errorMessage) // for now
  }

}


// vim: fdm=marker et ts=2 sw=2 tw=80 fo=tcqwn list

