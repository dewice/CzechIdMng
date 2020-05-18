package eu.bcvsolutions.idm.core.model.event.processor.delegation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.core.api.dto.IdmDelegationDefinitionDto;
import eu.bcvsolutions.idm.core.api.event.CoreEventProcessor;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;
import eu.bcvsolutions.idm.core.api.event.processor.DelegationDefinitionProcessor;
import eu.bcvsolutions.idm.core.api.service.IdmDelegationDefinitionService;
import eu.bcvsolutions.idm.core.model.event.ExportImportEvent.ExportImportEventType;

/**
 * Processor for delete definition of a delegation.
 *
 * @author Vít Švanda
 *
 */
@Component(DelegationDefinitionDeleteProcessor.PROCESSOR_NAME)
@Description("Processor for delete definition of a delegation.")
public class DelegationDefinitionDeleteProcessor extends CoreEventProcessor<IdmDelegationDefinitionDto> implements DelegationDefinitionProcessor {

	public static final String PROCESSOR_NAME = "delegation-def-delete-processor";

	private final IdmDelegationDefinitionService service;

	@Autowired
	public DelegationDefinitionDeleteProcessor(IdmDelegationDefinitionService service) {
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
	public EventResult<IdmDelegationDefinitionDto> process(EntityEvent<IdmDelegationDefinitionDto> event) {
		IdmDelegationDefinitionDto dto = event.getContent();
		// Internal delete
		service.deleteInternal(dto);

		return new DefaultEventResult<>(event, this);
	}
}
