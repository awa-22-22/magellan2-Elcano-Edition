// class magellan.library.utils.HTTPResult
// created on 02.09.2007
//
// Copyright 2003-2007 by magellan project team
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

import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Map;

/**
 * This is a container for HTTP responses.
 *
 * @author <a href="mailto:thoralf@m84.de">Thoralf Rickert</a>
 * @version 1.0, 02.09.2007
 */
public class HTTPResult {
  protected int status = 0;
  protected byte[] result = null;

  protected Hashtable<String, String> header = new Hashtable<String, String>();

  /**
   * Creates a HTTP Result instance based on the given response.
   */
  protected HTTPResult(HttpResponse<byte[]> response) {
    status = response.statusCode();
    result = response.body();
    response.headers().map().forEach((name, values) -> {
      if (!values.isEmpty()) {
        header.put(name, values.get(0));
      }
    });
  }

  /**
   * Returns the result of the web server.
   */
  public byte[] getResult() {
    return result;
  }

  /**
   * Returns the result from the webserver as a String. This method verifies the given encoding.
   */
  public String getResultAsString() {
    if (result == null)
      return "";

    String encoding = getEncoding();
    if (encoding != null) {
      try {
        return new String(result, Charset.forName(encoding));
      } catch (Exception exception) {
        // do nothing
      }
    }
    return new String(result, StandardCharsets.UTF_8);
  }

  /**
   * Returns the encoding of the result content.
   */
  public String getEncoding() {
    String contentType = Utils.notNullString(getHeader("Content-Type")).trim();
    String encoding = null;
    if (contentType.contains("charset=")) {
      encoding = contentType.substring(contentType.toLowerCase().indexOf("charset=") + 8).trim();
    }
    return encoding;
  }

  /**
   * Returns the status code sent by the server.
   */
  public int getStatus() {
    return status;
  }

  /**
   * Returns the headers of the response (first value of each header).
   */
  public Map<String, String> getHeader() {
    return header;
  }

  /**
   * Returns the value for the header with the given key name (case-insensitive).
   */
  public String getHeader(String key) {
    for (Map.Entry<String, String> e : header.entrySet()) {
      if (e.getKey().equalsIgnoreCase(key))
        return e.getValue();
    }
    return null;
  }

  @Override
  public String toString() {
    return getResultAsString();
  }
}
