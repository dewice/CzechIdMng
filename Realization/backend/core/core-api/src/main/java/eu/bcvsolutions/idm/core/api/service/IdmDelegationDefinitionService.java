package eu.bcvsolutions.idm.core.api.service;

import eu.bcvsolutions.idm.core.api.dto.IdmDelegationDefinitionDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmDelegationDefinitionFilter;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizableService;

/**
 * CRUD service for a definition of delegation.
 *
 * @author Vít Švanda
 *
 */
public interface IdmDelegationDefinitionService
		extends ReadWriteDtoService<IdmDelegationDefinitionDto, IdmDelegationDefinitionFilter>, AuthorizableService<IdmDelegationDefinitionDto> {

}
