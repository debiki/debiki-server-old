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

import actions.ApiActions._
import com.debiki.core._
import debiki._
import debiki.DebikiHttp._
import java.{util => ju}
import play.api._
import scala.concurrent.Future
import Prelude._


/**
 * Bundles and serves site specific assets.
 *
 * The assets are served from e.g.: `server/-/site/styles.<version>.css`
 * where `<version>` is a URL safe SHA1 hash of the asset bundle text.
 * (So whenever the bundle contents changes, the URL also changes — and
 * we can ask the browser to cache forever. This is asset versioning.)
 */
object SiteAssetBundlesController extends mvc.Controller {


  /**
   * Serves asset bundles.
   *
   * - Asks the client to cache the response forever (1 year), since asset
   * versioning is used (a new URL is generated whenever the bundle body
   * changes).
   * - Sets no cookies, since the intention is that the response be cached
   * by proxy servers.
   */
  def at(file: String) = GetAction { request =>
    // `file` is like: bundle-name.<version>.css.
    val result = file match {
      case AssetBundleFileNameRegex(nameNoSuffix, version, suffix) =>
        // Ignore `version` for now. It's only used for asset versioning —
        // but we always serve the most recent version of the bundle.
        val bundle = try {
          // SECURITY don't load foreign tenant stuff from any private
          // other-site/_hidden-underscore-folder/, or if read access restricted
          // in some other manner. (Fix later, in AssetBundleLoader?)
          request.dao.loadAssetBundle(nameNoSuffix, suffix)
        }
        catch {
          case ex: DebikiException =>
            throwNotFound(ex.errorCode, ex.details)
        }

        val etag = bundle.version
        val isEtagOk = request.headers.get(IF_NONE_MATCH) == Some(etag)
        if (isEtagOk) {
          NotModified
        }
        else {
          val contentType =
            if (request.uri endsWith "css") CSS
            else if (request.uri endsWith "js") JAVASCRIPT
            else TEXT
          Ok(bundle.body).withHeaders(
            CACHE_CONTROL -> "public, max-age=31536000",
            ETAG -> etag,
            // Really don't set any new cookies (don't know from where they
            // could come, but remove any anyway).
            SET_COOKIE -> "") as contentType
        }
      case _ =>
        NotFoundResult("DwE93BY1", s"Bad asset bundle URL path: $file")
    }
    Future.successful(result)
  }


  /**
   * <bundle-name-no-suffix>.<suffix>.
   */
  val AssetBundleNameRegex = """([a-z-]+)\.(css)""".r


  /**
   * <bundle-name-no-suffix>-<version>.<suffix>.
   * The version is a URL safe base64 hash, and used for asset versioning.
   */
  val AssetBundleFileNameRegex = """([a-z-]+)\.([a-zA-Z0-9_-]+)\.(css)""".r


  def assetBundleFileName(nameNoSuffix: String, version: String, suffix: String) =
    s"$nameNoSuffix.$version.$suffix"

}

