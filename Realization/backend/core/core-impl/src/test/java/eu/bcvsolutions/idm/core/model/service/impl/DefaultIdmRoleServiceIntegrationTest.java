package eu.bcvsolutions.idm.core.model.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.beans.IntrospectionException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.InitTestData;
import eu.bcvsolutions.idm.core.api.domain.RoleType;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleCatalogueDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleCatalogueRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleGuaranteeDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmAuthorizationPolicyFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleFilter;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.IdmAuthorizationPolicyService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleCatalogueRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.api.utils.EntityUtils;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity_;
import eu.bcvsolutions.idm.core.model.repository.IdmRoleGuaranteeRepository;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;
import eu.bcvsolutions.idm.test.api.TestHelper;

/**
 * Basic role service operations
 * 
 * @author Radek Tomiška
 * @author Marek Klement
 *
 */
public class DefaultIdmRoleServiceIntegrationTest extends AbstractIntegrationTest {

	@Autowired private TestHelper helper;
	@Autowired private IdmIdentityService identityService;
	@Autowired private IdmRoleCatalogueRoleService idmRoleCatalogueRoleService;
	@Autowired private IdmRoleService roleService;
	@Autowired private IdmRoleGuaranteeRepository roleGuaranteeRepository;
	@Autowired private IdmAuthorizationPolicyService authorizationPolicyService;
	
	@Before
	public void init() {
		loginAsAdmin(InitTestData.TEST_USER_1);
	}
	
	@After 
	public void logout() {
		super.logout();
	}
	
	@Test
	public void testReferentialIntegrity() {
		IdmIdentityDto identity = new IdmIdentityDto();
		String username = "delete_test_" + System.currentTimeMillis();
		identity.setUsername(username);
		identity.setPassword(new GuardedString("heslo")); // confidential storage
		identity.setFirstName("Test");
		identity.setLastName("Identity");
		identity = identityService.save(identity);
		// role
		IdmRoleDto role = new IdmRoleDto();
		String roleName = "test_r_" + System.currentTimeMillis();
		role.setName(roleName);
		IdmRoleGuaranteeDto roleGuarantee = new IdmRoleGuaranteeDto();
		roleGuarantee.setRole(role.getId());
		roleGuarantee.setGuarantee(identity.getId());
		role.setGuarantees(Lists.newArrayList(roleGuarantee));
		role = roleService.save(role);
		
		assertNotNull(roleService.getByCode(roleName));
		assertEquals(1, roleGuaranteeRepository.findAllByRole_Id(role.getId()).size());
		
		roleService.delete(role);
		
		assertNull(roleService.getByCode(roleName));
		assertEquals(0, roleGuaranteeRepository.findAllByRole_Id(role.getId()).size());
	}
	
	@Test(expected = ResultCodeException.class)
	public void testReferentialIntegrityAssignedRoles() {
		// prepare data
		IdmIdentityDto identity = helper.createIdentity("delete-test");
		IdmRoleDto role = helper.createRole("test-delete");
		// assigned role
		helper.createIdentityRole(identity, role);
		//
		roleService.delete(role);
	}
	
	@Test
	public void testReferentialIntegrityAuthorizationPolicies() {
		// prepare data
		IdmRoleDto role = helper.createRole();
		// policy
		helper.createBasePolicy(role.getId(), IdmBasePermission.ADMIN);
		//
		roleService.delete(role);
		//
		IdmAuthorizationPolicyFilter policyFilter = new IdmAuthorizationPolicyFilter();
		policyFilter.setRoleId(role.getId());
		assertEquals(0, authorizationPolicyService.find(policyFilter, null).getTotalElements());
	}

	@Test
	public void textFilterTest(){
		helper.createRole("SomeName001");
		helper.createRole("SomeName002");
		helper.createRole("SomeName003");
		helper.createRole("SomeName104");

		IdmRoleDto role5 = new IdmRoleDto();
		role5.setDescription("SomeName005");
		role5.setName("SomeName105");
		role5 = roleService.save(role5);

		IdmRoleFilter filter = new IdmRoleFilter();
		filter.setText("SomeName00");
		Page<IdmRoleDto> result = roleService.find(filter,null);
		assertEquals("Wrong text filter",4,result.getTotalElements());
		assertEquals("Wrong text filter description",true,result.getContent().contains(role5));
	}

