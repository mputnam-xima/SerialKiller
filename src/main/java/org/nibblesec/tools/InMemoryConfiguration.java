package org.nibblesec.tools;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class InMemoryConfiguration implements Configuration {
	
	private final boolean isProfiling;
	private final List<Pattern> blacklist;
	private final List<Pattern> whitelist;

	public InMemoryConfiguration(boolean isProfiling, List<String> blacklist, List<String> whitelist) {
		this.isProfiling = isProfiling;
		this.blacklist = toPattern(blacklist);
		this.whitelist = toPattern(whitelist);
	}
	
	private List<Pattern> toPattern(List<String> stringPatternList) {
		List<Pattern> patternList = new LinkedList<>();
		if (stringPatternList != null) {
			for (String patternString : stringPatternList) {
				if (patternString != null && !patternString.isEmpty()) {
					patternList.add(Pattern.compile(patternString));
				}
			}
		}
		return patternList;
	}

	@Override
	public void reloadIfNeeded() {
		//Do nothing
	}

	@Override
	public Iterable<Pattern> blacklist() {
		return blacklist;
	}

	@Override
	public Iterable<Pattern> whitelist() {
		return whitelist;
	}

	@Override
	public boolean isProfiling() {
		return isProfiling;
	}
}
