/*
 * Copyright © 2018 Knative Authors (knative-dev@googlegroups.com)
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
package dev.knative.eventing.kafka.broker.dispatcher.consumer.impl;

import dev.knative.eventing.kafka.broker.dispatcher.consumer.OffsetManager;
import io.vertx.core.Future;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the offset strategy that makes sure that, even unordered, the offset commit is ordered.
 */
public final class UnorderedOffsetManager implements OffsetManager {

  private static final Logger logger = LoggerFactory
    .getLogger(UnorderedOffsetManager.class);

  private final KafkaConsumer<?, ?> consumer;

  private final Map<TopicPartition, OffsetTracker> offsetTrackers;

  private final Consumer<Integer> onCommit;

  /**
   * All args constructor.
   *
   * @param consumer Kafka consumer.
   * @param onCommit Callback invoked when an offset is actually committed
   */
  public UnorderedOffsetManager(final KafkaConsumer<?, ?> consumer, final Consumer<Integer> onCommit) {
    Objects.requireNonNull(consumer, "provide consumer");

    this.consumer = consumer;
    this.offsetTrackers = new HashMap<>();
    this.onCommit = onCommit;
  }

  /**
   * {@inheritDoc}
   *
   * @return
   */
  @Override
  public Future<Void> recordReceived(final KafkaConsumerRecord<?, ?> record) {
    // un-ordered processing doesn't require pause/resume lifecycle.

    // Because recordReceived is guaranteed to be called in order,
    // we use it to set the last seen acked offset.
    // TODO If this assumption doesn't work, use this.consumer.committed(new TopicPartition(record.topic(), record.partition()))
    this.offsetTrackers.computeIfAbsent(new TopicPartition(record.topic(), record.partition()),
      v -> new OffsetTracker(record.offset() - 1));
    return Future.succeededFuture();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<Void> successfullySentToSubscriber(final KafkaConsumerRecord<?, ?> record) {
    return commit(record);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<Void> successfullySentToDLQ(final KafkaConsumerRecord<?, ?> record) {
    return commit(record);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<Void> failedToSendToDLQ(final KafkaConsumerRecord<?, ?> record, final Throwable ex) {
    this.offsetTrackers.get(new TopicPartition(record.topic(), record.partition()))
      .recordNewOffset(record.offset());
    return Future.succeededFuture();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<Void> recordDiscarded(final KafkaConsumerRecord<?, ?> record) {
    this.offsetTrackers.get(new TopicPartition(record.topic(), record.partition()))
      .recordNewOffset(record.offset());
    return Future.succeededFuture();
  }

  private Future<Void> commit(final KafkaConsumerRecord<?, ?> record) {
    TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
    OffsetTracker tracker = this.offsetTrackers.get(topicPartition);
    tracker.recordNewOffset(record.offset());

    if (tracker.shouldCommit()) {
      // Reset the state
      long newOffset = tracker.offsetToCommit();
      long uncommittedSize = tracker.uncommittedSize();
      tracker.reset(newOffset);

      // Execute the actual commit
      return consumer.commit(Map.of(
        topicPartition,
        new OffsetAndMetadata(newOffset, ""))
      )
        .onSuccess(ignored -> {
          if (onCommit != null) {
            onCommit.accept((int) uncommittedSize);
          }
          logger.debug(
            "committed for topic partition {} {} offset {}",
            record.topic(),
            record.partition(),
            newOffset
          );
        })
        .onFailure(cause ->
          logger.error(
            "failed to commit for topic partition {} {} offset {}",
            record.topic(),
            record.partition(),
            newOffset,
            cause
          )
        ).mapEmpty();
    }
    return Future.succeededFuture();
  }

  /**
   * This offset tracker keeps track of the committed records flipping a bit in the stored long.
   */
  private static class OffsetTracker {

    private final static int ACKS_GARBAGE_SIZE_THRESHOLD = 128; // Meaning 8192 messages are on hold
    private final static long[] POWS = new long[64];

    static {
      // Initialize POWS
      for (int i = 0; i < 64; i++) {
        long pow = 0;
        for (int j = 0; j <= i; j++) {
          pow |= 1L << j;
        }
        POWS[i] = pow;
      }
    }

    private long lastAcked;
    private long[] uncommitted;

    private int greaterBlockIndex;
    private int greaterBitIndexInGreaterBlock;

    public OffsetTracker(long initialOffset) {
      this.lastAcked = initialOffset;
      this.uncommitted = new long[1];
      this.greaterBlockIndex = -1;
      this.greaterBitIndexInGreaterBlock = -1;
    }

    public void recordNewOffset(long offset) {
      long diffWithLastCommittedOffset = offset - this.lastAcked - 1;
      int blockIndex = blockIndex(diffWithLastCommittedOffset);
      int bitIndex = (int) (diffWithLastCommittedOffset % 64); // That's obviously smaller than a long

      checkAcksArraySize(blockIndex);

      // Let's record this bit and update the greater indexes
      this.uncommitted[blockIndex] |= 1L << bitIndex;
      if (this.greaterBlockIndex < blockIndex) {
        this.greaterBlockIndex = blockIndex;
        this.greaterBitIndexInGreaterBlock = bitIndex;
      } else if (this.greaterBlockIndex == blockIndex && this.greaterBitIndexInGreaterBlock < bitIndex) {
        this.greaterBitIndexInGreaterBlock = bitIndex;
      }
    }

    public boolean shouldCommit() {
      // Let's check if we have all the bits to 1, except the last one
      for (int b = 0; b < this.greaterBlockIndex; b++) {
        if (this.uncommitted[b] != POWS[63]) {
          return false;
        }
      }

      return this.uncommitted[this.greaterBlockIndex] == POWS[this.greaterBitIndexInGreaterBlock];
    }

    public long uncommittedSize() {
      return this.greaterBitIndexInGreaterBlock + 1 + (greaterBlockIndex * 64L);
    }

    public long offsetToCommit() {
      return this.lastAcked + uncommittedSize() + 1;
    }

    public void reset(long committed) {
      this.lastAcked = committed - 1;
      this.greaterBlockIndex = -1;
      this.greaterBitIndexInGreaterBlock = -1;

      // Cleanup the acks array or overwrite it, depending on its size
      if (this.uncommitted.length > ACKS_GARBAGE_SIZE_THRESHOLD) {
        // No need to keep that big array
        this.uncommitted = new long[1];
      } else {
        Arrays.fill(this.uncommitted, 0L);
      }
    }

    private int blockIndex(long val) {
      return (int) (val >> 6);
    }

    private void checkAcksArraySize(int blockIndex) {
      if (this.uncommitted.length < blockIndex) {
        this.uncommitted = Arrays.copyOf(this.uncommitted, (blockIndex + 1) * 2);
      }
    }

  }

}
