package dev.meldau.sca;

import org.jgrapht.graph.DefaultEdge;

/**
 * Class which extends DefaultEdge of jgrapht by adding Labels. These Labels represent different
 * types of Coupling between classes In future releases this could be used to calculate better
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
   * @param connectionType
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
   * set type of connection (enum)
   *
   * @param connectionType
   */
  public void setConnectionType(ConnectionType connectionType) {
    this.connectionType = connectionType;
  }

  /** Overrides the toString() function of the superclass to show the type of connection. */
  @Override
  public String toString() {
    return "(" + getSource() + " : " + getTarget() + " : " + getConnectionType() + ")";
  }

  /** Enum of all connection types */
  public enum ConnectionType {
    SUPERCLASS,
    INSTANCE_VARIABLE,
    CALLS_METHOD,
    LOCAL_VARIABLE,
    PARAMETER_TYPE,
    ACCESS_PUBLIC_VARIABLE,
  }
}
