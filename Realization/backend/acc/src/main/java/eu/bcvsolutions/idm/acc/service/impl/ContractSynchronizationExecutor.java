package eu.bcvsolutions.idm.acc.service.impl;

import java.beans.IntrospectionException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.AttributeMapping;
import eu.bcvsolutions.idm.acc.domain.OperationResultType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationActionType;
import eu.bcvsolutions.idm.acc.domain.SynchronizationContext;
import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.dto.AccAccountDto;
import eu.bcvsolutions.idm.acc.dto.AccContractAccountDto;
import eu.bcvsolutions.idm.acc.dto.EntityAccountDto;
import eu.bcvsolutions.idm.acc.dto.SyncIdentityContractDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncActionLogDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncContractConfigDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncItemLogDto;
import eu.bcvsolutions.idm.acc.dto.SysSyncLogDto;
import eu.bcvsolutions.idm.acc.dto.SysSystemAttributeMappingDto;
import eu.bcvsolutions.idm.acc.dto.filter.AccContractAccountFilter;
import eu.bcvsolutions.idm.acc.dto.filter.EntityAccountFilter;
import eu.bcvsolutions.idm.acc.entity.SysSyncContractConfig_;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.service.api.AccAccountService;
import eu.bcvsolutions.idm.acc.service.api.AccContractAccountService;
import eu.bcvsolutions.idm.acc.service.api.ProvisioningService;
import eu.bcvsolutions.idm.acc.service.api.SynchronizationEntityExecutor;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaAttributeService;
import eu.bcvsolutions.idm.acc.service.api.SysSchemaObjectClassService;
import eu.bcvsolutions.idm.acc.service.api.SysSyncActionLogService;
import eu.bcvsolutions.idm.acc.service.api.SysSyncConfigService;
import eu.bcvsolutions.idm.acc.service.api.SysSyncItemLogService;
import eu.bcvsolutions.idm.acc.service.api.SysSyncLogService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemAttributeMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemEntityService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemMappingService;
import eu.bcvsolutions.idm.acc.service.api.SysSystemService;
import eu.bcvsolutions.idm.core.api.domain.ContractState;
import eu.bcvsolutions.idm.core.api.dto.IdmContractGuaranteeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeNodeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeTypeDto;
import eu.bcvsolutions.idm.core.api.dto.filter.CorrelationFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmContractGuaranteeFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityContractFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmTreeNodeFilter;
import eu.bcvsolutions.idm.core.api.event.EntityEvent;
import eu.bcvsolutions.idm.core.api.service.ConfidentialStorage;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.GroovyScriptService;
import eu.bcvsolutions.idm.core.api.service.IdmContractGuaranteeService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.api.service.IdmTreeNodeService;
import eu.bcvsolutions.idm.core.api.service.LookupService;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.api.utils.EntityUtils;
import eu.bcvsolutions.idm.core.eav.api.entity.FormableEntity;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityContract;
import eu.bcvsolutions.idm.core.model.entity.IdmTreeNode_;
import eu.bcvsolutions.idm.core.model.event.ContractGuaranteeEvent;
import eu.bcvsolutions.idm.core.model.event.ContractGuaranteeEvent.ContractGuaranteeEventType;
import eu.bcvsolutions.idm.core.model.event.IdentityContractEvent;
import eu.bcvsolutions.idm.core.model.event.IdentityContractEvent.IdentityContractEventType;
import eu.bcvsolutions.idm.core.workflow.service.WorkflowProcessInstanceService;
import eu.bcvsolutions.idm.ic.api.IcAttribute;
import eu.bcvsolutions.idm.ic.service.api.IcConnectorFacade;

