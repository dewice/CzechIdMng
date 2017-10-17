package eu.bcvsolutions.idm.core.model.service.api;

import eu.bcvsolutions.idm.core.api.dto.IdmScriptDto;
import eu.bcvsolutions.idm.core.api.dto.filter.ScriptFilter;
import eu.bcvsolutions.idm.core.api.service.CodeableService;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.Recoverable;

/**
 * Default service for script
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */

public interface IdmScriptService extends 
		ReadWriteDtoService<IdmScriptDto, ScriptFilter>, Recoverable<IdmScriptDto>,
		CodeableService<IdmScriptDto> {

	/**
	 * Return {@link IdmScriptDto} by name. This method return script by name,
	 * for one name may exist one or more script, method return only first!
	 * For code use method getScriptByCode.
	 * 
	 * @param name
	 * @return
	 */
	IdmScriptDto getScriptByName(String name);
	
	/**
	 * Method return script founded by code.
	 * 
	 * @param code
	 * @return
	 * @deprecated use {@link #getByCode(String)}
	 */
	@Deprecated
	IdmScriptDto getScriptByCode(String code);
}