/**
 * Copyright (c) 2012 Kaj Magnus Lindberg (born 1979)
 */

package controllers

import com.debiki.v0._
import debiki._
import debiki.DebikiHttp._
import java.{util => ju}
import play.api._
import play.api.mvc.{Action => _, AsyncResult, BodyParsers}
import Actions._
import Prelude._
import Utils.ValidationImplicits._


object AppManageTenant extends mvc.Controller {


  def urlToManage(tenantId: String)(implicit request: mvc.Request[_])
        : String = {
    routes.AppManageTenant.mainPage().absoluteURL()(request) +
      "#/id/"+ tenantId

  }


  def mainPage() = ExceptionActionNoBody { request =>
    Ok(views.html.manageTenants())
  }

}


// vim: fdm=marker et ts=2 sw=2 tw=80 fo=tcqwn list
