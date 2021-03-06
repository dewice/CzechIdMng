package eu.bcvsolutions.idm.core.api.audit.dto.filter;

import java.time.ZonedDateTime;

import eu.bcvsolutions.idm.core.api.domain.LogType;
import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;

/**
 * Filter for logging event
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */

public class IdmLoggingEventFilter implements BaseFilter {

	private Long id;
	private String text;
	private ZonedDateTime from;
	private ZonedDateTime till;
	private String loggerName;
	private LogType levelString;
	private String callerFilename;
	private String callerClass;
	private String callerMethod;
	private String callerLine;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ZonedDateTime getFrom() {
		return from;
	}

	public void setFrom(ZonedDateTime from) {
		this.from = from;
	}

	public ZonedDateTime getTill() {
		return till;
	}

	public void setTill(ZonedDateTime till) {
		this.till = till;
	}

	public String getLoggerName() {
		return loggerName;
	}

	public void setLoggerName(String loggerName) {
		this.loggerName = loggerName;
	}

	public LogType getLevelString() {
		return levelString;
	}

	public void setLevelString(LogType levelString) {
		this.levelString = levelString;
	}

	public String getCallerFilename() {
		return callerFilename;
	}

	public void setCallerFilename(String callerFilename) {
		this.callerFilename = callerFilename;
	}

	public String getCallerClass() {
		return callerClass;
	}

	public void setCallerClass(String callerClass) {
		this.callerClass = callerClass;
	}

	public String getCallerMethod() {
		return callerMethod;
	}

	public void setCallerMethod(String callerMethod) {
		this.callerMethod = callerMethod;
	}

	public String getCallerLine() {
		return callerLine;
	}

	public void setCallerLine(String callerLine) {
		this.callerLine = callerLine;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
