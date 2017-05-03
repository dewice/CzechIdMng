package eu.bcvsolutions.idm.core.model.service.api;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTime;

import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.PasswordChangeDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdentityFilter;
import eu.bcvsolutions.idm.core.api.service.IdentifiableByNameEntityService;
import eu.bcvsolutions.idm.core.api.service.ReadWriteEntityService;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;
import eu.bcvsolutions.idm.core.model.entity.IdmTreeType;
import eu.bcvsolutions.idm.core.security.api.service.AuthorizableEntityService;

/**
 * Operations with IdmIdentity
 * 
 * @author Radek Tomiška
 *
 */
public interface IdmIdentityService extends 
		ReadWriteEntityService<IdmIdentity, IdentityFilter>, 
		IdentifiableByNameEntityService<IdmIdentity>,
		AuthorizableEntityService<IdmIdentity, IdentityFilter> {
	
	@Deprecated
	static final String CONFIDENTIAL_PROPERTY_PASSWORD = "password";

	
	/**
	 * Returns identity by given username
	 * @param username
	 * @return
	 */
	IdmIdentity getByUsername(String username);

	/**
	 * Returns identity by given username
	 * @param username
	 * @return
	 */
	IdmIdentityDto getDtoByUsername(String username);

	/**
	 * Returns identity by given id
	 * @param id
	 * @return
	 */
	IdmIdentityDto getDto(Serializable id);

	/**
	 * Better "toString"
	 * 
	 * @param identity
	 * @return
	 */
	String getNiceLabel(IdmIdentity identity);
	
	/**
	 * Changes given identity's password
	 * 
	 * @param identity
	 * @param passwordChangeDto
	 */
	void passwordChange(IdmIdentity identity, PasswordChangeDto passwordChangeDto);
	
	
	/**
	 * Find all identities by assigned role
	 * 
	 * @param role
	 * @return List of IdmIdentity with assigned role
	 */
	List<IdmIdentity> findAllByRole(IdmRole role);
	
	/**
	 * Find all identities by assigned role name
	 * 
	 * @param roleName
	 * @return List of IdmIdentity with assigned role
	 */
	List<IdmIdentity> findAllByRoleName(String roleName);
	

	/**
	 * Method finds all identity's managers by identity contract (guarantee or by assigned tree structure).
	 * 
	 * @param forIdentity
	 * @param byTreeType If optional tree type is given, then only managers defined with this type is returned
	 * @return
	 */
	List<IdmIdentity> findAllManagers(IdmIdentity forIdentity, IdmTreeType byTreeType);

	/**
	 * Method finds all identity's managers by identity contract and return managers
	 * 
	 * @param identityId
	 * @return String - usernames separate by commas
	 */
	List<IdmIdentity> findAllManagers(UUID identityId);

	/**
	 * Contains list of identities some identity with given username.
	 * If yes, then return true.
	 * @param identities
	 * @param username
	 * @return
	 */
	boolean containsUser(List<IdmIdentity> identities, String username);

	/**
	 * Convert given identities to string of user names separate with comma 
	 * @param identities
	 * @return
	 */
	String convertIdentitiesToString(List<IdmIdentity> identities);

	/**
	 * Find all guarantees for given role ID
	 * @param roleId
	 * @return
	 */
	List<IdmIdentity> findAllGuaranteesByRoleId(UUID roleId);

	
	/**
	 * Update IdmAuthorityChange for all given identities and set 
	 * it to provided value. 
	 * @param identities identities to update
	 * @param changeTime change time to set
	 */
	void updateAuthorityChange(List<IdmIdentity> identities, DateTime changeTime);

}
