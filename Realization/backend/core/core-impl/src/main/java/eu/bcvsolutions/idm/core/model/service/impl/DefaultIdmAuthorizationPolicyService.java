package eu.bcvsolutions.idm.core.model.service.impl;

import static eu.bcvsolutions.idm.core.model.event.AuthorizationPolicyEvent.AuthorizationPolicyEventType.CREATE;
import static eu.bcvsolutions.idm.core.model.event.AuthorizationPolicyEvent.AuthorizationPolicyEventType.DELETE;
import static eu.bcvsolutions.idm.core.model.event.AuthorizationPolicyEvent.AuthorizationPolicyEventType.UPDATE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.dto.IdmAuthorizationPolicyDto;
import eu.bcvsolutions.idm.core.api.dto.filter.AuthorizationPolicyFilter;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.ModuleService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmAuthorizationPolicy;
import eu.bcvsolutions.idm.core.model.entity.IdmAuthorizationPolicy_;
import eu.bcvsolutions.idm.core.model.entity.IdmRole;
import eu.bcvsolutions.idm.core.model.entity.IdmRole_;
import eu.bcvsolutions.idm.core.model.event.AuthorizationPolicyEvent;
import eu.bcvsolutions.idm.core.model.event.AuthorizationPolicyEvent.AuthorizationPolicyEventType;
import eu.bcvsolutions.idm.core.model.repository.IdmAuthorizationPolicyRepository;
import eu.bcvsolutions.idm.core.model.service.api.IdmAuthorizationPolicyService;
import eu.bcvsolutions.idm.core.model.service.api.IdmRoleService;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.DefaultGrantedAuthority;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmGroupPermission;
import eu.bcvsolutions.idm.core.security.api.dto.AuthorizableType;

/**
 * Assign authorization evaluator to role.
 * 
 * @author Radek Tomiška
 *
 */
