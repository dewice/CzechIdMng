package eu.bcvsolutions.idm.acc.service.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.domain.SystemOperationType;
import eu.bcvsolutions.idm.acc.dto.AccAccountDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaObjectClassDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemAttributeMappingFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysSystemMappingFilter;
import eu.bcvsolutions.idm.acc.entity.AccAccount_;
import eu.bcvsolutions.idm.acc.entity.SysSystemAttributeMapping_;
import eu.bcvsolutions.idm.acc.entity.SysSystemMapping;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.repository.SysSystemMappingRepository;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.core.api.domain.IdmScriptCategory;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.AbstractEventableDtoService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.ExportManager;
import eu.bcvsolutions.idm.core.api.service.GroovyScriptService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.api.utils.EntityUtils;
import eu.bcvsolutions.idm.core.script.evaluator.AbstractScriptEvaluator;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;

/**
 * Default system entity handling
 * 
 * @author svandav
 * @author Ondrej Husnik
 *
 */
@Service
public class DefaultSysSystemMappingService
		extends AbstractEventableDtoService<SysSystemMappingDto, SysSystemMapping, SysSystemMappingFilter>
		implements SysSystemMappingService {

	private static final String SYSTEM_MISSING_IDENTIFIER = "systemMissingIdentifier";
	private static final String SYSTEM_MISSING_OWNER = "systemMissingOwner";
	//
	private final SysSystemMappingRepository repository;
	private final GroovyScriptService groovyScriptService;
	private final PluginRegistry<AbstractScriptEvaluator, IdmScriptCategory> pluginExecutors;
	private final ApplicationContext applicationContext;
	private SysSystemAttributeMappingService attributeMappingService;
	@Autowired
	@Lazy
	private SysSchemaAttributeService attributeService;

	@Autowired
	public DefaultSysSystemMappingService(SysSystemMappingRepository repository, EntityEventManager entityEventManager,
			GroovyScriptService groovyScriptService, List<AbstractScriptEvaluator> evaluators,
			ApplicationContext applicationContext) {
		super(repository, entityEventManager);
		//
		Assert.notNull(entityEventManager, "Manager is required.");
		Assert.notNull(groovyScriptService, "Groovy script service is required.");
		Assert.notNull(evaluators, "Script evaluators is required.");
		Assert.notNull(applicationContext, "Context is required.");
		//
		this.repository = repository;
		this.applicationContext = applicationContext;
		this.groovyScriptService = groovyScriptService;
		this.pluginExecutors = OrderAwarePluginRegistry.create(evaluators);
	}
	
	@Override
	public SysSystemMappingDto save(SysSystemMappingDto dto, BasePermission... permission) {
		SystemEntityType entityType = dto.getEntityType();
		if (SystemOperationType.PROVISIONING == dto.getOperationType() && !entityType.isSupportsProvisioning()) {
			throw new ResultCodeException(AccResultCode.PROVISIONING_NOT_SUPPORTS_ENTITY_TYPE, ImmutableMap.of("entityType", entityType));
		}
		
		// Validate all sub attributes
		getAttributeMappingService()
				.findBySystemMapping(dto).forEach(attribute -> {
					getAttributeMappingService().validate(attribute, dto);
				});
		return super.save(dto, permission);
	}

	@Override
	protected Page<SysSystemMapping> findEntities(SysSystemMappingFilter filter, Pageable pageable,
			BasePermission... permission) {
		if (filter == null) {
			return repository.findAll(pageable);
		}
		return repository.find(filter, pageable);
	}
	
	/**
	 * FIXME: refactor repository usage.
	 */
	@Override
	public Page<UUID> findIds(SysSystemMappingFilter filter, Pageable pageable, BasePermission... permission) {
		Page<SysSystemMapping> findEntities = findEntities(filter, pageable, permission);
		//
		List<UUID> ids = findEntities
				.getContent()
				.stream()
				.map(SysSystemMapping::getId)
				.collect(Collectors.toList());
		//
		return new PageImpl<UUID>(ids, findEntities.getPageable(), findEntities.getTotalElements());
	}

	@Override
	public List<SysSystemMappingDto> findBySystem(SysSystemDto system, SystemOperationType operation,
			SystemEntityType entityType) {
		Assert.notNull(system, "System is required.");
		//
		return findBySystemId(system.getId(), operation, entityType);
	}

	@Override
	public List<SysSystemMappingDto> findBySystemId(UUID systemId, SystemOperationType operation,
			SystemEntityType entityType) {
		Assert.notNull(systemId, "System identifier is required.");
		//
		SysSystemMappingFilter filter = new SysSystemMappingFilter();
		filter.setSystemId(systemId);
		filter.setOperationType(operation);
		filter.setEntityType(entityType);
		Page<SysSystemMappingDto> page = toDtoPage(repository.find(filter, null));
		return page.getContent();
	}

	@Override
	public List<SysSystemMappingDto> findByObjectClass(SysSchemaObjectClassDto objectClass,
			SystemOperationType operation, SystemEntityType entityType) {
		Assert.notNull(objectClass, "Object class is required.");

		SysSystemMappingFilter filter = new SysSystemMappingFilter();
		filter.setObjectClassId(objectClass.getId());
		filter.setOperationType(operation);
		filter.setEntityType(entityType);
		Page<SysSystemMappingDto> page = toDtoPage(repository.find(filter, null));
		return page.getContent();
	}

	@Override
	public boolean isEnabledProtection(AccAccountDto account) {
		Assert.notNull(account, "Account cannot be null!");
		Assert.notNull(account.getEntityType(), "EntityType cannot be null!");
		SysSystemDto system = DtoUtils.getEmbedded(account, AccAccount_.system);
		List<SysSystemMappingDto> mappings = this.findBySystem(system, SystemOperationType.PROVISIONING,
				account.getEntityType());
		if (mappings.isEmpty()) {
			return false;
		}
		// We assume only one mapping for provisioning and entity type.
		return this.isEnabledProtection(mappings.get(0));
	}

	@Override
	public Integer getProtectionInterval(AccAccountDto account) {
		Assert.notNull(account, "Account cannot be null!");
		Assert.notNull(account.getEntityType(), "EntityType cannot be null!");

		SysSystemDto system = DtoUtils.getEmbedded(account, AccAccount_.system);
		List<SysSystemMappingDto> mappings = this.findBySystem(system, SystemOperationType.PROVISIONING,
				account.getEntityType());
		if (mappings.isEmpty()) {
			return -1;
		}
		// We assume only one mapping for provisioning and entity type.
		return this.getProtectionInterval(mappings.get(0));
	}

	@Override
	public SysSystemMappingDto clone(UUID id) {
		SysSystemMappingDto original = this.get(id);
		Assert.notNull(original, "System mapping must be found!");

		original.setId(null);
		EntityUtils.clearAuditFields(original);
		return original;
	}
	
	@Override
	public void export(UUID id, IdmExportImportDto batch) {
		super.export(id, batch);
		
		// Export mapped attributes
		SysSystemAttributeMappingFilter filter = new SysSystemAttributeMappingFilter();
		filter.setSystemMappingId(id);
		List<SysSystemAttributeMappingDto> attributes = this.getAttributeMappingService().find(filter, null).getContent();
		if (attributes.isEmpty()) {
			this.getAttributeMappingService().export(ExportManager.BLANK_UUID, batch);
		}
		attributes.forEach(systemAttributeMapping -> {
					this.getAttributeMappingService().export(systemAttributeMapping.getId(), batch);
				});
		// Set parent field -> set authoritative mode.
		getExportManager().setAuthoritativeMode(SysSystemAttributeMapping_.systemMapping.getName(), "systemId",
				SysSystemAttributeMappingDto.class, batch);
	}

	/**
	 * Validate system mapping
	 * 
	 * @param id(UUID
	 *            system mapping)
	 */
	@Override
	public void validate(UUID id) {
		Assert.notNull(id, "Identifier is required.");
		//
		Map<String, Object> errors = new HashMap<>();
		SysSystemMappingDto systemMapping = this.get(id);
		List<SysSystemAttributeMappingDto> attributesList = getAttributeMappingService()
				.findBySystemMapping(systemMapping);
		//
		errors = validateIdentifier(errors, systemMapping, attributesList);
		errors = validateSynchronizationContracts(errors, systemMapping, attributesList);

		if (!errors.isEmpty()) {
			throw new ResultCodeException(AccResultCode.SYSTEM_MAPPING_VALIDATION, errors);
		}
	}

	/**
	 * Validation of sync: Missing Identifier
	 * 
	 * @param errors
	 * @param systemMapping
	 * @param attributesList
	 * @return
	 */
	private Map<String, Object> validateIdentifier(Map<String, Object> errors, SysSystemMappingDto systemMapping,
			List<SysSystemAttributeMappingDto> attributesList) {
		boolean isError = true;
		for (SysSystemAttributeMappingDto attribute : attributesList) {
			if (attribute.isUid()) {
				isError = false;
				break;
			}
		}
		if (isError) {
			errors.put(SYSTEM_MISSING_IDENTIFIER, "Identifier not found");
		}
		return errors;
	}

	/**
	 * Validation of sync
	 * 
	 * @param errors
	 * @param systemMapping
	 * @param attributesList
	 * @return
	 */
	private Map<String, Object> validateSynchronizationContracts(Map<String, Object> errors,
			SysSystemMappingDto systemMapping, List<SysSystemAttributeMappingDto> attributesList) {
		final String idmProperty = "identity";
		boolean isError = true;
		if (systemMapping.getOperationType() == SystemOperationType.SYNCHRONIZATION
				&& systemMapping.getEntityType() == SystemEntityType.CONTRACT) {
			for (SysSystemAttributeMappingDto attribute : attributesList) {
				if (attribute.isEntityAttribute() && attribute.getIdmPropertyName().equals(idmProperty)) {
					isError = false;
					break;
				}
			}
			if (isError) {
				errors.put(SYSTEM_MISSING_OWNER, "Synchronization does not have IdM key: identity");
			}
		}
		return errors;
	}

	@Override
	public boolean canBeAccountCreated(String uid, AbstractDto dto, String script, SysSystemDto system) {

		if (StringUtils.isEmpty(script)) {
			return true;
		} else {
			Map<String, Object> variables = new HashMap<>();
			variables.put(SysSystemAttributeMappingService.ACCOUNT_UID, uid);
			variables.put(SysSystemAttributeMappingService.SYSTEM_KEY, system);
			variables.put(SysSystemAttributeMappingService.ENTITY_KEY, dto);
			// add default script evaluator, for call another scripts
			variables.put(AbstractScriptEvaluator.SCRIPT_EVALUATOR,
					pluginExecutors.getPluginFor(IdmScriptCategory.TRANSFORM_TO));

			// Add access for script evaluator
			List<Class<?>> extraClass = new ArrayList<>();
			extraClass.add(AbstractScriptEvaluator.Builder.class);
			//
			Object result = groovyScriptService.evaluate(script, variables, extraClass);
			if (result instanceof Boolean) {
				return (boolean) result;
			} else {
				throw new ProvisioningException(
						AccResultCode.PROVISIONING_SCRIPT_CAN_BE_ACC_CREATED_MUST_RETURN_BOOLEAN,
						ImmutableMap.of("system", system.getCode()));
			}
		}
	}

	@Override
	public SysSystemMappingDto findProvisioningMapping(UUID systemId, SystemEntityType entityType) {
		Assert.notNull(systemId, "System identifier is required.");
		Assert.notNull(entityType, "Entity type is required.");
		SysSystemMappingFilter mappingFilter = new SysSystemMappingFilter();
		mappingFilter.setSystemId(systemId);
		mappingFilter.setEntityType(entityType);
		mappingFilter.setOperationType(SystemOperationType.PROVISIONING);
		List<SysSystemMappingDto> mappings = this.find(mappingFilter, null).getContent();
		if (mappings.isEmpty()) {
			return null;
		}
		// Only one mapping for provisioning and entity type and system can exists
		return mappings.get(0);
	}

	private Integer getProtectionInterval(SysSystemMappingDto systemMapping) {

		Assert.notNull(systemMapping, "Mapping cannot be null!");
		return systemMapping.getProtectionInterval();
	}

	private boolean isEnabledProtection(SysSystemMappingDto systemMapping) {
		Assert.notNull(systemMapping, "Mapping cannot be null!");
		return systemMapping.isProtectionEnabled();
	}

	private SysSystemAttributeMappingService getAttributeMappingService() {
		if (attributeMappingService == null)
			attributeMappingService = applicationContext.getBean(SysSystemAttributeMappingService.class);
		return attributeMappingService;
	}
	
	
	@Override
	@Transactional
	public SysSystemMappingDto duplicateMapping(UUID id, SysSchemaObjectClassDto schema,
			Map<UUID, UUID> schemaAttributesIds, Map<UUID, UUID> mappedAttributesIds, boolean usedInSameSystem) {
		Assert.notNull(id, "Id of duplication mapping, must be filled!");
		Assert.notNull(schema, "Parent schema must be filled!");

		SysSystemMappingDto clonedMapping = this.clone(id);
		clonedMapping.setObjectClass(schema.getId());

		if (usedInSameSystem) {
			String newName = duplicateName(clonedMapping.getName());
			clonedMapping.setName(newName);
			clonedMapping.setOperationType(SystemOperationType.SYNCHRONIZATION);
		}
		SysSystemMappingDto mapping = this.save(clonedMapping);

		// Clone mapped attributes
		SysSystemAttributeMappingFilter attributesFilter = new SysSystemAttributeMappingFilter();
		attributesFilter.setSystemMappingId(id);
		attributeMappingService.find(attributesFilter, null).forEach(attribute -> {
			UUID originalAttributeId = attribute.getId();
			SysSystemAttributeMappingDto clonedAttribute = attributeMappingService.clone(originalAttributeId);
			// Find cloned schema attribute in cache (by original Id)
			SysSchemaAttributeDto clonedSchemaAttribute = attributeService
					.get(schemaAttributesIds.get(clonedAttribute.getSchemaAttribute()));

			clonedAttribute.setSystemMapping(mapping.getId());
			clonedAttribute.setSchemaAttribute(clonedSchemaAttribute.getId());
			clonedAttribute = attributeMappingService.save(clonedAttribute);
			// Put original and new id to cache
			mappedAttributesIds.put(originalAttributeId, clonedAttribute.getId());
		});

		return mapping;
	}
	
	/**
	 * 
	 * @param name - name copy of which is to be created
	 */
	private String duplicateName(String name) {
		String newNameBase = MessageFormat.format("{0}{1}", "Copy-of-", name);
		String newName = newNameBase;
		SysSystemMappingFilter filter = new SysSystemMappingFilter();
		int i = 1;
		do {
			filter.setText(newName);
			if (!this.find(filter, null).hasContent()) {
				return newName;
			} else {
				newName = MessageFormat.format("{0}{1}", newNameBase, i);
				i++;
			}
		} while (true);
	}
}
