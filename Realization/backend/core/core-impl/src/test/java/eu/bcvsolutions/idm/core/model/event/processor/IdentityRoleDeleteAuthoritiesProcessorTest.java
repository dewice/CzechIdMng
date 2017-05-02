package eu.bcvsolutions.idm.core.model.event.processor;

import java.util.Collection;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.model.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.model.entity.IdmAuthorityChange;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;
import eu.bcvsolutions.idm.core.security.api.domain.DefaultGrantedAuthority;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;

/**
 * Tests authorities change for role removal.
 * 
 * @author Jan Helbich
 *
 */
public class IdentityRoleDeleteAuthoritiesProcessorTest extends AbstractIdentityAuthoritiesProcessorTest {
	
	/**
	 * Removing a role which grants authorities must raise
	 * the authorities modification event on identity.
	 */
	@Transactional
	@Test
	public void testRoleRemovedAuthorityRemoved() {
		IdmRole role = getTestRole();
		IdmIdentity i = getTestUser();
		IdmIdentityContractDto c = getTestContract(i);
		IdmIdentityRoleDto ir = getTestIdentityRole(role, c);
		
		removeModifiedTimestamp(i);
		ir = identityRoleService.getDto(ir.getId());
		Assert.assertNull(getAuthorityChange(i));
		Assert.assertEquals(1, identityRoleService.findAllByIdentity(i.getId()).size());
		
		checkAssignedAuthorities(i);
	
		identityRoleService.delete(ir);
		
		DateTime comparableAuthChangeInstant = new DateTime().minusMinutes(1);
		
		IdmAuthorityChange ac = getAuthorityChange(i);
		Assert.assertNotNull(ac);
		Assert.assertTrue(ac.getAuthChangeTimestamp().isAfter(comparableAuthChangeInstant));
		Assert.assertEquals(0, identityRoleService.findAllByIdentity(i.getId()).size());
		Assert.assertEquals(0, authoritiesFactory.getGrantedAuthoritiesForIdentity(i).size());
	}

	/**
	 * User has to roles with same authorities - removing just one role
	 * shall not change the authorities modification flag.
	 */
	@Transactional
	@Test
	public void testRoleRemovedAuthorityStays() {
		// two roles with same authorities
		IdmRole role = getTestRole();
		IdmRole role2 = getTestRole();
		IdmIdentity i = getTestUser();
		IdmIdentityContractDto c = getTestContract(i);
		IdmIdentityRoleDto ir = getTestIdentityRole(role, c);
		IdmIdentityRoleDto ir2 = getTestIdentityRole(role2, c);
		
		removeModifiedTimestamp(i);
		Assert.assertNull(getAuthorityChange(i));
		Assert.assertEquals(2, identityRoleService.findAllByIdentity(i.getId()).size());
		
		checkAssignedAuthorities(i);
	
		identityRoleService.delete(ir2);
		
		Assert.assertNull(getAuthorityChange(i));
		Assert.assertEquals(1, identityRoleService.findAllByIdentity(i.getId()).size());
		Assert.assertEquals(ir.getId(), identityRoleService.findAllByIdentity(i.getId()).get(0).getId());
		Assert.assertEquals(1, authoritiesFactory.getGrantedAuthoritiesForIdentity(i).size());
	}
	
	private void removeModifiedTimestamp(IdmIdentity i) {
		// addition of roles also modifies authorities -> set to null for the sake of testing
		IdmAuthorityChange ac = getAuthorityChange(i);
		Assert.assertNotNull(ac);
		acRepository.delete(ac);
	}
	
	private void checkAssignedAuthorities(IdmIdentity i) {
		GrantedAuthority g = new DefaultGrantedAuthority(CoreGroupPermission.IDENTITY, IdmBasePermission.DELETE);
		Collection<GrantedAuthority> authorities = authoritiesFactory.getGrantedAuthoritiesForIdentity(i);
		Assert.assertEquals(1, authorities.size());
		authorities.stream().forEach(a -> Assert.assertEquals(g, a));
	}
	
}
