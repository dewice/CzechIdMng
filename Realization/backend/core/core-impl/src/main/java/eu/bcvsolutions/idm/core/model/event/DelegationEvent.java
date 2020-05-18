package eu.bcvsolutions.idm.core.model.event;

import java.io.Serializable;
import java.util.Map;

import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.EventType;

/**
 * Events for save a delegation.
 *
 * @author Vít Švanda
 *
 */
public class DelegationEvent extends CoreEvent<IdmExportImportDto> {

	private static final long serialVersionUID = 1L;

	/**
	 * Supported events
	 *
	 */
	public enum DelegationEventType implements EventType {
		CREATE, UPDATE, DELETE
	}

	public DelegationEvent(DelegationEventType operation, IdmExportImportDto content) {
		super(operation, content);
	}

	public DelegationEvent(DelegationEventType operation, IdmExportImportDto content, Map<String, Serializable> properties) {
		super(operation, content, properties);
	}

}
