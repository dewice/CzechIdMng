package eu.bcvsolutions.idm.acc.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.acc.AccModuleDescriptor;
import eu.bcvsolutions.idm.acc.config.domain.ProvisioningBreakConfiguration;
import eu.bcvsolutions.idm.acc.domain.AccResultCode;
import eu.bcvsolutions.idm.acc.domain.ProvisioningEventType;
import eu.bcvsolutions.idm.acc.dto.SysProvisioningBreakConfigDto;
import eu.bcvsolutions.idm.acc.dto.SysProvisioningBreakItems;
import eu.bcvsolutions.idm.acc.dto.SysProvisioningBreakRecipientDto;
import eu.bcvsolutions.idm.acc.dto.filter.SysProvisioningBreakConfigFilter;
import eu.bcvsolutions.idm.acc.dto.filter.SysProvisioningBreakRecipientFilter;
import eu.bcvsolutions.idm.acc.entity.SysProvisioningBreakConfig;
import eu.bcvsolutions.idm.acc.entity.SysProvisioningBreakConfig_;
import eu.bcvsolutions.idm.acc.entity.SysProvisioningBreakRecipient_;
import eu.bcvsolutions.idm.acc.exception.ProvisioningException;
import eu.bcvsolutions.idm.acc.repository.SysProvisioningBreakConfigRepository;
import eu.bcvsolutions.idm.acc.service.api.SysProvisioningBreakConfigService;
import eu.bcvsolutions.idm.acc.service.api.SysProvisioningBreakRecipientService;
import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;
import eu.bcvsolutions.idm.core.api.entity.AbstractEntity_;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteDtoService;
import eu.bcvsolutions.idm.core.api.service.ExportManager;
import eu.bcvsolutions.idm.core.api.service.IdmCacheManager;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationTemplateDto;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;
import eu.bcvsolutions.idm.core.security.api.dto.AuthorizableType;

/**
 * Default implementation for {@link SysProvisioningBreakConfigService}
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * @author Peter Štrunc <peter.strunc@bcvsolutions.eu>
 */
