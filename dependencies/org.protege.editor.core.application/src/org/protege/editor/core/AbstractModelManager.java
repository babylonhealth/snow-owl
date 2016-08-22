package org.protege.editor.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Medical Informatics Group<br>
 * Date: Mar 17, 2006<br><br>
 * <p/>
 * matthew.horridge@cs.man.ac.uk<br>
 * www.cs.man.ac.uk/~horridgm<br><br>
 * <p/>
 * An abstract class that implements enough functionality from
 * <code>ModelManager</code> to support adding, removing of
 * <code>ModelManagerListener</code>s and firing of
 * <code>ModelManager</code> events.
 */
public abstract class AbstractModelManager implements ModelManager {

    private static final Logger logger = Logger.getLogger(AbstractModelManager.class);

    private Map<Object, Disposable> objects = new HashMap<Object, Disposable>();


    protected AbstractModelManager() {
    }


    public <T extends Disposable> void put(Object key, T object) {
        objects.put(key, object);
    }


    @SuppressWarnings("unchecked")
    public <T extends Disposable> T get(Object key) {
        return (T) objects.get(key);
    }


    public void dispose() {
        for (Disposable object : objects.values()){
            try {
                object.dispose();
            }
            catch (Exception e) {
                logger.error(e);
            }
        }
        objects.clear();
    }
}
