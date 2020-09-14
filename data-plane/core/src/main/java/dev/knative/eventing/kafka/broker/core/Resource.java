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

package dev.knative.eventing.kafka.broker.core;

import java.util.List;
import java.util.Set;

/**
 * Resource interface represents the Resource object.
 *
 * <p>Each implementation must override: equals(object) and hashCode(), and those implementation
 * must catch Resource updates (e.g. it's not safe to compare only the Resource UID). It's recommended
 * to not relying on equals(object) and hashCode() generated by Protocol Buffer compiler.
 *
 * <p>Testing equals(object) and hashCode() of newly added implementation is done by adding sources
 * to parameterized tests in ResourceTest.
 */
public interface Resource {

  /**
   * Get resource identifier.
   *
   * @return identifier.
   */
  String id();

  /**
   * Get broker topic.
   *
   * @return topic.
   */
  Set<String> topics();

  /**
   * A comma separated list of host/port pairs to use for establishing the initial connection to the
   * Kafka cluster.
   *
   * @return bootstrap servers.
   */
  String bootstrapServers();

  //TODO
  Ingress ingress();

  //TODO
  List<Egress> egresses();
}