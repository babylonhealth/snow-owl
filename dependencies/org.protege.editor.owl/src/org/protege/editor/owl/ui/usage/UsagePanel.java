package org.protege.editor.owl.ui.usage;

import org.protege.editor.owl.OWLEditorKit;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLProperty;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 21-Feb-2007<br><br>
 */
public class UsagePanel extends JPanel {


    /**
     * 
     */
    private static final long serialVersionUID = 6031827085477038591L;

    private UsageTree tree;

    private JCheckBox showAllCheckbox;
    private JCheckBox showDisjointsCheckbox;
    private JCheckBox showDifferentCheckbox;
    private JCheckBox showNamedSubSuperclassesCheckbox;

    private OWLEntity currentSelection;

    public UsagePanel(OWLEditorKit owlEditorKit) {
        setLayout(new BorderLayout());

        tree = new UsageTree(owlEditorKit);

        showAllCheckbox = new JCheckBox("this", !UsagePreferences.getInstance().isFilterActive(UsageFilter.filterSelf));
        showAllCheckbox.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent event) {
                UsagePreferences.getInstance().setFilterActive(UsageFilter.filterSelf, !showAllCheckbox.isSelected());
                setOWLEntity(currentSelection);
            }
        });

        showDisjointsCheckbox = new JCheckBox("disjoints", !UsagePreferences.getInstance().isFilterActive(UsageFilter.filterDisjoints));
        showDisjointsCheckbox.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent event) {
                UsagePreferences.getInstance().setFilterActive(UsageFilter.filterDisjoints, !showDisjointsCheckbox.isSelected());
                setOWLEntity(currentSelection);
            }
        });

        showDifferentCheckbox = new JCheckBox("different", !UsagePreferences.getInstance().isFilterActive(UsageFilter.filterDifferent));
        showDifferentCheckbox.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent event) {
                UsagePreferences.getInstance().setFilterActive(UsageFilter.filterDifferent, !showDifferentCheckbox.isSelected());
                setOWLEntity(currentSelection);
            }
        });

        showNamedSubSuperclassesCheckbox = new JCheckBox("named sub/superclasses", !UsagePreferences.getInstance().isFilterActive(UsageFilter.filterNamedSubsSupers));
        showNamedSubSuperclassesCheckbox.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent event) {
                UsagePreferences.getInstance().setFilterActive(UsageFilter.filterNamedSubsSupers, !showNamedSubSuperclassesCheckbox.isSelected());                                
                setOWLEntity(currentSelection);
            }
        });

        Box box = new Box(BoxLayout.LINE_AXIS);
        box.add(new JLabel("Show: "));
        box.add(showAllCheckbox);
        box.add(showDisjointsCheckbox);
        box.add(showDifferentCheckbox);
        box.add(showNamedSubSuperclassesCheckbox);

        add(box, BorderLayout.NORTH);
        add(new JScrollPane(tree), BorderLayout.CENTER);
    }


    public void setOWLEntity(OWLEntity entity) {
        currentSelection = entity;
        showNamedSubSuperclassesCheckbox.setVisible(entity != null && entity instanceof OWLClass);
        showDisjointsCheckbox.setVisible(entity != null && (entity instanceof OWLProperty || entity instanceof OWLClass));
        showDifferentCheckbox.setVisible(entity != null && entity instanceof OWLIndividual);
        tree.setOWLEntity(entity);
    }
}
