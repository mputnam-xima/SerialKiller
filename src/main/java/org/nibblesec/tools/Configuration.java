package org.nibblesec.tools;

import java.util.regex.Pattern;

public interface Configuration {

	public void reloadIfNeeded();
	public Iterable<Pattern> blacklist();
	public Iterable<Pattern> whitelist();
	public boolean isProfiling();
}
