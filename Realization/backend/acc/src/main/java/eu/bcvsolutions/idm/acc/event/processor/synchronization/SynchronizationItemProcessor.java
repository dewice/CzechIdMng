package eu.bcvsolutions.idm.acc.event.processor.synchronization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.acc.domain.SynchronizationContext;
import eu.bcvsolutions.idm.acc.dto.SysSyncItemLogDto;
import eu.bcvsolutions.idm.acc.event.SynchronizationEventType;
import eu.bcvsolutions.idm.acc.service.api.SynchronizationService;
import eu.bcvsolutions.idm.core.api.event.AbstractEntityEventProcessor;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.DefaultEventResult;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventResult;

/**
 * Synchronization for one item event processor. By default call method SynchronizationService.doItemSynchronization. 
 * @author svandav
 *
 */
@Component
@Description("Synchronization or reconciliation for one item. By default call method SynchronizationService.doItemSynchronization.")
public class SynchronizationItemProcessor extends AbstractEntityEventProcessor<SysSyncItemLogDto> {

	public static final String PROCESSOR_NAME = "synchronization-item-processor";
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SynchronizationItemProcessor.class);
	private final SynchronizationService synchronizationService;
	
	@Autowired
	public SynchronizationItemProcessor(
			SynchronizationService synchronizationService) {
		super(SynchronizationEventType.START_ITEM);
		//
		Assert.notNull(synchronizationService, "Service is required.");
		//
		this.synchronizationService = synchronizationService;
	}
	
	@Override
	public String getName() {
		return PROCESSOR_NAME;
	}
	
	@Override
	public EventResult<SysSyncItemLogDto> process(EntityEvent<SysSyncItemLogDto> event) {
		LOG.info("Synchronization item event");
		SynchronizationContext itemWrapper = (SynchronizationContext) event.getProperties().get(SynchronizationService.WRAPPER_SYNC_ITEM);
		// Do synchronization for one item (produces event)
		// Start in new Transaction
		boolean result = synchronizationService.doItemSynchronization(itemWrapper);
		event.getProperties().put(SynchronizationService.RESULT_SYNC_ITEM, result);
		return new DefaultEventResult<>(event, this);
	}

	@Override
	public int getOrder() {
		return CoreEvent.DEFAULT_ORDER;
	}
}