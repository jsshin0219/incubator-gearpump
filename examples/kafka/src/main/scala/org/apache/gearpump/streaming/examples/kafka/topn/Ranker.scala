/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.gearpump.streaming.examples.kafka.topn

import org.apache.gearpump.streaming.task.{TaskActor, TaskContext}
import org.apache.gearpump.Message
import org.apache.gearpump.util.Configs
import org.slf4j.{Logger, LoggerFactory}

object Ranker {
  private val LOG: Logger = LoggerFactory.getLogger(classOf[Ranker])
}

class Ranker(conf: Configs) extends TaskActor(conf) {

  import org.apache.gearpump.streaming.examples.kafka.topn.Ranker._
  import org.apache.gearpump.streaming.examples.kafka.topn.RollingTopWords.Config._

  private val config = conf.config
  private val windowLengthMS = getWindowLengthMS(config)
  private val emitFrequencyMS = getEmitFrequencyMS(config)
  private val topn = getTopN(config)
  private var lastEmitTime = 0L
  protected val rankings: Rankings[String] = new Rankings[String]

  def onStart(context: TaskContext): Unit = {}

  def onNext(msg: Message): Unit = {
    updateRankingsWithMessage(msg)
    val timestamp = msg.timestamp
    if (timestamp - lastEmitTime > emitFrequencyMS) {
      val topnR = rankings.getTopN(topn)
      LOG.info(s"top $topn words in last ${windowLengthMS / 1000.0 } seconds: ${topnR.toString}")
      topnR.foreach(r => output(Message(r, timestamp)))
      lastEmitTime = timestamp
      rankings.clear()
    }
  }

  def updateRankingsWithMessage(msg: Message): Unit = {
    val (obj, count) = msg.msg.asInstanceOf[(String, Long)]
    rankings.update(obj, count)
  }
}
