/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.eventhubs.client

import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ ConcurrentLinkedQueue, Executors, ScheduledExecutorService }

import com.azure.messaging.eventhubs.{ EventHubsClientBuilder, EventHubsConsumerClient, EventHubsProducerClient }
import com.azure.identity.{ DefaultAzureCredential, TokenCredential }
import org.apache.spark.eventhubs._
import org.apache.spark.eventhubs.utils.RetryUtils.retryJava
import org.apache.spark.internal.Logging

import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * A connection pool for Track 2 EventHubs clients. In Track 2, we maintain separate
 * consumer and producer clients per connection string rather than pooling a single
 * EventHubClient.
 *
 * @param ehConf The Event Hubs configurations corresponding to this specific connection pool.
 */
private class ClientConnectionPool(val ehConf: EventHubsConf) extends Logging {

  private[this] var producerClient: EventHubsProducerClient = _
  private[this] var consumerClient: EventHubsConsumerClient = _
  private[this] val clientLock = new Object()

  /**
   * Creates or retrieves the producer client for this connection pool.
   * Track 2: Returns EventHubsProducerClient for sending events.
   *
   * @return the [[EventHubsProducerClient]]
   */
  private def getOrCreateProducerClient: EventHubsProducerClient = {
    clientLock.synchronized {
      if (producerClient == null) {
        logInfo(
          s"Creating producer client. Namespace: ${ehConf.namespaceUri}, EventHub name: ${ehConf.name}")
        producerClient = createProducerClient()
      } else {
        logInfo(
          s"Reusing existing producer client. Namespace: ${ehConf.namespaceUri}, EventHub name: ${ehConf.name}")
      }
      producerClient
    }
  }

  /**
   * Creates or retrieves the consumer client for this connection pool.
   * Track 2: Returns EventHubsConsumerClient for receiving events from all partitions.
   *
   * @return the [[EventHubsConsumerClient]]
   */
  private def getOrCreateConsumerClient: EventHubsConsumerClient = {
    clientLock.synchronized {
      if (consumerClient == null) {
        val consumerGroup = ehConf.consumerGroup.getOrElse(DefaultConsumerGroup)
        logInfo(
          s"Creating consumer client. Namespace: ${ehConf.namespaceUri}, EventHub name: ${ehConf.name}, " +
            s"ConsumerGroup: $consumerGroup")
        consumerClient = createConsumerClient()
      } else {
        logInfo(
          s"Reusing existing consumer client. Namespace: ${ehConf.namespaceUri}, EventHub name: ${ehConf.name}")
      }
      consumerClient
    }
  }

  /**
   * Creates a new EventHubsProducerClient using Track 2 builder pattern.
   */
  private def createProducerClient(): EventHubsProducerClient = {
    val builder = new EventHubsClientBuilder()
      .connectionString(ehConf.connectionString)
      .eventHubName(ehConf.name)

    // Add custom credential if using AAD authentication
    if (ehConf.useAadAuth && ehConf.aadAuthCallback().isDefined) {
      // Note: For full AAD support, use TokenCredential from azure-identity
      // For now, connection string + endpoint works for most scenarios
      logInfo(s"AAD authentication requested for producer")
    }

    builder.buildProducerClient()
  }

  /**
   * Creates a new EventHubsConsumerClient using Track 2 builder pattern.
   */
  private def createConsumerClient(): EventHubsConsumerClient = {
    val consumerGroup = ehConf.consumerGroup.getOrElse(DefaultConsumerGroup)

    val builder = new EventHubsClientBuilder()
      .connectionString(ehConf.connectionString)
      .eventHubName(ehConf.name)
      .consumerGroup(consumerGroup)

    // Add custom credential if using AAD authentication
    if (ehConf.useAadAuth && ehConf.aadAuthCallback().isDefined) {
      // Note: For full AAD support, use TokenCredential from azure-identity
      logInfo(s"AAD authentication requested for consumer")
    }

    builder.buildConsumerClient()
  }

