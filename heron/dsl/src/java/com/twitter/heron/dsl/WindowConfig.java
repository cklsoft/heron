//  Copyright 2017 Twitter. All rights reserved.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package com.twitter.heron.dsl;


import java.time.Duration;

import com.twitter.heron.api.bolt.BaseWindowedBolt;

/**
 * A Streamlet is a (potentially unbounded) ordered collection of tuples.
 Streamlets originate from pub/sub systems(such Pulsar/Kafka), or from static data(such as
 csv files, HDFS files), or for that matter any other source. They are also created by
 transforming existing Streamlets using operations such as map/flatMap, etc.
 Besides the tuples, a Streamlet has the following properties associated with it
 a) name. User assigned or system generated name to refer the streamlet
 b) nPartitions. Number of partitions that the streamlet is composed of. The nPartitions
 could be assigned by the user or computed by the system
 */
public final class WindowConfig {
  public static WindowConfig createTimeWindow(Duration windowDuration) {
    return new WindowConfig(windowDuration, windowDuration);
  }
  public static WindowConfig createTimeWindow(Duration windowDuration, Duration slideInterval) {
    return new WindowConfig(windowDuration, slideInterval);
  }
  public static WindowConfig createCountWindow(int windowSize) {
    return new WindowConfig(windowSize, windowSize);
  }
  public static WindowConfig createCountWindow(int windowSize, int slideSize) {
    return new WindowConfig(windowSize, slideSize);
  }
  public void attachWindowConfig(BaseWindowedBolt bolt) {
    if (windowType == WindowType.COUNT) {
      bolt.withWindow(BaseWindowedBolt.Count.of(windowSize),
          BaseWindowedBolt.Count.of(slideInterval));
    } else {
      bolt.withWindow(windowDuration, slidingIntervalDuration);
    }
  }
  private enum WindowType { TIME, COUNT }
  private WindowType windowType;
  private int windowSize;
  private int slideInterval;
  private Duration windowDuration;
  private Duration slidingIntervalDuration;
  private WindowConfig(Duration windowDuration, Duration slidingIntervalDuration) {
    this.windowType = WindowType.TIME;
    this.windowDuration = windowDuration;
    this.slidingIntervalDuration = slidingIntervalDuration;
  }
  private WindowConfig(int windowSize, int slideInterval) {
    this.windowType = WindowType.COUNT;
    this.windowSize = windowSize;
    this.slideInterval = slideInterval;
  }
}