@Component
public class ContractSynchronizationExecutor extends AbstractSynchronizationExecutor<IdmIdentityContractDto>
		implements SynchronizationEntityExecutor {

	private final IdmIdentityContractService contractService;
	private final AccContractAccountService contractAccoutnService;
	private final IdmContractGuaranteeService guaranteeService;
	private final IdmTreeNodeService treeNodeService;
	private final LookupService lookupService;
	public final static String CONTRACT_STATE_FIELD = "state";
	public final static String CONTRACT_GUARANTEES_FIELD = "guarantees";
	public final static String CONTRACT_IDENTITY_FIELD = "identity";
	public final static String CONTRACT_WORK_POSITION_FIELD = "workPosition";
	public final static String SYNC_CONTRACT_FIELD = "sync_contract";

	@Autowired
	public ContractSynchronizationExecutor(IcConnectorFacade connectorFacade, SysSystemService systemService,
			SysSystemAttributeMappingService attributeHandlingService,
			SysSyncConfigService synchronizationConfigService, SysSyncLogService synchronizationLogService,
			SysSyncActionLogService syncActionLogService, AccAccountService accountService,
			SysSystemEntityService systemEntityService, ConfidentialStorage confidentialStorage,
			FormService formService, IdmIdentityContractService contractService,
			AccContractAccountService contractAccoutnService, SysSyncItemLogService syncItemLogService,
			EntityEventManager entityEventManager, GroovyScriptService groovyScriptService,
			WorkflowProcessInstanceService workflowProcessInstanceService, EntityManager entityManager,
			SysSystemMappingService systemMappingService, SysSchemaObjectClassService schemaObjectClassService,
			SysSchemaAttributeService schemaAttributeService, LookupService lookupService,
			IdmContractGuaranteeService guaranteeService, IdmTreeNodeService treeNodeService) {
		super(connectorFacade, systemService, attributeHandlingService, synchronizationConfigService,
				synchronizationLogService, syncActionLogService, accountService, systemEntityService,
				confidentialStorage, formService, syncItemLogService, entityEventManager, groovyScriptService,
				workflowProcessInstanceService, entityManager, systemMappingService, schemaObjectClassService,
				schemaAttributeService);
		//
		Assert.notNull(contractService, "Contract service is mandatory!");
		Assert.notNull(contractAccoutnService, "Contract-account service is mandatory!");
		Assert.notNull(lookupService, "Lookup service is mandatory!");
		Assert.notNull(guaranteeService, "Contract guarantee service is mandatory!");
		Assert.notNull(treeNodeService, "Tree node service is mandatory!");
		//
		this.contractService = contractService;
		this.contractAccoutnService = contractAccoutnService;
		this.lookupService = lookupService;
		this.guaranteeService = guaranteeService;
		this.treeNodeService = treeNodeService;
	}

	/**
	 * Call provisioning for given account
	 * 
	 * @param account
	 * @param entityType
	 * @param log
	 * @param logItem
	 * @param actionLogs
	 */
	protected void doUpdateAccount(AccAccountDto account, SystemEntityType entityType, SysSyncLogDto log,
			SysSyncItemLogDto logItem, List<SysSyncActionLogDto> actionLogs) {
		UUID entityId = getEntityByAccount(account.getId());
		IdmIdentityContractDto entity = null;
		if (entityId != null) {
			entity = contractService.get(entityId);
		}
		if (entity == null) {
			addToItemLog(logItem, "Entity account relation (with ownership = true) was not found!");
			initSyncActionLog(SynchronizationActionType.UPDATE_ENTITY, OperationResultType.WARNING, logItem, log,
					actionLogs);
			return;
		}
		// Call provisioning for this entity
		callProvisioningForEntity(entity, entityType, logItem);
	}

	/**
	 * Call provisioning for given account
	 * 
	 * @param entity
	 * @param entityType
	 * @param logItem
	 */
	@Override
	protected void callProvisioningForEntity(IdmIdentityContractDto entity, SystemEntityType entityType,
			SysSyncItemLogDto logItem) {
		addToItemLog(logItem, MessageFormat.format(
				"Call provisioning (process IdentityContractEvent.UPDATE) for contract ({0}) with position ({1}).",
				entity.getId(), entity.getPosition()));
		entityEventManager.process(new IdentityContractEvent(IdentityContractEventType.UPDATE, entity)).getContent();
	}

	/**
	 * Operation remove IdentityContractAccount relations and linked roles
	 * 
	 * @param account
	 * @param removeIdentityContractIdentityContract
	 * @param log
	 * @param logItem
	 * @param actionLogs
	 */
	protected void doUnlink(AccAccountDto account, boolean removeIdentityContractIdentityContract, SysSyncLogDto log,
			SysSyncItemLogDto logItem, List<SysSyncActionLogDto> actionLogs) {

		EntityAccountFilter entityAccountFilter = new AccContractAccountFilter();
		entityAccountFilter.setAccountId(account.getId());
		List<AccContractAccountDto> entityAccounts = contractAccoutnService
				.find((AccContractAccountFilter) entityAccountFilter, null).getContent();
		if (entityAccounts.isEmpty()) {
			addToItemLog(logItem, "Contract-account relation was not found!");
			initSyncActionLog(SynchronizationActionType.UPDATE_ENTITY, OperationResultType.WARNING, logItem, log,
					actionLogs);
			return;
		}
		addToItemLog(logItem, MessageFormat.format("Contract-account relations to delete {0}", entityAccounts));

		entityAccounts.stream().forEach(entityAccount -> {
			// We will remove contract account, but without delete connected
			// account
			contractAccoutnService.delete(entityAccount, false);
			addToItemLog(logItem, MessageFormat.format(
					"Contract-account relation deleted (without call delete provisioning) (contract id: {0}, contract-account id: {1})",
					entityAccount.getContract(), entityAccount.getId()));

		});
		return;
	}

	/**
	 * Fill entity with attributes from IC module (by mapped attributes).
	 * 
	 * @param mappedAttributes
	 * @param uid
	 * @param icAttributes
	 * @param entity
	 * @param create
	 *            (is create or update entity situation)
	 * @param context
	 * @return
	 */
	protected IdmIdentityContractDto fillEntity(List<SysSystemAttributeMappingDto> mappedAttributes, String uid,
			List<IcAttribute> icAttributes, IdmIdentityContractDto dto, boolean create,
			SynchronizationContext context) {
		mappedAttributes.stream().filter(attribute -> {
			// Skip disabled attributes
			// Skip extended attributes (we need update/ create entity first)
			// Skip confidential attributes (we need update/ create entity
			// first)
			boolean fastResult = !attribute.isDisabledAttribute() && attribute.isEntityAttribute()
					&& !attribute.isConfidentialAttribute();
			if (!fastResult) {
				return false;
			}
			// Can be value set by attribute strategy?
			return this.canSetValue(uid, attribute, dto, create);

		}).forEach(attribute -> {
			String attributeProperty = attribute.getIdmPropertyName();
			Object transformedValue = getValueByMappedAttribute(attribute, icAttributes, context);
			// Guarantees will be set no to the dto (we does not have field for
			// they), but to the embedded map.
			if (CONTRACT_GUARANTEES_FIELD.equals(attributeProperty)) {
				if (transformedValue instanceof SyncIdentityContractDto) {
					dto.getEmbedded().put(SYNC_CONTRACT_FIELD, (SyncIdentityContractDto) transformedValue);
				} else {
					dto.getEmbedded().put(SYNC_CONTRACT_FIELD, new SyncIdentityContractDto());
				}
				return;
			}
			// Set transformed value from target system to entity
			try {
				EntityUtils.setEntityValue(dto, attributeProperty, transformedValue);
			} catch (IntrospectionException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | ProvisioningException e) {
				throw new ProvisioningException(AccResultCode.SYNCHRONIZATION_IDM_FIELD_NOT_SET,
						ImmutableMap.of("property", attributeProperty, "uid", uid), e);
			}

		});
		return dto;
	}

	@Override
	protected Object getValueByMappedAttribute(AttributeMapping attribute, List<IcAttribute> icAttributes,
			SynchronizationContext context) {
		Object transformedValue = super.getValueByMappedAttribute(attribute, icAttributes, context);
		// Transform contract state enumeration from string
		if (CONTRACT_STATE_FIELD.equals(attribute.getIdmPropertyName()) && transformedValue instanceof String
				&& attribute.isEntityAttribute()) {
			return ContractState.valueOf((String) transformedValue);
		}
		// Transform contract guarantees
		if (CONTRACT_GUARANTEES_FIELD.equals(attribute.getIdmPropertyName()) && attribute.isEntityAttribute()) {
			return transformGuarantees(context, transformedValue);
		}
		// Transform work position (tree node)
		if (CONTRACT_WORK_POSITION_FIELD.equals(attribute.getIdmPropertyName()) && attribute.isEntityAttribute()) {

			if (transformedValue != null) {
				IdmTreeNodeDto workposition = this.findTreeNode(transformedValue, context);
				if (workposition != null) {
					return workposition.getId();
				}
				return null;
			} else {
				if (getConfig(context).getDefaultTreeNode() != null) {
					UUID defaultNode = ((SysSyncContractConfigDto) context.getConfig()).getDefaultTreeNode();
					IdmTreeNodeDto node = (IdmTreeNodeDto) lookupService.lookupDto(IdmTreeNodeDto.class, defaultNode);
					if (node != null) {
						context.getLogItem().addToLog(MessageFormat.format(
								"Warning! - None workposition was defined for this realtion, we use default workposition [{0}]!",
								node.getCode()));
						return node.getId();
					}
				}
			}
		}
		// Transform contract owner
		if (transformedValue != null && CONTRACT_IDENTITY_FIELD.equals(attribute.getIdmPropertyName())
				&& attribute.isEntityAttribute()) {
			context.getLogItem().addToLog(MessageFormat.format("Finding contract owner [{0}].", transformedValue));
			IdmIdentityDto identity = this.findIdentity(transformedValue, context);
			if (identity == null) {
				throw new ProvisioningException(AccResultCode.SYNCHRONIZATION_IDM_FIELD_CANNOT_BE_NULL,
						ImmutableMap.of("property", CONTRACT_IDENTITY_FIELD));
			}
			return identity.getId();
		}

		return transformedValue;
	}

	private Object transformGuarantees(SynchronizationContext context, Object transformedValue) {
		if (transformedValue != null) {
			SyncIdentityContractDto syncContract = new SyncIdentityContractDto();
			if (transformedValue instanceof List) {
				((List<?>) transformedValue).stream().forEach(guarantee -> {

					// Beware this DTO contains only identity ID, not
					// contract ... must be save separately.
					context.getLogItem().addToLog(MessageFormat.format("Finding guarantee [{0}].", guarantee));
					IdmIdentityDto guarranteeDto = this.findIdentity(guarantee, context);
					if (guarranteeDto != null) {
						syncContract.getGuarantees().add(guarranteeDto);
					}
				});
			} else {
				// Beware this DTO contains only identity ID, not
				// contract ... must be save separately.
				context.getLogItem().addToLog(MessageFormat.format("Finding guarantee [{0}].", transformedValue));
				IdmIdentityDto guarranteeDto = this.findIdentity(transformedValue, context);
				if (guarranteeDto != null) {
					syncContract.getGuarantees().add(guarranteeDto);
				}
			}
			transformedValue = syncContract;
		} else {
			if (getConfig(context).getDefaultLeader() != null) {
				UUID defaultLeader = ((SysSyncContractConfigDto) context.getConfig()).getDefaultLeader();
				IdmIdentityDto identity = (IdmIdentityDto) lookupService.lookupDto(IdmIdentityDto.class, defaultLeader);
				if (identity != null) {
					SyncIdentityContractDto syncContract = new SyncIdentityContractDto();
					syncContract.getGuarantees().add(identity);
					transformedValue = syncContract;
					context.getLogItem()
							.addToLog(MessageFormat.format(
									"Warning! - None leader was found for this realtion, we use default leader [{0}]!",
									identity.getCode()));
				}
			}
		}
		return transformedValue;
	}

	private IdmIdentityDto findIdentity(Object value, SynchronizationContext context) {
		if (value instanceof Serializable) {
			IdmIdentityDto identity = (IdmIdentityDto) lookupService.lookupDto(IdmIdentityDto.class,
					(Serializable) value);

			if (identity == null) {
				context.getLogItem()
						.addToLog(MessageFormat.format("Warning! - Identity [{0}] was not found for [{0}]!", value));
				this.initSyncActionLog(context.getActionType(), OperationResultType.WARNING, context.getLogItem(),
						context.getLog(), context.getActionLogs());
				return null;
			}

			return identity;
		} else {
			context.getLogItem()
					.addToLog(MessageFormat.format(
							"Warning! - Identity cannot be found, because transformed value [{0}] is not Serializable!",
							value));
			this.initSyncActionLog(context.getActionType(), OperationResultType.WARNING, context.getLogItem(),
					context.getLog(), context.getActionLogs());
		}
		return null;
	}

	private IdmTreeNodeDto findTreeNode(Object value, SynchronizationContext context) {
		if (value instanceof Serializable) {
			// Find by UUID
			context.getLogItem().addToLog(
					MessageFormat.format("Work position - try find directly by transformed value [{0}]!", value));
			IdmTreeNodeDto node = (IdmTreeNodeDto) lookupService.lookupDto(IdmTreeNodeDto.class, (Serializable) value);

			if (node != null) {
				IdmTreeTypeDto treeTypeDto = DtoUtils.getEmbedded(node, IdmTreeNode_.treeType, IdmTreeTypeDto.class);
				context.getLogItem().addToLog(MessageFormat.format(
						"Work position - One node [{1}] (in tree type [{2}]) was found directly by transformed value [{0}]!",
						value, node.getCode(), treeTypeDto.getCode()));
				return node;
			}
			context.getLogItem().addToLog(MessageFormat
					.format("Work position - was not not found directly from transformed value [{0}]!", value));
			if (value instanceof String) {
				// Find by code in default tree type
				IdmTreeNodeFilter treeNodeFilter = new IdmTreeNodeFilter();
				SysSyncContractConfigDto config = this.getConfig(context);
				IdmTreeTypeDto defaultTreeType = DtoUtils.getEmbedded(config, SysSyncContractConfig_.defaultTreeType,
						IdmTreeTypeDto.class);
				treeNodeFilter.setTreeTypeId(config.getDefaultTreeType());
				treeNodeFilter.setCode((String) value);
				context.getLogItem()
						.addToLog(MessageFormat.format(
								"Work position - try find in default tree type [{1}] with code [{0}]!", value,
								defaultTreeType.getCode()));
				List<IdmTreeNodeDto> nodes = treeNodeService.find(treeNodeFilter, null).getContent();
				if (nodes.isEmpty()) {
					context.getLogItem().addToLog(
							MessageFormat.format("Warning - Work position - none node found for code [{0}]!", value));
					this.initSyncActionLog(context.getActionType(), OperationResultType.WARNING, context.getLogItem(),
							context.getLog(), context.getActionLogs());
					return null;
				} else if (nodes.size() > 1) {
					context.getLogItem()
							.addToLog(MessageFormat.format(
									"Warning - Work position - more then one [{0}] node found for code [{1}]!", value,
									nodes.size()));
					this.initSyncActionLog(context.getActionType(), OperationResultType.WARNING, context.getLogItem(),
							context.getLog(), context.getActionLogs());
					return null;

				} else {
					MessageFormat.format("Work position - One node [{1}] was found for code [{0}]!", value,
							nodes.get(0).getId());
					return nodes.get(0);
				}
			}
		} else {
			context.getLogItem().addToLog(MessageFormat.format(
					"Warning! - Work position cannot be found, because transformed value [{0}] is not Serializable!",
					value));
			this.initSyncActionLog(context.getActionType(), OperationResultType.WARNING, context.getLogItem(),
					context.getLog(), context.getActionLogs());
		}
		return null;
	}

	private SysSyncContractConfigDto getConfig(SynchronizationContext context) {
		return ((SysSyncContractConfigDto) context.getConfig());
	}

	/**
	 * Save entity
	 * 
	 * @param entity
	 * @param skipProvisioning
	 * @return
	 */
	@Override
	protected IdmIdentityContractDto save(IdmIdentityContractDto entity, boolean skipProvisioning) {
		EntityEvent<IdmIdentityContractDto> event = new IdentityContractEvent(
				contractService.isNew(entity) ? IdentityContractEventType.CREATE : IdentityContractEventType.UPDATE,
				entity, ImmutableMap.of(ProvisioningService.SKIP_PROVISIONING, skipProvisioning));

		IdmIdentityContractDto contract = contractService.publish(event).getContent();
		if (entity.getEmbedded().containsKey(SYNC_CONTRACT_FIELD)) {
			SyncIdentityContractDto syncContract = (SyncIdentityContractDto) entity.getEmbedded()
					.get(SYNC_CONTRACT_FIELD);
			IdmContractGuaranteeFilter guaranteeFilter = new IdmContractGuaranteeFilter();
			guaranteeFilter.setIdentityContractId(entity.getId());

			List<IdmContractGuaranteeDto> currentGuarantees = guaranteeService.find(guaranteeFilter, null).getContent();

			// Search guarantees to delete
			List<IdmContractGuaranteeDto> guaranteesToDelete = currentGuarantees.stream().filter(sysImplementer -> {
				return sysImplementer.getGuarantee() != null
						&& !syncContract.getGuarantees().contains(new IdmIdentityDto(sysImplementer.getGuarantee()));
			}).collect(Collectors.toList());

			// Search guarantees to add
			List<IdmIdentityDto> guaranteesToAdd = syncContract.getGuarantees().stream().filter(identity -> {
				return !currentGuarantees.stream().filter(currentGuarrantee -> {
					return identity.getId().equals(currentGuarrantee.getGuarantee());
				}).findFirst().isPresent();
			}).collect(Collectors.toList());

			// Delete guarantees
			guaranteesToDelete.forEach(guarantee -> {
				EntityEvent<IdmContractGuaranteeDto> guaranteeEvent = new ContractGuaranteeEvent(
						ContractGuaranteeEventType.DELETE, guarantee,
						ImmutableMap.of(ProvisioningService.SKIP_PROVISIONING, skipProvisioning));
				guaranteeService.publish(guaranteeEvent);
			});

			// Create new guarantees
			guaranteesToAdd.forEach(identity -> {
				IdmContractGuaranteeDto guarantee = new IdmContractGuaranteeDto();
				guarantee.setIdentityContract(contract.getId());
				guarantee.setGuarantee(identity.getId());
				//
				EntityEvent<IdmContractGuaranteeDto> guaranteeEvent = new ContractGuaranteeEvent(
						ContractGuaranteeEventType.CREATE,
						guarantee, ImmutableMap.of(ProvisioningService.SKIP_PROVISIONING, skipProvisioning));
				guaranteeService.publish(guaranteeEvent);
			});
		}

		return contract;
	}

	@Override
	protected EntityAccountFilter createEntityAccountFilter() {
		return new AccContractAccountFilter();
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected ReadWriteDtoService getEntityAccountService() {
		return contractAccoutnService;
	}

	@Override
	protected EntityAccountDto createEntityAccountDto() {
		return new AccContractAccountDto();
	}

	@Override
	protected IdmIdentityContractDto createEntityDto() {
		return new IdmIdentityContractDto();
	}

	@Override
	protected IdmIdentityContractService getService() {
		return contractService;
	}

	@Override
	protected Class<? extends FormableEntity> getEntityClass() {
		return IdmIdentityContract.class;
	}

	@Override
	protected CorrelationFilter getEntityFilter() {
		return new IdmIdentityContractFilter();
	}

	@Override
	protected IdmIdentityContractDto findByAttribute(String idmAttributeName, String value) {
		CorrelationFilter filter = getEntityFilter();
		filter.setProperty(idmAttributeName);
		filter.setValue(value);

		List<IdmIdentityContractDto> entities = contractService.find((IdmIdentityContractFilter) filter, null)
				.getContent();

		if (CollectionUtils.isEmpty(entities)) {
			return null;
		}
		if (entities.size() > 1) {
			throw new ProvisioningException(AccResultCode.SYNCHRONIZATION_CORRELATION_TO_MANY_RESULTS,
					ImmutableMap.of("correlationAttribute", idmAttributeName, "value", value));
		}
		if (entities.size() == 1) {
			return entities.get(0);
		}
		return null;
	}
}