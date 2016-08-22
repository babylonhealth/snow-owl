package org.protege.editor.owl.ui.view.individual;

import org.protege.editor.core.ui.RefreshableComponent;
import org.protege.editor.core.ui.view.DisposableAction;
import org.protege.editor.owl.model.entity.OWLEntityCreationSet;
import org.protege.editor.owl.model.event.EventType;
import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
import org.protege.editor.owl.model.event.OWLModelManagerListener;
import org.protege.editor.owl.ui.OWLIcons;
import org.protege.editor.owl.ui.action.DeleteIndividualAction;
import org.protege.editor.owl.ui.list.OWLObjectList;
import org.protege.editor.owl.ui.view.ChangeListenerMediator;
import org.protege.editor.owl.ui.view.CreateNewTarget;
import org.protege.editor.owl.ui.view.Deleteable;
import org.protege.editor.owl.ui.view.Findable;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLEntityCollector;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.util.OWLEntitySetProvider;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 29-Jan-2007<br>
 * <br>
 * <p/> This definitely needs a rethink - it is a totally inefficient hack!
 */

/*
 * TODO - the need for this class probably indicates a problem with the individuals views
 *        I do use it but for an ontology with a large number of individuals this view is 
 *        totally worthless.   A good protege would not have this class.  Note that this is 
 *        also used in individual selection - that is a terrible thing.
 * TODO - This should not be constrained to named individuals only.
 */
