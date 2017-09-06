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

package com.twitter.heron.dsl.impl.streamlets;

import java.util.Set;

import com.twitter.heron.api.topology.TopologyBuilder;
import com.twitter.heron.dsl.SerializableBiFunction;
import com.twitter.heron.dsl.WindowConfig;
import com.twitter.heron.dsl.impl.KVStreamletImpl;
import com.twitter.heron.dsl.impl.WindowConfigImpl;
import com.twitter.heron.dsl.impl.bolts.JoinBolt;
import com.twitter.heron.dsl.impl.groupings.JoinCustomGrouping;

/**
 * JoinStreamlet represents a KVStreamlet that is the result of joining two KVStreamlets left
 * and right using a WindowConfig. For all left and right tuples in the window whose keys
 * match, the user supplied joinFunction is applied on the values to get the resulting value.
 * JoinStreamlet's elements are of KeyValue type where the key is KeyWindowInfo<K> type
 * and the value is of type VR.
 */
public final class JoinStreamlet<K, V1, V2, VR> extends KVStreamletImpl<K, VR> {
  private JoinBolt.JoinType joinType;
  private KVStreamletImpl<K, V1> left;
  private KVStreamletImpl<K, V2> right;
  private WindowConfigImpl windowCfg;
  private SerializableBiFunction<? super V1, ? super V2, ? extends VR> joinFn;

  public static <A, B, C, D> JoinStreamlet<A, B, C, D>
      createInnerJoinStreamlet(KVStreamletImpl<A, B> left,
                               KVStreamletImpl<A, C> right,
                               WindowConfig windowCfg,
                               SerializableBiFunction<? super B, ? super C, ? extends D> joinFn) {
    return new JoinStreamlet<A, B, C, D>(JoinBolt.JoinType.INNER, left, right, windowCfg, joinFn);
  }

  public static <A, B, C, D> JoinStreamlet<A, B, C, D>
      createLeftJoinStreamlet(KVStreamletImpl<A, B> left,
                              KVStreamletImpl<A, C> right,
                              WindowConfig windowCfg,
                              SerializableBiFunction<? super B, ? super C, ? extends D> joinFn) {
    return new JoinStreamlet<A, B, C, D>(JoinBolt.JoinType.LEFT, left, right, windowCfg, joinFn);
  }

  public static <A, B, C, D> JoinStreamlet<A, B, C, D>
      createOuterJoinStreamlet(KVStreamletImpl<A, B> left,
                               KVStreamletImpl<A, C> right,
                               WindowConfig windowCfg,
                               SerializableBiFunction<? super B, ? super C, ? extends D> joinFn) {
    return new JoinStreamlet<A, B, C, D>(JoinBolt.JoinType.OUTER, left, right, windowCfg, joinFn);
  }

  private JoinStreamlet(JoinBolt.JoinType joinType, KVStreamletImpl<K, V1> left,
                        KVStreamletImpl<K, V2> right,
                        WindowConfig windowCfg,
                        SerializableBiFunction<? super V1, ? super V2, ? extends VR> joinFn) {
    this.joinType = joinType;
    this.left = left;
    this.right = right;
    this.windowCfg = (WindowConfigImpl) windowCfg;
    this.joinFn = joinFn;
    setNumPartitions(left.getNumPartitions());
  }

  private void calculateName(Set<String> stageNames) {
    int index = 1;
    String name;
    while (true) {
      name = new StringBuilder("join").append(index).toString();
      if (!stageNames.contains(name)) {
        break;
      }
      index++;
    }
    setName(name);
  }

  @Override
  public boolean build_this(TopologyBuilder bldr, Set<String> stageNames) {
    if (!left.isBuilt() || !right.isBuilt()) {
      return false;
    }
    if (getName() == null) {
      calculateName(stageNames);
    }
    if (stageNames.contains(getName())) {
      throw new RuntimeException("Duplicate Names");
    }
    stageNames.add(getName());
    JoinBolt<K, V1, V2, VR> bolt = new JoinBolt<>(joinType, left.getName(),
        right.getName(), joinFn);
    windowCfg.attachWindowConfig(bolt);
    bldr.setBolt(getName(), bolt, getNumPartitions())
        .customGrouping(left.getName(), new JoinCustomGrouping<K, V1>())
        .customGrouping(right.getName(), new JoinCustomGrouping<K, V2>());
    return true;
  }
}
