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
package de.zbit.gui.wizard;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * This class is responsible for reacting to events generated by pushing any of
 * the three buttons, 'Next', 'Previous', and 'Cancel.' Based on what button is
 * pressed, the controller will update the model to show a new panel and reset
 * the state of the buttons as necessary.
 * 
 * 
 * This implementation is based on the tutorial for Wizads in Swing, retrieved
 * from http://java.sun.com/developer/technicalArticles/GUI/swing/wizard/ on
 * September 12th, 2011. Author of the tutorial article is Robert Eckstein.
 * 
 * @author Robert Eckstein
 * @author Florian Mittag
 * @version $Rev$
 */
public class WizardController implements ActionListener {

  private Wizard wizard;

  /**
   * This constructor accepts a reference to the Wizard component that created
   * it, which it uses to update the button components and access the
   * WizardModel.
   * 
   * @param w A callback to the Wizard component that created this controller.
   */
  public WizardController(Wizard w) {
    wizard = w;
  }

  /**
   * Calling method for the action listener interface. This class listens for
   * actions performed by the buttons in the Wizard class, and calls methods
   * below to determine the correct course of action.
   * 
   * @param evt The ActionEvent that occurred.
   */
  public void actionPerformed(java.awt.event.ActionEvent evt) {
    if (evt.getActionCommand().equals(Wizard.CANCEL_BUTTON_ACTION_COMMAND))
      cancelButtonPressed();
    else if (evt.getActionCommand().equals(Wizard.BACK_BUTTON_ACTION_COMMAND))
      backButtonPressed();
    else if (evt.getActionCommand().equals(Wizard.NEXT_BUTTON_ACTION_COMMAND))
      nextButtonPressed();
    else if (evt.getActionCommand().equals(Wizard.HELP_BUTTON_ACTION_COMMAND))
      helpButtonPressed();

  }

  private void cancelButtonPressed() {

    wizard.close(Wizard.CANCEL_RETURN_CODE);
  }

  public void nextButtonPressed() {

    WizardModel model = wizard.getModel();
    WizardPanelDescriptor descriptor = model.getCurrentPanelDescriptor();

    //  If it is a finishable panel, close down the dialog. Otherwise,
    //  get the ID that the current panel identifies as the next panel,
    //  and display it.

    Object nextPanelDescriptor = descriptor.getNextPanelDescriptor();

    if (nextPanelDescriptor instanceof WizardPanelDescriptor.FinishIdentifier) {
      wizard.getModel().getCurrentPanelDescriptor().aboutToHidePanel();
      wizard.close(Wizard.FINISH_RETURN_CODE);
    } else {
      wizard.setCurrentPanel(nextPanelDescriptor);
    }

  }

  public void backButtonPressed() {

    WizardModel model = wizard.getModel();
    WizardPanelDescriptor descriptor = model.getCurrentPanelDescriptor();

    //  Get the descriptor that the current panel identifies as the previous
    //  panel, and display it.

    Object backPanelDescriptor = descriptor.getBackPanelDescriptor();
    wizard.setCurrentPanel(backPanelDescriptor);

  }
  
  private void helpButtonPressed() {

    WizardModel model = wizard.getModel();
    WizardPanelDescriptor descriptor = model.getCurrentPanelDescriptor();
    Component helpComp = descriptor.getHelpAction();
    
    if (helpComp != null) {
    	helpComp.setVisible(true);
    }
  }

  void resetButtonsToPanelRules() {

    //  Reset the buttons to support the original panel rules,
    //  including whether the next or back buttons are enabled or
    //  disabled, or if the panel is finishable.

    WizardModel model = wizard.getModel();
    WizardPanelDescriptor descriptor = model.getCurrentPanelDescriptor();

    model.setCancelButtonText(Wizard.CANCEL_TEXT);
    model.setCancelButtonIcon(Wizard.CANCEL_ICON);

    model.setHelpButtonText(Wizard.HELP_TEXT);
    model.setHelpButtonIcon(Wizard.HELP_ICON);

    //  If the panel in question has a help action associated with it, enable
    //  the help button. Otherwise, disable it.

    if (descriptor.getHelpAction() != null)
      model.setHelpButtonEnabled(Boolean.TRUE);
    else
      model.setHelpButtonEnabled(Boolean.FALSE);

    //  If the panel in question has another panel behind it, enable
    //  the back button. Otherwise, disable it.

    model.setBackButtonText(Wizard.BACK_TEXT);
    model.setBackButtonIcon(Wizard.BACK_ICON);

    if (descriptor.getBackPanelDescriptor() != null)
      model.setBackButtonEnabled(Boolean.TRUE);
    else
      model.setBackButtonEnabled(Boolean.FALSE);

    //  If the panel in question has one or more panels in front of it,
    //  enable the next button. Otherwise, disable it.

    if (descriptor.getNextPanelDescriptor() != null)
      model.setNextFinishButtonEnabled(Boolean.TRUE);
    else
      model.setNextFinishButtonEnabled(Boolean.FALSE);

    //  If the panel in question is the last panel in the series, change
    //  the Next button to Finish. Otherwise, set the text back to Next.

    if (descriptor.getNextPanelDescriptor() instanceof WizardPanelDescriptor.FinishIdentifier) {
      model.setNextFinishButtonText(Wizard.FINISH_TEXT);
      model.setNextFinishButtonIcon(Wizard.FINISH_ICON);
    } else {
      model.setNextFinishButtonText(Wizard.NEXT_TEXT);
      model.setNextFinishButtonIcon(Wizard.NEXT_ICON);
    }

  }

}
