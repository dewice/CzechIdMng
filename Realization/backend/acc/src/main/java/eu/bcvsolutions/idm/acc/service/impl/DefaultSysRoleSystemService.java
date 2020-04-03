package eu.bcvsolutions.idm.acc.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.AccIdentityAccountFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysRoleSystemAttributeFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysRoleSystemFilter;
import eu.bcvsolutions.idm.acc.entity.SysRoleSystem;
import eu.bcvsolutions.idm.acc.entity.SysRoleSystemAttribute;
import eu.bcvsolutions.idm.acc.entity.SysRoleSystemAttribute_;
import eu.bcvsolutions.idm.acc.entity.SysRoleSystem_;
import eu.bcvsolutions.idm.acc.entity.SysSystemMapping_;
import eu.bcvsolutions.idm.acc.entity.SysSystem_;
import eu.bcvsolutions.idm.acc.repository.SysRoleSystemRepository;
import eu.bcvsolutions.idm.acc.service.api.AccIdentityAccountService;
import eu.bcvsolutions.idm.acc.service.api.SysRoleSystemAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysRoleSystemService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.ExportDescriptorDto;
import eu.bcvsolutions.idm.core.api.dto.IdmConceptRoleRequestDto;
import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleCompositionDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity_;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.ExportManager;
import eu.bcvsolutions.idm.core.api.service.IdmRoleCompositionService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.api.service.RequestManager;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.model.entity.IdmRole_;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;

/**
 * Role could assign identity account on target system.
 * 
 * @author Radek Tomiška
 *
 */
