/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of the SysBio API library.
 *
 * Copyright (C) 2009-2016 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.sbml.layout.y;

import org.sbml.jsbml.ext.layout.Curve;

import de.zbit.sbml.layout.Production;
import y.view.Arrow;
import y.view.EdgeRealizer;

/**
 * yFiles implementation of arc type {@link Production}.
 * 
 * @author Jakob Matthes
 * @version $Rev$
 */
public class YProduction extends YAbstractSBGNArc implements Production<EdgeRealizer> {
  
  /* (non-Javadoc)
   * @see de.zbit.sbml.layout.SBGNArc#draw(org.sbml.jsbml.ext.layout.Curve)
   */
  @Override
  public EdgeRealizer draw(Curve curve) {
    // Sometimes it can be necessary to reverse order of curve segments and of
    // start and end points because curves are always specified in the direction
    // of the reaction (from substrate process node, from process node to product).
    EdgeRealizer edgeRealizer = YLayoutBuilder.createEdgeRealizerFromCurve(curve, true);
    edgeRealizer.setTargetArrow(Arrow.DELTA);
    return edgeRealizer;
  }
  
}
