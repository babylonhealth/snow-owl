package org.protege.editor.core.ui.action;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.text.DateFormat;

/**
 * Author: drummond<br>
 * http://www.cs.man.ac.uk/~drummond/<br><br>
 * <p/>
 * The University Of Manchester<br>
 * Bio Health Informatics Group<br>
 * Date: Jun 25, 2008<br><br>
 */
public class TimestampOutputAction extends ProtegeAction {

    /**
     * 
     */
    private static final long serialVersionUID = -6250513189027502206L;
    private Logger logger = Logger.getLogger(TimestampOutputAction.class);

    public void actionPerformed(ActionEvent event) {
        String message = JOptionPane.showInputDialog(getWorkspace(),
                                                     "<html><body>Please enter a message to label your timestamp <br>(or leave blank for no message)</body><html>",
                                                     "Timestamp", JOptionPane.PLAIN_MESSAGE);

        long now = System.currentTimeMillis();
        String timestamp = DateFormat.getDateTimeInstance().format(now);

        if (message != null){

            logger.info("\n\n\n\n");
            logger.info("------------------------------------------");
            logger.info(timestamp + ": " + message);
            logger.info("------------------------------------------");
            logger.info("\n\n");
        }
    }


    public void initialise() throws Exception {
        // do nothing
    }


    public void dispose() throws Exception {
        // do nothing
    }
}
