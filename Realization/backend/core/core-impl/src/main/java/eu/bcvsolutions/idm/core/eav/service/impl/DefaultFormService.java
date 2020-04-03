package eu.bcvsolutions.idm.core.eav.service.impl;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.domain.ConfigurationClass;
import eu.bcvsolutions.idm.core.api.domain.ConfigurationClassProperty;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.Identifiable;
import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.ExportDescriptorDto;
import eu.bcvsolutions.idm.core.api.dto.FormableDto;
import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;
import eu.bcvsolutions.idm.core.api.event.CoreEvent;
import eu.bcvsolutions.idm.core.api.event.CoreEvent.CoreEventType;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.event.EventContext;
import eu.bcvsolutions.idm.core.api.exception.CoreException;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.api.utils.EntityUtils;
import eu.bcvsolutions.idm.core.eav.api.domain.PersistentType;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.dto.InvalidFormAttributeDto;
import eu.bcvsolutions.idm.core.eav.api.dto.filter.IdmFormAttributeFilter;
import eu.bcvsolutions.idm.core.eav.api.dto.filter.IdmFormDefinitionFilter;
import eu.bcvsolutions.idm.core.eav.api.dto.filter.IdmFormValueFilter;
import eu.bcvsolutions.idm.core.eav.api.entity.FormableEntity;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.eav.api.service.FormValueService;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormAttributeService;
import eu.bcvsolutions.idm.core.eav.api.service.IdmFormDefinitionService;
import eu.bcvsolutions.idm.core.eav.entity.IdmForm;
import eu.bcvsolutions.idm.core.eav.entity.IdmFormAttribute_;
import eu.bcvsolutions.idm.core.eav.entity.IdmFormDefinition_;
import eu.bcvsolutions.idm.core.ecm.api.dto.IdmAttachmentDto;
import eu.bcvsolutions.idm.core.ecm.api.service.AttachmentManager;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.api.utils.PermissionUtils;

/**
 * Work with form definitions, attributes and their values
 * 
 * @author Radek Tomiška
 *
 */
