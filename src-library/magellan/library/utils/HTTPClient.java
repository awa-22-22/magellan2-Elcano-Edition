// class magellan.library.utils.HTTPClient
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

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import magellan.library.utils.logging.Logger;

/**
 * This is a small wrapper for the JDK HTTP client to initialize the proxy settings and so on...
 *
 * @author <a href="mailto:thoralf@m84.de">Thoralf Rickert</a>
 * @version 1.0, 02.09.2007
 */
public class HTTPClient {
  /** Log-class */
  private static final Logger log = Logger.getInstance(HTTPClient.class);

  private static final String USER_AGENT = "magellan-http/1.0";

  private final Properties properties;
  private HttpClient client;

  private int timeOutSeconds = 5;
  private String username;
  private String password;
  private String authHost;
  private int authPort = -1;

  /** to let other know if we succeeded */
  private boolean connectionFailed = false;

  /**
   * Creates a new HTTP Client to connect to remote HTTP servers.
   */
  public HTTPClient(Properties properties) {
    this.properties = properties;
    rebuild();
  }

  private void rebuild() {
    HttpClient.Builder builder = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL);

    if (timeOutSeconds > 0) {
      builder.connectTimeout(Duration.ofSeconds(timeOutSeconds));
    }

    boolean proxyEnabled =
        Boolean.valueOf(properties.getProperty("http.proxy.enabled", String.valueOf(false)));
    String host = properties.getProperty("http.proxy.host");
    int port = Integer.parseInt(properties.getProperty("http.proxy.port", String.valueOf(0)));
    if (proxyEnabled && host != null && host.length() != 0) {
      builder.proxy(ProxySelector.of(new InetSocketAddress(host, port)));
    } else {
      builder.proxy(ProxySelector.getDefault());
    }

    try {
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, new TrustManager[] { new EasyX509TrustManager(null) }, null);
      builder.sslContext(context);
    } catch (Exception e) {
      log.warn("could not initialize SSL context, using the default one", e);
    }

    if (username != null) {
      final String user = username;
      final char[] pass = (password == null ? "" : password).toCharArray();
      final String aHost = authHost;
      final int aPort = authPort;
      builder.authenticator(new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          if ((aHost == null || aHost.equalsIgnoreCase(getRequestingHost()))
              && (aPort < 0 || aPort == getRequestingPort()))
            return new PasswordAuthentication(user, pass);
          return null;
        }
      });
    }

    client = builder.build();
  }

  /**
   * Sets the credentials that are used if the server asks for authentication.
   *
   * @param port a negative value means any port
   */
  public void setAuthentication(String username, String password, String host, int port) {
    this.username = username;
    this.password = password;
    this.authHost = host;
    this.authPort = port;
    rebuild();
  }

  /**
   * Makes a GET request.
   */
  public HTTPResult get(String uri) {
    try {
      return get(new URI(uri));
    } catch (Exception exception) {
      HTTPClient.log.error("Invalid URI", exception);
    }
    return null;
  }

  /**
   * Makes a GET request.
   */
  public HTTPResult get(URI uri) {
    return send(requestBuilder(uri).GET(), uri, "GET");
  }

  /**
   * Makes a POST request with the given text content (sent as text/xml).
   */
  public HTTPResult post(URI uri, String content) {
    return send(requestBuilder(uri)
        .header("Content-Type", "text/xml; charset=" + Encoding.DEFAULT)
        .POST(BodyPublishers.ofString(content, StandardCharsets.UTF_8)), uri, "POST");
  }

  /**
   * Makes a multipart/form-data POST request that uploads the given file.
   *
   * @param fieldName the name of the form field
   */
  public HTTPResult postFile(URI uri, String fieldName, File file) {
    try {
      String boundary = "----magellan" + UUID.randomUUID().toString().replace("-", "");
      byte[] head = ("--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"" + fieldName
          + "\"; filename=\"" + file.getName() + "\"\r\n"
          + "Content-Type: application/octet-stream\r\n"
          + "Content-Transfer-Encoding: binary\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1);
      byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.ISO_8859_1);
      byte[] data = Files.readAllBytes(file.toPath());

      byte[] body = new byte[head.length + data.length + tail.length];
      System.arraycopy(head, 0, body, 0, head.length);
      System.arraycopy(data, 0, body, head.length, data.length);
      System.arraycopy(tail, 0, body, head.length + data.length, tail.length);

      BodyPublisher publisher = BodyPublishers.ofByteArray(body);
      return send(requestBuilder(uri).header("Content-Type", "multipart/form-data; boundary=" + boundary)
          .POST(publisher), uri, "POST");
    } catch (IOException e) {
      HTTPClient.log.error("Could not read " + file, e);
    }
    return null;
  }

  private HttpRequest.Builder requestBuilder(URI uri) {
    HttpRequest.Builder builder = HttpRequest.newBuilder(uri).header("User-Agent", USER_AGENT);
    if (timeOutSeconds > 0) {
      builder.timeout(Duration.ofSeconds(timeOutSeconds));
    }
    return builder;
  }

  private HTTPResult send(HttpRequest.Builder builder, URI uri, String method) {
    try {
      HttpResponse<byte[]> response = client.send(builder.build(), BodyHandlers.ofByteArray());
      return new HTTPResult(response);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      HTTPClient.log.warn("Interrupted during HTTP-" + method + " on '" + uri + "'");
    } catch (Exception exception) {
      HTTPClient.log.warn("Error during HTTP-" + method + " on '" + uri + "'. "
          + exception.getMessage(), exception);
      connectionFailed = true;
    }
    return null;
  }

  /**
   * Sets the timeout of the client in seconds.
   */
  public void setTimeOut(int seconds) {
    if (seconds > 0) {
      timeOutSeconds = seconds;
      rebuild();
    }
  }

  /**
   * Returns true if and only if no connection could be established.
   */
  public boolean isConnectionFailed() {
    return connectionFailed;
  }

}

/**
 * Trust manager that accepts single (e.g., self-signed) server certificates without validity check,
 * as the previous HTTP client implementation did.
 */
class EasyX509TrustManager implements X509TrustManager {
  private X509TrustManager standardTrustManager = null;

  public EasyX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
    super();
    TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    factory.init(keystore);
    TrustManager[] trustmanagers = factory.getTrustManagers();
    if (trustmanagers.length == 0)
      throw new NoSuchAlgorithmException("no trust manager found");
    standardTrustManager = (X509TrustManager) trustmanagers[0];
  }

  public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
    standardTrustManager.checkClientTrusted(certificates, authType);
  }

  public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
    if ((certificates != null) && (certificates.length == 1)) {
      // single certificate: accepted without check
    } else {
      standardTrustManager.checkServerTrusted(certificates, authType);
    }
  }

  public X509Certificate[] getAcceptedIssuers() {
    return standardTrustManager.getAcceptedIssuers();
  }
}
