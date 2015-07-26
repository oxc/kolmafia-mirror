/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */

package org.tmatesoft.svn.core;

import java.util.Collection;

import org.tmatesoft.svn.core.internal.util.SVNHashSet;

/**
 * The <b>SVNRevisionProperty</b> class represents revision properties - those
 * unversioned properties supported by Subversion.
 *
 * <p>
 * Revision properties are unversioned, so there is always a risk to
 * lose information when modifying revision property values.
 *
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNRevisionProperty {

    /**
     * An <span class="javastring">"txn-"</span> prefix for revision properties
     * that when set as commit revision property would be available
     * to the hook scripts, but not actually set on a commit.
     */
    public static final String SVN_TXN_PREFIX = SVNProperty.SVN_PREFIX + "txn-";

    private static final Collection REVISION_PROPS = new SVNHashSet();

    static {
        REVISION_PROPS.add(SVNRevisionProperty.AUTHOR);
        REVISION_PROPS.add(SVNRevisionProperty.LOG);
        REVISION_PROPS.add(SVNRevisionProperty.DATE);
        REVISION_PROPS.add(SVNRevisionProperty.ORIGINAL_DATE);
        REVISION_PROPS.add(SVNRevisionProperty.AUTOVERSIONED);
    }

    /**
     * Says if the given revision property name is really a valid
     * revision property name.
     *
     * @param   name a property name
     * @return  <span class="javakeyword">true</span> if it's a
     *          revision property name, <span class="javakeyword">false</span>
     *          otherwise
     */
    public static boolean isRevisionProperty(String name) {
        return name != null && REVISION_PROPS.contains(name);
    }

    /**
     * An <span class="javastring">"svn:author"</span> revision
     * property (that holds the name of the revision's author).
     */
    public static final String AUTHOR = "svn:author";
    /**
     * An <span class="javastring">"svn:log"</span> revision property -
     * the one that stores a log message attached to a revision
     * during a commit operation.
     */
    public static final String LOG = "svn:log";
    /**
     * An <span class="javastring">"svn:date"</span> revision property
     * that is a date & time stamp representing the time when the
     * revision was created.
     */
    public static final String DATE = "svn:date";

    /**
     * <span class="javastring">"svn:sync-lock"</span> revision property.
     * @since 1.1, new in Subversion 1.4
     */
    public static final String LOCK = "svn:sync-lock";

    /**
     * <span class="javastring">"svn:sync-from-url"</span> revision property.
     * @since 1.1, new in Subversion 1.4
     */
    public static final String FROM_URL = "svn:sync-from-url";

    /**
     * <span class="javastring">"svn:sync-from-uuid"</span> revision property.
     * @since 1.1, new in Subversion 1.4
     */
    public static final String FROM_UUID = "svn:sync-from-uuid";

    /**
     * <span class="javastring">"svn:sync-last-merged-rev"</span> revision property.
     * @since 1.1, new in Subversion 1.4
     */
    public static final String LAST_MERGED_REVISION = "svn:sync-last-merged-rev";

    /**
     * <span class="javastring">"svn:sync-currently-copying"</span> revision property.
     * @since 1.1, new in Subversion 1.4
     */
    public static final String CURRENTLY_COPYING = "svn:sync-currently-copying";

    /**
     * The presence of this fs revision property indicates that the
     * revision was automatically generated by the mod_dav_svn
     * autoversioning feature. The value is irrelevant.
     */
    public static final String AUTOVERSIONED = "svn:autoversioned";


    /**
     * The fs revision property that stores a commit's "original" date.
     *
     * The <span class="javastring">svn:date</span> property must be monotonically increasing, along with
     * the revision number. In certain scenarios, this may pose a problem
     * when the revision represents a commit that occurred at a time which
     * does not fit within the sequencing required for svn:date. This can
     * happen, for instance, when the revision represents a commit to a
     * foreign version control system, or possibly when two Subversion
     * repositories are combined. This property can be used to record the
     * TRUE, original date of the commit.
     */
    public static final String ORIGINAL_DATE = "svn:original-date";

    /**
     * Ephemeral revision property that, when set as commit revision property,
     * will be available to the hook scripts, but not actually set on a revision.
     */
    public static final String SVN_TXN_CLIENT_COMPAT_VERSION = SVN_TXN_PREFIX + "client-compat-version";
    /**
     * Ephemeral revision property that, when set as commit revision property,
     * will be available to the hook scripts, but not actually set on a revision.
     */
    public static final String SVN_TXN_USER_AGENT = SVN_TXN_PREFIX + "user-agent";
}
