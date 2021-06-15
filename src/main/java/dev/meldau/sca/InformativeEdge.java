package dev.meldau.sca;

import org.jgrapht.graph.DefaultEdge;

/**
 * Class which extends DefaultEdge of jgrapht by making getSource() and getTarget() public
 * @author Ingo Meldau
 */
public class InformativeEdge extends DefaultEdge {

  /**
   * This function returns the source of the directed Edge.
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
   * This function is overwritten because it is needed for testing and private in the super class.
   *
   * @return target String
   */
  @Override
  public Object getTarget() {
    return super.getTarget();
  }

}