package org.nibblesec.tools;

public interface LoggingProvider {

	public void logInfo(String logString);
	public void logError(String logString);
}