@Service
public class DefaultSysProvisioningBreakConfigService extends
		AbstractReadWriteDtoService<SysProvisioningBreakConfigDto, SysProvisioningBreakConfig, SysProvisioningBreakConfigFilter>
		implements SysProvisioningBreakConfigService {

	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultSysProvisioningBreakConfigService.class);
	//
	private static final Integer MAX_CONFIGS_FOR_SYSTEM = 3;
	public static final String CACHE_NAME = AccModuleDescriptor.MODULE_ID + ":provisioning-cache";
	private static final Map<UUID, SysProvisioningBreakItems> localCache = new ConcurrentHashMap<>();
	//

	private final SysProvisioningBreakRecipientService breakRecipientService;
	private final ProvisioningBreakConfiguration provisioningBreakConfiguration;
	private final IdmCacheManager idmCacheManager;

	@Autowired
	public DefaultSysProvisioningBreakConfigService(
			SysProvisioningBreakConfigRepository repository,
			SysProvisioningBreakRecipientService breakRecipientService,
			ProvisioningBreakConfiguration provisioningBreakConfiguration,
			IdmCacheManager idmCacheManager) {
		super(repository);
		//
		Assert.notNull(breakRecipientService, "Service is required.");
		Assert.notNull(idmCacheManager, "Cache manager is required.");
		Assert.notNull(provisioningBreakConfiguration, "Configuration is required.");
		//
		this.breakRecipientService = breakRecipientService;
		this.idmCacheManager = idmCacheManager;
		this.provisioningBreakConfiguration = provisioningBreakConfiguration;
	}
	
	@Override
	public AuthorizableType getAuthorizableType() {
		// TODO: provisioning break group
		return null;
	}
	
	@Override
	public Page<SysProvisioningBreakConfigDto> find(SysProvisioningBreakConfigFilter filter, Pageable pageable,
			BasePermission... permission) {
		Page<SysProvisioningBreakConfigDto> configs = super.find(filter, pageable, permission);
		//
		// if include global config and set systemId add global configurations
		if (filter != null && filter.isIncludeGlobalConfig() && filter.getSystemId() != null && configs.getTotalElements() != MAX_CONFIGS_FOR_SYSTEM) {
			List<SysProvisioningBreakConfigDto> configsList = addGlobalConfigs(configs.getContent(), filter.getSystemId());
			//
			if (!configsList.isEmpty()) {
				PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), configsList.size(), pageable.getSort());
				Page<SysProvisioningBreakConfigDto> dtoPage = new PageImpl<>(configsList, pageRequest, configsList.size());
				return dtoPage;
			}
		}
		//
		return configs;
	}
	
	@Override
	public void delete(SysProvisioningBreakConfigDto dto, BasePermission... permission) {
		Assert.notNull(dto, "DTO is required.");
		// check global configuration
		if (dto.getGlobalConfiguration() != null && dto.getGlobalConfiguration()) {
			throw new ProvisioningException(AccResultCode.PROVISIONING_BREAK_GLOBAL_CONFIG_DELETE, ImmutableMap.of("operationType", dto.getOperationType()));
		}
		breakRecipientService.deleteAllByBreakConfig(dto.getId());
		super.delete(dto, permission);
	}
	
	@Override
	public SysProvisioningBreakConfigDto save(SysProvisioningBreakConfigDto dto, BasePermission... permission) {
		// check global configuration
		if (dto.getGlobalConfiguration() != null && dto.getGlobalConfiguration()) {
			throw new ProvisioningException(AccResultCode.PROVISIONING_BREAK_GLOBAL_CONFIG_SAVE, ImmutableMap.of("operationType", dto.getOperationType()));
		}
		// check if for same system doesn't exist same operation type
		SysProvisioningBreakConfigFilter filter = new SysProvisioningBreakConfigFilter();
		filter.setSystemId(dto.getSystem());
		filter.setOperationType(dto.getOperationType());
		List<SysProvisioningBreakConfigDto> similarConfigs = this.find(filter, null).getContent();
		boolean existSimilar = similarConfigs.stream().filter(config -> !config.getId().equals(dto.getId())).findFirst().isPresent();
		if (!existSimilar) {
			return super.save(dto, permission);			
		}
		throw new ProvisioningException(AccResultCode.PROVISIONING_BREAK_OPERATION_EXISTS,
				ImmutableMap.of("operationType", dto.getOperationType(), "systemId", dto.getSystem()));
	}
	
	@Override
	public SysProvisioningBreakItems getCacheProcessedItems(UUID systemId) {
		Optional<Object> cachedValueWrapper = this.idmCacheManager.getValue(CACHE_NAME, systemId);
		SysProvisioningBreakItems cache = cachedValueWrapper.map(o -> (SysProvisioningBreakItems)o).orElseGet(() -> localCache.get(systemId));
		cachedValueWrapper.ifPresent(o -> localCache.remove(systemId));
		//
		return cache == null ? new SysProvisioningBreakItems() : cache;
	}

	@Override
	public void saveCacheProcessedItems(UUID systemId, SysProvisioningBreakItems cache) {
		if (!idmCacheManager.cacheValue(CACHE_NAME, systemId, cache)) {
			// if item was not cached by default cache, we will fallback to an in-memory hashmap
			LOG.warn("Cannot save provisioning brake info into cache. Using fallback in-memory map.");
			localCache.put(systemId, cache);
		}
	}
	
	@Override
	protected SysProvisioningBreakConfigDto toDto(SysProvisioningBreakConfig entity,
			SysProvisioningBreakConfigDto dto) {
		SysProvisioningBreakConfigDto newDto = super.toDto(entity, dto);
		//
		if (newDto != null) {
			// set provisioning break counter
			newDto.setActualOperationCount(getCounter(newDto.getSystem(), newDto.getOperationType()));
		}
		//
		return newDto;
	}

	@Override
	public SysProvisioningBreakConfigDto getConfig(ProvisioningEventType operationType, UUID systemId) {
		SysProvisioningBreakConfigFilter filter = new SysProvisioningBreakConfigFilter();
		filter.setOperationType(operationType);
		filter.setSystemId(systemId);
		List<SysProvisioningBreakConfigDto> configs = this.find(filter, null).getContent();
		//
		if (configs.isEmpty()) {
			return null;
		}
		// must exists only one configs for operation type and system id
		return configs.stream().findFirst().get();
	}

	@Override
	public SysProvisioningBreakConfigDto getGlobalBreakConfiguration(ProvisioningEventType eventType) {
		SysProvisioningBreakConfigDto globalConfig = new SysProvisioningBreakConfigDto();
		Boolean disable = provisioningBreakConfiguration.getDisabled(eventType);
		if (disable == null) {
			// global provisioning break configuration isn't set
			return null;
		}
		globalConfig.setDisabled(disable);
		globalConfig.setDisableLimit(provisioningBreakConfiguration.getDisableLimit(eventType));
		globalConfig.setGlobalConfiguration(Boolean.TRUE);
		//
		IdmNotificationTemplateDto disabledTemplate = provisioningBreakConfiguration.getDisableTemplate(eventType);
		if (disabledTemplate != null) {
			globalConfig.setDisableTemplate(disabledTemplate.getId());
			globalConfig.setDisableTemplateEmbedded(disabledTemplate);
		}
		//
		IdmNotificationTemplateDto warningTemplate = provisioningBreakConfiguration.getWarningTemplate(eventType);
		if (warningTemplate != null) {
			globalConfig.setWarningTemplate(warningTemplate.getId());
			globalConfig.setWarningTemplateEmbedded(warningTemplate);
		}
		//
		globalConfig.setOperationType(eventType);
		globalConfig.setPeriod(provisioningBreakConfiguration.getPeriod(eventType));
		globalConfig.setSystem(null); // global provisioning break hasn't system id, don't save global config
		globalConfig.setWarningLimit(provisioningBreakConfiguration.getWarningLimit(eventType));
		globalConfig.setTrimmed(true);
		return globalConfig;
	}
	
	@Override
	protected List<Predicate> toPredicates(Root<SysProvisioningBreakConfig> root, CriteriaQuery<?> query,
			CriteriaBuilder builder, SysProvisioningBreakConfigFilter filter) {
		List<Predicate> predicates = super.toPredicates(root, query, builder, filter);
		//
		if (filter.getSystemId() != null) {
			predicates.add(builder.equal(root.get(SysProvisioningBreakConfig_.system).get(AbstractEntity_.id), filter.getSystemId()));
		}
		//
		if (filter.getPeriod() != null) {
			predicates.add(builder.equal(root.get(SysProvisioningBreakConfig_.period), filter.getPeriod()));
		}
		//
		if (filter.getWarningLimit() != null) {
			predicates.add(builder.equal(root.get(SysProvisioningBreakConfig_.warningLimit), filter.getWarningLimit()));
		}
		//
		if (filter.getDisableLimit() != null) {
			predicates.add(builder.equal(root.get(SysProvisioningBreakConfig_.disableLimit), filter.getDisableLimit()));
		}
		//
		if (filter.getOperationType() != null) {
			predicates.add(builder.equal(root.get(SysProvisioningBreakConfig_.operationType), filter.getOperationType()));
		}
		//
		return predicates;
	}

	@Override
	public void clearCache(UUID systemId, ProvisioningEventType event) {
		SysProvisioningBreakItems cache = this.getCacheProcessedItems(systemId);
		cache.clearRecords(event);
	}
	
	@Override
	public void export(UUID id, IdmExportImportDto batch) {
		Assert.notNull(batch, "Export batch must exist!");
		// Export break configuration
		super.export(id, batch);

		// Export break recipients
		SysProvisioningBreakRecipientFilter provisioningBreakRecipientFilter = new SysProvisioningBreakRecipientFilter();
		provisioningBreakRecipientFilter.setBreakConfigId(id);
		List<SysProvisioningBreakRecipientDto> recipients = breakRecipientService
				.find(provisioningBreakRecipientFilter, null).getContent();
		if (recipients.isEmpty()) {
			breakRecipientService.export(ExportManager.BLANK_UUID, batch);
		}
		recipients.forEach(breakConfig -> {
			breakRecipientService.export(breakConfig.getId(), batch);
		});
		// Set parent field -> set authoritative mode.
		this.getExportManager().setAuthoritativeMode(SysProvisioningBreakRecipient_.breakConfig.getName(), "systemId", SysProvisioningBreakRecipientDto.class,
				batch);
	}
	
	/**
	 * Method return counter for system id and operation type
	 * 
	 * @param systemId
	 * @param operationType
	 * @return
	 */
	private Integer getCounter(UUID systemId, ProvisioningEventType operationType) {
		// set provisioning break counter
		SysProvisioningBreakItems cache = this.getCacheProcessedItems(systemId);
		return cache.getSize(operationType);
	}
	
	/**
	 * Methods for system and his provisioning break config add global configuration
	 *
	 * @param configsOld
	 * @param systemId
	 * @return
	 */
	private List<SysProvisioningBreakConfigDto> addGlobalConfigs(List<SysProvisioningBreakConfigDto> configsOld, UUID systemId) {
		boolean containsCreate = configsOld.stream().filter(item -> item.getOperationType() == ProvisioningEventType.CREATE).findFirst().isPresent();
		boolean containsDelete = configsOld.stream().filter(item -> item.getOperationType() == ProvisioningEventType.DELETE).findFirst().isPresent();
		boolean containsUpdate = configsOld.stream().filter(item -> item.getOperationType() == ProvisioningEventType.UPDATE).findFirst().isPresent();
		// unmodifiable list, create copy
		List<SysProvisioningBreakConfigDto> configs = new ArrayList<>(configsOld);
		if (!containsCreate) {
			SysProvisioningBreakConfigDto global = this.getGlobalBreakConfiguration(ProvisioningEventType.CREATE);
			if (global != null) {
				global.setActualOperationCount(getCounter(systemId, ProvisioningEventType.CREATE));
				configs.add(global);
			}
		}
		if (!containsDelete) {
			SysProvisioningBreakConfigDto global = this.getGlobalBreakConfiguration(ProvisioningEventType.DELETE);
			if (global != null) {
				global.setActualOperationCount(getCounter(systemId, ProvisioningEventType.DELETE));
				configs.add(global);
			}
		}
		if (!containsUpdate) {
			SysProvisioningBreakConfigDto global = this.getGlobalBreakConfiguration(ProvisioningEventType.UPDATE);
			if (global != null) {
				global.setActualOperationCount(getCounter(systemId, ProvisioningEventType.UPDATE));
				configs.add(global);
			}
		}
		return configs;
	}
}
