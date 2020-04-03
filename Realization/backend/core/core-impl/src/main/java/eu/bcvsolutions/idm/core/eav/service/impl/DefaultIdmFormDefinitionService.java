package eu.bcvsolutions.idm.core.eav.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.dto.ExportDescriptorDto;
import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleFilter;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.AbstractEventableDtoService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.ExportManager;
import eu.bcvsolutions.idm.core.api.service.IdmRoleService;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.api.utils.EntityUtils;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.filter.IdmFormAttributeFilter;
import eu.bcvsolutions.idm.core.eav.api.dto.filter.IdmFormDefinitionFilter;
import eu.bcvsolutions.idm.core.eav.api.entity.FormableEntity;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormAttributeService;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormDefinitionService;
import eu.bcvsolutions.idm.core.eav.entity.IdmFormAttribute_;
import eu.bcvsolutions.idm.core.eav.entity.IdmFormDefinition;
import eu.bcvsolutions.idm.core.eav.entity.IdmFormDefinition_;
import eu.bcvsolutions.idm.core.eav.repository.IdmFormDefinitionRepository;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.security.api.dto.AuthorizableType;

/**
 * Default implementation of form definition service
 * 
 * @author Radek Tomiška
 *
 */
public class DefaultIdmFormDefinitionService 
		extends AbstractEventableDtoService<IdmFormDefinitionDto, IdmFormDefinition, IdmFormDefinitionFilter> 
		implements IdmFormDefinitionService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultIdmFormDefinitionService.class);

	private final IdmFormDefinitionRepository formDefinitionRepository;
	//
	@Autowired private IdmFormAttributeService formAttributeService;
	@Autowired private LookupService lookupService;
	@Autowired private IdmRoleService roleService;

	@Autowired
	public DefaultIdmFormDefinitionService(
			IdmFormDefinitionRepository formDefinitionRepository,
			EntityEventManager entityEventManager) {
		super(formDefinitionRepository, entityEventManager);
		//
		this.formDefinitionRepository = formDefinitionRepository;
	}
	
	@Override
	public AuthorizableType getAuthorizableType() {
		return new AuthorizableType(CoreGroupPermission.FORMDEFINITION, getEntityClass());
	}
	
	/**
	 * Fill default definition code and name, if no code / name is given
	 */
	@Override
	@Transactional
	public IdmFormDefinitionDto saveInternal(IdmFormDefinitionDto dto) {
		if (StringUtils.isEmpty(dto.getCode())) {
			dto.setMain(true);
			dto.setCode(DEFAULT_DEFINITION_CODE);
		}
		if (StringUtils.isEmpty(dto.getName())) {
			dto.setName(dto.getCode());
		}
		if (dto.isMain()) {
			IdmFormDefinitionDto mainDefinition = findOneByMain(dto.getType());
			if (mainDefinition != null && !mainDefinition.getId().equals(dto.getId())) {
				mainDefinition.setMain(false);
				save(mainDefinition);
			}
		}
		return super.saveInternal(dto);
	}
	
	@Override
	protected IdmFormDefinitionDto toDto(IdmFormDefinition entity, IdmFormDefinitionDto dto) {
		dto = super.toDto(entity, dto);
		if (dto != null) {
			if (!dto.isTrimmed()) {
				// set mapped attributes
				// TODO: this is dangerous ... permission are not propagated lower - AUTOCOMPLETE permission on attributes by default?
				IdmFormAttributeFilter filter = new IdmFormAttributeFilter();
				filter.setDefinitionId(dto.getId());
				dto.setFormAttributes(
						formAttributeService
						.find(filter, getPageableAll(Sort.by(IdmFormAttribute_.seq.getName(), IdmFormAttribute_.name.getName())))
						.getContent());
				LOG.trace("Form attributes were loaded for definition [{},{}]", dto.getType(), dto.getCode());
			}
		}
		return dto;
	}
	
	@Override
	@Transactional
	public void deleteInternal(IdmFormDefinitionDto dto) {
		// delete all attributes in definition
		IdmFormAttributeFilter filter = new IdmFormAttributeFilter();
		filter.setDefinitionId(dto.getId());
		formAttributeService.find(filter, null).forEach(formAttribute -> {
			formAttributeService.delete(formAttribute);
		});
		//
		if(dto.getId() != null) {
			IdmRoleFilter roleFilter = new IdmRoleFilter();
			roleFilter.setAttributeFormDefinitionId(dto.getId());
			
			List<IdmRoleDto> roles = roleService.find(roleFilter, PageRequest.of(0, 1)).getContent();
			if(roles.size() > 0) {
				throw new ResultCodeException(CoreResultCode.FORM_DEFINITION_DELETE_FAILED_ROLE, ImmutableMap.of("definition", dto.getCode(), "role", roles.get(0).getCode()));
			}
		}
		super.deleteInternal(dto);
	}
	
	
	@Override
	protected List<Predicate> toPredicates(Root<IdmFormDefinition> root, CriteriaQuery<?> query,
			CriteriaBuilder builder, IdmFormDefinitionFilter filter) {
		List<Predicate> predicates = super.toPredicates(root, query, builder, filter);
		// quick - "fulltext"
		if (StringUtils.isNotEmpty(filter.getText())) {
			predicates.add(builder.or(
					builder.like(builder.lower(root.get(IdmFormDefinition_.type)), "%" + filter.getText().toLowerCase() + "%"),
					builder.like(builder.lower(root.get(IdmFormDefinition_.code)), "%" + filter.getText().toLowerCase() + "%"),
					builder.like(builder.lower(root.get(IdmFormDefinition_.name)), "%" + filter.getText().toLowerCase() + "%"),
					builder.like(builder.lower(root.get(IdmFormDefinition_.description)), "%" + filter.getText().toLowerCase() + "%")			
					));
		}
		//
		List<String> types = filter.getTypes();
		if (CollectionUtils.isNotEmpty(types)) {
			predicates.add(root.get(IdmFormDefinition_.type).in(types));
		}
		//
		if (StringUtils.isNotEmpty(filter.getCode())) {
			predicates.add(builder.equal(root.get(IdmFormDefinition_.code), filter.getCode()));
		}
		//
		if (StringUtils.isNotEmpty(filter.getName())) {
			predicates.add(builder.equal(root.get(IdmFormDefinition_.name), filter.getName()));
		}
		//
		if (filter.getMain() != null) {
			predicates.add(builder.equal(root.get(IdmFormDefinition_.main), filter.getMain()));
		}
		return predicates;
	}
	
	@Override
	@Transactional(readOnly = true)
	public IdmFormDefinitionDto findOneByTypeAndCode(String type, String code) {
		return toDto(formDefinitionRepository.findOneByTypeAndCode(type, code != null ? code : DEFAULT_DEFINITION_CODE));
	}
	
	@Override
	@Transactional(readOnly = true)
	public IdmFormDefinitionDto findOneByMain(String type) {
		return toDto(formDefinitionRepository.findOneByTypeAndMainIsTrue(type));
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<IdmFormDefinitionDto> findAllByType(String type) {
		return toDtos(formDefinitionRepository.findAllByType(type), true);
	}

	@Override
	@Transactional
	public IdmFormDefinitionDto updateDefinition(Class<? extends Identifiable> ownerType, String definitionCode, List<IdmFormAttributeDto> attributes) {
		Assert.notNull(ownerType, "Owner type is required.");
		//
		IdmFormDefinitionDto definition = new IdmFormDefinitionDto();
		definition.setType(getOwnerType(ownerType));
		definition.setModule(EntityUtils.getModule(ownerType));
		definition.setCode(definitionCode);
		definition.setFormAttributes(attributes);
		//
		return updateDefinition(definition);
	}
	
	@Override
	@Transactional
	public IdmFormDefinitionDto updateDefinition(String definitionType, String definitionCode, List<IdmFormAttributeDto> attributes) {
		Assert.notNull(definitionType, "Definition type is required.");
		//
		IdmFormDefinitionDto definition = new IdmFormDefinitionDto();
		definition.setType(definitionType);
		definition.setCode(definitionCode);
		definition.setFormAttributes(attributes);
		//
		return updateDefinition(definition);
	}
	
	@Override
	@Transactional
	public IdmFormDefinitionDto updateDefinition(IdmFormDefinitionDto definition) {
		Assert.notNull(definition, "Form definition is required.");
		Assert.notNull(definition.getType(), "Form definition type is required.");
		Assert.notNull(definition.getCode(), "Form definition code is required.");
		//
		IdmFormDefinitionDto formDefinition = findOneByTypeAndCode(definition.getType(), definition.getCode());
		if (formDefinition == null) {
			formDefinition = new IdmFormDefinitionDto();
			formDefinition.setType(definition.getType());
			formDefinition.setCode(definition.getCode());
			formDefinition.setName(definition.getName());
			formDefinition.setDescription(definition.getDescription());
			formDefinition.setModule(definition.getModule());
			// 
			formDefinition = save(formDefinition);
		}
		//
		List<IdmFormAttributeDto> attributes = definition.getFormAttributes();
		if (attributes == null || attributes.isEmpty()) {
			// delete attributes is not supported - its incompatible change
			// change script has to be provided
			return formDefinition;
		}
		// upgrade definition
		boolean changed = false;
		Short seq = 0;
		for(IdmFormAttributeDto attribute : attributes) {
			// update seq - attributes can be simply given in different order
			if (attribute.getSeq() == null) {
				attribute.setSeq(seq);
			}
			IdmFormAttributeDto savedAttribute = formAttributeService.findAttribute(formDefinition.getType(), formDefinition.getCode(), attribute.getCode());
			if (savedAttribute == null) {
				savedAttribute = attribute;
				savedAttribute.setFormDefinition(formDefinition.getId());
				if (savedAttribute.getSeq() == null) {
					savedAttribute.setSeq(seq);
				}
				//
				formAttributeService.save(savedAttribute);
				changed = true;
			} else {
				// throw exception, if incompatible change was found
				checkIncompatibleChanges(formDefinition, savedAttribute, attribute);
				// save compatible changes
				if(compareCompatibleChanges(savedAttribute, attribute) != 0) {
					// update attribute - compatible changes
					savedAttribute.setSeq(attribute.getSeq());
					savedAttribute.setName(attribute.getName());
					savedAttribute.setFaceType(attribute.getFaceType());
					savedAttribute.setReadonly(attribute.isReadonly());
					savedAttribute.setRequired(attribute.isRequired());
					savedAttribute.setDefaultValue(attribute.getDefaultValue());
					savedAttribute.setDescription(attribute.getDescription());
					savedAttribute.setMultiple(attribute.isMultiple());
					savedAttribute.setPlaceholder(attribute.getPlaceholder());
					savedAttribute.setUnmodifiable(attribute.isUnmodifiable());
					formAttributeService.save(savedAttribute);
					changed = true;
				}
			}
			seq++;
		}
		if (changed) {
			formDefinition = get(formDefinition.getId());
		}
		return formDefinition;
	}
	
	@Override
	public boolean isFormable(Class<? extends Identifiable> ownerType) {
		return getFormableOwnerType(ownerType) != null;
	}
	
	@Override
	public String getOwnerType(Identifiable owner) {
		Assert.notNull(owner, "Owner is required.");
		//
		return getOwnerType(owner.getClass());
	}
	
	@Override
	public String getOwnerType(Class<? extends Identifiable> ownerType) {
		Assert.notNull(ownerType, "Owner type is required.");
		//
		// dto class was given
		Class<? extends FormableEntity> ownerEntityType = getFormableOwnerType(ownerType);
		if (ownerEntityType == null) {
			throw new IllegalArgumentException(String.format("Owner type [%s] has to generatize [FormableEntity]", ownerType));
		}
		return ownerEntityType.getCanonicalName();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends FormableEntity> getFormableOwnerType(Class<? extends Identifiable> ownerType) {
		Assert.notNull(ownerType, "Owner type is required!");
		// formable entity class was given
		if (FormableEntity.class.isAssignableFrom(ownerType)) {
			return (Class<? extends FormableEntity>) ownerType;
		}
		// dto class was given
		Class<?> ownerEntityType = lookupService.getEntityClass(ownerType);
		if (FormableEntity.class.isAssignableFrom(ownerEntityType)) {
			return (Class<? extends FormableEntity>) ownerEntityType;
		}
		return null;
	}
	
	@Override
	public void exportOnlyDefinition(UUID id, IdmExportImportDto batch) {
		IdmFormDefinitionDto definition = internalExport(id);
		definition.setFormAttributes(Lists.newArrayList());
		
		batch.getExportedDtos().add(definition);
		ExportDescriptorDto exportDescriptorDto = new ExportDescriptorDto(definition.getClass());

		if (!batch.getExportOrder().contains(exportDescriptorDto)) {
			batch.getExportOrder().add(exportDescriptorDto);
		}
		// super.export(id, batch);
	}
	
	
	@Override
	public void export(UUID id, IdmExportImportDto batch) {
		super.export(id, batch);
		
		// Export form attributes
		IdmFormAttributeFilter systemMappingFilter = new IdmFormAttributeFilter();
		systemMappingFilter.setDefinitionId(id);
		List<IdmFormAttributeDto> attributes = formAttributeService.find(systemMappingFilter, null).getContent();

		if (attributes.isEmpty()) {
			formAttributeService.export(ExportManager.BLANK_UUID, batch);
		}
		attributes.forEach(mapping -> {
			formAttributeService.export(mapping.getId(), batch);
			// Authoritative mode is not set here only parent field could be sets.
			ExportDescriptorDto descriptor = getExportManager().getDescriptor(batch, mapping.getClass());
			if (descriptor != null) {
				descriptor.getParentFields().add(IdmFormAttribute_.formDefinition.getName());
			}
		});
	}
	
	/**
	 * Attribute's fileds persistentType, confidential cannot be updated automatically - provide change script, or create new definition (~version)
	 * 
	 * @param formDefinition
	 * @param savedAttribute
	 * @param attribute
	 */
	private void checkIncompatibleChanges(IdmFormDefinitionDto formDefinition, IdmFormAttributeDto savedAttribute, IdmFormAttributeDto attribute) {
		Map<String, Object> parameters = new LinkedHashMap<>();
		parameters.put("formDefinitionType", formDefinition.getType());
		parameters.put("formDefinitionCode", formDefinition.getCode());
		parameters.put("formAttributeCode", savedAttribute.getCode());
		//
		if (savedAttribute.getPersistentType() != attribute.getPersistentType()) {
			parameters.put("field", "persistentType");
			parameters.put("oldValue", savedAttribute.getPersistentType().name());
			parameters.put("newValue", attribute.getPersistentType().name());
			//
			throw new ResultCodeException(CoreResultCode.FORM_DEFINITION_INCOMPATIBLE_CHANGE, parameters);
		}
		if (savedAttribute.isConfidential() != attribute.isConfidential()) {
			parameters.put("field", "confidential");
			parameters.put("oldValue", Boolean.valueOf(savedAttribute.isConfidential()).toString());
			parameters.put("newValue", Boolean.valueOf(attribute.isConfidential()).toString());
			//
			throw new ResultCodeException(CoreResultCode.FORM_DEFINITION_INCOMPATIBLE_CHANGE, parameters);
		}
	}
	
	/**
	 * Compare attributes, which can be update
	 * 
	 * @param savedAttribute
	 * @param attribute
	 * @return
	 */
	private int compareCompatibleChanges(IdmFormAttributeDto savedAttribute, IdmFormAttributeDto attribute) {
		CompareToBuilder builder = new CompareToBuilder();
		//
		if (attribute.getSeq() != null) {
			builder.append(savedAttribute.getSeq(), attribute.getSeq());
		}
		builder.append(savedAttribute.getName(), attribute.getName());
		builder.append(savedAttribute.getFaceType(), attribute.getFaceType());
		builder.append(savedAttribute.isReadonly(), attribute.isReadonly());
		builder.append(savedAttribute.isRequired(), attribute.isRequired());
		builder.append(savedAttribute.getDefaultValue(), attribute.getDefaultValue());
		builder.append(savedAttribute.getDescription(), attribute.getDescription());
		builder.append(savedAttribute.isMultiple(), attribute.isMultiple());
		builder.append(savedAttribute.getPlaceholder(), attribute.getPlaceholder());
		builder.append(savedAttribute.isUnmodifiable(), attribute.isUnmodifiable());
		//
		return builder.toComparison();
	}
}
