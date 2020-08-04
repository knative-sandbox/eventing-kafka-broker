/*
 * Copyright 2020 The Knative Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.knative.eventing.kafka.broker.receiver;

import static dev.knative.eventing.kafka.broker.core.testing.utils.CoreObjects.broker1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.knative.eventing.kafka.broker.core.BrokerWrapper;
import dev.knative.eventing.kafka.broker.core.config.BrokersConfig.Broker;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.RecordMetadata;
import io.vertx.kafka.client.producer.impl.KafkaProducerRecordImpl;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class RequestHandlerTest {

  private static final int TIMEOUT = 3;

  @Test
  public void shouldSendRecordAndTerminateRequestWithRecordProduced() throws InterruptedException {
    shouldSendRecord(false, RequestHandler.RECORD_PRODUCED);
  }

  @Test
  public void shouldSendRecordAndTerminateRequestWithFailedToProduce() throws InterruptedException {
    shouldSendRecord(true, RequestHandler.FAILED_TO_PRODUCE);
  }

  @SuppressWarnings("unchecked")
  private static void shouldSendRecord(boolean failedToSend, int statusCode)
      throws InterruptedException {
    final var record = new KafkaProducerRecordImpl<>(
        "topic", "key", "value", 10
    );

    final RequestToRecordMapper<String, String> mapper
        = request -> Future.succeededFuture(record);

    final KafkaProducer<String, String> producer = mock(KafkaProducer.class);

    when(producer.send(any(), any())).thenAnswer(invocationOnMock -> {

      final var handler = (Handler<AsyncResult<RecordMetadata>>) invocationOnMock
          .getArgument(1, Handler.class);
      final var result = mock(AsyncResult.class);
      when(result.failed()).thenReturn(failedToSend);
      when(result.succeeded()).thenReturn(!failedToSend);

      handler.handle(result);

      return producer;
    });

    final var broker = broker1();

    final var request = mock(HttpServerRequest.class);
    when(request.path()).thenReturn(broker.path());
    final var response = mockResponse(request, statusCode);

    final var handler = new RequestHandler<>(
        new Properties(),
        mapper,
        properties -> producer
    );

    final var countDown = new CountDownLatch(1);

    handler.reconcile(Map.of(broker, new HashSet<>()))
        .onFailure(cause -> fail())
        .onSuccess(v -> countDown.countDown());

    countDown.await(TIMEOUT, TimeUnit.SECONDS);

    handler.handle(request);

    verifySetStatusCodeAndTerminateResponse(statusCode, response);
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void shouldReturnBadRequestIfNoRecordCanBeCreated() throws InterruptedException {
    final var producer = mock(KafkaProducer.class);

    final RequestToRecordMapper<Object, Object> mapper
        = (request) -> Future.failedFuture("");

    final var broker = broker1();

    final var request = mock(HttpServerRequest.class);
    when(request.path()).thenReturn(broker.path());
    final var response = mockResponse(request, RequestHandler.MAPPER_FAILED);

    final var handler = new RequestHandler<Object, Object>(
        new Properties(),
        mapper,
        properties -> producer
    );

    final var countDown = new CountDownLatch(1);
    handler.reconcile(Map.of(broker, new HashSet<>()))
        .onFailure(cause -> fail())
        .onSuccess(v -> countDown.countDown());

    countDown.await(TIMEOUT, TimeUnit.SECONDS);

    handler.handle(request);

    verifySetStatusCodeAndTerminateResponse(RequestHandler.MAPPER_FAILED, response);
  }

  private static void verifySetStatusCodeAndTerminateResponse(
      final int statusCode,
      final HttpServerResponse response) {
    verify(response, times(1)).setStatusCode(statusCode);
    verify(response, times(1)).end();
  }

  private static HttpServerResponse mockResponse(
      final HttpServerRequest request,
      final int statusCode) {

    final var response = mock(HttpServerResponse.class);
    when(response.setStatusCode(statusCode)).thenReturn(response);

    when(request.response()).thenReturn(response);
    return response;
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldRecreateProducerWhenBootstrapServerChange(final VertxTestContext context) {

    final RequestToRecordMapper<Object, Object> mapper
        = (request) -> Future.succeededFuture();

    final var first = new AtomicBoolean(true);
    final var recreated = new AtomicBoolean(false);

    final var handler = new RequestHandler<Object, Object>(
        new Properties(),
        mapper,
        properties -> {
          if (!first.getAndSet(false)) {
            recreated.set(true);
          }
          return mock(KafkaProducer.class);
        }
    );

    final var checkpoint = context.checkpoint();

    final var broker1 = new BrokerWrapper(Broker.newBuilder()
        .setId("1")
        .setBootstrapServers("kafka-1:9092,kafka-2:9092")
        .build());

    final var broker2 = new BrokerWrapper(Broker.newBuilder()
        .setId("1")
        .setBootstrapServers("kafka-1:9092,kafka-3:9092")
        .build());

    handler.reconcile(Map.of(broker1, new HashSet<>()))
        .onSuccess(ignored -> handler.reconcile(Map.of(broker2, new HashSet<>()))
            .onSuccess(i -> context.verify(() -> {
              assertThat(recreated.get()).isTrue();
              checkpoint.flag();
            }))
            .onFailure(context::failNow)
        )
        .onFailure(context::failNow);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldNotRecreateProducerWhenBootstrapServerNotChanged(
      final VertxTestContext context) {

    final RequestToRecordMapper<Object, Object> mapper
        = (request) -> Future.succeededFuture();

    final var first = new AtomicBoolean(true);
    final var recreated = new AtomicBoolean(false);

    final var handler = new RequestHandler<Object, Object>(
        new Properties(),
        mapper,
        properties -> {
          if (!first.getAndSet(false)) {
            context.failNow(new IllegalStateException("producer should be recreated"));
          }
          return mock(KafkaProducer.class);
        }
    );

    final var checkpoint = context.checkpoint();

    final var broker1 = new BrokerWrapper(Broker.newBuilder()
        .setId("1")
        .setBootstrapServers("kafka-1:9092,kafka-2:9092")
        .build());

    final var broker2 = new BrokerWrapper(Broker.newBuilder()
        .setId("1")
        .setBootstrapServers("kafka-1:9092,kafka-2:9092")
        .build());

    handler.reconcile(Map.of(broker1, new HashSet<>()))
        .onSuccess(ignored -> handler.reconcile(Map.of(broker2, new HashSet<>()))
            .onSuccess(i -> context.verify(() -> {
              assertThat(recreated.get()).isFalse();
              checkpoint.flag();
            }))
            .onFailure(context::failNow)
        )
        .onFailure(context::failNow);
  }
}