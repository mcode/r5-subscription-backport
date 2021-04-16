package org.mitre.hapifhir.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class RequestHandler {
    public RequestHandler() { }

    public String sendGet(String endpoint, String params) throws Exception {
        String url = endpoint;
        System.out.println("Sending Get to: " + url);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
        in.close();
        return response.toString();
  }

  public String sendPost(String endpoint, String data) throws Exception {
        String url = endpoint;
        // Do not use this in production
        TrustManager[] trustAllCerts = new TrustManager[] {
           new X509TrustManager() {
              public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
              }
              public void checkClientTrusted(X509Certificate[] certs, String authType) {  }
              public void checkServerTrusted(X509Certificate[] certs, String authType) {  }
           }
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
              return true;
            }
        };
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        System.out.println("Sending Post to: " + url);
        URL obj = new URL(url);

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setDoOutput(true);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = data.getBytes("utf-8");
            os.write(input, 0, input.length);
        } catch (Exception e) {
            System.out.println(e);
        }

        int responseCode = con.getResponseCode();
        System.out.println("Response Code: " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }

        in.close();
        System.out.println("Response:" + response.toString());
        return response.toString();
    }

    public String sendPut(String endpoint, String data) throws Exception {
          String url = endpoint;
          // Do not use this in production
          TrustManager[] trustAllCerts = new TrustManager[] {
             new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                  return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {  }
                public void checkServerTrusted(X509Certificate[] certs, String authType) {  }
             }
          };

          SSLContext sc = SSLContext.getInstance("SSL");
          sc.init(null, trustAllCerts, new java.security.SecureRandom());
          HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

          // Create all-trusting host name verifier
          HostnameVerifier allHostsValid = new HostnameVerifier() {
              public boolean verify(String hostname, SSLSession session) {
                return true;
              }
          };

          // Install the all-trusting host verifier
          HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

          System.out.println("Sending Put to: " + url);
          URL obj = new URL(url);

          HttpURLConnection con = (HttpURLConnection) obj.openConnection();
          con.setRequestMethod("PUT");
          con.setRequestProperty("Content-Type", "application/json; utf-8");
          con.setDoOutput(true);

          try (OutputStream os = con.getOutputStream()) {
              byte[] input = data.getBytes("utf-8");
              os.write(input, 0, input.length);
          } catch (Exception e) {
              System.out.println(e);
          }

          int responseCode = con.getResponseCode();
          System.out.println("Response Code: " + responseCode);

          BufferedReader in = new BufferedReader(
                  new InputStreamReader(con.getInputStream()));
          String inputLine;
          StringBuffer response = new StringBuffer();

          while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
          }

          in.close();
          System.out.println("Response:" + response.toString());
          return response.toString();
      }
}
