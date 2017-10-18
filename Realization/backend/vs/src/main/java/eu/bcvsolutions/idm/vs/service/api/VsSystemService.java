package eu.bcvsolutions.idm.vs.service.api;

import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.vs.dto.VsSystemDto;

/**
 * Service for virtual system
 * 
 * @author Svanda
 *
 */
public interface VsSystemService {
	
	static final String IMPLEMENTERS_PROPERTY = "implementers";
	static final String IMPLEMENTER_ROLES_PROPERTY = "implementerRoles";
	static final String ATTRIBUTES_PROPERTY = "attributes";
	// Multivalued default attribute
	static final String RIGHTS_ATTRIBUTE = "rights";

	/**
	 * Create virtual system. System will be included mapping by default fields
	 * @param vsSystem
	 * @return
	 */
	SysSystemDto create(VsSystemDto vsSystem);
		
}