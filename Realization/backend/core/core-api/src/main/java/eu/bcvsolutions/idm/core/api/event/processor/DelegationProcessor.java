package eu.bcvsolutions.idm.core.api.event.processor;

import eu.bcvsolutions.idm.core.api.dto.IdmDelegationDto;
import eu.bcvsolutions.idm.core.api.event.EntityEventProcessor;

/**
 * Delegation processors should implement this interface.
 * 
 * @author Vít Švanda
 *
 */
public interface DelegationProcessor extends EntityEventProcessor<IdmDelegationDto> {

}
