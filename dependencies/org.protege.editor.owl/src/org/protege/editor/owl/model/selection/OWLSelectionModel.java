package org.protege.editor.owl.model.selection;

import org.protege.editor.owl.model.util.OWLAxiomInstance;
import org.semanticweb.owlapi.model.*;


/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Medical Informatics Group<br>
 * Date: Mar 27, 2006<br><br>
 * <p/>
 * matthew.horridge@cs.man.ac.uk<br>
 * www.cs.man.ac.uk/~horridgm<br><br>
 * <p/>
 * <p/>
 * An <code>OWLSelectionModel</code> keeps track
 * of a list of <code>OWLObjects</code>.  If the list
 * of objects changes, any listeners are notified.  Note that
 * lists are compared using <code>equals</code>.
 */
public interface OWLSelectionModel {

    /**
     * Gets the most recently selected class.
     * @return The selected <code>OWLClass</code>, or <code>null</code>
     *         if a class is not selected.
     */
    public OWLClass getLastSelectedClass();


    /**
     * Gets the most recently selected property
     * @return The selected <code>OWLObjectProperty</code>, or <code>null</code>
     *         if there is no selected property.
     */
    public OWLObjectProperty getLastSelectedObjectProperty();


    /**
     * Gets the most recently selected property
     * @return The selected <code>OWLDataProperty</code>, or <code>null</code>
     *         if there is no selected property.
     */
    public OWLDataProperty getLastSelectedDataProperty();

    /**
     * Gets the most recently selected annotation property
     * @return The selected <code>OWLAnnotationProperty</code>, or <code>null</code>
     *         if there is no selected property.
     */
    public OWLAnnotationProperty getLastSelectedAnnotationProperty();


    /**
     * Gets the most recently selected individual.
     * @return The selected individual, or <code>null</code> if
     *         there is no selected individual.
     */
    public OWLNamedIndividual getLastSelectedIndividual();


    /**
     * Gets the most recently selected datatype.
     * @return The selected datatype, or <code>null</code> if
     *         there is no selected datatype.
     */
    public OWLDatatype getLastSelectedDatatype();


    /**
     * Gets the last selected entity.
     * @return The <code>OWLEntity</code> that was last selected.
     */
    public OWLEntity getSelectedEntity();

    /**
     * If any of the last selected entities are equal to
     * the specified entity then the selection is cleared.
     */
    public void clearLastSelectedEntity(OWLEntity entity);


    /**
     * A convenience method that will delegate to the appropriate
     * selection method depending on the type of entity.
     * @param entity The entity to be selected.  Must not be <code>null</code>.
     */
    public void setSelectedEntity(OWLEntity entity);


    /**
     * Instances of an axiom wrt the containing ontology
     */
    public void setSelectedAxiom(OWLAxiomInstance axiomInstance);

    /**
     * Instances of an axiom wrt the containing ontology
     * @return and OWLAxiomInstance (an axiom, ontology pair)
     */
    public OWLAxiomInstance getLastSelectedAxiomInstance();


    public void setSelectedObject(OWLObject object);


    public OWLObject getSelectedObject();


    /**
     * Adds an <code>OWLSelectionModelListener</code> to the list of listeners.
     * @param listener The listener to to be added.  This listener will be notified
     *                 of any changes to the set of selected objects.
     */
    public void addListener(OWLSelectionModelListener listener);


    /**
     * Removes a previously added <code>OWLSelectionModelListener</code>.  If the
     * listener was not added then this method will have no effect.
     * @param listener The listener to remove.
     */
    public void removeListener(OWLSelectionModelListener listener);
}
