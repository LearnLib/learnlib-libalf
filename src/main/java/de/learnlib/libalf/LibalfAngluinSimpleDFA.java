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

import net.automatalib.words.Alphabet;
import de.learnlib.api.MembershipOracle;

/**
 * The "Angluin Simple" algorithm provided by libalf, DFA version.
 * 
 * @author Malte Isberner
 *
 * @param <I> input symbol type
 */
public class LibalfAngluinSimpleDFA<I> extends LibalfActiveDFALearner<I> {

	/**
	 * Constructor.
	 * @param alphabet the learning alphabet
	 * @param oracle the membership oracle
	 */
	public LibalfAngluinSimpleDFA(Alphabet<I> alphabet, MembershipOracle<I,Boolean> oracle) {
		super(LibAlf.AlgorithmID.ANGLUIN_SIMPLE_DFA, alphabet, oracle);
	}

}
