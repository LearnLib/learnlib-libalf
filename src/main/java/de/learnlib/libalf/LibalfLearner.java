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

import java.io.IOException;

import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.NFA;
import net.automatalib.serialization.saf.SAFSerialization;
import net.automatalib.words.Alphabet;

abstract class LibalfLearner<M,I,D> implements AutoCloseable {
	
	protected static int encodeOutputAcceptor(Boolean out) {
		return out.booleanValue() ? 1 : 0;
	}
	
	protected static <I> DFA<?,I> decodeConjectureDFA(
			LibalfLearner<DFA<?,I>, I, Boolean> learner,
			byte[] conjecture) {
		try {
			return SAFSerialization.getInstance().readCustomDFA(conjecture, learner.inputAlphabet);
		}
		catch (IOException ex) {
			throw new LibalfException(ex);
		}
	}
	
	protected static <I> NFA<?,I> decodeConjectureNFA(
			LibalfLearner<NFA<?,I>, I, Boolean> learner,
			byte[] conjecture) {
		try {
			return SAFSerialization.getInstance().readCustomNFA(conjecture, learner.inputAlphabet);
		}
		catch (IOException ex) {
			throw new LibalfException(ex);
		}
	}

	protected final Alphabet<I> inputAlphabet;
	
	// pointer to native object
	protected byte[] ptr;
	
	protected static native byte[] advance(byte[] ptr);
	protected static native void dispose(byte[] ptr);
	
	protected LibalfLearner(LibAlf.AlgorithmID algId, Alphabet<I> alphabet, int... otherOpts) {
		this.inputAlphabet = alphabet;
		this.ptr = LibAlf.getInstance().initAlgorithm(algId, alphabet.size(), otherOpts);
		if (ptr == null) {
			throw new LibalfException("Could not initialize algorithm " + algId);
		}
	}
	
	public Alphabet<I> getInputAlphabet() {
		return inputAlphabet;
	}
	
	protected void checkState() {
		if (ptr == null) {
			throw new LibalfObjectDisposedException();
		}
	}

	protected M advance() {
		checkState();
		
		byte[] cj = advance(ptr);
		if (cj == null) {
			return null;
		}
		M hyp = decodeConjecture(cj);
		return hyp;
	}
	
	protected abstract M decodeConjecture(byte[] conjecture);
	protected abstract int encodeOutput(D output);
	
	public void dispose() {
		if (ptr != null) {
			dispose(ptr);
			this.ptr = null;
		}
	}
	
	@Override
	public void close() {
		dispose();
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			dispose();
		}
		finally {
			super.finalize();
		}
	}
}
