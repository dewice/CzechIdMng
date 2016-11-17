package eu.bcvsolutions.idm.acc.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.bcvsolutions.idm.acc.domain.SystemEntityType;
import eu.bcvsolutions.idm.acc.domain.SystemOperationType;
import eu.bcvsolutions.idm.acc.dto.SystemEntityHandlingFilter;
import eu.bcvsolutions.idm.acc.entity.SysSystem;
import eu.bcvsolutions.idm.acc.entity.SysSystemEntityHandling;
import eu.bcvsolutions.idm.acc.repository.SysSystemEntityHandlingRepository;
import eu.bcvsolutions.idm.acc.service.api.SysSystemEntityHandlingService;
import eu.bcvsolutions.idm.core.api.repository.AbstractEntityRepository;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteEntityService;

/**
 * Default system entity handling
 * 
 * @author svandav
 *
 */
@Service
public class DefaultSysSystemEntityHandlingService extends
		AbstractReadWriteEntityService<SysSystemEntityHandling, SystemEntityHandlingFilter> implements SysSystemEntityHandlingService {

	@Autowired
	private SysSystemEntityHandlingRepository repository;

	@Override
	protected AbstractEntityRepository<SysSystemEntityHandling, SystemEntityHandlingFilter> getRepository() {
		return repository;
	}
	
	public List<SysSystemEntityHandling> findBySystem(SysSystem system, SystemOperationType operation, SystemEntityType entityType){
		Assert.notNull(system);
		
		SystemEntityHandlingFilter filter = new SystemEntityHandlingFilter();
		filter.setSystemId(system.getId());
		filter.setOperationType(operation);
		filter.setEntityType(entityType);
		Page<SysSystemEntityHandling> page = repository.find(filter, null);
		return page.getContent();
	}
}
