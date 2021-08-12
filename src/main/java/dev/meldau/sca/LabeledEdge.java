package dev.meldau.sca;

import org.jgrapht.graph.DefaultEdge;

/*
 * Copyright 2020-2021 Ingo Meldau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Class which extends DefaultEdge of jgrapht by adding Labels. These Labels represent different
 * types of Coupling between classes. In future releases this could be used to calculate better
 * metrics by giving each type a weight.
 *
 * @author Ingo Meldau
 */
public class LabeledEdge extends DefaultEdge {

  /** Stores the type of connection of the Object. */
  private ConnectionType connectionType;

  /**
   * Constructor for labeled Edges takes the connectionType (enum) as a parameter.
   *
   * @param connectionType Type of connection. Defined in enum.
   */
  public LabeledEdge(ConnectionType connectionType) {
    super();
    this.connectionType = connectionType;
  }

  /**
   * Returns the connection type of the edge
   *
   * @return type of connection (enum)
   */
  public ConnectionType getConnectionType() {
    return connectionType;
  }

  /**
   * Set type of connection (enum)
   *
   * @param connectionType Type of connection. Defined in enum.
   */
  @SuppressWarnings("unused")
  public void setConnectionType(ConnectionType connectionType) {
    this.connectionType = connectionType;
  }

  /**
   * This function returns the source of the directed Edge.
   * <!-- -->
   * This function is overwritten because it is needed for testing and private in the super class.
   *
   * @return source (String)
   */
  @Override
  public Object getSource() {
    return super.getSource();
  }

  /**
   * This function returns the target of the directed Edge.
   * <!-- -->
   * This function is overwritten because it is needed for testing and private in the super class.
   *
   * @return target String
   */
  @Override
  public Object getTarget() {
    return super.getTarget();
  }

  /** Overrides the toString() function of the superclass to show the type of connection. */
  @Override
  public String toString() {
    return "(" + getSource() + " : " + getTarget() + " : " + getConnectionType() + ")";
  }

}
