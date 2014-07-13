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


package debiki.dao

import com.debiki.core._
import com.debiki.core.Prelude._
import java.{util => ju}
import play.api.Play
import play.api.Play.current
import CachingDao.{CacheKeyAnySite, CacheValueIgnoreVersion}


/**
 * Delegates most requests to SystemDbDao. However, hides some
 * SystemDbDao methods, because calling them directly would mess up
 * the cache in SystemDao's subclass CachingSystemDao.
 *
 * Thread safe.
 */
class SystemDao(protected val systemDbDao: SystemDbDao) {

  def applyEvolutions() {
    systemDbDao.applyEvolutions()
  }


  // ----- Websites (a.k.a. tenants)

  def checkInstallationStatus(): InstallationStatus =
    systemDbDao.checkInstallationStatus()

  def createFirstSite(firstSiteData: FirstSiteData): Tenant =
    systemDbDao.createFirstSite(firstSiteData)

  // COULD rename to loadWebsitesByIds
  def loadTenants(tenantIds: Seq[SiteId]): Seq[Tenant] =
    systemDbDao.loadTenants(tenantIds)

  def loadSite(siteId: SiteId): Option[Tenant] =
    systemDbDao.loadTenants(Seq(siteId)).headOption

  // COULD rename to findWebsitesCanonicalHost
  def lookupTenant(scheme: String, host: String): TenantLookup = {
    systemDbDao.lookupTenant(scheme, host) match {
      case FoundNothing if Play.isDev =>
        // When running on localhost in Dev mode, Play listens on e.g. 9000 or 9443,
        // however in production the web server would listen on port 80 or 443
        // and the port would not need to be specified. — Remove the port, so one won't
        // need to add entries with suffixes ':9000' and ':9443' to all websites in Dev mode.
        val hostNoPort = host.takeWhile(_ != ':')
        systemDbDao.lookupTenant(scheme, hostNoPort)
      case x =>
        x
    }
  }


  // ----- Emails

  def loadNotfsToMailOut(delayInMinutes: Int, numToLoad: Int): NotfsToMail =
    systemDbDao.loadNotfsToMailOut(delayInMinutes, numToLoad)


  // ----- Quota

  def loadQuotaState(consumers: Seq[QuotaConsumer])
  : Map[QuotaConsumer, QuotaState] =
    systemDbDao.loadQuotaState(consumers)

  def useMoreQuotaUpdateLimits(deltas: Map[QuotaConsumer, QuotaDelta]): Unit =
    systemDbDao.useMoreQuotaUpdateLimits(deltas)


  // ----- Testing

  def emptyDatabase() {
    systemDbDao.emptyDatabase()
  }

}



/**
 * Caches tenant ids by server address and port.
 * Currently never removes anything from the cache, because it's not possible
 * to programmatically remove/reassign a tenant's origins (for example,
 * remove http://some-host-addr.com, or have it point to another tenant)
 *
 * Thread safe.
 */
class CachingSystemDao(systemDbDao: SystemDbDao)
  extends SystemDao(systemDbDao) with CachingDao {

  def siteId = "?"


  override def lookupTenant(scheme: String, host: String): TenantLookup = {
    val key = _tenantLookupByOriginKey(scheme, host)
    lookupInCache[TenantLookup](key) foreach {
      return _
    }
    super.lookupTenant(scheme, host) match {
      case FoundNothing =>
        // Don't cache this.
        // There are infinitely many origins that maps to nothing, since
        // DNS names *.debiki.net resolves to Debiki's servers.
        FoundNothing
      case tenantLookup =>
        putInCache(key, CacheValueIgnoreVersion(tenantLookup))
        tenantLookup
    }
  }


  private def _tenantLookupByOriginKey(scheme: String, host: String) =
    // Site id unknown, that's what we're about to lookup.
    CacheKeyAnySite(s"$scheme|$host|TenantByOrigin")

}

