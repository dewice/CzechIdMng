package eu.bcvsolutions.idm.acc.service.impl;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.AttributeMapping;
import eu.bcvsolutions.idm.acc.domain.SystemOperationType;
import eu.bcvsolutions.idm.acc.dto.filter.SystemAttributeMappingFilter;
import eu.bcvsolutions.idm.acc.entity.SysSchemaAttribute;
import eu.bcvsolutions.idm.acc.entity.SysSystem;
import eu.bcvsolutions.idm.acc.entity.SysSystemAttributeMapping;
import eu.bcvsolutions.idm.acc.entity.SysSystemMapping;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.repository.SysRoleSystemAttributeRepository;
import eu.bcvsolutions.idm.acc.repository.SysSyncConfigRepository;
import eu.bcvsolutions.idm.acc.repository.SysSystemAttributeMappingRepository;
import eu.bcvsolutions.idm.acc.service.api.FormPropertyManager;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteEntityService;
import eu.bcvsolutions.idm.core.api.service.ConfidentialStorage;
import eu.bcvsolutions.idm.core.api.service.GroovyScriptService;
import eu.bcvsolutions.idm.core.eav.api.entity.FormableEntity;
import eu.bcvsolutions.idm.core.eav.entity.AbstractFormValue;
import eu.bcvsolutions.idm.core.eav.entity.IdmFormAttribute;
import eu.bcvsolutions.idm.core.eav.service.api.FormService;
import eu.bcvsolutions.idm.core.model.domain.EntityUtilities;
import eu.bcvsolutions.idm.core.model.domain.IdmScriptCategory;
import eu.bcvsolutions.idm.core.script.evaluator.AbstractScriptEvaluator;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.ic.api.IcAttribute;
import eu.bcvsolutions.idm.ic.impl.IcAttributeImpl;
import eu.bcvsolutions.idm.ic.impl.IcPasswordAttributeImpl;
import eu.bcvsolutions.idm.ic.service.api.IcConnectorFacade;

/**
 * Default schema attributes handling
 * 
 * @author svandav
 *
 */
