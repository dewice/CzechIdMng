package eu.bcvsolutions.idm.tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import eu.bcvsolutions.idm.core.api.domain.PropertyModuleDescriptor;
import eu.bcvsolutions.idm.core.api.domain.ResultCode;
import eu.bcvsolutions.idm.core.notification.api.dto.NotificationConfigurationDto;
import eu.bcvsolutions.idm.tool.domain.ToolResultCode;

/**
 * Tool module descriptor
 *
 * @author BCV solutions s.r.o.
 */
@Component
@PropertySource("classpath:module-" + ToolModuleDescriptor.MODULE_ID + ".properties")
@ConfigurationProperties(prefix = "module." + ToolModuleDescriptor.MODULE_ID + ".build", ignoreUnknownFields = true, ignoreInvalidFields = true)
public class ToolModuleDescriptor extends PropertyModuleDescriptor {

	public static final String MODULE_ID = "tool";

	@Override
	public String getId() {
		return MODULE_ID;
	}

	/**
	 * Enables links to swagger documentation
	 */
	@Override
	public boolean isDocumentationAvailable() {
		return true;
	}

	@Override
	public List<NotificationConfigurationDto> getDefaultNotificationConfigurations() {
		List<NotificationConfigurationDto> configs = new ArrayList<>();
		return configs;
	}

	@Override
	public List<ResultCode> getResultCodes() {
		return Arrays.asList(ToolResultCode.values());
	}
}
