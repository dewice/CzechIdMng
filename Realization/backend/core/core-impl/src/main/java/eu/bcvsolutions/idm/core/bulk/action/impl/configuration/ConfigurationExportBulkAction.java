package eu.bcvsolutions.idm.core.bulk.action.impl.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.core.api.bulk.action.AbstractExportBulkAction;
import eu.bcvsolutions.idm.core.api.dto.IdmConfigurationDto;
import eu.bcvsolutions.idm.core.api.dto.filter.DataFilter;
import eu.bcvsolutions.idm.core.api.service.IdmConfigurationService;
import eu.bcvsolutions.idm.core.api.service.ReadWriteDtoService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;

/**
 * Bulk operation to export the configuration
 * 
 * @author Vít Švanda
 *
 */
@Component("configurationExportBulkAction")
@Description("Bulk operation to export the configuration.")
public class ConfigurationExportBulkAction extends AbstractExportBulkAction<IdmConfigurationDto, DataFilter> {

	public static final String NAME = "configuration-export-bulk-action";

	@Autowired
	private IdmConfigurationService configurationService;
	
	@Override
	protected void exportDto(IdmConfigurationDto dto) {
		IdmConfigurationDto configurationDto = configurationService.get(dto.getId(), IdmBasePermission.READ);
		
		// Create new batch (if doesn't exist)
		initBatch("Export of configurations");

		// Export system
		configurationService.export(configurationDto.getId(), getBatch());
	}
	

	@Override
	public List<String> getAuthorities() {
		List<String> authorities = super.getAuthorities();
		authorities.add(CoreGroupPermission.CONFIGURATION_READ);
		authorities.add(CoreGroupPermission.EXPORTIMPORT_CREATE);
		authorities.add(CoreGroupPermission.EXPORTIMPORT_READ);
		authorities.add(CoreGroupPermission.EXPORTIMPORT_UPDATE);
		
		return authorities;
	}

	@Override
	protected List<String> getAuthoritiesForEntity() {
		return Lists.newArrayList(CoreGroupPermission.CONFIGURATION_READ);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getOrder() {
		return super.getOrder();
	}

	@Override
	public ReadWriteDtoService<IdmConfigurationDto, DataFilter> getService() {
		return configurationService;
	}


}
