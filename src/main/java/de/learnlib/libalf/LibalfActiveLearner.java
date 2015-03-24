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

import java.util.ArrayList;
import java.util.List;

import de.learnlib.api.LearningAlgorithm;
import de.learnlib.api.MembershipOracle;
import de.learnlib.oracles.DefaultQuery;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

abstract class LibalfActiveLearner<M, I, D> extends LibalfLearner<M, I, D>
		implements LearningAlgorithm<M, I, D> {
	

	protected static native byte[] fetchQueryBatch(byte[] ptr);
	protected static native int[] getQueries(byte[] batchPtr);
	protected static native void processAnswers(byte[] ptr, byte[] batchPtr, int[] answers);
	protected static native void addCounterExample(byte[] ptr, int[] ceWord);
	

	protected final MembershipOracle<I, D> oracle;
	protected M hypothesis = null;
	
	protected LibalfActiveLearner(LibAlf.AlgorithmID algId,
			Alphabet<I> alphabet, MembershipOracle<I, D> oracle, int ...otherOpts) {
		super(algId, alphabet, otherOpts);
		this.oracle = oracle;
	}

	/**
	 * Learns until the next conjecture is produced.
	 */
	protected void learn() {
		checkState();
		
		M conjecture;
		while ((conjecture = advance()) == null) {
			byte[] batchPtr = fetchQueryBatch(ptr);
			int[] encQueries = getQueries(batchPtr);
			List<DefaultQuery<I,D>> queries = decodeQueries(encQueries);
			oracle.processQueries(queries);
			int[] answers = encodeAnswers(queries);
			processAnswers(ptr, batchPtr, answers);
		}
		this.hypothesis = conjecture;
	}
	
	/**
	 * Encodes a word into an {@code int} array.
	 * @param word the word to encode
	 * @return the word encoded as an {@code int} array.
	 */
	protected int[] encodeWord(Word<I> word) {
		int[] wordEnc = new int[word.length()];
		int i = 0;
		for (I sym : word) {
			int symEnc = inputAlphabet.getSymbolIndex(sym);
			wordEnc[i++] = symEnc;
		}
		return wordEnc;
	}
	
	/**
	 * Decodes a list of encoded queries.
	 * @param encQueries the queries (inputs only), encoded as an {@code int} array
	 * @return
	 */
	protected List<DefaultQuery<I,D>> decodeQueries(int[] encQueries) {
		int p = 0;
		int numQueries = encQueries[p++];
		List<DefaultQuery<I,D>> queries = new ArrayList<>(numQueries);
		
		for (int i = 0; i < numQueries; i++) {
			int queryLen = encQueries[p++];
			WordBuilder<I> wb = new WordBuilder<>(queryLen);
			for (int j = 0; j < queryLen; j++) {
				int symEnc = encQueries[p++];
				I sym = inputAlphabet.getSymbol(symEnc);
				wb.add(sym);
			}
			Word<I> queryWord = wb.toWord();
			queries.add(new DefaultQuery<I,D>(queryWord));
		}
		
		assert p == encQueries.length;
		return queries;
	}
	
	protected int[] encodeAnswers(List<DefaultQuery<I,D>> answeredQueries) {
		int[] encAnswers = new int[answeredQueries.size()];
		int i = 0;
		for (DefaultQuery<I,D> qry : answeredQueries) {
			D out = qry.getOutput();
			int outEnc = encodeOutput(out);
			encAnswers[i++] = outEnc;
		}
		return encAnswers;
	}
	
	
	
	@Override
	public void startLearning() {
		if (hypothesis != null) {
			throw new IllegalStateException("startLearning has already been called");
		}
		learn();
	}
	
	@Override
	public boolean refineHypothesis(DefaultQuery<I, D> ceQuery) {
		if (hypothesis == null) {
			throw new IllegalStateException("Learning has to be started before refineHypothesis may be invoked");
		}
		checkState();
		
		Word<I> ceWord = ceQuery.getInput();
		int[] ceWordEnc = encodeWord(ceWord);
		addCounterExample(ptr, ceWordEnc);
		learn();
		return true;
	}
	
	@Override
	public M getHypothesisModel() {
		return hypothesis;
	}
}
