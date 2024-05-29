package org.springframework.ai.spark.api;

import org.junit.Test;
import org.springframework.ai.spark.SparkOptions;
import reactor.core.publisher.Flux;

import java.util.Arrays;

/**
 * @author nottyjay
 */
public class SparkApiTest {

  @Test
  public void simpleChat() throws InterruptedException {
    SparkApi.ChatCompletionRequestHeader header = new SparkApi.ChatCompletionRequestHeader("771fe687", "1");
    SparkApi.ChatCompletionRequestParameter parameter = new SparkApi.ChatCompletionRequestParameter(
        new SparkApi.ChatCompletionRequestParameterChat("generalv3.5", 0.5f, 1024));
    SparkApi.ChatCompletionRequestPayload payload = new SparkApi.ChatCompletionRequestPayload(
            new SparkApi.ChatCompletionRequestPayloadMessage(Arrays.asList(new SparkApi.ChatCompletionMessage("hello", SparkApi.Role.USER))));
    SparkApi.ChatCompletionRequest request = new SparkApi.ChatCompletionRequest(header, parameter, payload);
    Flux<SparkApi.ChatCompletionChunk> response = new SparkApi(new SparkOptions("771fe687", "e6fe05233979a39e400548f2d8bddf29", "ZGQ5OWM2ZGYwYTcyNzg4Y2YwYjMyZmRm")).chatCompletionStream(request);
    System.out.println("subscribe time:" + System.currentTimeMillis());
    response.subscribe(item -> {
      item.payload().choices().text().stream().forEach(System.out::println);
    });
    Thread.sleep(5000);
  }
}
