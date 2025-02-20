/**
 * Copyright (C) 2015-2016 Uber Technology Inc. (streaming-core@uber.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gm.mqtransfer.manager.support.helix;

import org.apache.helix.HelixDefinedState;
import org.apache.helix.model.StateModelDefinition;

/**
 * Helix built-in Online-offline state model definition
 */
public final class OnlineOfflineStateModel {

public static final String STATE_MODEL_NAME = "OnlineOffline";

  public enum States {
    ONLINE,
    OFFLINE
  }
  
  /**
   * Build OnlineOffline state model definition
   * @return
   */
  public static StateModelDefinition build() {
    StateModelDefinition.Builder builder = new StateModelDefinition.Builder(STATE_MODEL_NAME);
    // init state
    builder.initialState(States.OFFLINE.name());

    // add states
    builder.addState(States.ONLINE.name(), 2);
    builder.addState(States.OFFLINE.name(), 1);
    for (HelixDefinedState state : HelixDefinedState.values()) {
      builder.addState(state.name(), -1);
    }

    // add transitions

    builder.addTransition(States.ONLINE.name(), States.OFFLINE.name(), 3);
    builder.addTransition(States.OFFLINE.name(), States.ONLINE.name(), 2);
    builder.addTransition(States.OFFLINE.name(), HelixDefinedState.DROPPED.name(), 0);

    // bounds
    builder.dynamicUpperBound(States.ONLINE.name(), "R");
    
    return builder.build();
  }

}
