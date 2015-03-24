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
import java.util.Collection;
import java.util.List;

import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import de.learnlib.libalf.LibAlf.AlgorithmID;
import de.learnlib.oracles.DefaultQuery;
import de.learnlib.passive.api.PassiveLearningAlgorithm;

abstract class LibalfPassiveLearner<M,I,D> extends LibalfLearner<M, I, D>
		implements PassiveLearningAlgorithm<M, I, D> {
	
	protected static native boolean addSamples(byte[] ptr, int numSamples, int[] samplesEnc, int[] outputsEnc);
	
	protected M model = null;

	protected LibalfPassiveLearner(AlgorithmID algId, Alphabet<I> alphabet,
			int[] otherOpts) {
		super(algId, alphabet, otherOpts);
	}
	
	private int encodeWord(Word<I> word, int[] store, int ofs) {
		int len = word.length();
		store[ofs++] = len;
		for (I sym : word) {
			int symEnc = inputAlphabet.getSymbolIndex(sym);
			store[ofs++] = symEnc;
		}
		return ofs;
	}

	@Override
	public void addSamples(Collection<? extends DefaultQuery<I, D>> samples) {
		if (samples.isEmpty()) {
			return;
		}
		int numSamples = samples.size();
		
		int sampleLength = 0;
		List<Word<I>> inputs = new ArrayList<>(numSamples);
		int[] outputsEnc = new int[numSamples];
		int i = 0;
		for (DefaultQuery<I, D> sample : samples) {
			Word<I> input = sample.getInput();
			inputs.add(input);
			sampleLength += 1 + input.length();
			D out = sample.getOutput();
			outputsEnc[i++] = encodeOutput(out);
		}
		int[] samplesEnc = new int[sampleLength];
		int curOfs = 0;
		for (Word<I> input : inputs) {
			curOfs = encodeWord(input, samplesEnc, curOfs);
		}
		addSamples(ptr, numSamples, samplesEnc, outputsEnc);
		model = null;
	}

	@Override
	public M computeModel() {
		if (model == null) {
			model = advance();
		}
		return model;
	}

}