public class DefaultIdmAuthorizationPolicyService 
		extends AbstractReadWriteDtoService<IdmAuthorizationPolicyDto, IdmAuthorizationPolicy, AuthorizationPolicyFilter> 
		implements IdmAuthorizationPolicyService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultIdmAuthorizationPolicyService.class);
	private final IdmAuthorizationPolicyRepository repository;
	private final IdmRoleService roleService;
	private final ModuleService moduleService;
	private final EntityEventManager eventManager;
	
	public DefaultIdmAuthorizationPolicyService(
			IdmAuthorizationPolicyRepository repository, 
			IdmRoleService roleService,
			ModuleService moduleService,
			EntityEventManager eventManager) {
		super(repository);
		//
		Assert.notNull(roleService);
		Assert.notNull(moduleService);
		Assert.notNull(eventManager);
		//
		this.repository = repository;
		this.roleService = roleService;
		this.moduleService = moduleService;
		this.eventManager = eventManager;
	}
	
	@Override
	public AuthorizableType getAuthorizableType() {
		return new AuthorizableType(CoreGroupPermission.AUTHORIZATIONPOLICY, getEntityClass());
	}
	
	@Override
	@Transactional
	public IdmAuthorizationPolicyDto save(IdmAuthorizationPolicyDto dto, BasePermission... permissions) {
		AuthorizationPolicyEventType eType = getSaveEventType(dto);
		IdmAuthorizationPolicy policyEntity = checkAccess(getPolicyEntity(dto), permissions);
		//
		return saveAuthorizationPolicy(eType, dto, policyEntity);
	}

	@Override
	@Transactional
	public IdmAuthorizationPolicyDto saveInternal(IdmAuthorizationPolicyDto dto) {
		Assert.notNull(dto);
		//
		return saveAuthorizationPolicy(getSaveEventType(dto), dto, getPolicyEntity(dto));
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmAuthorizationPolicyDto> getEnabledPolicies(String username, Class<? extends Identifiable> entityType) {
		Assert.notNull(entityType);
		//
		List<IdmAuthorizationPolicyDto> results = toDtos(repository.getPolicies(username, entityType.getCanonicalName(), false, new LocalDate()), false);
		results.addAll(getDefaultPolicies(entityType));
		return results;
	}
	
	@Override
	protected Predicate toPredicate(AuthorizationPolicyFilter filter, Root<IdmAuthorizationPolicy> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		List<Predicate> predicates = new ArrayList<>();
		// role id
		if (filter.getRoleId() != null) {
			predicates.add(builder.equal(root.get(IdmAuthorizationPolicy_.role).get(IdmRole_.id), filter.getRoleId()));
		}
		if (filter.getDisabled() != null) {
			predicates.add(builder.equal(root.get(IdmAuthorizationPolicy_.disabled), filter.getDisabled()));
		}
		if (filter.getAuthorizableType() != null) {
			predicates.add(builder.or(
					builder.and(
							builder.isNull(root.get(IdmAuthorizationPolicy_.authorizableType)),
							builder.isNull(root.get(IdmAuthorizationPolicy_.groupPermission))
							),
					builder.equal(root.get(IdmAuthorizationPolicy_.authorizableType), filter.getAuthorizableType())
					));
		}
		return builder.and(predicates.toArray(new Predicate[predicates.size()]));
	}
	
	@Override
	@Transactional(readOnly = true)
	public Set<GrantedAuthority> getDefaultAuthorities() {
		IdmRole defaultRole = roleService.getDefaultRole();
		if (defaultRole == null) {
			LOG.debug("Default role not found, no default authorities will be added. Change configuration [{}].", IdmRoleService.PROPERTY_DEFAULT_ROLE);
			return Collections.<GrantedAuthority>emptySet();
		}
		if (defaultRole.isDisabled()) {
			LOG.debug("Default role [{}] is disabled, no default authorities will be added.", defaultRole.getName());
			return Collections.<GrantedAuthority>emptySet();
		}
		//
		Set<GrantedAuthority> defaultAuthorities = getEnabledRoleAuthorities(defaultRole.getId());
		//
		LOG.debug("Found [{}] default authorities", defaultAuthorities.size());
		return defaultAuthorities;
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmAuthorizationPolicyDto> getDefaultPolicies(Class<? extends Identifiable> entityType) {
		IdmRole defaultRole = roleService.getDefaultRole();
		if (defaultRole == null) {
			LOG.debug("Default role not found, no default authorization policies will be added.  Change configuration [{}].", IdmRoleService.PROPERTY_DEFAULT_ROLE);
			return Collections.<IdmAuthorizationPolicyDto>emptyList();
		}
		if (defaultRole.isDisabled()) {
			LOG.debug("Default role [{}] is disabled, no default authorization policies will be added.", defaultRole.getName());
			return Collections.<IdmAuthorizationPolicyDto>emptyList();
		}
		//
		AuthorizationPolicyFilter filter = new AuthorizationPolicyFilter();
		filter.setRoleId(defaultRole.getId());
		filter.setDisabled(Boolean.FALSE);
		if(entityType != null) { // optional
			filter.setAuthorizableType(entityType.getCanonicalName());
		}
		List<IdmAuthorizationPolicy> defaultPolicies = find(filter, null).getContent();
		//
		LOG.debug("Found [{}] default policies", defaultPolicies.size());
		return toDtos(defaultPolicies, true);
	}
	
	@Override
	@Transactional(readOnly = true)
	public Set<GrantedAuthority> getEnabledRoleAuthorities(UUID roleId) {
		return getGrantedAuthorities(repository.getPolicies(roleId, false));
	}
	
	@Override
	@Transactional(readOnly = true)
	public Set<GrantedAuthority> getEnabledPersistedRoleAuthorities(UUID roleId) {
		return getGrantedAuthorities(repository.getPersistedPolicies(roleId, false));
	}

	@Override
	@Transactional(readOnly = true)
	public Set<GrantedAuthority> getGrantedAuthorities(List<IdmAuthorizationPolicy> policies) {
		final Set<GrantedAuthority> authorities = new HashSet<>();
		// find all active policies and return their authority by authorizable type
		for (IdmAuthorizationPolicy policy : policies) {
			if (IdmGroupPermission.APP.getName().equals(policy.getGroupPermission())
					|| (StringUtils.isEmpty(policy.getGroupPermission()) && policy.getPermissions().contains(IdmBasePermission.ADMIN.getName()))) {
				// admin
				return Sets.newHashSet(new DefaultGrantedAuthority(IdmGroupPermission.APP.getName(), IdmBasePermission.ADMIN.getName()));
			}
			if (StringUtils.isEmpty(policy.getGroupPermission())) {			
				moduleService.getAvailablePermissions().forEach(groupPermission -> {
					if (IdmGroupPermission.APP != groupPermission) { // app is wildcard only
						for(String permission : policy.getPermissions()) {
							authorities.add(new DefaultGrantedAuthority(groupPermission.getName(), permission));
						};
					}
				});
			} else if (policy.getPermissions().contains(IdmBasePermission.ADMIN.getName())) {	
				authorities.add(new DefaultGrantedAuthority(policy.getGroupPermission(), IdmBasePermission.ADMIN.getName()));					
			} else {
				for(String permission : policy.getPermissions()) {
					authorities.add(new DefaultGrantedAuthority(policy.getGroupPermission(), permission));
				};
			}			
		}
		//
		return authorities;
	}
	
	@Override
	protected void deleteEntity(UUID id) {
		IdmAuthorizationPolicy entity = get(id);
		eventManager.process(new AuthorizationPolicyEvent(DELETE, entity));
	}

	private IdmAuthorizationPolicyDto saveAuthorizationPolicy(AuthorizationPolicyEventType eType, 
			IdmAuthorizationPolicyDto dto, IdmAuthorizationPolicy entity) {
		//
		if (StringUtils.isNotEmpty(dto.getAuthorizableType()) && StringUtils.isEmpty(dto.getGroupPermission())) {
			throw new ResultCodeException(CoreResultCode.AUTHORIZATION_POLICY_GROUP_AUTHORIZATION_TYPE, 
					ImmutableMap.of("authorizableType", dto.getAuthorizableType(), "groupPermission", dto.getGroupPermission()));
		}
		//
		return toDto(eventManager.process(new AuthorizationPolicyEvent(eType, entity)).getContent());
	}

	private IdmAuthorizationPolicy getPolicyEntity(IdmAuthorizationPolicyDto dto) {
		return toEntity(dto, dto.getId() != null ? get(dto.getId()) : null);
	}

	private AuthorizationPolicyEventType getSaveEventType(IdmAuthorizationPolicyDto dto) {
		return dto.getId() == null ? CREATE : UPDATE;
	}


}
