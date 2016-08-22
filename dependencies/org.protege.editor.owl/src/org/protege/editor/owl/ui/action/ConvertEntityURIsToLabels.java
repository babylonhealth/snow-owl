package org.protege.editor.owl.ui.action;

import org.protege.editor.core.ui.preferences.PreferencesDialogPanel;
import org.protege.editor.core.ui.util.ComponentFactory;
import org.protege.editor.owl.model.refactor.ontology.ConvertEntityURIsToIdentifierPattern;
import org.protege.editor.owl.model.refactor.ontology.OntologyTargetResolver;
import org.protege.editor.owl.ui.UIHelper;
import org.protege.editor.owl.ui.selector.OWLOntologySelectorPanel;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Set;


/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Medical Informatics Group<br>
 * Date: 30-Aug-2006<br><br>
 * <p/>
 * matthew.horridge@cs.man.ac.uk<br>
 * www.cs.man.ac.uk/~horridgm<br><br>
 */
public class ConvertEntityURIsToLabels extends ProtegeOWLAction {

    private JRadioButton askButton;

    private boolean ask = true;


    public void actionPerformed(ActionEvent e) {

        if (getOWLModelManager().getOntologies().size() == 1){
            JComponent selPanel = createConfirmPanel(false);

            int ret = new UIHelper(getOWLEditorKit()).showDialog("Convert entity URIs to labels",
                                                                 selPanel);
            if (ret == JOptionPane.OK_OPTION) {
                performConversion(null);
            }
        }
        else{
            JComponent selPanel = createConfirmPanel(true);

            int ret = new UIHelper(getOWLEditorKit()).showDialog("Convert entity URIs to labels",
                                                                 selPanel);
            if (ret == JOptionPane.OK_OPTION) {

                OntologyTargetResolver resolver;

                if (askButton.isSelected()){
                    ask = true;
                    resolver = new OntologyTargetResolver(){
                        public Set<OWLOntology> resolve(OWLEntity entity, Set<OWLOntology> ontologies) {
                            return handleResolveTarget(entity, ontologies);
                        }
                    };
                }
                else{
                    ask = false;
                    resolver = new OntologyTargetResolver(){
                        public Set<OWLOntology> resolve(OWLEntity entity, Set<OWLOntology> ontologies) {
                            return ontologies;
                        }
                    };
                }

                performConversion(resolver);
            }
        }
    }


    private void performConversion(OntologyTargetResolver resolver) {
        ConvertEntityURIsToIdentifierPattern converter = new ConvertEntityURIsToIdentifierPattern(getOWLModelManager(),
                                                                                                  getOWLModelManager().getOntologies());
        if (resolver != null){
            converter.setOntologyResolver(resolver);
        }
        converter.performConversion();

        converter.dispose();
    }


    private JComponent createConfirmPanel(boolean showOption) {
        JComponent selPanel = new JPanel(new BorderLayout(8, 12));

        JEditorPane label = ComponentFactory.createHTMLPane(new HyperlinkListener(){
            public void hyperlinkUpdate(HyperlinkEvent event) {
                if (event.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)){
                    showNewEntitiesPrefs();
                }
            }
        });

        StringBuffer html = new StringBuffer("<html><body>");
        html.append("For each entity missing a label in the currently loaded ontologies:<ul>");
        html.append("<li>create a label annotation using its current URI fragment</li>");
        html.append("<li>change its URI to an auto ID</li>");
        html.append("</ul>This conversion uses the current settings in <a href='#'>Preferences | New Entities</a>.");

        if (showOption){
            html.append("<p>Protege will automatically detect the lowest ontologies in the import graph in which to add labels.<br>");
            html.append("If more than one is found what would you like to do?");

            askButton = new JRadioButton("Always ask me for confirmation", ask);
            JRadioButton addToAllReferencingOntologiesButton = new JRadioButton("Automatically add labels to all ontologies found", !ask);

            ButtonGroup bg = new ButtonGroup();
            bg.add(askButton);
            bg.add(addToAllReferencingOntologiesButton);

            Box optionsPanel = new Box(BoxLayout.PAGE_AXIS);
            optionsPanel.setBorder(new EmptyBorder(0, 25, 0, 0));
            optionsPanel.add(askButton);
            optionsPanel.add(addToAllReferencingOntologiesButton);
            selPanel.add(optionsPanel, BorderLayout.CENTER);
        }

        html.append("</body></html>");

        label.setText(html.toString());

        selPanel.add(label, BorderLayout.NORTH);

        return selPanel;
    }


    private void showNewEntitiesPrefs() {
        PreferencesDialogPanel.showPreferencesDialog("New Entities", getEditorKit());
    }


    private Set<OWLOntology> handleResolveTarget(OWLEntity entity, Set<OWLOntology> ontologies) {
        OWLOntologySelectorPanel ontPanel = new OWLOntologySelectorPanel(getOWLEditorKit(), ontologies);
        ontPanel.setSelection(ontologies.iterator().next());
        ontPanel.setMultipleSelectionEnabled(true);

        int ret = new UIHelper(getOWLEditorKit()).showDialog("Select ontologies that will contain a label for " +
                                                             getOWLModelManager().getRendering(entity),
                                                             ontPanel);
        if (ret == JOptionPane.OK_OPTION) {
            return ontPanel.getSelectedOntologies();
        }
        else {
            return Collections.emptySet();
        }
    }


    public void initialise() throws Exception {
    }


    public void dispose() {
    }
}
