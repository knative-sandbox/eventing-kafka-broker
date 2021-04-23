package dev.knative.eventing.control.protocol.conformance;

import dev.knative.eventing.control.protocol.ControlMessage;
import dev.knative.eventing.control.protocol.impl.TCPControlServerVerticle;
import io.vertx.core.Vertx;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConformanceTester {

  private static final Logger logger = LoggerFactory.getLogger(ConformanceTester.class);

  private static final String INCOMING_EB_ADDRESS = "incoming.control.protocol";
  private static final String OUTGOING_EB_ADDRESS = "outgoing.control.protocol";

  private static final byte ACCEPT_OP_CODE = 0;
  private static final byte FAIL_OP_CODE = 1;
  private static final byte DONE_OP_CODE = 2;

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    int port = Integer.parseInt(
      System.getenv("PORT")
    );

    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new TCPControlServerVerticle(port, INCOMING_EB_ADDRESS, OUTGOING_EB_ADDRESS))
      .toCompletionStage()
      .toCompletableFuture()
      .get();

    logger.info("Started TCP Control server verticle on port {}", port);

    CountDownLatch receiveLatch = new CountDownLatch(1);
    Queue<ControlMessage> receivedMessages = new ConcurrentLinkedQueue<>();

    vertx.runOnContext(v ->
      vertx.eventBus().<ControlMessage>consumer(INCOMING_EB_ADDRESS, message -> {
        ControlMessage controlMessage = message.body();
        logger.info("Received control message: {}", controlMessage);

        receivedMessages.offer(controlMessage);

        switch (controlMessage.opCode()) {
          case ACCEPT_OP_CODE -> message.reply(null);
          case FAIL_OP_CODE -> message.reply("expected error");
          case DONE_OP_CODE -> {
            message.reply(null);
            receiveLatch.countDown();
          }
          default -> {
            message.reply("Received unknown control message opcode");
            logger.error("Received unknown control message opcode: {}", controlMessage.opCode());
            System.exit(1);
          }
        }

      })
    );

    // Wait for the done message
    receiveLatch.await();

    logger.info("Received done message");

    // Now let's start dispatching the message
    while (true) {
      CountDownLatch sendLatch = new CountDownLatch(1);

      ControlMessage message = receivedMessages.poll();
      if (message == null) {
        break;
      }

      logger.info("Sending control message: {}", message);

      vertx.runOnContext(v ->
        vertx.eventBus().request(OUTGOING_EB_ADDRESS, message).onComplete(ar -> {
          if (message.opCode() == FAIL_OP_CODE && ar.succeeded()) {
            logger.error("Expecting message to fail, but it succeeded {}", message);
            System.exit(1);
          } else if (message.opCode() != FAIL_OP_CODE && ar.failed()) {
            logger.error("Expecting message to succeed, but it failed {}", message);
            System.exit(1);
          }

          sendLatch.countDown();
        })
      );

      sendLatch.await();
    }

    logger.info("Test passed");
  }

}