public class OWLIndividualListViewComponent extends AbstractOWLIndividualViewComponent
        implements Findable<OWLNamedIndividual>, Deleteable, CreateNewTarget, RefreshableComponent {

    /**
     * 
     */
    private static final long serialVersionUID = -1519269944342726754L;
    private OWLObjectList<OWLNamedIndividual> list;
    private OWLOntologyChangeListener listener;
    private ChangeListenerMediator changeListenerMediator;
    private OWLModelManagerListener modelManagerListener;
    private int selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
    private boolean selectionChangedByUser = true;

    protected Set<OWLNamedIndividual> individualsInList;

    private ListSelectionListener listSelectionListener = new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                if (list.getSelectedValue() != null && selectionChangedByUser) {
                    setGlobalSelection((OWLNamedIndividual)list.getSelectedValue());
                }
                changeListenerMediator.fireStateChanged(OWLIndividualListViewComponent.this);
            }
        }
    };


    public void initialiseIndividualsView() throws Exception {
        list = new OWLObjectList<OWLNamedIndividual>(getOWLEditorKit());
        list.setSelectionMode(selectionMode);
        setLayout(new BorderLayout());
        add(new JScrollPane(list));
        list.addListSelectionListener(listSelectionListener);
        list.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                setGlobalSelection((OWLNamedIndividual)list.getSelectedValue());
            }
        });
        listener = new OWLOntologyChangeListener() {
            public void ontologiesChanged(
                    List<? extends OWLOntologyChange> changes) {
                processChanges(changes);
            }
        };
        getOWLModelManager().addOntologyChangeListener(listener);

        setupActions();
        changeListenerMediator = new ChangeListenerMediator();
        individualsInList = new TreeSet<OWLNamedIndividual>(getOWLModelManager().getOWLObjectComparator());
        refill();
        modelManagerListener = new OWLModelManagerListener() {
            public void handleChange(OWLModelManagerChangeEvent event) {
                if (event.isType(EventType.ACTIVE_ONTOLOGY_CHANGED) || event.isType(EventType.ONTOLOGY_RELOADED)) {
                    refill();
                }
            }
        };
        getOWLModelManager().addListener(modelManagerListener);
    }


    protected void setupActions() {
        addAction(new AddIndividualAction(), "A", "A");
        addAction(new DeleteIndividualAction(getOWLEditorKit(),
                                             new OWLEntitySetProvider<OWLNamedIndividual>() {
                                                 public Set<OWLNamedIndividual> getEntities() {
                                                     return getSelectedIndividuals();
                                                 }
                                             }), "B", "A");
    }


    public void refreshComponent() {
        refill();
    }


    protected void refill() {
        // Initial fill
        individualsInList.clear();
        for (OWLOntology ont : getOntologies()) {
            individualsInList.addAll(ont.getIndividualsInSignature());
        }
        reset();
    }


    protected Set<OWLOntology> getOntologies() {
        return getOWLModelManager().getActiveOntologies();
    }


    public void setSelectedIndividual(OWLIndividual individual) {
        list.setSelectedValue(individual, true);
    }


    protected void reset() {
        list.setListData(individualsInList.toArray());
        OWLNamedIndividual individual = getSelectedOWLIndividual();
        selectionChangedByUser = false;
        try {
            list.setSelectedValue(individual, true);
        }
        finally {
            selectionChangedByUser = true;
        }
    }

    public OWLNamedIndividual updateView(OWLNamedIndividual selelectedIndividual) {
        if (!isPinned()) {
            list.setSelectedValue(selelectedIndividual, true);
        }
        return (OWLNamedIndividual) list.getSelectedValue();
    }

    public void disposeView() {
        getOWLModelManager().removeOntologyChangeListener(listener);
        getOWLModelManager().removeListener(modelManagerListener);
    }

    public OWLNamedIndividual getSelectedIndividual() {
        return (OWLNamedIndividual) list.getSelectedValue();
    }

    public Set<OWLNamedIndividual> getSelectedIndividuals() {
        Set<OWLNamedIndividual> inds = new HashSet<OWLNamedIndividual>();
        for (Object obj : list.getSelectedValues()) {
            inds.add((OWLNamedIndividual) obj);
        }
        return inds;
    }

    protected void processChanges(List<? extends OWLOntologyChange> changes) {
    	Set<OWLEntity> possiblyAddedObjects = new HashSet<OWLEntity>();
    	Set<OWLEntity> possiblyRemovedObjects = new HashSet<OWLEntity>();
        OWLEntityCollector addedCollector = new OWLEntityCollector(possiblyAddedObjects);
        OWLEntityCollector removedCollector = new OWLEntityCollector(possiblyRemovedObjects);
        for (OWLOntologyChange chg : changes) {
            if (chg.isAxiomChange()) {
                OWLAxiomChange axChg = (OWLAxiomChange) chg;
                if (axChg instanceof AddAxiom) {
                    axChg.getAxiom().accept(addedCollector);
                } else {
                    axChg.getAxiom().accept(removedCollector);
                }
            }
        }
        boolean mod = false;
        for (OWLEntity ent : possiblyAddedObjects) {
            if (ent instanceof OWLIndividual) {
                if (individualsInList.add((OWLNamedIndividual) ent)) {
                    mod = true;
                }
            }
        }
        for (OWLEntity ent : possiblyRemovedObjects) {
            if (ent instanceof OWLIndividual) {
                boolean stillReferenced = false;
                for (OWLOntology ont : getOntologies()) {
                    if (ont.containsIndividualInSignature(ent.getIRI())) {
                        stillReferenced = true;
                        break;
                    }
                }
                if (!stillReferenced) {
                    if (individualsInList.remove(ent)) {
                        mod = true;
                    }
                }
            }
        }
        if (mod) {
            reset();
        }
    }

    protected void addIndividual() {
        OWLEntityCreationSet<OWLNamedIndividual> set = getOWLWorkspace().createOWLIndividual();
        if (set == null) {
            return;
        }
        List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
        changes.addAll(set.getOntologyChanges());
        changes.addAll(dofurtherCreateSteps(set.getOWLEntity()));
        getOWLModelManager().applyChanges(changes);
        OWLNamedIndividual ind = set.getOWLEntity();
        if (ind != null) {
            list.setSelectedValue(ind, true);
        }
    }


    protected List<OWLOntologyChange> dofurtherCreateSteps(OWLIndividual newIndividual) {
        return Collections.EMPTY_LIST;
    }


    public List<OWLNamedIndividual> find(String match) {
        return new ArrayList<OWLNamedIndividual>(getOWLModelManager().getOWLEntityFinder().getMatchingOWLIndividuals(match));
    }

    public void show(OWLNamedIndividual owlEntity) {
        list.setSelectedValue(owlEntity, true);
    }


    public void setSelectedIndividuals(Set<OWLNamedIndividual> individuals) {
        list.setSelectedValues(individuals, true);
    }


    private class AddIndividualAction extends DisposableAction {
        /**
         * 
         */
        private static final long serialVersionUID = 4574601252717263757L;

        public AddIndividualAction() {
            super("Add individual", OWLIcons.getIcon("individual.add.png"));
        }

        public void actionPerformed(ActionEvent e) {
            addIndividual();
        }

        public void dispose() {
        }
    }

    public void addChangeListener(ChangeListener listener) {
        changeListenerMediator.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changeListenerMediator.removeChangeListener(listener);
    }

    public void handleDelete() {
        OWLEntityRemover entityRemover = new OWLEntityRemover(getOWLModelManager().getOWLOntologyManager(),
                                                              getOWLModelManager().getOntologies());
        for (OWLNamedIndividual ind : getSelectedIndividuals()) {
            ind.accept(entityRemover);
        }
        getOWLModelManager().applyChanges(entityRemover.getChanges());
    }

    public boolean canDelete() {
        return !getSelectedIndividuals().isEmpty();
    }

    public boolean canCreateNew() {
        return true;
    }

    public void createNewObject() {
        addIndividual();
    }

    public void setSelectionMode(int selectionMode) {
        selectionMode = selectionMode;
        if (list != null) {
            list.setSelectionMode(selectionMode);
        }
    }
    
    public void setIndividualListColor(Color c) {
        list.setBackground(c);
    }
}
