package org.protege.editor.owl.model.entity;

import org.semanticweb.owlapi.model.OWLEntity;

import java.util.Stack;

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
 * Date: Jul 25, 2008<br><br>
 */
public class IterativeAutoIDGenerator extends AbstractIDGenerator implements Revertable {

	//TT: Not happy about this solution. The autoId generator is currently static,
	//and we need a way to say if the startId has changed in the preferences,
	//so that the generator will use the new start id.
	private long previousStartId;
    private long id;

    private Stack<Long> checkpoints = new Stack<Long>();


    public IterativeAutoIDGenerator() {
        id = EntityCreationPreferences.getAutoIDStart();
        previousStartId = id;      
    }


    protected long getRawID(Class<? extends OWLEntity> type) throws AutoIDException{
    	//check if start id prefs have changed meanwhile
    	if (previousStartId != EntityCreationPreferences.getAutoIDStart()) {
    		id = EntityCreationPreferences.getAutoIDStart();
    		previousStartId = id;
    		checkpoints.removeAllElements();
    		checkpoints.push(id);
    	}
    	
    	long end = EntityCreationPreferences.getAutoIDEnd();
        if (end != -1 && id > end){
            throw new AutoIDException("You have run out of IDs for creating new entities - max = " + end);
        }
        if (EntityCreationPreferences.getSaveAutoIDStart()) {
        	previousStartId = id + 1;
        	EntityCreationPreferences.setAutoIDStart((int) (previousStartId));
        }
        return id++;
    }


    public void checkpoint() {
        checkpoints.push(id);
    }


    public void revert() {
        id = checkpoints.pop();
    }
}