/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.commons.vfs.impl;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelector;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.provider.FileReplicator;
import org.apache.commons.vfs.provider.FileSystemProviderContext;

/**
 * A file replicator that wraps another file replicator, performing
 * the replication as a privileged action.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision: 1.2 $ $Date: 2002/08/21 07:00:11 $
 */
public class PrivilegedFileReplicator
    implements FileReplicator
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( PrivilegedFileReplicator.class );

    private final FileReplicator replicator;

    public PrivilegedFileReplicator( FileReplicator replicator )
    {
        this.replicator = replicator;
    }

    /**
     * Sets the context for the replicator.
     */
    public void setContext( final FileSystemProviderContext context )
    {
        AccessController.doPrivileged( new SetContextAction( context ) );
    }

    /**
     * Creates a local copy of the file, and all its descendents.
     */
    public File replicateFile( FileObject srcFile, FileSelector selector )
        throws FileSystemException
    {
        try
        {
            final ReplicateAction action = new ReplicateAction( srcFile, selector );
            return (File)AccessController.doPrivileged( action );
        }
        catch ( final PrivilegedActionException e )
        {
            final String message = REZ.getString( "replicate-file.error", srcFile.getName() );
            throw new FileSystemException( message, e );
        }
    }

    /**
     * Closes the replicator.
     */
    public void close()
    {
        AccessController.doPrivileged( new CloseAction() );
    }

    /** An action that sets the context of the wrapped replicator. */
    private class SetContextAction implements PrivilegedAction
    {
        private final FileSystemProviderContext context;

        public SetContextAction( final FileSystemProviderContext context )
        {
            this.context = context;
        }

        /**
         * Performs the action.
         */
        public Object run()
        {
            replicator.setContext( context );
            return null;
        }
    }

    /** An action that replicates a file using the wrapped replicator. */
    private class ReplicateAction implements PrivilegedExceptionAction
    {
        private final FileObject srcFile;
        private final FileSelector selector;

        public ReplicateAction( final FileObject srcFile, final FileSelector selector )
        {
            this.srcFile = srcFile;
            this.selector = selector;
        }

        /** Performs the action. */
        public Object run() throws Exception
        {
            // TODO - Do not pass the selector through.  It is untrusted
            // TODO - Need to determine which files can be read
            return replicator.replicateFile( srcFile, selector );
        }
    }

    /** An action that closes the wrapped replicator. */
    private class CloseAction implements PrivilegedAction
    {
        /** Performs the action. */
        public Object run()
        {
            replicator.close();
            return null;
        }
    }
}
