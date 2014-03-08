package com.gmail.michaelchentejada.fanfictionreader.util;

/**
 * Represents the current state of the application
 * @author Michael Chen
 *
 */
public enum currentState{
	/**
	 * Normal mode: used for browse story->non-xover
	 */
	NORMAL,
	/**
	 * Author mode: used in search by author
	 */
	AUTHOR,
	/**
	 * Crossover mode: used to indicate crossovers only
	 */
	CROSSOVER,
	/**
	 * Communities: used to show communities
	 */
	COMMUNITIES,
	/**
	 * Just in: the just in page
	 */
	JUSTIN
}
