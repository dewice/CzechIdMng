package eu.bcvsolutions.idm.acc.service.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.acc.domain.AttributeMapping;
import eu.bcvsolutions.idm.acc.domain.ProvisioningOperationType;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.dto.AccAccountDto;
import eu.bcvsolutions.idm.acc.dto.SysRoleSystemAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSchemaAttributeDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemEntityDto;
import eu.bcvsolutions.idm.acc.service.api.ProvisioningEntityExecutor;
import eu.bcvsolutions.idm.acc.service.api.ProvisioningService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemEntityService;
import eu.bcvsolutions.idm.core.api.dto.AbstractDto;
import eu.bcvsolutions.idm.core.api.dto.PasswordChangeDto;
import eu.bcvsolutions.idm.core.api.entity.OperationResult;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.ic.api.IcUidAttribute;

/**
 * Service for do provisioning
 * 
 * @author svandav
 *
 */
@Service("provisioningService")
public class DefaultProvisioningService implements ProvisioningService {

	private final SysSystemEntityService systemEntityService;
	private final PluginRegistry<ProvisioningEntityExecutor<?>, SystemEntityType> pluginExecutors;

	@Autowired
	public DefaultProvisioningService(List<ProvisioningEntityExecutor<?>>  executors,
			SysSystemEntityService systemEntityService) {
		Assert.notNull(executors);
		Assert.notNull(systemEntityService);
		//
		this.pluginExecutors = OrderAwarePluginRegistry.create(executors);
		this.systemEntityService = systemEntityService;
	}

	@Override
	public void doProvisioning(AbstractDto entity) {
		Assert.notNull(entity);
		this.getExecutor(SystemEntityType.getByClass(entity.getClass())).doProvisioning(entity);		
	}

	@Override
	public void doProvisioning(AccAccountDto account) {
		Assert.notNull(account);
		SysSystemEntityDto systemEntityDto = systemEntityService.get(account.getSystemEntity());
		this.getExecutor(systemEntityDto.getEntityType()).doProvisioning(account);
	}

	@Override
	public void doProvisioning(AccAccountDto account, AbstractDto entity) {
		Assert.notNull(account);
		Assert.notNull(entity);
		this.getExecutor(SystemEntityType.getByClass(entity.getClass())).doProvisioning(account, entity);
	}
	
	@Override
	public void doInternalProvisioning(AccAccountDto account, AbstractDto entity) {
		Assert.notNull(account);
		Assert.notNull(entity);
		this.getExecutor(SystemEntityType.getByClass(entity.getClass())).doInternalProvisioning(account, entity);
	}

	@Override
	public void doDeleteProvisioning(AccAccountDto account, SystemEntityType entityType, UUID entityId) {
		Assert.notNull(account);
		this.getExecutor(entityType).doDeleteProvisioning(account, entityId);
	}

	@Override
	public List<OperationResult> changePassword(AbstractDto entity, PasswordChangeDto passwordChange) {
		Assert.notNull(entity);
		//
		return this.getExecutor(SystemEntityType.getByClass(entity.getClass())).changePassword(entity, passwordChange);
	}

	@Override
	public void doProvisioningForAttribute(SysSystemEntityDto systemEntity, AttributeMapping mappedAttribute, Object value,
			ProvisioningOperationType operationType, AbstractDto entity) {
		Assert.notNull(entity);
		//
		this.getExecutor(SystemEntityType.getByClass(entity.getClass())).doProvisioningForAttribute(systemEntity, mappedAttribute, value, operationType, entity);
	}

	@Override
	public IcUidAttribute authenticate(String username, GuardedString password, SysSystemDto system,
			SystemEntityType entityType) {
		Assert.notNull(entityType);
		return this.getExecutor(entityType).authenticate(username, password, system, entityType);
	}

	@Override
	public List<AttributeMapping> resolveMappedAttributes(AccAccountDto account, AbstractDto entity,
			SysSystemDto system, SystemEntityType entityType) {
		Assert.notNull(entityType);
		return this.getExecutor(entityType).resolveMappedAttributes(account, entity, system, entityType);
	}

	@Override
	public List<AttributeMapping> compileAttributes(List<? extends AttributeMapping> defaultAttributes,
			List<SysRoleSystemAttributeDto> overloadingAttributes, SystemEntityType entityType) {
		Assert.notNull(entityType);
		return this.getExecutor(entityType).compileAttributes(defaultAttributes, overloadingAttributes, entityType);
	}

	@Override
	public void createAccountsForAllSystems(AbstractDto entity) {
		Assert.notNull(entity);
		this.getExecutor(SystemEntityType.getByClass(entity.getClass())).createAccountsForAllSystems(entity);
	}
	
	@Override
	public boolean accountManagement(AbstractDto entity) {
		Assert.notNull(entity);
		return this.getExecutor(SystemEntityType.getByClass(entity.getClass())).accountManagement(entity);
	}
	
	@Override
	public boolean isAttributeValueEquals(Object idmValue, Object icValueTransformed,
			SysSchemaAttributeDto schemaAttribute) {
		if (schemaAttribute.isMultivalued() && idmValue != null && !(idmValue instanceof List)) {
			List<Object> values = new ArrayList<>();
			values.add(idmValue);
			return Objects.equals(values, icValueTransformed);
		}

		// Multivalued values are equals, when value from system is null and value in
		// IdM is empty list
		if (schemaAttribute.isMultivalued() && idmValue instanceof Collection && ((Collection<?>) idmValue).isEmpty()
				&& icValueTransformed == null) {
			return true;
		}

		// Multivalued values are equals, when value in IdM is null and value from
		// system is empty list
		if (schemaAttribute.isMultivalued() && icValueTransformed instanceof Collection
				&& ((Collection<?>) icValueTransformed).isEmpty() && idmValue == null) {
			return true;
		}
		
		// If IdM and system value are lists, then will be compared without order the values.
		if (schemaAttribute.isMultivalued() && idmValue instanceof List && icValueTransformed instanceof List) {
			 return CollectionUtils.isEqualCollection((Collection<?>) idmValue,
					(Collection<?>) icValueTransformed);
		}
		
		// For array of bytes we need to use equals on Arrays
		if (byte[].class.getName().equals(schemaAttribute.getClassType())) {
			return Arrays.equals((byte[]) idmValue, (byte[]) icValueTransformed);
		}
		
		return Objects.equals(idmValue, icValueTransformed);
	}



	/**
	 * Find executor for given entity type
	 * @param entityType
	 * @return
	 */
	private ProvisioningEntityExecutor<AbstractDto> getExecutor(SystemEntityType entityType){
		
		@SuppressWarnings("unchecked")
		ProvisioningEntityExecutor<AbstractDto> executor =  (ProvisioningEntityExecutor<AbstractDto>) pluginExecutors.getPluginFor(entityType);
		if (executor == null) {
			throw new UnsupportedOperationException(
					MessageFormat.format("Provisioning executor for SystemEntityType {0} is not supported!", entityType));
		}
		return executor;
	}
}
