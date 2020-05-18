package eu.bcvsolutions.idm.core.api.service;

import eu.bcvsolutions.idm.core.api.dto.IdmDelegationDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmDelegationFilter;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizableService;

/**
 * CRUD service for a delegations.
 *
 * @author Vít Švanda
 *
 */
public interface IdmDelegationService
		extends ReadWriteDtoService<IdmDelegationDto, IdmDelegationFilter>, AuthorizableService<IdmDelegationDto> {

}
