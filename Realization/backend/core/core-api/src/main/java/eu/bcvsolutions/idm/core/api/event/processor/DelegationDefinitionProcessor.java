package eu.bcvsolutions.idm.core.api.event.processor;

import eu.bcvsolutions.idm.core.api.dto.IdmDelegationDefinitionDto;
import eu.bcvsolutions.idm.core.api.event.EntityEventProcessor;

/**
 * Delegation definition processors should implement this interface.
 * 
 * @author Vít Švanda
 *
 */
public interface DelegationDefinitionProcessor extends EntityEventProcessor<IdmDelegationDefinitionDto> {

}
