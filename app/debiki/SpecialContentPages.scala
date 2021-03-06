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

package debiki

import com.debiki.core._
import com.debiki.core.Prelude._


/** Provides default values for special content pages, e.g. a default website
  * Content License, or a default Jurisdiction section, for the Terms of Use page.
  *
  * These special content pages have special ids so they can be looked up
  * easily. The ids starts with "_" to indicate that they're special page ids.
  * E.g. "_tou_content_license".
  */
object SpecialContentPages {

  case class Content(text: String)


  /** A magic string that means the default contents is to be used. */
  val UseDefaultContentMark = "__use_default__"


  def lookup(pageId: PageId): Option[Content] = Some(pageId match {
    case StylesheetId => Content("")
    case TermsOfUseContentLicenseId => TermsOfUseContentLicense
    case TermsOfUseJurisdictionId => TermsOfUseJurisdiction
    case x => return None
  })


  val StylesheetId = "_stylesheet"


  val TermsOfUseContentLicenseId = "_tou_content_license"

  val TermsOfUseContentLicense = Content(
    text = o"""
      User contributions are licensed under a
      <a href="http://creativecommons.org/licenses/by-nc-sa/4.0/">Creative Commons
      Attribution-NonCommercial-ShareAlike 4.0 International License</a>.""")


  val TermsOfUseJurisdictionId = "_tou_jurisdiction"

  /** I don't know if this makes sense or is legally enforceable, but,
    * since forum owners might live just anywhere, this feels better
    * than using WordPress'default license text that stipulates that the site
    * be governed by the laws of California and that disputes be resolved in
    * San Francisco.
    */
  val TermsOfUseJurisdiction = Content(
    text = o"""
      Except to the extent applicable law, if any, provides otherwise,
      this Agreement, any access to or use of the Website will be governed by
      the laws of the country and state where the owners of %{company_short_name} live,
      and the proper venue for any disputes arising out of or relating to any of the same
      will be the federal courts (or courts, if there are no federal courts) located
      in the city where they live, or closest to where they live if there are no courts
      in that city.
      The language used shall be the English language or the native language
      of the owners of %{company_short_name}.""")

}

