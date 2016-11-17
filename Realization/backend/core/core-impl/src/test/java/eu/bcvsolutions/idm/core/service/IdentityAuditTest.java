package eu.bcvsolutions.idm.core.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.rest.domain.ResourceWrapper;
import eu.bcvsolutions.idm.core.api.rest.domain.ResourcesWrapper;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityRole;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;
import eu.bcvsolutions.idm.core.model.repository.IdmIdentityContractRepository;
import eu.bcvsolutions.idm.core.model.repository.IdmIdentityRoleRepository;
import eu.bcvsolutions.idm.core.model.service.api.IdmIdentityService;
import eu.bcvsolutions.idm.core.model.service.api.IdmRoleService;
import eu.bcvsolutions.idm.core.rest.impl.IdmIdentityController;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Test audit configuration on identity entity
 * 
 * @author Radek Tomiška 
 *
 */
public class IdentityAuditTest extends AbstractIntegrationTest {
	
	@Autowired
	private IdmIdentityService identityService;
	
	@Autowired
	private IdmRoleService roleService;
	
	@Autowired
	private IdmIdentityRoleRepository identityRoleRepository;
	
	@Autowired
	private IdmIdentityController identityController;	
	
	@Autowired
	private IdmIdentityContractRepository identityWorkingPositionRepository;

	@PersistenceContext
	private EntityManager entityManager;
	
	private IdmIdentity identity;
	private IdmRole role = null;

	@Before
	public void transactionTemplate() {
		identity = constructTestIdentity();
		loginAsAdmin("admin");
	}
	
	@After
	@Transactional
	public void deleteIdentity() {
		// we need to ensure "rollback" manually the same as we are starting transaction manually		
		identityService.delete(identity);
		if (role != null) {
			roleService.delete(role);
			role = null;
		}
		logout();
	}

	@Test
	public void testCreateIdentity() {
		identity = saveInTransaction(identity, identityService);

		assertNotNull(identity.getId());

		getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
				AuditReader reader = AuditReaderFactory.get(entityManager);
				assertEquals(1, reader.getRevisions(IdmIdentity.class, identity.getId()).size());
			}
		});	
	}
	
	@Test
	public void testUpdateIdentity() {
		identity = saveInTransaction(identity, identityService);
		identity.setFirstName("One"); 
		identity = saveInTransaction(identity, identityService);
		
		getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				AuditReader reader = AuditReaderFactory.get(entityManager);
				assertEquals(2, reader.getRevisions(IdmIdentity.class, identity.getId()).size());
				
			}
		});			
	}	

	@Test
	public void testWorkingPositionChange() {
		identity = saveInTransaction(identity, identityService);
		
		IdmIdentityContract position = new IdmIdentityContract();
		position.setIdentity(identity);
		
		saveInTransaction(position, identityWorkingPositionRepository);
		
		getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				AuditReader reader = AuditReaderFactory.get(entityManager);
				assertEquals(2, reader.getRevisions(IdmIdentity.class, identity.getId()).size());
				
			}
		});
	}

	@Test
	public void testAssignedRoleChanges() {
		identity = saveInTransaction(identity, identityService);
		
		role = new IdmRole();
		role.setName("audit_role");
		
		role = saveInTransaction(role, roleService);
		
		IdmIdentityRole identityRole = new IdmIdentityRole();
		identityRole.setIdentity(identity);
		identityRole.setRole(role);
		
		saveInTransaction(identityRole, identityRoleRepository);
		
		getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				AuditReader reader = AuditReaderFactory.get(entityManager);
				assertEquals(2, reader.getRevisions(IdmIdentity.class, identity.getId()).size());
				
			}
		});
	}
	
	@Test
	public void testIdentityController() {
		getTransactionTemplate().execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus arg0) {
				identity = constructTestIdentity();
				identity = saveInTransaction(identity, identityService);
				
				String nonExistIdentityId = "NON_EXIST_IDENTITY_ID";
				
				ResponseEntity<ResourcesWrapper<ResourceWrapper<DefaultRevisionEntity>>> result = identityController.findRevisions(identity.getName());
				
				assertEquals(true, result.hasBody());
				
				Exception exception = null;
				
				try {
					identityController.findRevisions(nonExistIdentityId);
				} catch (ResultCodeException e) {
					exception = e;
				} catch (Exception e) {
					// do nothing
				}
				
				assertNotNull(exception);
				
				exception = null;
				
				try {
					identityController.findRevision(nonExistIdentityId, Integer.MAX_VALUE);
				} catch (ResultCodeException e) {
					exception = e;
				} catch (Exception e) {
					// do nothing
				}
				
				assertNotNull(exception);
			}
		});
	}
	
	private IdmIdentity constructTestIdentity() {
		IdmIdentity identity = new IdmIdentity();
		identity.setUsername("audit_test_user");
		identity.setLastName("Auditor");
		return identity;
	}
}
