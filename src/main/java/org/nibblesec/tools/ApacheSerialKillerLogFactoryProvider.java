package org.nibblesec.tools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ApacheSerialKillerLogFactoryProvider implements LoggingProvider {
	
	private static final Log logger = LogFactory.getLog(SerialKiller.class.getName());

	@Override
	public void logInfo(String logString) {
		logger.info(logString);
	}

	@Override
	public void logError(String logString) {
		logger.error(logString);
	}
}
