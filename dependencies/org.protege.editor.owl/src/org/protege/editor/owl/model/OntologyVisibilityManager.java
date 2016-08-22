package org.protege.editor.owl.model;

import org.semanticweb.owlapi.model.OWLOntology;

import java.util.Set;


/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Medical Informatics Group<br>
 * Date: Apr 19, 2006<br><br>
 * <p/>
 * matthew.horridge@cs.man.ac.uk<br>
 * www.cs.man.ac.uk/~horridgm<br><br>
 */
public interface OntologyVisibilityManager {

    public boolean isVisible(OWLOntology ontology);


    public Set<OWLOntology> getVisibleOntologies();


    public void setVisible(OWLOntology ontology, boolean b);


    public void setVisible(Set<OWLOntology> ontologies);


    public void addListener(OntologyVisibilityManagerListener listener);


    public void removeListener(OntologyVisibilityManagerListener listener);
}
