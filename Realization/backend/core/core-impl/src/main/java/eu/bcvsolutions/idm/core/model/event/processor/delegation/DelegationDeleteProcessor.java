package eu.bcvsolutions.idm.core.model.event.processor.delegation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.dto.IdmDelegationDto;
import eu.bcvsolutions.idm.core.api.event.CoreEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.event.processor.DelegationProcessor;
import eu.bcvsolutions.idm.core.api.service.IdmDelegationService;
import eu.bcvsolutions.idm.core.model.event.ExportImportEvent.ExportImportEventType;

/**
 * Processor for delete a delegation.
 *
 * @author Vít Švanda
 *
 */
@Component(DelegationDeleteProcessor.PROCESSOR_NAME)
@Description("Processor for delete a delegation.")
public class DelegationDeleteProcessor extends CoreEventProcessor<IdmDelegationDto> implements DelegationProcessor {

	public static final String PROCESSOR_NAME = "delegation-delete-processor";

	private final IdmDelegationService service;

	@Autowired
	public DelegationDeleteProcessor(IdmDelegationService service) {
		super(ExportImportEventType.DELETE);
		//
		Assert.notNull(service, "Service is required.");
		//
		this.service = service;
	}

	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}

	@Override
	public EventResult<IdmDelegationDto> process(EntityEvent<IdmDelegationDto> event) {
		IdmDelegationDto dto = event.getContent();
		// Internal delete
		service.deleteInternal(dto);

		return new DefaultEventResult<>(event, this);
	}
}