public class DefaultFormService implements FormService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultFormService.class);
	//
	private final PluginRegistry<FormValueService<?>, Class<?>> formValueServices;
	//
	@Autowired private IdmFormDefinitionService formDefinitionService;
	@Autowired private IdmFormAttributeService formAttributeService;
	@Autowired private EntityEventManager entityEventManager;
	@Autowired private LookupService lookupService;
	@Autowired private AttachmentManager attachmentManager;

	@Autowired
	public DefaultFormService(List<? extends FormValueService<?>> formValueServices) {
		this.formValueServices = OrderAwarePluginRegistry.create(formValueServices);
	}
	
	@Override
	@Transactional(readOnly = true)
	public IdmFormDefinitionDto getDefinition(UUID definitionId, BasePermission... permission) {
		return formDefinitionService.get(definitionId, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public IdmFormDefinitionDto getDefinition(String type, BasePermission... permission) {
		return this.getDefinition(type, null, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormDefinitionDto> getDefinitions(String type, BasePermission... permission) {
		IdmFormDefinitionFilter filter = new IdmFormDefinitionFilter();
		filter.setType(type);
		//
		return formDefinitionService
				.find(
					filter,
					PageRequest.of(0, Integer.MAX_VALUE, Sort.by(IdmFormDefinition_.code.getName())),
					permission)
				.getContent();
	}

	@Override
	@Transactional(readOnly = true)
	public IdmFormDefinitionDto getDefinition(String type, String code, BasePermission... permission) {
		IdmFormDefinitionDto formDefinition = null; 
		if (StringUtils.isEmpty(code)) {
			formDefinition = formDefinitionService.findOneByMain(type);
		} else {
			formDefinition = formDefinitionService.findOneByTypeAndCode(type, code);
		}
		return formDefinitionService.checkAccess(formDefinition, permission);	}

	@Override
	@Transactional(readOnly = true)
	public IdmFormDefinitionDto getDefinition(Class<? extends Identifiable> ownerType, BasePermission... permission) {
		return getDefinition(ownerType, null, permission);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<IdmFormDefinitionDto> getDefinitions(Identifiable owner, BasePermission... permission) {
		Assert.notNull(owner, "Owner is required.");
		//
		return getDefinitions(owner.getClass(), permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormDefinitionDto> getDefinitions(Class<? extends Identifiable> ownerType, BasePermission... permission) {
		return getDefinitions(getDefaultDefinitionType(ownerType), permission);
	}
	
	@Override
	@Transactional(readOnly = true)
	public IdmFormDefinitionDto getDefinition(Class<? extends Identifiable> ownerType, String code, BasePermission... permission) {
		Assert.notNull(ownerType, "Owner type is required.");
		//
		return getDefinition(getDefaultDefinitionType(ownerType), code, permission);
	}

	/**
	 * Return default form definition, if no definition was given. Returns given
	 * definition otherwise.
	 * 
	 * @param ownerType
	 * @param formDefinitionId
	 * @return
	 * @throws IllegalArgumentException
	 *             if default definition does not exist and no definition was given.
	 */
	private UUID checkDefaultDefinition(Class<? extends Identifiable> ownerType, UUID formDefinitionId) {
		if (formDefinitionId != null) {
			return formDefinitionId;
		}
		// values from default form definition
		return checkDefaultDefinition(ownerType, (IdmFormDefinitionDto) null).getId();
	}

	/**
	 * Return default form definition, if no definition was given. Returns given
	 * definition otherwise.
	 * 
	 * @param ownerType
	 * @param formDefinition
	 * @return
	 * @throws IllegalArgumentException if default definition does not exist and no definition was given.
	 */
	private IdmFormDefinitionDto checkDefaultDefinition(
			Class<? extends Identifiable> ownerType,
			IdmFormDefinitionDto formDefinition) {
		if (formDefinition == null) {
			// values from default form definition
			formDefinition = getDefinition(ownerType);
			Assert.notNull(formDefinition, MessageFormat.format(
					"Default form definition for ownerType [{0}] not found and is required, fix application configuration!",
					ownerType));
		}
		return formDefinition;
	}

	@Override
	public String getDefaultDefinitionType(Class<? extends Identifiable> ownerType) {
		return formDefinitionService.getOwnerType(ownerType);
	}

	@Override
	public boolean isFormable(Class<? extends Identifiable> ownerType) {
		return formDefinitionService.isFormable(ownerType);
	}
	
	@Override
	@Transactional(readOnly = true)
	public IdmFormAttributeDto getAttribute(UUID attributeId, BasePermission... permission) {
		return formAttributeService.get(attributeId, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public IdmFormAttributeDto getAttribute(Class<? extends Identifiable> ownerType, String attributeCode, BasePermission... permission) {
		return getAttribute(ownerType, null, attributeCode, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public IdmFormAttributeDto getAttribute(
			Class<? extends Identifiable> ownerType,
			String definitionCode,
			String attributeCode,
			BasePermission... permission) {
		Assert.notNull(ownerType, "Owner type is required.");
		Assert.hasLength(attributeCode, "Form attribute code is required.");
		//
		IdmFormDefinitionDto definition = getDefinition(ownerType, definitionCode);
		Assert.notNull(definition, "Definition is required.");
		//
		return formAttributeService.findAttribute(
				getDefaultDefinitionType(ownerType), 
				definition.getCode(),
				attributeCode,
				permission);
	}
	
	@Override
	@Transactional(readOnly = true)
	public IdmFormAttributeDto getAttribute(
			IdmFormDefinitionDto formDefinition,
			String attributeCode,
			BasePermission... permission) {
		Assert.notNull(formDefinition, "Definition is required.");
		Assert.hasLength(attributeCode, "Form attribute code is required.");
		//
		return formAttributeService.findAttribute(
				formDefinition.getType(),
				formDefinition.getCode(),
				attributeCode,
				permission);
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<IdmFormAttributeDto> getAttributes(IdmFormDefinitionDto formDefinition, BasePermission... permission) {
		Assert.notNull(formDefinition, "Form definition is required.");
		//
		IdmFormAttributeFilter filter = new IdmFormAttributeFilter();
		filter.setDefinitionType(formDefinition.getType());
		filter.setDefinitionCode(formDefinition.getCode());
		//
		return formAttributeService.find(filter, PageRequest.of(0, Integer.MAX_VALUE, Sort.by(IdmFormAttribute_.seq.getName())), permission).getContent();
	}

	@Override
	@Transactional
	public IdmFormDefinitionDto saveDefinition(IdmFormDefinitionDto formDefinition, BasePermission... permission) {
		return formDefinitionService.save(formDefinition, permission);
	}
	
	@Override
	@Transactional
	public IdmFormDefinitionDto createDefinition(
			String type,
			String code,
			List<IdmFormAttributeDto> formAttributes,
			BasePermission... permission) {
		return createDefinition(type, code, null, formAttributes, permission);
	}

	@Override
	@Transactional
	public IdmFormDefinitionDto createDefinition(
			String type,
			String code,
			String module,
			List<IdmFormAttributeDto> formAttributes,
			BasePermission... permission) {
		Assert.hasLength(type, "Form definition type is required.");
		//
		// create definition
		IdmFormDefinitionDto formDefinition = new IdmFormDefinitionDto();
		formDefinition.setType(type);
		formDefinition.setCode(code);
		formDefinition.setModule(module);
		formDefinition = formDefinitionService.save(formDefinition, permission);
		//
		// and their attributes
		if (formAttributes != null) {
			Short seq = 0;
			for (IdmFormAttributeDto formAttribute : formAttributes) {
				// default attribute order
				if (formAttribute.getSeq() == null) {
					formAttribute.setSeq(seq);
					seq++;
				}
				formAttribute.setFormDefinition(formDefinition.getId());
				formAttribute = formAttributeService.save(formAttribute, permission);
			}
			formDefinition = formDefinitionService.get(formDefinition);
		}
		return formDefinition;
	}

	@Override
	@Transactional
	public IdmFormDefinitionDto createDefinition(
			Class<? extends Identifiable> ownerType,
			List<IdmFormAttributeDto> formAttributes,
			BasePermission... permission) {
		return createDefinition(ownerType, null, formAttributes, permission);
	}

	@Override
	@Transactional
	public IdmFormDefinitionDto createDefinition(
			Class<? extends Identifiable> ownerType,
			String code,
			List<IdmFormAttributeDto> formAttributes,
			BasePermission... permission) {
		Assert.notNull(ownerType, "Owner type is required.");
		//
		return createDefinition(getDefaultDefinitionType(ownerType), code, EntityUtils.getModule(ownerType), formAttributes, permission);
	}

	@Override
	@Transactional
	public IdmFormAttributeDto saveAttribute(IdmFormAttributeDto attribute, BasePermission... permission) {
		Assert.notNull(attribute, "Form attribute type is required.");
		Assert.notNull(attribute.getFormDefinition(),
				String.format("Form definition for attribute [%s] is required.", attribute.getCode()));
		//
		return formAttributeService.save(attribute, permission);
	}

	@Override
	@Transactional
	public IdmFormAttributeDto saveAttribute(Class<? extends Identifiable> ownerType, IdmFormAttributeDto attribute, BasePermission... permission) {
		Assert.notNull(attribute, "Form attribute type is required.");
		attribute.setFormDefinition(checkDefaultDefinition(ownerType, attribute.getFormDefinition()));
		//
		return saveAttribute(attribute, permission);
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			Identifiable owner,
			IdmFormDefinitionDto formDefinition,
			List<IdmFormValueDto> values,
			BasePermission... permission) {
		return saveFormInstance(owner, formDefinition, values, permission).getValues();
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			Identifiable owner,
			UUID formDefinitionId,
			List<IdmFormValueDto> values,
			BasePermission... permission) {
		IdmFormDefinitionDto formDefinition = null;
		if (formDefinitionId != null) {
			formDefinition = formDefinitionService.get(formDefinitionId);
			if (formDefinition == null) {
				throw new ResultCodeException(CoreResultCode.NOT_FOUND,
						ImmutableMap.of("formDefinition", formDefinitionId));
			}
		}
		return saveFormInstance(owner, formDefinition, values, permission).getValues();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Only given form attributes by the given values will be saved. Other attributes will be left untouched.
	 * 
	 * TODO: validations by given form definition? I don't think, it will not be
	 * useful in synchronization etc. - only FE validations will be enough ...
	 */
	@Override
	@Transactional
	public IdmFormInstanceDto saveFormInstance(
			Identifiable owner,
			IdmFormDefinitionDto formDefinition,
			List<IdmFormValueDto> newValues,
			BasePermission... permission) {
		FormableEntity ownerEntity = getOwnerEntity(owner);
		Assert.notNull(ownerEntity, "Form values owner is required.");
		formDefinition = checkDefaultDefinition(ownerEntity.getClass(), formDefinition);
		IdmFormInstanceDto formInstance = new IdmFormInstanceDto(ownerEntity, formDefinition, newValues);
		//
		CoreEvent<IdmFormInstanceDto> event = new CoreEvent<IdmFormInstanceDto>(CoreEventType.UPDATE, formInstance);
		// check permissions - check access to filled form values
		event.setPermission(permission);
		// publish event for save form instance - see {@link #saveFormInstance(EntityEvent<IdmFormInstanceDto>)}
		return entityEventManager.process(event).getContent();
	}
	
	@Override
	@Transactional
	public EventContext<IdmFormInstanceDto> publish(EntityEvent<IdmFormInstanceDto> event, BasePermission... permission) {
		return publish(event, (EntityEvent<?>) null, permission);
	}
	
	@Override
	@Transactional
	public EventContext<IdmFormInstanceDto> publish(EntityEvent<IdmFormInstanceDto> event, EntityEvent<?> parentEvent, BasePermission... permission) {
		Assert.notNull(event, "Event must be not null!");
		Assert.notNull(event.getContent(), "Content (dto) in event must be not null!");
		// check permissions - check access to filled form values
		event.setPermission(permission);
		//
		return entityEventManager.process(event, parentEvent);
	}
	
	@Override
	@Transactional
	public IdmFormInstanceDto saveFormInstance(EntityEvent<IdmFormInstanceDto> event) {
		IdmFormInstanceDto formInstance = event.getContent();
		FormableEntity ownerEntity = getOwnerEntity(formInstance.getOwnerId(), formInstance.getOwnerType());
		Assert.notNull(ownerEntity, "Form values owner is required.");
		IdmFormDefinitionDto formDefinition = checkDefaultDefinition(formInstance.getOwnerType(), formInstance.getFormDefinition());
		//
		FormValueService<FormableEntity> formValueService = getFormValueService(formInstance.getOwnerType());
		//
		Map<UUID, Map<UUID, IdmFormValueDto>> previousValues = new HashMap<>(); // values by attributes
		formValueService.getValues(ownerEntity, formDefinition).forEach(formValue -> {
			if (!previousValues.containsKey(formValue.getFormAttribute())) {
				previousValues.put(formValue.getFormAttribute(), new LinkedHashMap<>()); // sort by seq
			}
			previousValues.get(formValue.getFormAttribute()).put(formValue.getId(), formValue);
		});
		//
		List<IdmFormValueDto> results = new ArrayList<>();
		for (Entry<String, List<IdmFormValueDto>> attributeEntry : formInstance.toValueMap().entrySet()) {
			IdmFormAttributeDto attribute = formInstance.getMappedAttributeByCode(attributeEntry.getKey());
			List<IdmFormValueDto> attributePreviousValues = new ArrayList<>();
			if (previousValues.containsKey(attribute.getId())) {
				attributePreviousValues.addAll(previousValues.get(attribute.getId()).values());
			}
			results.addAll(
					saveAttributeValues(
						ownerEntity, 
						attribute, 
						attributePreviousValues,
						attributeEntry.getValue(),
						event.getPermission()));
		}
		//
		return new IdmFormInstanceDto(ownerEntity, formDefinition, results);
	}
	
	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			Identifiable owner,
			IdmFormAttributeDto attribute,
			List<Serializable> persistentValues,
			BasePermission... permission) {
		Assert.notNull(owner, "Form values owner is required.");
		Assert.notNull(owner.getId(), "Owner id is required.");
		Assert.notNull(attribute, "Form attribute definition is required.");
		//
		FormableEntity ownerEntity = getOwnerEntity(owner);
		FormValueService<FormableEntity> formValueService = getFormValueService(ownerEntity);
		//
		// get previous (old) values
		List<IdmFormValueDto> previousValues = formValueService.getValues(ownerEntity, attribute);
		// prepare new values
		List<IdmFormValueDto> newValues = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(persistentValues)) {
			for (short seq = 0; seq < persistentValues.size(); seq++) {
				IdmFormValueDto newValue = new IdmFormValueDto();
				newValue.setOwnerAndAttribute(ownerEntity, attribute);
				newValue.setValue(persistentValues.get(seq));
				newValue.setSeq(seq);
				newValues.add(newValue);
			}
		}
		return saveAttributeValues(ownerEntity, attribute, previousValues, newValues, permission);
	}
	
	/**
	 * Save single attribute values. Main business logic is here => all method ends here.
	 * 
	 * 
	 * @param ownerEntity
	 * @param attribute
	 * @param previousValues
	 * @param newValues
	 * @param permission
	 * @return
	 */
	private List<IdmFormValueDto> saveAttributeValues(
			FormableEntity ownerEntity,
			IdmFormAttributeDto attribute,
			List<IdmFormValueDto> previousValues,
			List<IdmFormValueDto> newValues,
			BasePermission... permission) {
		//
		IdmFormDefinitionDto formDefinition = formDefinitionService.get(attribute.getFormDefinition());
		FormValueService<FormableEntity> formValueService = getFormValueService(ownerEntity);
		List<IdmFormValueDto> results = new ArrayList<>();
		Map<UUID, IdmFormValueDto> unprocessedPreviousValues = new LinkedHashMap<>(); // ordered by seq
		if (CollectionUtils.isNotEmpty(previousValues)) {
			previousValues.forEach(previousValue -> {
				unprocessedPreviousValues.put(previousValue.getId(), previousValue);
			});
		}
		//
		if (newValues == null || newValues.isEmpty()) {
			// confidential values has to removed directly, they could not be sent with form (only changed values)
			if (!attribute.isConfidential()) {
				// delete previous attributes
				unprocessedPreviousValues.values().forEach(value -> {
					formValueService.delete(value, permission);
					if (value.getPersistentType() == PersistentType.ATTACHMENT) {
						// delete attachment - permissions are evaluated before
						attachmentManager.deleteAttachments(value.getId(), attachmentManager.getOwnerType(formValueService.getEntityClass()));
					}
					LOG.trace("FormValue [{}:{}] for owner [{}] was deleted", value.getFormAttribute(), value.getId(), ownerEntity);
				});
			}
			return results;
		}
		//
		if (!attribute.isMultiple() && newValues.size() > 1) {
			throw new IllegalArgumentException(
					MessageFormat.format("Form attribute [{0}:{1}] does not support multivalue, sent [{2}] values.",
							formDefinition.getCode(), attribute.getCode(), newValues.size()));
		}
		//
		// compare values
		IdmFormValueDto[] sortedPreviousValues = resolvePreviousValues(unprocessedPreviousValues, newValues);
		for (short index = 0; index < newValues.size(); index++) {
			IdmFormValueDto previousValue = sortedPreviousValues[index];
			IdmFormValueDto newValue = newValues.get(index);
			//
			if (previousValue == null) {
				if (newValue.getOwnerId() != null && !newValue.getOwnerId().equals(ownerEntity.getId())) {
					// owner was changed, new value will be created => prevent to move (update) value into new owner. 
					newValue.setId(null);
					DtoUtils.clearAuditFields(newValue);
					//
					// FIXME: move deep copy of attachments an confidential storage here.
				}
				newValue.setOwnerAndAttribute(ownerEntity, attribute);
				newValue.setSeq(index);
				//
				if (!newValue.isNull()) { // null values are not saved
					newValue = formValueService.save(newValue, permission);
					//
					if (newValue.getPersistentType() == PersistentType.ATTACHMENT) {
						// update attachment - set current owner, if temporary owner is given
						IdmAttachmentDto attachment = attachmentManager.get(newValue.getUuidValue());
						if (attachment != null && attachment.getOwnerType().equals(AttachmentManager.TEMPORARY_ATTACHMENT_OWNER_TYPE)) {
							attachment.setOwnerType(attachmentManager.getOwnerType(formValueService.getEntityClass()));
							attachment.setOwnerId(newValue.getId());
							attachmentManager.save(attachment); // permissions are evaluated above
						}
					}
					results.add(newValue);
					LOG.trace("FormValue [{}:{}] for owner [{}] was created.", attribute.getCode(), newValue.getId(), ownerEntity);
				}
			} else {
				//
				// we using filled value only and set her into previous value => value id is preserved 
				// the same value should not be updated
				// confidential value is always updated - only new values are sent from client
				if (newValue.isConfidential() || !previousValue.isEquals(newValue)) {
					UUID previousUuidValue = previousValue.getUuidValue();
					// set value for the previous value
					previousValue.setValues(newValue);
					// attribute persistent type could be changed
					previousValue.setOwnerAndAttribute(ownerEntity, attribute);
					previousValue.setSeq(index);
					if (!previousValue.isNull()) { // null values are not saved
						previousValue = formValueService.save(previousValue, permission);
						//
						if (previousValue.getPersistentType() == PersistentType.ATTACHMENT) {
							// fill attachments version and owners
							IdmAttachmentDto previousAttachment = previousUuidValue == null ? null : attachmentManager.get(previousUuidValue);
							// update attachment - set current owner, if temporary owner is given
							IdmAttachmentDto attachment = attachmentManager.get(previousValue.getUuidValue());
							if (attachment != null && attachment.getOwnerType().equals(AttachmentManager.TEMPORARY_ATTACHMENT_OWNER_TYPE)) {
								attachment.setOwnerType(attachmentManager.getOwnerType(formValueService.getEntityClass()));
								attachment.setOwnerId(previousValue.getId());
								if (previousAttachment != null) {
									if (previousAttachment.getParent() != null) {
										attachment.setParent(previousAttachment.getParent());
									} else {
										attachment.setParent(previousAttachment.getId());
									}
									attachment.setVersionNumber(previousAttachment.getVersionNumber() + 1);
									attachment.setVersionLabel(attachment.getVersionNumber() + ".0");
								}
								attachment = attachmentManager.save(attachment); // permissions are evaluated above
								if (previousAttachment != null) {
									previousAttachment.setNextVersion(attachment.getId());
									attachmentManager.save(previousAttachment);
								}
							}
						}
						results.add(previousValue);
						LOG.trace("FormValue [{}:{}] for owner [{}] was updated.", attribute.getCode(), previousValue.getId(), ownerEntity);
					} else {
						formValueService.delete(previousValue, permission);
						if (previousValue.getPersistentType() == PersistentType.ATTACHMENT) {
							// delete attachment - permissions are evaluated before
							attachmentManager.deleteAttachments(previousValue.getId(), attachmentManager.getOwnerType(formValueService.getEntityClass()));
						}
						LOG.trace("FormValue [{}:{}] for owner [{}] was deleted.", attribute.getCode(), previousValue.getId(), ownerEntity);
					}
				} else {
					results.add(previousValue);
					LOG.trace("FormValue [{}:{}] for owner [{}] was preserved unchanged.", attribute.getCode(), previousValue.getId(), ownerEntity);
				}
			}
		}
		// remove unprocessed values
		// confidential property will be removed too => none or all confidential values have to be given for multiple attributes
		unprocessedPreviousValues
			.values()
			.forEach(value -> {
				formValueService.delete(value, permission);
				if (value.getPersistentType() == PersistentType.ATTACHMENT) {
					// delete attachment - permissions are evaluated before
					attachmentManager.deleteAttachments(value.getId(), attachmentManager.getOwnerType(formValueService.getEntityClass()));
				}
				LOG.trace("FormValue [{}:{}] for owner [{}] was deleted", value.getFormAttribute(), value.getId(), ownerEntity);
			});

		return results;
	}
	
	/**
	 * Returns previous values sorted accordingly to given new values => previous(index) ~ new(index).
	 * Returned array length is the same as size of list with new values.
	 * if previous value is not found, then position in array will be null.
	 * 
	 * Values are paired by:
	 * - 1. by id (if new value has id)
	 * - 2. by value
	 * - 3. by seq
	 * - 4. use remaining unprocessed values
	 * 
	 * @param unprocessedPreviousValues if value is resolved, value is removed from this map (=> processed)
	 * @param newValue
	 * @return
	 */
	@Override
	public IdmFormValueDto[] resolvePreviousValues(Map<UUID, IdmFormValueDto> unprocessedPreviousValues, List<IdmFormValueDto> newValues) {
		IdmFormValueDto[] sortedPreviousValues = new IdmFormValueDto[newValues.size()];
		// by id - highest priority
		// wee need to iterate through all values
		for (int index = 0; index < newValues.size(); index++) {
			IdmFormValueDto newValue = newValues.get(index);
			if (newValue.getId() != null && unprocessedPreviousValues.containsKey(newValue.getId())) {
				sortedPreviousValues[index] = unprocessedPreviousValues.remove(newValue.getId());
			}
		}
		// by value
		// wee need to iterate through all values
		for (int index = 0; index < newValues.size(); index++) {
			if (sortedPreviousValues[index] != null) {
				// mapped by previous iteration
				continue;
			}
			IdmFormValueDto newValue = newValues.get(index);
			IdmFormValueDto previousValue = unprocessedPreviousValues
					.values()
					.stream()
					.filter(p -> p.isEquals(newValue))
					.findFirst()
					.orElse(null);
			if (previousValue != null) {
				sortedPreviousValues[index] = previousValue;
				unprocessedPreviousValues.remove(previousValue.getId());
			}
		}
		// by seq
		// wee need to iterate through all values
		for (int index = 0; index < newValues.size(); index++) {
			if (sortedPreviousValues[index] != null) {
				// mapped by previous iteration
				continue;
			}
			IdmFormValueDto newValue = newValues.get(index);
			IdmFormValueDto previousValue = unprocessedPreviousValues
					.values()
					.stream()
					.filter(p -> p.getSeq() == newValue.getSeq())
					.findFirst()
					.orElse(null);
			if (previousValue != null) {
				sortedPreviousValues[index] = previousValue;
				unprocessedPreviousValues.remove(previousValue.getId());
			}
		}
		//
		// try to fill remaining holes
		if (!unprocessedPreviousValues.isEmpty()) {
			for (int index = 0; index < sortedPreviousValues.length; index++) {
				if (sortedPreviousValues[index] != null) {
					// mapped by previous iteration
					continue;
				}
				if (unprocessedPreviousValues.isEmpty()) {
					break;
				}
				// get the first previos value
				sortedPreviousValues[index] = unprocessedPreviousValues.entrySet().iterator().next().getValue();
				unprocessedPreviousValues.remove(sortedPreviousValues[index].getId());
			}
		}
		return sortedPreviousValues;
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			Identifiable owner,
			String attributeName,
			List<Serializable> persistentValues,
			BasePermission... permission) {
		return saveValues(owner, null, attributeName, persistentValues, permission);
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			Identifiable owner,
			IdmFormDefinitionDto formDefinition,
			String attributeCode,
			List<Serializable> persistentValues,
			BasePermission... permission) {
		Assert.notNull(owner, "Form values owner is required.");
		Assert.notNull(owner.getId(), "Owner id is required.");
		Assert.hasLength(attributeCode, "Form attribute code is required.");
		formDefinition = checkDefaultDefinition(owner.getClass(), formDefinition);
		//
		return saveValues(owner, getAttribute(formDefinition, attributeCode), persistentValues, permission);
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormAttributeDto attribute,
			List<Serializable> persistentValues,
			BasePermission... permission) {
		return saveValues(getOwnerEntity(ownerId, ownerType), attribute, persistentValues, permission);
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormDefinitionDto formDefinition,
			String attributeName,
			List<Serializable> persistentValues,
			BasePermission... permission) {
		return saveValues(getOwnerEntity(ownerId, ownerType), formDefinition, attributeName, persistentValues, permission);
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormDefinitionDto formDefinition,
			List<IdmFormValueDto> values,
			BasePermission... permission) {
		return saveValues(getOwnerEntity(ownerId, ownerType), formDefinition, values, permission);
	}

	@Override
	@Transactional
	public List<IdmFormValueDto> saveValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			String attributeName,
			List<Serializable> persistentValues,
			BasePermission... permission) {
		return saveValues(getOwnerEntity(ownerId, ownerType), attributeName, persistentValues, permission);
	}
	
	@Override
	@Transactional(readOnly = true)
	public IdmFormValueDto getValue(Identifiable owner, UUID formValueId, BasePermission... permission) {
		FormValueService<FormableEntity> formValueService = getFormValueService(owner);
		//
		return formValueService.get(formValueId, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(Identifiable owner, BasePermission... permission) {
		return getValues(owner, (IdmFormDefinitionDto) null, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(Identifiable owner, UUID formDefinitionId, BasePermission... permission) {
		IdmFormDefinitionDto formDefinition = null;
		if (formDefinitionId != null) {
			formDefinition = formDefinitionService.get(formDefinitionId);
			if (formDefinition == null) {
				throw new ResultCodeException(CoreResultCode.NOT_FOUND,
						ImmutableMap.of("formDefinition", formDefinitionId));
			}
		}
		return getValues(owner, formDefinition, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(Identifiable owner, IdmFormDefinitionDto formDefinition, BasePermission... permission) {
		return getFormInstance(owner, formDefinition ,permission).getValues();
	}

	@Override
	@Transactional(readOnly = true)
	public IdmFormInstanceDto getFormInstance(Identifiable owner, BasePermission... permission) {
		return getFormInstance(owner, (IdmFormDefinitionDto) null, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public IdmFormInstanceDto getFormInstance(Identifiable owner, IdmFormDefinitionDto formDefinition, BasePermission... permission) {
		Assert.notNull(owner, "Form values owner is required.");
		Assert.notNull(owner.getId(), "Owner id is required.");
		//
		BasePermission[] permissions = PermissionUtils.trimNull(permission);
		FormableEntity ownerEntity = getOwnerEntity(owner);
		// Definition will be reloaded only if is given definition trimmed (we need to not reloading the definition in case use the sub-definition (role-attributes))
		formDefinition = checkDefaultDefinition(owner.getClass(), formDefinition);
		if (formDefinition.isTrimmed()) {
			formDefinition = getDefinition(formDefinition.getId()); // load => prevent to modify input definition
		}
		FormValueService<FormableEntity> formValueService = getFormValueService(owner);
		List<IdmFormValueDto> values = formValueService.getValues(ownerEntity, formDefinition, permission);
		IdmFormInstanceDto formInstance = new IdmFormInstanceDto(ownerEntity, formDefinition, values);
		//
		// evaluate permissions for form definition attributes by values - change attribute properties or remove attribute at all
		if (!ObjectUtils.isEmpty(permissions)) {
			Set<UUID> checkedAttributes = new HashSet<>(values.size());
			for(IdmFormValueDto value : values) {
				checkedAttributes.add(value.getFormAttribute());
				Set<String> valuePermissions = formValueService.getPermissions(value);
				if (!PermissionUtils.hasPermission(valuePermissions, IdmBasePermission.READ)) {
					// TODO: hidden?
					formInstance.getFormDefinition().removeFormAttribute(value.getFormAttribute());
				} else if (!PermissionUtils.hasPermission(valuePermissions, IdmBasePermission.UPDATE)) {
					formInstance.getMappedAttribute(value.getFormAttribute()).setReadonly(true);
				}
			}
			// evaluate permissions for new values - iterate through unprocessed attributes and check update permission
			List<IdmFormAttributeDto> formAttributes = Lists.newArrayList(formInstance.getFormDefinition().getFormAttributes());
			for(IdmFormAttributeDto formAttribute : formAttributes) {
				if (checkedAttributes.contains(formAttribute.getId())) {
					continue;
				}
				IdmFormValueDto newValue = new IdmFormValueDto();
				newValue.setOwnerAndAttribute(ownerEntity, formAttribute);
				Set<String> valuePermissions = formValueService.getPermissions(newValue);
				if (!PermissionUtils.hasPermission(valuePermissions, IdmBasePermission.READ)) {
					// TODO: hidden?
					formInstance.getFormDefinition().removeFormAttribute(newValue.getFormAttribute());
				} else if (!PermissionUtils.hasPermission(valuePermissions, IdmBasePermission.UPDATE)) {
					formAttribute.setReadonly(true);
				}
			}
		}
		//
		return formInstance;
	}
	
	@Override
	@Transactional(readOnly = true)
	public List<IdmFormInstanceDto> getFormInstances(Identifiable owner, BasePermission... permission) {
		return getDefinitions(owner, !PermissionUtils.isEmpty(permission) ? IdmBasePermission.AUTOCOMPLETE : null)
				.stream()
				.map(definition -> {
					return getFormInstance(owner, definition, permission);
				})
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(Identifiable owner, IdmFormAttributeDto attribute, BasePermission... permission) {
		Assert.notNull(owner, "Form values owner is required.");
		Assert.notNull(owner.getId(), "Owner id is required.");
		Assert.notNull(attribute, "Form attribute definition is required.");
		//
		FormableEntity ownerEntity = getOwnerEntity(owner);
		FormValueService<FormableEntity> formValueService = getFormValueService(ownerEntity);
		return formValueService.getValues(ownerEntity, attribute, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(Identifiable owner, String attributeCode, BasePermission... permission) {
		return getValues(owner, null, attributeCode, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(
			Identifiable owner,
			IdmFormDefinitionDto formDefinition,
			String attributeCode,
			BasePermission... permission) {
		Assert.notNull(owner, "Form values owner is required.");
		Assert.notNull(owner.getId(), "Owner id is required.");
		Assert.hasLength(attributeCode, "Attribute code is required");
		formDefinition = checkDefaultDefinition(owner.getClass(), formDefinition);
		//
		return getValues(owner, getAttribute(formDefinition, attributeCode), permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(UUID ownerId, Class<? extends Identifiable> ownerType, BasePermission... permission) {
		return getValues(getOwnerEntity(ownerId, ownerType), permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormDefinitionDto formDefinition,
			BasePermission... permission) {
		return getValues(getOwnerEntity(ownerId, ownerType), formDefinition, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormDefinitionDto formDefinition,
			String attributeName,
			BasePermission... permission) {
		return getValues(getOwnerEntity(ownerId, ownerType), formDefinition, attributeName, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			IdmFormAttributeDto attribute,
			BasePermission... permission) {
		return getValues(getOwnerEntity(ownerId, ownerType), attribute, permission);
	}

	@Override
	@Transactional(readOnly = true)
	public List<IdmFormValueDto> getValues(
			UUID ownerId,
			Class<? extends Identifiable> ownerType,
			String attributeName,
			BasePermission... permission) {
		return getValues(getOwnerEntity(ownerId, ownerType), attributeName, permission);
	}

	@Override
	@Transactional
	public void deleteAttribute(IdmFormAttributeDto attribute, BasePermission... permission) {
		formAttributeService.delete(attribute, permission);
	}
	
	@Override
	@Transactional
	public void deleteValue(IdmFormValueDto formValue, BasePermission... permission) {
		Assert.notNull(formValue, "Form value is required.");
		Assert.notNull(formValue.getOwnerId(), "Owner id is required.");
		Class<? extends FormableEntity> ownerType = formValue.getOwnerType();
		Assert.notNull(ownerType, "Form value owner type is required.");
		//
		FormValueService<FormableEntity> formValueService = getFormValueService(ownerType);
		formValueService.delete(formValue, permission);
	}

	@Override
	@Transactional
	public void deleteValues(Identifiable owner, BasePermission... permission) {
		deleteValues(owner, (IdmFormDefinitionDto) null, permission);
	}

	@Override
	@Transactional
	public void deleteValues(Identifiable owner, IdmFormDefinitionDto formDefinition, BasePermission... permission) {
		Assert.notNull(owner, "Form values owner is required.");
		Assert.notNull(owner.getId(), "Owner id is required.");
		FormableEntity ownerEntity = getOwnerEntity(owner);
		//
		FormValueService<FormableEntity> formValueService = getFormValueService(ownerEntity);
		formValueService.deleteValues(ownerEntity, formDefinition, permission);
	}

	@Override
	@Transactional
	public void deleteValues(Identifiable owner, IdmFormAttributeDto attribute, BasePermission... permission) {
		Assert.notNull(owner, "Form values owner is required.");
		Assert.notNull(owner.getId(), "Owner id is required.");
		Assert.notNull(attribute, "Form attribute definition is required.");
		//
		FormableEntity ownerEntity = getOwnerEntity(owner);
		FormValueService<FormableEntity> formValueService = getFormValueService(ownerEntity);
		formValueService.deleteValues(ownerEntity, attribute, permission);
	}

	@Override
	public String getConfidentialStorageKey(Identifiable owner, IdmFormAttributeDto attribute) {
		Assert.notNull(owner, "Form values owner is required.");
		Assert.notNull(owner.getId(), "Owner id is required.");
		Assert.notNull(attribute, "Form attribute is required.");
		//
		FormValueService<FormableEntity> formValueService = getFormValueService(owner);
		String key = formValueService.getConfidentialStorageKey(attribute.getId());
		LOG.debug("Confidential storage key for attribute [{}] is [{}].", attribute.getCode(), key);
		return key;
	}

	@Override
	public Serializable getConfidentialPersistentValue(IdmFormValueDto guardedValue) {
		Assert.notNull(guardedValue, "Guarder value is required.");
		Assert.notNull(guardedValue.getOwnerId(), "Owner identifier is required.");
		//
		FormableEntity ownerEntity = getOwnerEntity(guardedValue.getOwnerId(), guardedValue.getOwnerType());
		FormValueService<?> formValueService = getFormValueService(ownerEntity);
		return formValueService.getConfidentialPersistentValue(guardedValue);
	}

	@Override
	@Transactional(readOnly = true)
	@SuppressWarnings("unchecked")
	public <O extends BaseDto> Page<O> findOwners(Class<? extends Identifiable> ownerType,
			IdmFormAttributeDto attribute, Serializable persistentValue, Pageable pageable) {
		Assert.notNull(ownerType, "Owner type is required.");
		Assert.notNull(attribute, "Form attribute is required.");
		if (attribute.isConfidential()) {
			throw new UnsupportedOperationException(MessageFormat
					.format("Find owners by confidential attributes [{0}] are not supported.", attribute.getCode()));
		}
		//
		FormValueService<FormableEntity> formValueService = getFormValueService(ownerType);
		//
		if (pageable == null) {
			// pageable is required now
			pageable = PageRequest.of(0, Integer.MAX_VALUE);
		}
		Page<FormableEntity> ownerEntities = formValueService.findOwners(attribute, persistentValue, pageable);
		//
		// convert to dtos
		List<O> ownerDtos = ownerEntities
				.getContent()
				.stream()
				.map(ownerEntity -> {
					return (O) lookupService.lookupDto(ownerType, ownerEntity.getId());
				})
				.collect(Collectors.toList());

		return new PageImpl<>(ownerDtos, pageable, ownerEntities.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public <O extends BaseDto> Page<O> findOwners(Class<? extends Identifiable> ownerType, String attributeName,
			Serializable persistentValue, Pageable pageable) {
		IdmFormAttributeDto attribute = getAttribute(ownerType, attributeName);
		Assert.notNull(attribute, MessageFormat.format(
				"Attribute [{0}] does not exist in default form definition for owner [{1}]", attributeName, ownerType));
		//
		return findOwners(ownerType, attribute, persistentValue, pageable);
	}
	
	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Transactional(readOnly = true)
	public Page<IdmFormValueDto> findValues(IdmFormValueFilter filter, Pageable pageable, BasePermission... permission) {
		Assert.notNull(filter, "Filter is required.");
		//
		// resolve owner by definition
		if (filter.getOwner() == null) {
			IdmFormDefinitionDto formDefinition = null;
			UUID definitionId = filter.getDefinitionId();
			if (definitionId != null) {
				formDefinition = formDefinitionService.get(definitionId);
			}
			//
			UUID attributeId = filter.getAttributeId();
			if (formDefinition == null && attributeId != null) {
				IdmFormAttributeDto formAttribute = formAttributeService.get(attributeId);
				if (formAttribute != null) {
					formDefinition = DtoUtils.getEmbedded(formAttribute, IdmFormAttribute_.formDefinition);
				}
			}
			if (formDefinition == null) {
				throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", "formDefinition"));
			}	
			filter.setOwner(getEmptyOwner(formDefinition));
		}
		Identifiable owner = (Identifiable) filter.getOwner();
		Assert.notNull(owner, "Filter - attribute owner is required. Is possible to filter form values by given owner only");
		//
		FormValueService<FormableEntity> formValueService = getFormValueService(owner.getClass());
		//
		return formValueService.find(filter, pageable, permission);
	}

	@Override
	public List<String> getOwnerTypes() {
		return formValueServices
				.getPlugins()
				.stream()
				.map(service -> {
					return getDefaultDefinitionType(service.getOwnerClass());
				})
				.sorted()
				.collect(Collectors.toList());
	}
	
	@Override
	public List<FormValueService<?>> getAvailableFormValueServices() {
		return formValueServices.getPlugins();
	}
	
	@Override
	@SuppressWarnings({ "unchecked" })
	public <O extends FormableEntity> FormValueService<O> getFormValueService(Class<? extends Identifiable> ownerType) {
		FormValueService<O> formValueService = (FormValueService<O>) formValueServices.getPluginFor(lookupService.getEntityClass(ownerType));
		if (formValueService == null) {
			// common form
			formValueService = (FormValueService<O>) formValueServices.getPluginFor(IdmForm.class);
		}
		//
		if (formValueService == null) {
			throw new IllegalStateException(MessageFormat.format(
					"FormValueService for class [{0}] not found, please check configuration", ownerType));
		}
		return formValueService;
	}
	
	@Override
	@Transactional(readOnly = true)
	public void mergeValues(IdmFormDefinitionDto definition, FormableDto source, FormableDto target) {
		IdmFormInstanceDto formInstance = getFormInstance(source, definition);
		refillDeletedAttributeValue(formInstance);
		//
		target.getEavs().clear();
		target.getEavs().add(formInstance);
	}
	
	/**
	 * Form instance must contains delete attribute (with empty value). Without it
	 * could be form values not deleted.
	 * 
	 * @param formInstance
	 */
	private void refillDeletedAttributeValue(IdmFormInstanceDto formInstance) {
		Assert.notNull(formInstance, "Form instance is required.");
		IdmFormDefinitionDto formDefinition = formInstance.getFormDefinition();
		Assert.notNull(formDefinition, "Form definition is required.");
		//
		formDefinition
			.getFormAttributes()
			.stream()
			.forEach(formAttribute -> { //
				List<IdmFormValueDto> values = formInstance.getValues();
				boolean valueExists = values //
						.stream() //
						.filter(formValue -> formAttribute.getId().equals(formValue.getFormAttribute())) //
						.findFirst() //
						.isPresent();
				if (!valueExists) {
					ArrayList<IdmFormValueDto> newValues = Lists.newArrayList(values);
					IdmFormValueDto deletedValue = new IdmFormValueDto(formAttribute);
					newValues.add(deletedValue);
					formInstance.setValues(newValues);
				}
			});
	}
	

	/**
	 * Returns FormValueService for given owner
	 * 
	 * @param owner
	 * @param <O> values owner
	 * @return
	 */
	private <O extends FormableEntity> FormValueService<O> getFormValueService(Identifiable owner) {
		return getFormValueService(owner.getClass());
	}
	
	/**
	 * 
	 * @param owner
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <O extends FormableEntity> O getOwnerEntity(Identifiable owner) {
		Assert.notNull(owner, "Form values owner instance is required.");
		if (owner instanceof FormableEntity) {
			return (O) owner;
		}
		//
		return getOwnerEntity(owner.getId(), owner.getClass());
	}

	/**
	 * Returns owner entity by given id and type
	 * 
	 * @param ownerId
	 * @param ownerType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <O extends FormableEntity> O getOwnerEntity(Serializable ownerId, Class<? extends Identifiable> ownerType) {
		Assert.notNull(ownerId, "Form values owner id is required.");
		Assert.notNull(ownerType, "Form values owner type is required.");
		O owner = (O) lookupService.lookupEntity(ownerType, ownerId);
		Assert.notNull(owner, String.format("Form values owner not found and is required. Owner type [%s], id [%s].", ownerType, ownerId));
		//
		return owner;
	}

	/**
	 * Create instance form definition from the given configuration class
	 * 
	 * @param configurationClass
	 * @return
	 */
	@Override
	public IdmFormDefinitionDto convertConfigurationToFormDefinition(
			Class<? extends ConfigurationClass> configurationClass) {
		Assert.notNull(configurationClass, "Class with the configuration is required.");
		try {
			ConfigurationClass configurationClassInstance = configurationClass.getDeclaredConstructor().newInstance();
			List<IdmFormAttributeDto> properties = new ArrayList<>();

			PropertyDescriptor[] descriptors = Introspector.getBeanInfo(configurationClass).getPropertyDescriptors();

			Lists.newArrayList(descriptors).stream().forEach(descriptor -> {
				Method readMethod = descriptor.getReadMethod();
				String propertyName = descriptor.getName();
				ConfigurationClassProperty property = readMethod.getAnnotation(ConfigurationClassProperty.class);
				if (property != null) {
					IdmFormAttributeDto formAttribute = this.convertConfigurationProperty(property);
					formAttribute.setCode(propertyName);
					// TODO: Better convertors  (move from IC and ACC module to the Core)!
					initPersistentType(readMethod, formAttribute);
					
					try {
						formAttribute.setDefaultValue(this.convertDefaultValue(readMethod.invoke(configurationClassInstance), formAttribute.isMultiple()));
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						throw new CoreException("Cannot read value of connector configuration property!", e);
					}

					properties.add(formAttribute);
				}
			});

			IdmFormDefinitionDto definition = new IdmFormDefinitionDto();
			definition.setFormAttributes(properties);

			return definition;

		} catch (ReflectiveOperationException | IntrospectionException ex) {
			throw new CoreException("Cannot read configuration property!", ex);
		}
	}
	
	@Override
	public List<InvalidFormAttributeDto> validate(IdmFormInstanceDto formInstance) {
		Assert.notNull(formInstance, "Form instance cannot be null!");
		IdmFormDefinitionDto formDefinition = formInstance.getFormDefinition();
		Assert.notNull(formDefinition, "Form definition cannot be null!");
		//
		List<InvalidFormAttributeDto> results = Lists.newArrayList();
		formDefinition
			.getFormAttributes()
			.stream()
			.forEach(formAttribute -> { //
				List<IdmFormValueDto> formValueForAttributes = formInstance //
						.getValues() //
						.stream() //
						.filter(formValue -> formAttribute.getId().equals(formValue.getFormAttribute())) //
						.peek(formValue -> {
							// we don't trust value persistent type - set current from attribute 
							formValue.setPersistentType(formAttribute.getPersistentType());
						})
						.collect(Collectors.toList()); //
				InvalidFormAttributeDto result = this.validateAttribute(formDefinition, formAttribute, formValueForAttributes);
				if (!result.isValid()) {
					results.add(result);
				}
			});
		return results;
	}

	private InvalidFormAttributeDto validateAttribute(
			IdmFormDefinitionDto formDefinition,
			IdmFormAttributeDto formAttribute,
			List<IdmFormValueDto> formValues) {
		Assert.notNull(formAttribute, "Form attribute is required.");
		//
		InvalidFormAttributeDto result = new InvalidFormAttributeDto(formAttribute);
		if (formAttribute.isRequired()) {
			if (CollectionUtils.isEmpty(formValues) || !formValues
						.stream()
						.filter(formValue -> !formValue.isEmpty())
						.findFirst()
						.isPresent()) {
				LOG.debug("Form attribute [{}] validation failed - value is required.", formAttribute.getCode());
				//
				result.setMissingValue(true);
			}
		}
		if (CollectionUtils.isEmpty(formValues)) {
			// values are not filled => other validations is not needed.
			return result;
		}
		// TODO: multiple values -> the last validation error is returned. Return invalid attribute for the all values ...
		formValues
			.stream()
			.filter(formValue -> !formValue.isEmpty())
			.forEach(formValue -> {
				// minimum value validation
				if (formAttribute.getMin() != null) {
					if (formValue.getLongValue() != null
							&& formAttribute.getMin().compareTo(BigDecimal.valueOf(formValue.getLongValue())) > 0) {
						LOG.debug("Form attribute [{}] validation failed - given value [{}] is lesser than min [{}].",
								formAttribute.getCode(), formValue.getLongValue(), formAttribute.getMin());
						//
						result.setMinValue(formAttribute.getMin());
					}
					if (formValue.getDoubleValue() != null
							&& formAttribute.getMin().compareTo(formValue.getDoubleValue()) > 0) {
						LOG.debug("Form attribute [{}] validation failed - given value [{}] is lesser than min [{}].",
								formAttribute.getCode(), formValue.getDoubleValue(), formAttribute.getMin());
						//
						result.setMinValue(formAttribute.getMin());
					}
				}
				// maximum value validation
				if (formAttribute.getMax() != null) {
					if (formValue.getLongValue() != null
							&& formAttribute.getMax().compareTo(BigDecimal.valueOf(formValue.getLongValue())) < 0) {
						LOG.debug("Form attribute [{}] validation failed - given value [{}] is greater than max [{}].",
								formAttribute.getCode(), formValue.getLongValue(), formAttribute.getMax());
						//
						result.setMaxValue(formAttribute.getMax());
					}
					if (formValue.getDoubleValue() != null
							&& formAttribute.getMax().compareTo(formValue.getDoubleValue()) < 0) {
						LOG.debug("Form attribute [{}] validation failed - given value [{}] is greater than max [{}].",
								formAttribute.getCode(), formValue.getDoubleValue(), formAttribute.getMax());
						//
						result.setMaxValue(formAttribute.getMax());
					}
				}
				String regex = formAttribute.getRegex();
				if (StringUtils.isNotEmpty(regex)) {
					Pattern p = Pattern.compile(regex);
					String stringValue = formValue.getValue().toString();
					Matcher m = p.matcher(stringValue); // all persistent types are supported on BE, but string values makes the good sense.
					if (!m.matches()) {
						LOG.debug("Form attribute [{}] validation failed - given value [{}] does not match regex [{}].",
								formAttribute.getCode(), stringValue, regex);
						//
						result.setRegexValue(regex);
					}
				}
				if (formAttribute.isUnique()) {
					IdmFormValueFilter<FormableEntity> valueFilter = new IdmFormValueFilter<>(); 
					valueFilter.setAttributeId(formValue.getFormAttribute());
					valueFilter.setPersistentType(formValue.getPersistentType());
					valueFilter.setStringValue(formValue.getStringValue());
					valueFilter.setShortTextValue(formValue.getShortTextValue());
					valueFilter.setBooleanValue(formValue.getBooleanValue());
					valueFilter.setLongValue(formValue.getLongValue());
					valueFilter.setDoubleValue(formValue.getDoubleValue());
					valueFilter.setDateValue(formValue.getDateValue());
					valueFilter.setUuidValue(formValue.getUuidValue());
					//
					Identifiable owner = getEmptyOwner(formDefinition);
					Assert.notNull(owner, "Filter - attribute owner is required. Is possible to filter form values by given owner only");
					//
					FormValueService<FormableEntity> formValueService = getFormValueService(owner.getClass());
					//
					List<IdmFormValueDto> existValues = formValueService.find(valueFilter, PageRequest.of(0, 2)).getContent();
					//
					if (existValues
							.stream()
							.filter(v -> formValue.getId() == null || !formValue.getId().equals(v.getId()))
							.findFirst()
							.isPresent()) {
						LOG.debug("Form attribute [{}] validation failed - given value [{}] is not unigue.",
								formAttribute.getCode(), formValue.getValue());
						//
						result.setUniqueValue(formValue.getValue().toString());
					}
				}
			});
		//
		return result;
	}

	private void initPersistentType(Method readMethod, IdmFormAttributeDto formAttribute) {
		Assert.notNull(readMethod, "Read metod is required.");
		Assert.notNull(formAttribute, "Form attribute is required.");
		
		String typeName = readMethod.getGenericReturnType().getTypeName();
		if (typeName.equals(boolean.class.getTypeName())) {
			formAttribute.setPersistentType(PersistentType.BOOLEAN);
			formAttribute.setMultiple(false);
		} else if (typeName.equals(String[].class.getTypeName())) {
			formAttribute.setPersistentType(PersistentType.TEXT);
			formAttribute.setMultiple(true);
		} else if (typeName.equals(String.class.getTypeName())) {
			formAttribute.setPersistentType(PersistentType.TEXT);
			formAttribute.setMultiple(false);
		} else if (typeName.equals(UUID.class.getTypeName())) {
			formAttribute.setPersistentType(PersistentType.UUID);
			formAttribute.setMultiple(false);
		} else if (typeName.equals(UUID[].class.getTypeName())) {
			formAttribute.setPersistentType(PersistentType.UUID);
			formAttribute.setMultiple(true);
		} else {
			throw new CoreException(
					MessageFormat.format("For return type [{0}] was not found persistent type!", typeName));
		}

	}

	private IdmFormAttributeDto convertConfigurationProperty(ConfigurationClassProperty property) {
		if (property == null) {
			return null;
		}
		IdmFormAttributeDto icProperty = new IdmFormAttributeDto();
		icProperty.setConfidential(property.confidential());
		icProperty.setName(property.displayName());
		icProperty.setDescription(property.helpMessage());
		icProperty.setRequired(property.required());
		icProperty.setSeq((short) property.order());
		icProperty.setFaceType(property.face());

		return icProperty;
	}
	
	private String convertDefaultValue(Object value, boolean multivalue) {
		//
		if (value == null) {
			return null;
		}		
		if (!multivalue) {
			return value.toString();
		}		
		StringBuilder result = new StringBuilder();
		// arrays only
		// override for other data types
		Object[] values = (Object[]) value;
		for (Object singleValue : values) {
			if (result.length() > 0) {
				result.append(System.getProperty("line.separator"));
			}
			result.append(singleValue);
		}
		return result.toString();
	}
	
	/**
	 * Prepares new owner instance
	 * 
	 * @param formDefinition
	 * @return
	 */
	private FormableEntity getEmptyOwner(IdmFormDefinitionDto formDefinition) {
		Assert.notNull(formDefinition, "Form definition is required.");
		//
		try {
			return (FormableEntity) Class.forName(formDefinition.getType()).getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException ex) {
			throw new ResultCodeException(CoreResultCode.BAD_VALUE, ImmutableMap.of("formDefinition", formDefinition.getType()), ex);
		}
	}
	
	@Override
	public void export(IdmFormInstanceDto formInstanceDto, IdmExportImportDto batch) {
		Assert.notNull(batch, "Export batch must exist!");
		Assert.notNull(formInstanceDto, "Instance of cannot be null for export!");
		
		// All confidential values will be removed from the export. We don't want change
		// confidential value on a target IdM.
		List<IdmFormValueDto> valuesWithoutConfidential = formInstanceDto.getValues().stream()//
				.filter(formValue -> !formValue.isConfidential())//
				.collect(Collectors.toList());
		formInstanceDto.setValues(valuesWithoutConfidential);
		
		batch.getExportedDtos().add(formInstanceDto);
		
		ExportDescriptorDto exportDescriptorDto = new ExportDescriptorDto(formInstanceDto.getClass());
		if (!batch.getExportOrder().contains(exportDescriptorDto)) {
			batch.getExportOrder().add(exportDescriptorDto);
		}
	}
}
