package eu.bcvsolutions.idm.core.rest.impl;

import java.util.Set;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resources;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.bcvsolutions.idm.core.api.config.swagger.SwaggerConfig;
import eu.bcvsolutions.idm.core.api.dto.IdmImportLogDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmImportLogFilter;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.BaseController;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.api.service.IdmImportLogService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

/**
 * Import log controller
 * 
 * @author Vít Švanda
 *
 */
@RestController
@RequestMapping(value = BaseDtoController.BASE_PATH + "/import-logs") 
@Api(
		value = IdmImportLogController.TAG,  
		tags = { IdmImportLogController.TAG }, 
		description = "Import logs",
		produces = BaseController.APPLICATION_HAL_JSON_VALUE,
		consumes = MediaType.APPLICATION_JSON_VALUE)
public class IdmImportLogController extends AbstractReadWriteDtoController<IdmImportLogDto, IdmImportLogFilter>  {

	protected static final String TAG = "Import logs";
	
	@Autowired
	public IdmImportLogController(
			IdmImportLogService service) {
		super(service);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.EXPORTIMPORT_READ + "')")
	@ApiOperation(
			value = "Search batches (/search/quick alias)", 
			nickname = "searchBatchs", 
			tags = { IdmImportLogController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_READ, description = "") })
				})
	public Resources<?> find(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.find(parameters, pageable);
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/search/quick", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.EXPORTIMPORT_READ + "')")
	@ApiOperation(
			value = "Search batches", 
			nickname = "searchQuickBatchs", 
			tags = { IdmImportLogController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_READ, description = "") })
				})
	public Resources<?> findQuick(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.findQuick(parameters, pageable);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/search/autocomplete", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.EXPORTIMPORT_AUTOCOMPLETE + "')")
	@ApiOperation(
			value = "Autocomplete batches (selectbox usage)", 
			nickname = "autocompleteBatchs", 
			tags = { IdmImportLogController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_AUTOCOMPLETE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_AUTOCOMPLETE, description = "") })
				})
	public Resources<?> autocomplete(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters, 
			@PageableDefault Pageable pageable) {
		return super.autocomplete(parameters, pageable);
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.EXPORTIMPORT_READ + "')")
	@ApiOperation(
			value = "Batch detail", 
			nickname = "getBatch", 
			response = IdmImportLogDto.class, 
			tags = { IdmImportLogController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_READ, description = "") })
				})
	public ResponseEntity<?> get(
			@ApiParam(value = "Batch's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.get(backendId);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.DELETE)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.EXPORTIMPORT_DELETE + "')")
	@ApiOperation(
			value = "Delete batch", 
			nickname = "deleteBatch", 
			tags = { IdmImportLogController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_DELETE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_DELETE, description = "") })
				})
	public ResponseEntity<?> delete(
			@ApiParam(value = "Batch's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.delete(backendId);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}/permissions", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.EXPORTIMPORT_READ + "')"
			+ " or hasAuthority('" + CoreGroupPermission.EXPORTIMPORT_AUTOCOMPLETE + "')")
	@ApiOperation(
			value = "What logged identity can do with given record", 
			nickname = "getPermissionsOnBatch", 
			tags = { IdmImportLogController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_READ, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_AUTOCOMPLETE, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_READ, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_AUTOCOMPLETE, description = "")})
				})
	public Set<String> getPermissions(
			@ApiParam(value = "Batch's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.getPermissions(backendId);
	}

	@Override
	protected IdmImportLogFilter toFilter(MultiValueMap<String, Object> parameters) {
		IdmImportLogFilter filter = new IdmImportLogFilter(parameters);
		// If parent property contains ID of IdmImportLog, then we need to change filter to DTO id.
		if (filter.getParent() != null) {
			IdmImportLogDto logDto = getService().get(filter.getParent());
			if (logDto != null) {
				filter.setParent(logDto.getDtoId());
				filter.setBatchId(logDto.getBatch());
			}
		}
		return filter;
	}

}
