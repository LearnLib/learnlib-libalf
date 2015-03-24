/* Copyright (C) 2015 TU Dortmund
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * LearnLib is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 3.0 as published by the Free Software Foundation.
 *
 * LearnLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with LearnLib; if not, see
 * <http://www.gnu.de/documents/lgpl.en.html>.
 */
package de.learnlib.libalf;

import java.util.logging.Logger;

import net.automatalib.commons.util.lib.LibLoader;
import net.automatalib.commons.util.lib.LoadLibraryException;
import net.automatalib.commons.util.lib.LoadPolicy;

/**
 * Main class for interacting with the native libalf. This class is responsible for
 * library initialization, as well as instantiation of learning algorithms.
 * 
 * @author Malte Isberner
 *
 */
class LibAlf {
	
	/**
	 * The (online) learning algorithms available in libalf.
	 */
	enum AlgorithmID {
		// active learning algorithms ("online")
		ANGLUIN_SIMPLE_DFA,
		ANGLUIN_COL_DFA,
		RS_DFA,
		KV_DFA,
		NLSTAR,
		MVCA_ANGLUINLIKE,
		
		// passive learning algorithms ("offline")
		RPNI,
		DELETE2,
		BIERMANN_ORIGINAL_DFA,
		BIERMANN_MINISAT,
	}
	
	private static final Logger LOG = Logger.getLogger(LibAlf.class.getName());
	
	// Name of the native library
	private static final String LIB_NAME = "learnlib-libalf";
	private static final LibAlf INSTANCE;
	
	// Exceptions in the static initializer will be caught, to prevent an
	// ExceptionInInitializerError. However, when the user attempts to use
	// libalf after initialization failed, the original failure cause will
	// be reported as the cause of the LibalfNotAvailableException.
	private static final Exception failureCause;
	
	static {
		LibAlf libAlf = null;
		
		Exception cause = null;
		try {
			libAlf = new LibAlf();
		}
		catch (Exception ex) {
			LOG.severe("Could not load native " + LIB_NAME + " library: " + ex.getMessage());
			LOG.severe("Using LibAlf will NOT work");
			cause = ex;
		}
		
		INSTANCE = libAlf;
		failureCause = cause;
	}
	
	/**
	 * Retrieves the instance of the LibAlf object. If libalf could not be loaded,
	 * a {@link LibalfNotAvailableException} will be thrown.
	 * 
	 * @return the instance of the LibAlf object
	 * @throws LibalfNotAvailableException if libalf could not be loaded.
	 */
	public static LibAlf getInstance() throws LibalfNotAvailableException {
		if (INSTANCE == null) {
			throw new LibalfNotAvailableException(failureCause);
		}
		return INSTANCE;
	}
	
	/**
	 * Initializes libalf.
	 * 
	 * @param algIds the IDs of the algorithms that should be made available
	 * @return pointer to the native object
	 */
	protected static native byte[] init(AlgorithmID[] algIds);
	
	/**
	 * Disposes of the native libalf interface object.
	 * @param ptr pointer to the native object
	 */
	protected static native void dispose(byte[] ptr);
	
	/**
	 * Initializes a libalf learning algorithm.
	 * 
	 * @param ptr pointer to the native libalf interface object
	 * @param algorithmId the ID of the algorithm to instantiate (must match the {@link AlgorithmID#ordinal()} value)
	 * @param alphabetSize the size of the alphabet
	 * @param otherArgs other arguments to the learning algorithm
	 * @return pointer to the native algorithm object
	 */
	protected static native byte[] initAlgorithm(byte[] ptr, int algorithmId, int alphabetSize, int[] otherArgs);
	
	// pointer to the native libalf interface object
	private byte[] ptr;
	
	/**
	 * (Re-)Initializes libalf.
	 */
	private void init() {
		this.ptr = init(AlgorithmID.values());
	}
	
	/**
	 * Constructor. Loads and initializes the native library.
	 * @throws LoadLibraryException if loading the native library fails.
	 */
	private LibAlf() throws LoadLibraryException {
		LibLoader.getInstance().loadLibrary(getClass(), LIB_NAME, LoadPolicy.PREFER_SHIPPED);
		init();
	}
	
	/**
	 * Initializes a libalf algorithm.
	 * 
	 * @param algId the algorithm to initialize
	 * @param alphabetSize the alphabet size
	 * @param otherArgs other arguments to the learning algorithm
	 * @return pointer to the native algorithm object, or {@code null} if the algorithm could not
	 * be initialized.
	 */
	public synchronized byte[] initAlgorithm(AlgorithmID algId, int alphabetSize, int[] otherArgs) {
		if (ptr == null) {
			// re-init, for whatever reason
			init();
		}
		return initAlgorithm(ptr, algId.ordinal(), alphabetSize, otherArgs);
	}
	
	/**
	 * Unloads libalf.
	 */
	public synchronized void unload() {
		dispose(ptr);
		this.ptr = null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		try {
			unload();
		}
		finally {
			super.finalize();
		}
	}
}
