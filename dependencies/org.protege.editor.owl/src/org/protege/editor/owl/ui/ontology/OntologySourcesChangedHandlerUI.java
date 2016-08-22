package org.protege.editor.owl.ui.ontology;

import org.protege.editor.core.ProtegeManager;
import org.protege.editor.core.ui.util.JOptionPaneEx;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.OWLWorkspace;
import org.protege.editor.owl.model.io.OntologySourcesListener;
import org.protege.editor.owl.model.io.OntologySourcesManager;
import org.protege.editor.owl.ui.selector.OWLOntologySelectorPanel2;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
/*
* Copyright (C) 2007, University of Manchester
*
* Modifications to the initial code base are copyright of their
* respective authors, or their employers as appropriate.  Authorship
* of the modifications may be determined from the ChangeLog placed at
* the end of this file.
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.

* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.

* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

/**
 * Author: drummond<br>
 * http://www.cs.man.ac.uk/~drummond/<br><br>
 * <p/>
 * The University Of Manchester<br>
 * Bio Health Informatics Group<br>
 * Date: Sep 24, 2008<br><br>
 */
public class OntologySourcesChangedHandlerUI implements OntologySourcesListener {

    private OWLEditorKit eKit;

    private static final String TITLE = "Ontology sources changed";

    private boolean handlingChange = false;

    private OWLOntologySelectorPanel2 ontologiesPanel;


    public OntologySourcesChangedHandlerUI(final OWLWorkspace workspace) {
        this.eKit = workspace.getOWLEditorKit();

        getSourcesManager().addListener(this);

        // when the workspace is first shown
        workspace.addAncestorListener(new AncestorListener(){
            public void ancestorAdded(AncestorEvent event) {
                // add a listener for when the window gets activated
                ProtegeManager.getInstance().getFrame(workspace).addWindowListener(new WindowAdapter() {
                    public void windowActivated(WindowEvent event) {
                        handleWindowActivated();
                    }
                });
                workspace.removeAncestorListener(this);
            }


            public void ancestorRemoved(AncestorEvent event) {
                // do nothing
            }


            public void ancestorMoved(AncestorEvent event) {
                // do nothing
            }
        });
    }


    public void ontologySourcesChanged(OntologySourcesChangeEvent event) {
        handlingChange = true;
        Set<OWLOntology> onts = event.getOntologies();
        if (onts.size() == 1){
            OWLOntology ont = onts.iterator().next();
            StringBuilder message = new StringBuilder("<html>An ontology has changed outside of Protege.");
            message.append("<p><p><code>").append(eKit.getModelManager().getRendering(ont)).append("</code>");
            message.append("<p><p>Would you like to reload?");
            if (eKit.getModelManager().getDirtyOntologies().contains(ont)){
                message.append("<p><p><b>Warning: this ontology has been edited so you will lose local changes</b>");
            }
            message.append("</html>");
            if (JOptionPane.showConfirmDialog(eKit.getWorkspace(),
                                              message.toString(),
                                              TITLE,
                                              JOptionPane.YES_NO_OPTION,
                                              JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION){
                try {
                    eKit.getModelManager().reload(ont);
                }
                catch (OWLOntologyCreationException e) {
                    handleFailedToReload(ont);
                }
            }
            else{
                getSourcesManager().ignoreUpdates(onts);
            }
        }
        else{
            ontologiesPanel = new OWLOntologySelectorPanel2(eKit, onts);
            ontologiesPanel.add(new JLabel("<html>The following ontologies have changed outside of Protege.<p><p>Would you like to reload?</html>"), BorderLayout.NORTH);


            final Set<OWLOntology> ignoreOnts = new HashSet<OWLOntology>(onts);

            if (JOptionPaneEx.showConfirmDialog(eKit.getWorkspace(),
                                                TITLE,
                                                ontologiesPanel,
                                                JOptionPane.WARNING_MESSAGE,
                                                JOptionPane.YES_NO_OPTION,
                                                null) == JOptionPane.OK_OPTION){

                final Set<OWLOntology> reloadOnts = getFilteredValues();
                ignoreOnts.removeAll(reloadOnts);
                for (OWLOntology ont : reloadOnts){
                    try {
                        eKit.getModelManager().reload(ont);
                    }
                    catch (OWLOntologyCreationException e) {
                        handleFailedToReload(ont);
                    }
                }
            }

            if (!ignoreOnts.isEmpty()){
                getSourcesManager().ignoreUpdates(ignoreOnts);
            }
        }
        handlingChange = false;
    }


    private Set<OWLOntology> getFilteredValues() {
        return ontologiesPanel.getSelectedOntologies();
    }


    private void handleFailedToReload(OWLOntology ont) {
        JOptionPane.showMessageDialog(eKit.getWorkspace(),
                                      "<html>Failed to reload ontology<p><p>" +
                                      eKit.getModelManager().getRendering(ont) +
                                      ".<p><p>Ignoring update.</html>");
        getSourcesManager().ignoreUpdates(Collections.singleton(ont));
    }


    protected void handleWindowActivated() {
        if (!handlingChange){ // don't bother when we are already performing a check
            OntologySourcesManager sourcesMngr = getSourcesManager();
            if (sourcesMngr != null){
                sourcesMngr.checkSources();
            }
        }
    }


    private OntologySourcesManager getSourcesManager() {
        return eKit.getModelManager().get(OntologySourcesManager.ID);
    }
}
