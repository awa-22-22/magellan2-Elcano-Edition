// class magellan.library.utils.MagellanURL
// created on Apr 12, 2013
//
// Copyright 2003-2013 by magellan project team
//
// Author : $Author: $
// $Id: $
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program (see doc/LICENCE.txt); if not, write to the
// Free Software Foundation, Inc.,
// 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package magellan.library.utils;

import java.util.Properties;

import magellan.library.utils.logging.Logger;

/**
 * Utility class maintaining important URLs for Magellan Elcano Edition.
 * <p>
 * The URLs are fixed; unlike in the original Magellan there is no remote file with locations that is fetched at
 * runtime, so the program does not contact any server on its own.
 * </p>
 *
 * @author stm
 */
public class MagellanUrl {
  private static final Logger log = Logger.getInstance(MagellanUrl.class);

  /** Project repository, the base of the other URLs. */
  private static final String REPOSITORY = "https://github.com/awa-22-22/magellan2-Elcano-Edition";

  /** Key for basic Magellan URL */
  public static final String WWW_ROOT = "www.root";

  /** Key for Magellan bugtracker URL */
  public static final String WWW_BUGS = "www.bugtracker";

  /** Key for download URL */
  public static final String WWW_DOWNLOAD = "www.download";

  /** Key for files URL */
  public static final String WWW_FILES = "www.files";

  /** Key for the Wikipedia article about Ferdinand Magellan, shown in the About dialog. */
  public static final String WWW_FERNANDO = "www.fernando";

  private static final Properties urls = new Properties();

  static {
    urls.put(WWW_ROOT, REPOSITORY);
    urls.put(WWW_BUGS, REPOSITORY + "/issues");
    urls.put(WWW_DOWNLOAD, REPOSITORY + "/releases");
    urls.put(WWW_FILES, REPOSITORY + "/releases");
    urls.put(WWW_FERNANDO, "https://de.wikipedia.org/wiki/Ferdinand_Magellan");
  }

  /**
   * Returns the URL for a key.
   *
   * @param key One of the <code>WWW_*</code> keys.
   * @return The url for the key, <code>null</code> if there is none.
   */
  public static String getMagellanUrl(String key) {
    String url = urls.getProperty(key);
    if (url == null) {
      log.warn("Could not find Magellan URL " + key);
    }
    return url;
  }

  /**
   * Returns Magellan's primary WWW URL.
   */
  public static String getRootUrl() {
    return getMagellanUrl(WWW_ROOT);
  }

}