@Service
public class DefaultSysRoleSystemService
		extends AbstractReadWriteDtoService<SysRoleSystemDto, SysRoleSystem, SysRoleSystemFilter>
		implements SysRoleSystemService {

	@Autowired private AccIdentityAccountService identityAccountService;
	@Autowired private IdmRoleService roleService;
	@Autowired private RequestManager requestManager;
	@Autowired private SysSystemMappingService systemMappingService;
	@Autowired private SysRoleSystemAttributeService roleSystemAttributeService;
	@Autowired private IdmRoleCompositionService roleCompositionService;

	@Autowired
	public DefaultSysRoleSystemService(SysRoleSystemRepository repository) {
		super(repository);
	}

	@Override
	@Transactional
	public void delete(SysRoleSystemDto roleSystem, BasePermission... permission) {
		Assert.notNull(roleSystem, "Role system relation is required.");
		Assert.notNull(roleSystem.getId(), "Role system relation identifier is required.");

		SysRoleSystem roleSystemEntity = this.getEntity(roleSystem.getId());
		//
		// delete attributes
		SysRoleSystemAttributeFilter filter = new SysRoleSystemAttributeFilter();
		filter.setRoleSystemId(roleSystem.getId());
		List<SysRoleSystemAttributeDto> attributes = roleSystemAttributeService.find(filter, null).getContent();
		// We must delete attribute against service NOT repository. Historical
		// controlled values are created by service.
		for (SysRoleSystemAttributeDto attribute : attributes) {
			roleSystemAttributeService.delete(attribute);
		}
		//
		// clear identityAccounts - only link on roleSystem
		AccIdentityAccountFilter identityAccountFilter = new AccIdentityAccountFilter();
		identityAccountFilter.setRoleSystemId(roleSystemEntity.getId());
		identityAccountService
			.find(identityAccountFilter, null)
			.getContent()
			.forEach(identityAccount -> {
				identityAccount.setRoleSystem(null);
				identityAccountService.save(identityAccount);
			});
		//
		// Cancel requests and request items using that deleting DTO
		requestManager.onDeleteRequestable(roleSystem);

		super.delete(roleSystem, permission);
	}

	@Override
	public SysRoleSystemDto save(SysRoleSystemDto dto, BasePermission... permission) {
		Assert.notNull(dto, "RoleSystem cannot be null!");
		Assert.notNull(dto.getRole(), "Role cannot be null!");
		Assert.notNull(dto.getSystem(), "System cannot be null!");
		Assert.notNull(dto.getSystemMapping(), "System mapping cannot be null!");

		// Only Identity supports ACM by role
		SysSystemMappingDto systemMappingDto = systemMappingService.get(dto.getSystemMapping());
		if (systemMappingDto != null && SystemEntityType.IDENTITY != systemMappingDto.getEntityType()) {
			throw new ResultCodeException(AccResultCode.ROLE_SYSTEM_SUPPORTS_ONLY_IDENTITY,
					ImmutableMap.of("entityType", systemMappingDto.getEntityType().name()));
		}

		SysRoleSystemFilter filter = new SysRoleSystemFilter();
		filter.setRoleId(dto.getRole());
		filter.setSystemId(dto.getSystem());

		List<SysRoleSystemDto> roleSystems = this.find(filter, null).getContent();
		boolean isDuplicated = roleSystems.stream().filter(roleSystem -> {
			return !roleSystem.getId().equals(dto.getId());
		}).findFirst().isPresent();

		if (isDuplicated) {
			IdmRoleDto roleDto = roleService.get(dto.getRole());
			SysSystemDto systemDto = DtoUtils.getEmbedded(roleSystems.get(0), SysRoleSystem_.system);
			throw new ResultCodeException(AccResultCode.ROLE_SYSTEM_ALREADY_EXISTS,
					ImmutableMap.of("role", roleDto.getCode(), "system", systemDto.getName()));
		}

		return super.save(dto, permission);
	}

	@Override
	public List<IdmConceptRoleRequestDto> getConceptsForSystem(List<IdmConceptRoleRequestDto> concepts, UUID systemId) {
		// Roles using in concepts
		Set<UUID> roleIds = concepts.stream() //
				.map(IdmConceptRoleRequestDto::getRole) //
				.filter(Objects::nonNull) //
				.distinct() //
				.collect(Collectors.toSet());
		// We have direct roles, but we need sub-roles too. Beware here could be many
		// selects!
		Set<UUID> allSubRoles = Sets.newHashSet(roleIds);
		Map<UUID, Set<UUID>> roleWithSubroles = new HashMap<UUID, Set<UUID>>();

		roleIds.forEach(roleId -> {
			Set<UUID> subRoles = roleCompositionService.findAllSubRoles(roleId).stream() //
					.map(IdmRoleCompositionDto::getSub) //
					.distinct() //
					.collect(Collectors.toSet()); //
			// Put to result map, where key is super role and value set of all sub-roles
			roleWithSubroles.put(roleId, subRoles);
			allSubRoles.addAll(subRoles);
		});

		SysRoleSystemFilter roleSystemFilter = new SysRoleSystemFilter();
		roleSystemFilter.setSystemId(systemId);
		roleSystemFilter.setRoleIds(allSubRoles);

		Set<UUID> roles = this.find(roleSystemFilter, null).getContent() //
				.stream() //
				.map(SysRoleSystemDto::getRole) //
				.distinct() //
				.collect(Collectors.toSet());

		return concepts.stream() //
				.filter(concept -> {
					UUID roleId = concept.getRole();
					if (roleId == null) {
						return false;
					}
					if (roles.contains(roleId)) {
						// Direct role
						return true;
					}
					Set<UUID> subRoles = roleWithSubroles.get(roleId);
					if (subRoles == null) {
						return false;
					}
					// Sub-role
					return roles.stream() //
							.filter(role -> subRoles.contains(role)) //
							.findFirst() //
							.isPresent();

				}).collect(Collectors.toList());
	}
	
	@Override
	protected SysRoleSystemDto internalExport(UUID id) {
		 SysRoleSystemDto roleSystemDto = this.get(id);
		 
		 // We cannot clear all embedded data, because we need to export DTO for connected role.
		 BaseDto roleDto = roleSystemDto.getEmbedded().get(SysRoleSystem_.role.getName());
		 roleSystemDto.getEmbedded().clear();
		 roleSystemDto.getEmbedded().put(SysRoleSystem_.role.getName(), roleDto);
		 
		 return roleSystemDto;
	}
	
	@Override
	public void export(UUID id, IdmExportImportDto batch) {
		Assert.notNull(batch, "Export batch must exist!");
		// Export role-system
		super.export(id, batch);

		ExportDescriptorDto descriptorDto = getExportManager().getDescriptor(batch, this.getDtoClass());
		descriptorDto.setOptional(true);
		descriptorDto.getAdvancedParingFields().add(SysRoleSystem_.role.getName());
		
		// Export role systems
		SysRoleSystemAttributeFilter roleSystemAttributeFilter = new SysRoleSystemAttributeFilter();
		roleSystemAttributeFilter.setRoleSystemId(id);
		List<SysRoleSystemAttributeDto> roleSystemAttributes = roleSystemAttributeService.find(roleSystemAttributeFilter, null).getContent();
		if (roleSystemAttributes.isEmpty()) {
			roleSystemAttributeService.export(ExportManager.BLANK_UUID, batch);
		}
		roleSystemAttributes.forEach(roleSystemAttribute -> {
			roleSystemAttributeService.export(roleSystemAttribute.getId(), batch);
		});
		// Set parent field -> set authoritative mode for override attributes.
		this.getExportManager().setAuthoritativeMode(SysRoleSystemAttribute_.roleSystem.getName(), "systemId", SysRoleSystemAttributeDto.class,
				batch);
		// The override attribute is optional too.
		ExportDescriptorDto descriptorAttributeDto = getExportManager().getDescriptor(batch, SysRoleSystemAttributeDto.class);
		descriptorAttributeDto.setOptional(true);
		descriptorAttributeDto.getAdvancedParingFields().add(SysRoleSystemAttribute_.roleSystem.getName());
	}

	@Override
	protected List<Predicate> toPredicates(Root<SysRoleSystem> root, CriteriaQuery<?> query, CriteriaBuilder builder,
			SysRoleSystemFilter filter) {
		List<Predicate> predicates = super.toPredicates(root, query, builder, filter);

		if (filter.getRoleId() != null) {
			predicates.add(builder.equal(root.get(SysRoleSystem_.role).get(IdmRole_.id), filter.getRoleId()));
		}

		if (filter.getSystemId() != null) {
			predicates.add(builder.equal(root.get(SysRoleSystem_.system).get(SysSystem_.id), filter.getSystemId()));
		}

		if (filter.getSystemMappingId() != null) {
			predicates.add(builder.equal(root.get(SysRoleSystem_.systemMapping).get(SysSystemMapping_.id),
					filter.getSystemMappingId()));
		}

		// Return role-system where is uses given attribute mapping
		if (filter.getAttributeMappingId() != null) {
			Subquery<SysRoleSystemAttribute> subquery = query.subquery(SysRoleSystemAttribute.class);
			Root<SysRoleSystemAttribute> subRoot = subquery.from(SysRoleSystemAttribute.class);
			subquery.select(subRoot);

			subquery.where(builder.and( //
					builder.equal(subRoot.get(SysRoleSystemAttribute_.roleSystem), root), // Correlation attribute
					builder.equal(subRoot.get(SysRoleSystemAttribute_.systemAttributeMapping).get(AbstractEntity_.id),
							filter.getAttributeMappingId())));

			predicates.add(builder.exists(subquery));
		}

		Set<UUID> ids = filter.getRoleIds();
		if (CollectionUtils.isNotEmpty(ids)) {
			predicates.add(root.get(SysRoleSystem_.role).get(IdmRole_.id).in(ids));
		}

		return predicates;
	}

}
