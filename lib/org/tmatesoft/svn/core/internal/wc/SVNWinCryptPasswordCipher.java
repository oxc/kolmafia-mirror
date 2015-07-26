/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc;

import org.tmatesoft.svn.core.internal.util.jna.SVNJNAUtil;



/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNWinCryptPasswordCipher extends SVNPasswordCipher {

    public static boolean isEnabled() {
        return SVNJNAUtil.isWinCryptEnabled();
    }
    
    public char[] decrypt(char[] encryptedData) {
        return SVNJNAUtil.decrypt(encryptedData);
    }

    public char[] encrypt(char[] rawData) {
        return SVNJNAUtil.encrypt(rawData);
    }

    public String getCipherType() {
        return SVNPasswordCipher.WINCRYPT_CIPHER_TYPE;
    }

}
