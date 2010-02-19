/**
 * 
 */
package de.zbit.kegg.gui;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.squeezer.SBMLsqueezer;
import org.sbml.squeezer.gui.GUITools;
import org.sbml.squeezer.gui.SBMLModelSplitPane;

import de.zbit.kegg.io.KEGG2jSBML;

/**
 * @author draeger
 * 
 */
public class ConverterUI extends JFrame {

	/**
	 * Generated serial version id
	 */
	private static final long serialVersionUID = -3833481758555783529L;

	/**
	 * Shows a small GUI.
	 */
	public ConverterUI() {
		super("KGML2SBMLconverter");
		JFileChooser chooser = GUITools.createJFileChooser("usr.dir", false,
				false, JFileChooser.FILES_ONLY, new FileFilterKGML());
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			SBMLDocument doc = KEGG2jSBML.Kegg2jSBML(chooser.getSelectedFile().getAbsolutePath());
			getContentPane().add(new SBMLModelSplitPane(doc.getModel(), SBMLsqueezer.getDefaultSettings()));
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			pack();
			setVisible(true);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new ConverterUI();
	}

}
