/*
 * ====================================================================
 * Copyright (c) 2004-2010 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc17.db.statement;

import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb;
import org.tmatesoft.svn.core.internal.db.SVNSqlJetUpdateStatement;

/**
 * UPDATE actual_node SET conflict_old = ?3, conflict_new = ?4, conflict_working
 * = ?5 WHERE wc_id = ?1 AND local_relpath = ?2;
 *
 * @version 1.4
 * @author TMate Software Ltd.
 */
public class SVNWCDbUpdateActualTextConflicts extends SVNSqlJetUpdateStatement {

    public SVNWCDbUpdateActualTextConflicts(SVNSqlJetDb sDb) throws SVNException {
        super(sDb, SVNWCDbSchema.ACTUAL_NODE);
    }

    protected Object[] getWhere() throws SVNException {
        return new Object[] {
                getBind(1), getBind(2)
        };
    }

    public Map<String, Object> getUpdateValues() throws SVNException {
        Map<String, Object> rowValues = getRowValues();
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_old.toString(), getBind(3));
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_new.toString(), getBind(4));
        rowValues.put(SVNWCDbSchema.ACTUAL_NODE__Fields.conflict_working.toString(), getBind(5));
        return rowValues;
    }

}
