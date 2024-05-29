package org.springframework.ai.spark.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import groovy.util.logging.Slf4j;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.spark.SparkOptions;
import org.springframework.ai.spark.util.SparkUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author nottyjay
 */
@Slf4j
public class SparkApi {

  private static final Logger log = LoggerFactory.getLogger(SparkApi.class);
  private static ObjectMapper mapper = new ObjectMapper();
  private SparkOptions options;

  static {
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
  }

  public SparkApi(SparkOptions options) {
    this.options = options;
  }

  public Flux<ChatCompletionChunk> chatCompletionStream(ChatCompletionRequest request) {
    HttpClient client = HttpClient.create();
    String url ="wss://spark-api.xf-yun.com/v3.5/chat?" + SparkUtils.signature(this.options.appKey(), this.options.appSecret(), "spark-api.xf-yun.com", Model.Spark_3_5_MAX);
    if(log.isDebugEnabled()) {
      log.debug(url);
    }
    Flux<String> response = client.websocket().uri(url)
            .handle((websocketInbound, websocketOutbound) -> {
              try {
                String content = mapper.writeValueAsString(request);
                if(log.isDebugEnabled()){
                  log.debug("content: {}", content);
                }
                websocketOutbound.send(Mono.just(Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8)))).neverComplete().subscribe();
                return websocketInbound.receive().asString();
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            });
    return response.map(message -> {
        try {
            return mapper.readValue(message, ChatCompletionChunk.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    });
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ChatCompletionRequest (
    @JsonProperty("header") ChatCompletionRequestHeader header,
    @JsonProperty("parameter") ChatCompletionRequestParameter parameter,
    @JsonProperty("payload") ChatCompletionRequestPayload playload) {

  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ChatCompletionRequestHeader (
      @JsonProperty("app_id") String appId,
      @JsonProperty("uid") String uid) {
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ChatCompletionRequestParameter (
          @JsonProperty("chat") ChatCompletionRequestParameterChat chat) {
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ChatCompletionRequestParameterChat(
          @JsonProperty("domain") String domain,
          @JsonProperty("temperature") Float temperature,
          @JsonProperty("max_tokens") Integer maxTokens) {
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ChatCompletionRequestPayload(
          @JsonProperty("message") ChatCompletionRequestPayloadMessage message,
          @JsonProperty("functions") ChatCompletionRequestPayloadFunction function){
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ChatCompletionRequestPayloadMessage(
          @JsonProperty("text") List<ChatCompletionMessage> messages){
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ChatCompletionMessage(
          @JsonProperty("content") String content,
          @JsonProperty("role") Role role) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ChatCompletionRequestPayloadFunction(
          @JsonProperty("text") List<FunctionText> text
  ) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record FunctionText (
          @JsonProperty("name") String name,
          @JsonProperty("description") String description,
          @JsonProperty("parameters") Map<String, Object> parameters
          ) {}

  /**
   * The role of the author of this message.
   */
  public enum Role {
    /**
     * System message.
     */
    @JsonProperty("system") SYSTEM,
    /**
     * User message.
     */
    @JsonProperty("user") USER,
    /**
     * Assistant message.
     */
    @JsonProperty("assistant") ASSISTANT
  }

  public enum Model {
    Spark_3_5_MAX("Spark3.5 Max", "/v3.5/chat"),
    Spark_PRO("Spark Pro", "/v3.1/chat"),
    Spark_V2_0("Spark v2.0", "/v2.1/chat"),
    Spark_LITE_MAX("Spark List", "/v1.1/chat");

    private String version;
    private String path;

    Model(String version, String path) {
      this.version = version;
      this.path = path;
    }

    public String getVersion() {
      return version;
    }

    public String getPath() {
      return path;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ChatCompletionChunk(
          @JsonProperty("header") ChatCompletionChunkHeader header,
          @JsonProperty("payload") ChatCompletionChunkPayload payload) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ChatCompletionChunkHeader(
          @JsonProperty("code") Integer code,
          @JsonProperty("message") String message,
          @JsonProperty("sid") String sid,
          @JsonProperty("status") Integer status) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ChatCompletionChunkPayload(
          @JsonProperty("choices") ChatCompletionChunkPayloadChoices choices,
          @JsonProperty("usage") ChatCompletionChunkPayloadUsage usage
  ){}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ChatCompletionChunkPayloadChoices(
          @JsonProperty("status") Integer status,
          @JsonProperty("seq") Integer seq,
          @JsonProperty("text") List<ChatCompletionChunkPayloadChoicesText> text
  ){}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ChatCompletionChunkPayloadChoicesText(
          @JsonProperty("content") String content,
          @JsonProperty("role") String role,
          @JsonProperty("index") Integer index
  ){}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ChatCompletionChunkPayloadUsage(
          @JsonProperty("text") ChatCompletionChunkPayloadUsageText text
  ){}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ChatCompletionChunkPayloadUsageText(
          @JsonProperty("question_tokens") Integer questionTokens,
          @JsonProperty("prompt_tokens") Integer promptTokens,
          @JsonProperty("completion_tokens") Integer completionTokens,
          @JsonProperty("total_tokens") Integer totalTokens
  ) {}
}
