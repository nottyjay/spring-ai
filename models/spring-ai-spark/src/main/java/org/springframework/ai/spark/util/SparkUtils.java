package org.springframework.ai.spark.util;

import org.springframework.ai.spark.api.SparkApi;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author nottyjay
 */
public class SparkUtils {

  private static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
  static {
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  public static String signature(String appKey, String appSecret, String host, SparkApi.Model model) {

    String dateTime = dateFormat.format(Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTime());
    String tempStr = "host: " + host + "\n" + "date: " + dateTime + "\n" + "GET " + model.getPath() + " HTTP/1.1";
    try {
      byte[] key = appSecret.getBytes(StandardCharsets.UTF_8);
      Mac sha256Hmac = Mac.getInstance("HmacSHA256");
      SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");
      sha256Hmac.init(secretKeySpec);
      byte[] hmacData = sha256Hmac.doFinal(tempStr.getBytes(StandardCharsets.UTF_8));
      String hmacBase64 = Base64.getEncoder().encodeToString(hmacData);
      String signatureSourceStr = String.format("api_key=\"%s\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"%s\"", appKey, hmacBase64);
      String authorization = Base64.getEncoder().encodeToString(signatureSourceStr.getBytes(StandardCharsets.UTF_8));
      return String.format("authorization=%s&date=%s&host=%s", authorization, URLEncoder.encode(dateTime, Charset.forName("UTF-8")), host);
    }catch (Exception e) {
      e.printStackTrace();
    }
    return dateTime;
  }

  public static void main(String[] args) {
    String dateTime = SparkUtils.signature(System.getenv("Spark_App_Key"), System.getenv("Spark_App_Secret"), "spark-api.xf-yun.com", SparkApi.Model.Spark_3_5_MAX);
    System.out.println(dateTime);
  }
}
