/*
 * ====================================================================
 * Copyright (c) 2004-2008 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package net.sourceforge.kolmafia.svn;

import net.sourceforge.kolmafia.RequestLogger;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;

/*
 * This class is an implementation of ISVNEventHandler intended for  processing   
 * events generated by do*() methods of an SVNWCClient object. An  instance  of 
 * this handler will be provided to an SVNWCClient. When  calling, for example, 
 * SVNWCClient.doDelete(..) on some path, that method will  generate  an  event 
 * for each 'delete' action it will perform upon every path being deleted.  And
 * this event is passed to 
 * 
 * ISVNEventHandler.handleEvent(SVNEvent event,  double progress) 
 * 
 * to notify the handler.  The  event  contains detailed  information about the 
 * path, action performed upon the path and some other. 
 */
public class WCEventHandler implements ISVNEventHandler {
    /*
     * progress  is  currently  reserved  for future purposes and now is always
     * ISVNEventHandler.UNKNOWN  
     */
    public void handleEvent(SVNEvent event, double progress) {
        /*
         * Gets the current action. An action is represented by SVNEventAction.
         */
        SVNEventAction action = event.getAction();
        if (action == SVNEventAction.ADD){
            /*
             * The item is scheduled for addition.
             */
            RequestLogger.printLine("A     " + event.getURL());
            return;
        }else if (action == SVNEventAction.COPY){
            /*
             * The  item  is  scheduled for addition  with history (copied,  in 
             * other words).
             */
            RequestLogger.printLine("A  +  " + event.getURL());
            return;
        }else if (action == SVNEventAction.DELETE){
            /*
             * The item is scheduled for deletion. 
             */
			SVNManager.queueFileEvent( new SVNFileEvent( event.getFile(), event ) );
            RequestLogger.printLine("D     " + event.getURL());
            return;
        } else if (action == SVNEventAction.LOCKED){
            /*
             * The item is locked.
             */
            RequestLogger.printLine("L     " + event.getURL());
            return;
        } else if (action == SVNEventAction.LOCK_FAILED){
            /*
             * Locking operation failed.
             */
            RequestLogger.printLine("failed to lock    " + event.getURL());
            return;
        }
    }

    /*
     * Should be implemented to check if the current operation is cancelled. If 
     * it is, this method should throw an SVNCancelException. 
     */
    public void checkCancelled() throws SVNCancelException {
    }

}
