package eu.bcvsolutions.idm.core.rest.impl;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.multipart.MultipartFile;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.config.swagger.SwaggerConfig;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmExportImportFilter;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.rest.AbstractReadWriteDtoController;
import eu.bcvsolutions.idm.core.api.rest.BaseController;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.api.service.ExportManager;
import eu.bcvsolutions.idm.core.api.service.IdmExportImportService;
import eu.bcvsolutions.idm.core.api.service.ImportManager;
import eu.bcvsolutions.idm.core.api.utils.SpinalCase;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

/**
 * Export/Import controller
 * 
 * @author Vít Švanda
 *
 */
@RestController
@RequestMapping(value = BaseDtoController.BASE_PATH + "/export-imports") 
@Api(
		value = IdmExportImportController.TAG,  
		tags = { IdmExportImportController.TAG }, 
		description = "Exports and imports",
		produces = BaseController.APPLICATION_HAL_JSON_VALUE,
		consumes = MediaType.APPLICATION_JSON_VALUE)
public class IdmExportImportController extends AbstractReadWriteDtoController<IdmExportImportDto, IdmExportImportFilter>  {

	protected static final String TAG = "Exports";
	@Autowired
	private ImportManager importManager;
	
	@Autowired
	public IdmExportImportController(
			IdmExportImportService service) {
		super(service);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.EXPORTIMPORT_READ + "')")
	@ApiOperation(
			value = "Search batchs (/search/quick alias)", 
			nickname = "searchBatchs", 
			tags = { IdmExportImportController.TAG }, 
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
			value = "Search batchs", 
			nickname = "searchQuickBatchs", 
			tags = { IdmExportImportController.TAG }, 
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
			value = "Autocomplete batchs (selectbox usage)", 
			nickname = "autocompleteBatchs", 
			tags = { IdmExportImportController.TAG }, 
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
			response = IdmExportImportDto.class, 
			tags = { IdmExportImportController.TAG }, 
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

	/**
	 * Upload new import
	 * 
	 * @param name
	 * @param fileName
	 * @param data
	 * @return
	 * @throws IOException
	 */
	@ResponseBody
	@RequestMapping(method = RequestMethod.POST)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.EXPORTIMPORT_CREATE + "')")
	@ApiOperation(
			value = "Upload new import zip. New import batch will be created.", 
			nickname = "uploadImport", 
			response = IdmExportImportDto.class, 
			tags = { IdmExportImportController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_CREATE, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_CREATE, description = "")})
				},
			notes = "Upload new import zip. New import batch will be created.")
	public Resource<IdmExportImportDto> uploadImport(String name, String fileName, MultipartFile data)
			throws IOException {
		IdmExportImportDto batch = importManager.uploadImport(name, fileName, data.getInputStream(), IdmBasePermission.CREATE);
		Link selfLink = ControllerLinkBuilder.linkTo(this.getClass()).slash(batch.getId()).withSelfRel();
		
		return new Resource<IdmExportImportDto>(batch, selfLink);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.DELETE)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.EXPORTIMPORT_DELETE + "')")
	@ApiOperation(
			value = "Delete batch", 
			nickname = "deleteBatch", 
			tags = { IdmExportImportController.TAG }, 
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
			tags = { IdmExportImportController.TAG }, 
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
	
	@ResponseBody
	@RequestMapping(value = "/{backendId}/download", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.EXPORTIMPORT_READ + "')")
	@ApiOperation(
			value = "Download export", 
			nickname = "downloadExport", 
			tags = { IdmExportImportController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_READ, description = "") })
				})
	public ResponseEntity<InputStreamResource> download(
			@ApiParam(value = "Batch's uuid identifier.", required = true)
			@PathVariable @NotNull String backendId) {
		//
		IdmExportImportDto batch = getDto(backendId);
		if (batch == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", backendId));
		}
		try {
			// Batch read rights check was performed above (getDto).
			InputStream is = ((IdmExportImportService)getService()).download(batch);
			//
			// Generate name of ZIP from batch name.
			String zipName = batch.getExecutorName();
			if (Strings.isNotEmpty(batch.getName())) {
				String spinaledName = SpinalCase.format(batch.getName());
				if (spinaledName.length() > 30) {
					spinaledName = spinaledName.substring(0, 29);
				}
				zipName = MessageFormat.format("{0}.{1}", spinaledName, ExportManager.EXTENSION_ZIP);
			}
			return ResponseEntity.ok()
					.contentLength(is.available())
					.contentType(MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", zipName))
					.body(new InputStreamResource(is));
		} catch (Exception ex) {
			throw new ResultCodeException(CoreResultCode.INTERNAL_SERVER_ERROR, ex);
		}
	}
	
	/**
	 * Execute import
	 * 
	 * @param backendId
	 * @return
	 */
	@ResponseBody
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.EXPORTIMPORT_UPDATE + "')")
	@RequestMapping(value = "/{backendId}/execute-import", method = RequestMethod.PUT)
	@ApiOperation(value = "Execute import", nickname = "executeImport", response = IdmExportImportDto.class, tags = {
			IdmExportImportController.TAG }, authorizations = {
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = {
							@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_UPDATE, description = "") }),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = {
							@AuthorizationScope(scope = CoreGroupPermission.EXPORTIMPORT_UPDATE, description = "") }) }, notes = "Execute import")
	public ResponseEntity<?> executeImport(
			@ApiParam(value = "Import batch UUID identifier.", required = true) @PathVariable @NotNull String backendId,
			@ApiParam(value = "Import batch is executed as dry run." ) @RequestParam("dryRun") boolean dryRun) {

		return new ResponseEntity<>(
				toResource(importManager.executeImport(this.getService().get(backendId), dryRun)),
				HttpStatus.OK);
	}

	
	@Override
	protected IdmExportImportFilter toFilter(MultiValueMap<String, Object> parameters) {
		IdmExportImportFilter filter = new IdmExportImportFilter(parameters);
		filter.setFrom(getParameterConverter().toDateTime(parameters, "from"));
		filter.setTill(getParameterConverter().toDateTime(parameters, "till"));
		return filter;
	}

}
