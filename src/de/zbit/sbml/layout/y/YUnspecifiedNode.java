/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of the SysBio API library.
 *
 * Copyright (C) 2009-2012 by the University of Tuebingen, Germany.
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

import org.sbml.jsbml.SBO;

import y.view.NodeRealizer;
import de.zbit.graph.io.def.SBGNVisualizationProperties;
import de.zbit.sbml.layout.UnspecifiedNode;

/**
 * @author Jakob Matthes
 * @version $Rev$
 */
public class YUnspecifiedNode extends UnspecifiedNode<NodeRealizer> {

	/* (non-Javadoc)
	 * @see de.zbit.sbml.layout.SBGNNode#draw(double, double, double, double, double, double)
	 */
	@Override
	public NodeRealizer draw(double x, double y, double z, double width,
			double height, double depth) {
		int sboTerm = SBO.getUnknownMolecule();
		NodeRealizer nr = SBGNVisualizationProperties.getNodeRealizer(sboTerm);
		nr = nr.createCopy();
		nr.setCenterX(x);
		nr.setCenterY(y);
		nr.setWidth(width);
		nr.setHeight(height);
		
		return nr;
	}

}