	@Test
	public void typeFilterTest(){
		IdmRoleDto role = helper.createRole();
		IdmRoleDto role2 = helper.createRole();
		IdmRoleDto role3 = helper.createRole();

		RoleType type = RoleType.LOGIN;
		RoleType type2 = RoleType.BUSINESS;

		role = roleService.get(role.getId());
		role.setRoleType(type);
		role = roleService.save(role);

		role2 = roleService.get(role2.getId());
		role2.setRoleType(type);
		role2 = roleService.save(role2);

		role3 = roleService.get(role3.getId());
		role3.setRoleType(type2);
		role3 = roleService.save(role3);

		IdmRoleFilter filter = new IdmRoleFilter();
		filter.setRoleType(type);
		Page<IdmRoleDto> result = roleService.find(filter,null);
		assertEquals("Wrong type #1", 2, result.getTotalElements());
		assertTrue("Wrong type #1 contains", result.getContent().contains(role));
		filter.setRoleType(type2);
		result = roleService.find(filter,null);
		assertEquals("Wrong type #2", 1, result.getTotalElements());
		assertTrue("Wrong type #2 contains", result.getContent().contains(role3));
	}

	@Test
	public void guaranteeFilterTest(){
		IdmIdentityDto identity = helper.createIdentity();

		IdmRoleDto role = new IdmRoleDto();
		role.setName("IgnacMikinaRole");
		helper.createRole();

		IdmRoleGuaranteeDto roleGuarantee = new IdmRoleGuaranteeDto();
		roleGuarantee.setRole(role.getId());
		roleGuarantee.setGuarantee(identity.getId());
		role.setGuarantees(Lists.newArrayList(roleGuarantee));
		role = roleService.save(role);

		IdmRoleFilter filter = new IdmRoleFilter();
		filter.setGuaranteeId(identity.getId());
		Page<IdmRoleDto> result = roleService.find(filter, null);
		assertEquals("Wrong guarantee", 1, result.getTotalElements());
		assertEquals("Wrong guarantee id", role.getId(), result.getContent().get(0).getId());
	}

	@Test
	public void catalogueFilterTest(){
		IdmRoleDto role = new IdmRoleDto();
		role.setName("PetrSadloRole");
		role = roleService.save(role);

		IdmRoleCatalogueDto catalogue = helper.createRoleCatalogue();
		IdmRoleCatalogueRoleDto catalogueRole = new IdmRoleCatalogueRoleDto();
		catalogueRole.setRole(role.getId());
		catalogueRole.setRoleCatalogue(catalogue.getId());
		catalogueRole = idmRoleCatalogueRoleService.save(catalogueRole);

		IdmRoleFilter filter = new IdmRoleFilter();
		filter.setRoleCatalogueId(catalogue.getId());
		Page<IdmRoleDto> result = roleService.find(filter,null);
		assertEquals("Wrong catalogue", 1, result.getTotalElements());
		assertTrue("Wrong catalogue id #1", result.getContent().contains(role));
	}
	
	@Test
	/**
	 * Test find role by all string fields
	 */
	public void testCorrelableFilter() {
		IdmRoleDto role = helper.createRole();
		role.setExternalId(UUID.randomUUID().toString());
		role.setName((UUID.randomUUID().toString()));
		role.setDescription(UUID.randomUUID().toString());
		IdmRoleDto roleFull = roleService.save(role);

		ArrayList<Field> fields = Lists.newArrayList(IdmIdentity_.class.getFields());
		IdmRoleFilter filter = new IdmRoleFilter();

		fields.forEach(field -> {
			filter.setProperty(field.getName());

			try {
				Object value = EntityUtils.getEntityValue(roleFull, field.getName());
				if (value == null || !(value instanceof String)) {
					return;
				}
				filter.setValue(value.toString());
				List<IdmRoleDto> identities = roleService.find(filter, null).getContent();
				assertTrue(identities.contains(roleFull));

			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| IntrospectionException e) {
				e.printStackTrace();
			}

		});

	}
}