  /**
   * Closes all client resources when the pool is no longer needed.
   */
  def close(): Unit = {
    clientLock.synchronized {
      try {
        if (producerClient != null) {
          logInfo(s"Closing producer client for ${ehConf.name}")
          producerClient.close()
        }
      } catch {
        case e: Exception =>
          logWarning(s"Error closing producer client: ${e.getMessage}")
      }
      try {
        if (consumerClient != null) {
          logInfo(s"Closing consumer client for ${ehConf.name}")
          consumerClient.close()
        }
      } catch {
        case e: Exception =>
          logWarning(s"Error closing consumer client: ${e.getMessage}")
      }
      producerClient = null
      consumerClient = null
    }
  }
}

/**
 * The connection pool singleton that is created per JVM. This holds a map
 * of Event Hubs connection strings to their specific connection pool instances.
 * Track 2: Manages separate producer and consumer clients.
 */
object ClientConnectionPool extends Logging {

  private def notInitializedMessage(name: String): String = {
    s"Connection pool is not initialized for EventHubs: $name"
  }

  type MutableMap[A, B] = scala.collection.mutable.HashMap[A, B]

  private[this] val pools = new MutableMap[String, ClientConnectionPool]()

  private def isInitialized(key: String): Boolean = pools.synchronized {
    pools.get(key).isDefined
  }

  private def ensureInitialized(key: String): Unit = {
    if (!isInitialized(key)) {
      val message = notInitializedMessage(key)
      throw new IllegalStateException(message)
    }
  }

  private def key(ehConf: EventHubsConf): String = {
    ehConf.connectionString.toLowerCase
  }

  private def getOrCreatePool(ehConf: EventHubsConf): ClientConnectionPool = pools.synchronized {
    val poolKey = key(ehConf)
    pools.getOrElseUpdate(poolKey, new ClientConnectionPool(ehConf))
  }

  private def get(key: String): ClientConnectionPool = pools.synchronized {
    pools.getOrElse(key, {
      val message = notInitializedMessage(key)
      throw new IllegalStateException(message)
    })
  }

  /**
   * Gets or creates a producer client for the given Event Hubs configuration.
   * Track 2: Returns EventHubsProducerClient for sending events.
   *
   * @param ehConf the [[EventHubsConf]] used to lookup the connection pool
   * @return the [[EventHubsProducerClient]]
   */
  def getProducerClient(ehConf: EventHubsConf): EventHubsProducerClient = {
    val pool = getOrCreatePool(ehConf)
    pool.getOrCreateProducerClient
  }

  /**
   * Gets or creates a consumer client for the given Event Hubs configuration.
   * Track 2: Returns EventHubsConsumerClient for receiving events.
   *
   * @param ehConf the [[EventHubsConf]] used to lookup the connection pool
   * @return the [[EventHubsConsumerClient]]
   */
  def getConsumerClient(ehConf: EventHubsConf): EventHubsConsumerClient = {
    val pool = getOrCreatePool(ehConf)
    pool.getOrCreateConsumerClient
  }

  /**
   * Closes all clients in all pools. Should be called when the application shuts down.
   */
  def closeAllPools(): Unit = pools.synchronized {
    pools.values.foreach { pool =>
      try {
        pool.close()
      } catch {
        case e: Exception =>
          logWarning(s"Error closing pool: ${e.getMessage}")
      }
    }
    pools.clear()
  }
}

/**
 * Cache for [[ScheduledExecutorService]]s.
 */
object ClientThreadPool {
  type MutableMap[A, B] = scala.collection.mutable.HashMap[A, B]

  private[this] val pools = new MutableMap[String, ScheduledExecutorService]()

  private def key(ehConf: EventHubsConf): String = {
    ehConf.connectionString.toLowerCase
  }

  def get(ehConf: EventHubsConf): ScheduledExecutorService = {
    pools.synchronized {
      val keyName = key(ehConf)
      pools.getOrElseUpdate(
        keyName,
        Executors.newScheduledThreadPool(ehConf.threadPoolSize.getOrElse(DefaultThreadPoolSize)))
    }
  }
}