@Service
public class DefaultSysSystemAttributeMappingService
		extends AbstractReadWriteEntityService<SysSystemAttributeMapping, SystemAttributeMappingFilter>
		implements SysSystemAttributeMappingService {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
			.getLogger(DefaultSysSystemAttributeMappingService.class);

	private final SysSystemAttributeMappingRepository repository;
	private final GroovyScriptService groovyScriptService;
	private final FormService formService;
	private final ConfidentialStorage confidentialStorage;
	private final SysRoleSystemAttributeRepository roleSystemAttributeRepository;
	private final FormPropertyManager formPropertyManager;
	private final SysSyncConfigRepository syncConfigRepository;
	private final PluginRegistry<AbstractScriptEvaluator, IdmScriptCategory> pluginExecutors; 
	@Autowired
	public DefaultSysSystemAttributeMappingService(
			SysSystemAttributeMappingRepository repository,
			GroovyScriptService groovyScriptService, 
			FormService formService,
			SysRoleSystemAttributeRepository roleSystemAttributeRepository,
			FormPropertyManager formPropertyManager,
			SysSyncConfigRepository syncConfigRepository,
			List<AbstractScriptEvaluator> evaluators,
			ConfidentialStorage confidentialStorage) {
		super(repository);
		//
		Assert.notNull(groovyScriptService);
		Assert.notNull(formService);
		Assert.notNull(roleSystemAttributeRepository);
		Assert.notNull(formPropertyManager);
		Assert.notNull(syncConfigRepository);
		Assert.notNull(evaluators);
		Assert.notNull(confidentialStorage);
		//
		this.formService = formService;
		this.repository = repository;
		this.groovyScriptService = groovyScriptService;
		this.roleSystemAttributeRepository = roleSystemAttributeRepository;
		this.formPropertyManager = formPropertyManager;
		this.syncConfigRepository = syncConfigRepository;
		this.confidentialStorage = confidentialStorage;
		//
		this.pluginExecutors = OrderAwarePluginRegistry.create(evaluators);
	}

	public List<SysSystemAttributeMapping> findBySystemMapping(SysSystemMapping systemMapping) {
		Assert.notNull(systemMapping);
		//
		SystemAttributeMappingFilter filter = new SystemAttributeMappingFilter();
		filter.setSystemMappingId(systemMapping.getId());
		Page<SysSystemAttributeMapping> page = repository.find(filter, null);
		return page.getContent();
	}

	@Override
	public Object transformValueToResource(Object value, AttributeMapping attributeMapping,
			AbstractEntity entity) {
		Assert.notNull(attributeMapping);
		//
		return transformValueToResource(value, attributeMapping.getTransformToResourceScript(), entity,
				attributeMapping.getSchemaAttribute().getObjectClass().getSystem());
	}

	@Override
	public Object transformValueToResource(Object value, String script, AbstractEntity entity, SysSystem system) {
		if (!StringUtils.isEmpty(script)) {
			Map<String, Object> variables = new HashMap<>();
			variables.put(ATTRIBUTE_VALUE_KEY, value);
			variables.put(SYSTEM_KEY, system);
			variables.put(ENTITY_KEY, entity);
			variables.put(AbstractScriptEvaluator.SCRIPT_EVALUATOR, pluginExecutors.getPluginFor(IdmScriptCategory.TRANSFORM_TO)); // add default script evaluator, for call another scripts
			//
			// Add access for script evaluator
			List<Class<?>> extraClass = new ArrayList<>();
			extraClass.add(AbstractScriptEvaluator.Builder.class);
			//
			return groovyScriptService.evaluate(script, variables, extraClass);
		}

		return value;
	}

	@Override
	public Object transformValueFromResource(Object value, AttributeMapping attributeMapping,
			List<IcAttribute> icAttributes) {
		Assert.notNull(attributeMapping);
		//
		return transformValueFromResource(value, attributeMapping.getTransformFromResourceScript(), icAttributes,
				attributeMapping.getSchemaAttribute().getObjectClass().getSystem());
	}

	@Override
	public Object transformValueFromResource(Object value, String script, List<IcAttribute> icAttributes,
			SysSystem system) {

		if (!StringUtils.isEmpty(script)) {
			Map<String, Object> variables = new HashMap<>();
			variables.put(ATTRIBUTE_VALUE_KEY, value);
			variables.put(SYSTEM_KEY, system);
			variables.put(IC_ATTRIBUTES_KEY, icAttributes);
			variables.put(AbstractScriptEvaluator.SCRIPT_EVALUATOR, pluginExecutors.getPluginFor(IdmScriptCategory.TRANSFORM_FROM)); // add default script evaluator, for call another scripts
			// Add access for script evaluator
			List<Class<?>> extraClass = new ArrayList<>();
			extraClass.add(AbstractScriptEvaluator.Builder.class);
			//
			return groovyScriptService.evaluate(script, variables, extraClass);
		}

		return value;
	}

	@Override
	@Transactional
	public SysSystemAttributeMapping save(SysSystemAttributeMapping entity) {
		// Check if exist some else attribute which is defined like unique identifier
		if (entity.isUid()) {			
			SystemAttributeMappingFilter filter = new SystemAttributeMappingFilter();
			filter.setSystemMappingId(entity.getSystemMapping().getId());
			filter.setIsUid(Boolean.TRUE);
			List<SysSystemAttributeMapping> list = this.find(filter, null).getContent();
			
			if (list.size() > 0 && !list.get(0).getId().equals(entity.getId())) {
				throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_MORE_UID, ImmutableMap.of("system", entity.getSystemMapping().getSystem().getName()));
			}
		}
		
		// We will do script validation (on compilation errors), before save
		// attribute handling

		if (entity.getTransformFromResourceScript() != null) {
			groovyScriptService.validateScript(entity.getTransformFromResourceScript());
		}
		if (entity.getTransformToResourceScript() != null) {
			groovyScriptService.validateScript(entity.getTransformToResourceScript());
		}
		Class<?> entityType = entity.getSystemMapping().getEntityType().getEntityType();
		if (entity.isExtendedAttribute() && FormableEntity.class.isAssignableFrom(entityType)) {
			createExtendedAttributeDefinition(entity, entityType);
		}
		return super.save(entity);
	}

	/**
	 * Check on exists EAV definition for given attribute. If the definition not exist, then we try create it.
	 * @param entity
	 * @param entityType
	 */
	@Override
	@Transactional
	@SuppressWarnings("unchecked")
	public void createExtendedAttributeDefinition(AttributeMapping entity, Class<?> entityType) {
		Class<? extends FormableEntity> ownerClass = (Class<? extends FormableEntity>)entityType;
		IdmFormAttribute attribute = formService.getAttribute(ownerClass, entity.getIdmPropertyName());
		if (attribute == null) {
			log.info(MessageFormat.format(
					"IdmFormAttribute for identity and property {0} not found. We will create definition now.",
					entity.getIdmPropertyName()));
			formService.saveAttribute(ownerClass, convertMappingAttribute(entity));
		}		
	}
	
	@Override
	@Transactional
	public void delete(SysSystemAttributeMapping entity) {
		Assert.notNull(entity);
		
		if (syncConfigRepository.countByCorrelationAttribute(entity) > 0) {
			throw new ResultCodeException(AccResultCode.ATTRIBUTE_MAPPING_DELETE_FAILED_USED_IN_SYNC, ImmutableMap.of("attribute", entity.getName()));
		}
		if (syncConfigRepository.countByFilterAttribute(entity) > 0) {
			throw new ResultCodeException(AccResultCode.ATTRIBUTE_MAPPING_DELETE_FAILED_USED_IN_SYNC, ImmutableMap.of("attribute", entity.getName()));
		}
		if (syncConfigRepository.countByTokenAttribute(entity) > 0) {
			throw new ResultCodeException(AccResultCode.ATTRIBUTE_MAPPING_DELETE_FAILED_USED_IN_SYNC, ImmutableMap.of("attribute", entity.getName()));
		}
		// delete attributes
		roleSystemAttributeRepository.deleteBySystemAttributeMapping(entity);
		//
		super.delete(entity);
	}
	
	/**
	 * Create instance of IC attribute for given name. Given idm value will be
	 * transformed to resource.
	 * 
	 * @param attributeMapping
	 * @param idmValue
	 * @return
	 */
	@Override
	public IcAttribute createIcAttribute(SysSchemaAttribute schemaAttribute, Object idmValue) {
		// Check type of value
		try {
			Class<?> classType = Class.forName(schemaAttribute.getClassType());
			
			// If is multivalue and value is list, then we will iterate list and check every item on correct type
			if (schemaAttribute.isMultivalued() && idmValue instanceof List){
				((List<?>)idmValue).stream().forEachOrdered(value ->{
					if (value != null && !(classType.isAssignableFrom(value.getClass()))) {
						throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_VALUE_WRONG_TYPE,
								ImmutableMap.of("attribute", schemaAttribute.getName(), "schemaAttributeType",
										schemaAttribute.getClassType(), "valueType", value.getClass().getName()));
					}
				});
				
			// Check single value on correct type
			}else if (idmValue != null && !(classType.isAssignableFrom(idmValue.getClass()))) {
				throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_VALUE_WRONG_TYPE,
						ImmutableMap.of("attribute", schemaAttribute.getName(), "schemaAttributeType",
								schemaAttribute.getClassType(), "valueType", idmValue.getClass().getName()));
			}
		} catch (ClassNotFoundException | ProvisioningException e) {
			throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_TYPE_NOT_FOUND,
					ImmutableMap.of("attribute", schemaAttribute.getName(), "schemaAttributeType",
							schemaAttribute.getClassType()),
					e);
		}

		IcAttribute icAttributeForUpdate = null;
		if (IcConnectorFacade.PASSWORD_ATTRIBUTE_NAME.equals(schemaAttribute.getName())) {
			// Attribute is password type
			icAttributeForUpdate = new IcPasswordAttributeImpl(schemaAttribute.getName(), (GuardedString) idmValue);

		} else {
			if(idmValue instanceof List){
				@SuppressWarnings("unchecked")
				List<Object> values = (List<Object>)idmValue;
				icAttributeForUpdate = new IcAttributeImpl(schemaAttribute.getName(), values, true);
			}else{
				icAttributeForUpdate = new IcAttributeImpl(schemaAttribute.getName(), idmValue);
			}
		}
		return icAttributeForUpdate;
	}

	/**
	 * Convert schema attribute handling to Form attribute
	 * 
	 * @param entity
	 * @return
	 */
	private IdmFormAttribute convertMappingAttribute(AttributeMapping entity) {
		SysSchemaAttribute schemaAttribute = entity.getSchemaAttribute();
		IdmFormAttribute attributeDefinition = new IdmFormAttribute();
		attributeDefinition.setSeq((short) 0);
		attributeDefinition.setName(entity.getIdmPropertyName());
		attributeDefinition.setDisplayName(entity.getName());
		attributeDefinition.setPersistentType(formPropertyManager.getPersistentType(schemaAttribute.getClassType()));
		attributeDefinition.setRequired(schemaAttribute.isRequired());
		attributeDefinition.setMultiple(schemaAttribute.isMultivalued());
		attributeDefinition.setReadonly(!schemaAttribute.isUpdateable());
		attributeDefinition.setConfidential(entity.isConfidentialAttribute());
		attributeDefinition.setUnmodifiable(false); // attribute can be deleted
		attributeDefinition.setDescription(
				MessageFormat.format("Genereted by schema attribute {0} in resource {1}. Created by SYSTEM.",
						schemaAttribute.getName(), schemaAttribute.getObjectClass().getSystem().getName()));
		return attributeDefinition;
	}

	@Override
	public SysSystemAttributeMapping getAuthenticationAttribute(UUID systemId) {
		// authentication attribute is only from provisioning operation type
		SysSystemAttributeMapping attr = this.repository.findAuthenticationAttribute(systemId, SystemOperationType.PROVISIONING);
		// defensive, if authentication attribute don't exists find attribute flagged as UID
		if (attr == null) {
			return this.repository.findUidAttribute(systemId, SystemOperationType.PROVISIONING);
		}
		return attr;
	}
	
	/**
	 * Find value for this mapped attribute by property name. Returned value can be list of objects. Returns transformed value.
	 * 
	 * @param uid
	 * @param entity
	 * @param attributeHandling
	 * @param idmValue
	 * @return
	 * @throws IntrospectionException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	@Override
	public Object getAttributeValue(AbstractEntity entity, AttributeMapping attributeHandling) {
		Object idmValue = null;
		//
		if (attributeHandling.isExtendedAttribute()) {
			List<AbstractFormValue<FormableEntity>> formValues = formService.getValues((FormableEntity) entity, attributeHandling.getIdmPropertyName());
			if (formValues.isEmpty()) {
				idmValue = null;
			} else if(attributeHandling.getSchemaAttribute().isMultivalued()){
				// Multiple value extended attribute
				List<Object> values = new ArrayList<>();
				formValues.stream().forEachOrdered(formValue -> {
					values.add(formValue.getValue());
				});
				idmValue = values;
			} else {
				// Single value extended attribute
				AbstractFormValue<FormableEntity> formValue = formValues.get(0);
				if (formValue.isConfidential()) {
					idmValue = formService.getConfidentialPersistentValue(formValue);
				} else {
					idmValue = formValue.getValue();
				}
			}
		}
		// Find value from entity
		else if (attributeHandling.isEntityAttribute()) {
			if (attributeHandling.isConfidentialAttribute()) {
				// If is attribute isConfidential, then we will find value in
				// secured storage
				idmValue = confidentialStorage.getGuardedString(entity, attributeHandling.getIdmPropertyName());
			} else {
				try {
					// We will search value directly in entity by property name
					idmValue = EntityUtilities.getEntityValue(entity, attributeHandling.getIdmPropertyName());
				} catch (IntrospectionException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | ProvisioningException o_O) {
					throw new ProvisioningException(AccResultCode.PROVISIONING_IDM_FIELD_NOT_FOUND,
							ImmutableMap.of("property", attributeHandling.getIdmPropertyName(), "entityType", entity.getClass()), o_O);
				}
			}
		} else {
			// If Attribute value is not in entity nor in extended attribute, then idmValue is null.
			// It means attribute is static ... we will call transformation to resource.
		}
		return this.transformValueToResource(idmValue, attributeHandling, entity);
	}
	
	@Override
	public String generateUid(AbstractEntity entity, SysSystemAttributeMapping uidAttribute){
		Object uid = this.getAttributeValue(entity, uidAttribute);
		if(uid == null) {
			throw new ProvisioningException(AccResultCode.PROVISIONING_GENERATED_UID_IS_NULL,
					ImmutableMap.of("system", uidAttribute.getSystemMapping().getSystem().getName()));
		}
		if (!(uid instanceof String)) {
			throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_UID_IS_NOT_STRING,
					ImmutableMap.of("uid", uid));
		}
		return (String)uid;
	}
	
	@Override
	public SysSystemAttributeMapping getUidAttribute(List<SysSystemAttributeMapping> mappedAttributes, SysSystem system) {
		List<SysSystemAttributeMapping> schemaHandlingAttributesUid = mappedAttributes.stream()
				.filter(attribute -> {
					return !attribute.isDisabledAttribute() && attribute.isUid();
				}).collect(Collectors.toList());

		if (schemaHandlingAttributesUid.size() > 1) {
			throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_MORE_UID,
					ImmutableMap.of("system", system.getName()));
		}
		if (schemaHandlingAttributesUid.isEmpty()) {
			throw new ProvisioningException(AccResultCode.PROVISIONING_ATTRIBUTE_UID_NOT_FOUND,
					ImmutableMap.of("system", system.getName()));
		}
		return schemaHandlingAttributesUid.get(0);
	}

}
