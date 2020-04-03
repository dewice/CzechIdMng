package eu.bcvsolutions.idm.core.rest.impl;


import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.audit.dto.IdmAuditDto;
import eu.bcvsolutions.idm.core.api.audit.service.IdmAuditService;
import eu.bcvsolutions.idm.core.api.bulk.action.dto.IdmBulkActionDto;
import eu.bcvsolutions.idm.core.api.config.domain.PrivateIdentityConfiguration;
import eu.bcvsolutions.idm.core.api.config.swagger.SwaggerConfig;
import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.domain.IdentityState;
import eu.bcvsolutions.idm.core.api.domain.OperationState;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityContractDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmPasswordDto;
import eu.bcvsolutions.idm.core.api.dto.IdmProfileDto;
import eu.bcvsolutions.idm.core.api.dto.IdmRoleDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeNodeDto;
import eu.bcvsolutions.idm.core.api.dto.IdmTreeTypeDto;
import eu.bcvsolutions.idm.core.api.dto.OperationResultDto;
import eu.bcvsolutions.idm.core.api.dto.ResolvedIncompatibleRoleDto;
import eu.bcvsolutions.idm.core.api.dto.ResultModels;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmIdentityRoleFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmProfileFilter;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmRoleRequestFilter;
import eu.bcvsolutions.idm.core.api.exception.EntityNotFoundException;
import eu.bcvsolutions.idm.core.api.exception.ForbiddenEntityException;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.rest.AbstractEventableDtoController;
import eu.bcvsolutions.idm.core.api.rest.BaseController;
import eu.bcvsolutions.idm.core.api.rest.BaseDtoController;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityContractService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.api.service.IdmIncompatibleRoleService;
import eu.bcvsolutions.idm.core.api.service.IdmPasswordService;
import eu.bcvsolutions.idm.core.api.service.IdmProfileService;
import eu.bcvsolutions.idm.core.api.service.IdmRoleRequestService;
import eu.bcvsolutions.idm.core.api.service.IdmTreeNodeService;
import eu.bcvsolutions.idm.core.api.utils.DtoUtils;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormDefinitionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormInstanceDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormProjectionDto;
import eu.bcvsolutions.idm.core.eav.api.dto.IdmFormValueDto;
import eu.bcvsolutions.idm.core.eav.api.dto.filter.IdmFormAttributeFilter;
import eu.bcvsolutions.idm.core.eav.api.service.FormService;
import eu.bcvsolutions.idm.core.eav.rest.impl.IdmFormDefinitionController;
import eu.bcvsolutions.idm.core.ecm.api.dto.IdmAttachmentDto;
import eu.bcvsolutions.idm.core.ecm.api.service.AttachmentManager;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.dto.WorkPositionDto;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentityRole_;
import eu.bcvsolutions.idm.core.model.service.api.CheckLongPollingResult;
import eu.bcvsolutions.idm.core.model.service.api.LongPollingManager;
import eu.bcvsolutions.idm.core.rest.DeferredResultWrapper;
import eu.bcvsolutions.idm.core.rest.LongPollingSubscriber;
import eu.bcvsolutions.idm.core.security.api.domain.IdentityBasePermission;
import eu.bcvsolutions.idm.core.security.api.domain.IdmBasePermission;
import eu.bcvsolutions.idm.core.security.api.service.GrantedAuthoritiesFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;

/**
 * Rest methods for IdmIdentity resource
 * 
 * @author Radek Tomiška
 * @author Petr Hanák
 */
@RestController
@RequestMapping(value = BaseDtoController.BASE_PATH + "/identities") //produces= BaseController.APPLICATION_HAL_JSON_VALUE - I have to remove this (username cannot have "@.com" in user name)
@Api(
		value = IdmIdentityController.TAG,  
		tags = { IdmIdentityController.TAG }, 
		description = "Operations with identities",
		produces = BaseController.APPLICATION_HAL_JSON_VALUE,
		consumes = MediaType.APPLICATION_JSON_VALUE)
public class IdmIdentityController extends AbstractEventableDtoController<IdmIdentityDto, IdmIdentityFilter> {

	protected static final String TAG = "Identities";
	//
	@Autowired private GrantedAuthoritiesFactory grantedAuthoritiesFactory;
	@Autowired private IdmIdentityContractService identityContractService;
	@Autowired private IdmIdentityRoleService identityRoleService;
	@Autowired private IdmAuditService auditService;
	@Autowired private IdmTreeNodeService treeNodeService;
	@Autowired private PrivateIdentityConfiguration identityConfiguration;
	@Autowired private IdmProfileService profileService;
	@Autowired private AttachmentManager attachmentManager;
	@Autowired private IdmIncompatibleRoleService incompatibleRoleService;
	//
	@Autowired private IdmFormDefinitionController formDefinitionController;
	@Autowired private IdmProfileController profileController;
	@Autowired private FormService formService;
	@Autowired private IdmPasswordService passwordService;
	@Autowired private IdmPasswordController passwordController;
	@Autowired private IdmRoleRequestService roleRequestService;
	@Autowired private LongPollingManager longPollingManager;
	//
	private final IdmIdentityService identityService;

	@Autowired
	public IdmIdentityController(IdmIdentityService identityService) {
		super(identityService);
		//
		this.identityService = identityService;
	}

	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Search identities (/search/quick alias)", 
			nickname = "searchIdentities", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") })
				})
	public Resources<?> find(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.find(parameters, pageable);
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/search/quick", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Search identities", 
			nickname = "searchQuickIdentities", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") })
				})
	public Resources<?> findQuick(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters,
			@PageableDefault Pageable pageable) {
		return super.findQuick(parameters, pageable);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/search/autocomplete", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_AUTOCOMPLETE + "')")
	@ApiOperation(
			value = "Autocomplete identities (selectbox usage)", 
			nickname = "autocompleteIdentities", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_AUTOCOMPLETE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_AUTOCOMPLETE, description = "") })
				})
	public Resources<?> autocomplete(
			@RequestParam(required = false) MultiValueMap<String, Object> parameters, 
			@PageableDefault Pageable pageable) {
		return super.autocomplete(parameters, pageable);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/search/count", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_COUNT + "')")
	@ApiOperation(
			value = "The number of entities that match the filter", 
			nickname = "countIdentities", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_COUNT, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_COUNT, description = "") })
				})
	public long count(@RequestParam(required = false) MultiValueMap<String, Object> parameters) {
		return super.count(parameters);
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Identity detail", 
			nickname = "getIdentity", 
			response = IdmIdentityDto.class, 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") })
				})
	public ResponseEntity<?> get(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable @NotNull String backendId) {
		IdmIdentityDto identityDto = this.getDto(backendId);
		if (identityDto == null) {
			throw new EntityNotFoundException(getService().getEntityClass(), backendId);
		}
		//
		// set information about password
		// password may not exist, set block login date only if active
		IdmPasswordDto passwordDto = passwordService.findOneByIdentity(identityDto.getId());
		if (passwordDto != null && passwordDto.getBlockLoginDate() != null && passwordDto.getBlockLoginDate().isAfter(ZonedDateTime.now())) {
			identityDto.setBlockLoginDate(passwordDto.getBlockLoginDate());
		}
		//
		ResourceSupport resource = toResource(identityDto);
		if (resource == null) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
		//
		return new ResponseEntity<>(resource, HttpStatus.OK);
	}

	@Override
	@ResponseBody
	@RequestMapping(method = RequestMethod.POST)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_CREATE + "')"
			+ " or hasAuthority('" + CoreGroupPermission.IDENTITY_UPDATE + "')")
	@ApiOperation(
			value = "Create / update identity", 
			nickname = "postIdentity", 
			response = IdmIdentityDto.class, 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_CREATE, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_UPDATE, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_CREATE, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_UPDATE, description = "")})
				})
	public ResponseEntity<?> post(@Valid @RequestBody IdmIdentityDto dto) {
		return super.post(dto);
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.PUT)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_UPDATE + "')")
	@ApiOperation(
			value = "Update identity", 
			nickname = "putIdentity", 
			response = IdmIdentityDto.class, 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_UPDATE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_UPDATE, description = "") })
				})
	public ResponseEntity<?> put(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable @NotNull String backendId, 
			@Valid @RequestBody IdmIdentityDto dto) {
		return super.put(backendId, dto);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.PATCH)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_UPDATE + "')")
	@ApiOperation(
			value = "Update identity", 
			nickname = "patchIdentity", 
			response = IdmIdentityDto.class, 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_UPDATE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_UPDATE, description = "") })
				})
	public ResponseEntity<?> patch(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable @NotNull String backendId,
			HttpServletRequest nativeRequest)
			throws HttpMessageNotReadableException {
		return super.patch(backendId, nativeRequest);
	}
	
	/**
	 * @since 7.6.0
	 * @param backendId
	 * @return
	 */
	@ResponseBody
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_MANUALLYENABLE + "')")
	@RequestMapping(value = "/{backendId}/enable", method = RequestMethod.PATCH)
	@ApiOperation(
			value = "Activate identity", 
			nickname = "activateIdentity", 
			response = IdmIdentityDto.class, 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_MANUALLYENABLE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_MANUALLYENABLE, description = "") })
				},
			notes = "Enable manually disabled identity. Identity will have automatically recounted state assigned by their contract state." )
	public ResponseEntity<?> enable(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable @NotNull String backendId) {
		IdmIdentityDto identity = getDto(backendId);
		if (identity == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", backendId));
		}
		return new ResponseEntity<>(toResource(identityService.enable(identity.getId(), IdentityBasePermission.MANUALLYENABLE)), HttpStatus.OK);
	}
	
	/**
	 * @since 7.6.0
	 * @param backendId
	 * @return
	 */
	@ResponseBody
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_MANUALLYDISABLE + "')")
	@RequestMapping(value = "/{backendId}/disable", method = RequestMethod.PATCH)
	@ApiOperation(
			value = "Disable identity", 
			nickname = "disableIdentity", 
			response = IdmIdentityDto.class, 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_MANUALLYDISABLE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_MANUALLYDISABLE, description = "") })
				},
			notes = "Disable identity manually. This identity will be disabled even with valid contracts."
					+ " Identity can be enabled manually again only. See 'enable' method." )
	public ResponseEntity<?> disable(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable @NotNull String backendId) {
		IdmIdentityDto identity = getDto(backendId);
		if (identity == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", backendId));
		}
		return new ResponseEntity<>(toResource(identityService.disable(identity.getId(), IdentityBasePermission.MANUALLYDISABLE)), HttpStatus.OK);
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}", method = RequestMethod.DELETE)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_DELETE + "')")
	@ApiOperation(
			value = "Delete identity", 
			nickname = "deleteIdentity", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_DELETE, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_DELETE, description = "") })
				})
	public ResponseEntity<?> delete(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.delete(backendId);
	}
	
	@Override
	@ResponseBody
	@RequestMapping(value = "/{backendId}/permissions", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')"
			+ " or hasAuthority('" + CoreGroupPermission.IDENTITY_AUTOCOMPLETE + "')")
	@ApiOperation(
			value = "What logged identity can do with given record", 
			nickname = "getPermissionsOnIdentity", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_AUTOCOMPLETE, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_AUTOCOMPLETE, description = "")})
				})
	public Set<String> getPermissions(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable @NotNull String backendId) {
		return super.getPermissions(backendId);
	}
	
	/**
	 * Get available bulk actions for identity
	 *
	 * @return
	 */
	@Override
	@ResponseBody
	@RequestMapping(value = "/bulk/actions", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Get available bulk actions", 
			nickname = "availableBulkAction", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") })
				})
	public List<IdmBulkActionDto> getAvailableBulkActions() {
		return super.getAvailableBulkActions();
	}
	
	/**
	 * Process bulk action for identities
	 *
	 * @param bulkAction
	 * @return
	 */
	@Override
	@ResponseBody
	@RequestMapping(path = "/bulk/action", method = RequestMethod.POST)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Process bulk action for identity", 
			nickname = "bulkAction", 
			response = IdmBulkActionDto.class, 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "")})
				})
	public ResponseEntity<IdmBulkActionDto> bulkAction(@Valid @RequestBody IdmBulkActionDto bulkAction) {
		return super.bulkAction(bulkAction);
	}
	
	/**
	 * Prevalidate bulk action for identities
	 *
	 * @param bulkAction
	 * @return
	 */
	@Override
	@ResponseBody
	@RequestMapping(path = "/bulk/prevalidate", method = RequestMethod.POST)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Prevalidate bulk action for identities", 
			nickname = "prevalidateBulkAction", 
			response = IdmBulkActionDto.class, 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "")})
				})
	public ResponseEntity<ResultModels> prevalidateBulkAction(@Valid @RequestBody IdmBulkActionDto bulkAction) {
		return super.prevalidateBulkAction(bulkAction);
	}

	/**
	 * Returns given identity's granted authorities
	 * 
	 * @param backendId
	 * @return list of granted authorities
	 */
	@ResponseBody
	@RequestMapping(value = "/{backendId}/authorities", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Identity granted authorities", 
			nickname = "getIdentityAuthorities", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") })
				})
	public List<? extends GrantedAuthority> getGrantedAuthotrities(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable String backendId) {
		IdmIdentityDto identity = getDto(backendId);
		if (identity == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", backendId));
		}
		checkAccess(identity, IdmBasePermission.READ);
		//
		return grantedAuthoritiesFactory.getGrantedAuthorities(identity.getUsername());
	}
	
	@ResponseBody
	@RequestMapping(value = "/{backendId}/incompatible-roles", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Incompatible roles assigned to identity", 
			nickname = "getIdentityIncompatibleRoles", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") })
				},
			notes = "Incompatible roles are resolved from assigned identity roles, which can logged used read.")
	public Resources<?> getIncompatibleRoles(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable String backendId) {	
		IdmIdentityDto identity = getDto(backendId);
		if (identity == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", backendId));
		}
		//
		IdmIdentityRoleFilter filter = new IdmIdentityRoleFilter();
		filter.setIdentityId(identity.getId());		
		List<IdmIdentityRoleDto> identityRoles = identityRoleService.find(filter, null, IdmBasePermission.READ).getContent();
		//
		Set<ResolvedIncompatibleRoleDto> incompatibleRoles = incompatibleRoleService.resolveIncompatibleRoles(
				identityRoles
					.stream()
					.map(ir -> {
						IdmRoleDto role = DtoUtils.getEmbedded(ir, IdmIdentityRole_.role);
						//
						return role;
					})
					.collect(Collectors.toList())
				);
		//
		return toResources(incompatibleRoles, ResolvedIncompatibleRoleDto.class);
	}
	
	/**
	 * Get given identity's prime position in organization.
	 * 
	 * @param backendId
	 * @return Positions from root to closest parent
	 */
	@ResponseBody
	@RequestMapping(value = "/{backendId}/work-position", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Identity prime position in organization.", 
			nickname = "getIdentityPosition", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") })
				})
	public ResponseEntity<?> organizationPosition(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable String backendId) {
		IdmIdentityDto identity = getDto(backendId);
		if (identity == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", backendId));
		}
		//
		IdmIdentityContractDto primeContract = identityContractService.getPrimeContract(identity.getId());
		if (primeContract == null) {
			return new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
		}
		WorkPositionDto position = new WorkPositionDto(identity, primeContract);
		if (primeContract.getWorkPosition() != null) {
			IdmTreeNodeDto contractPosition = treeNodeService.get(primeContract.getWorkPosition());
			position.getPath().addAll(treeNodeService.findAllParents(contractPosition.getId(), new Sort(Direction.ASC, "forestIndex.lft")));
			position.getPath().add(contractPosition);
		}
		return new ResponseEntity<WorkPositionDto>(position, HttpStatus.OK);
	}
	
	@ResponseBody
	@RequestMapping(value = "/{backendId}/revisions/{revId}", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Identity audit - read revision detail", 
			nickname = "getIdentityRevision", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") })
				})
	public ResponseEntity<?> findRevision(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable("backendId") String backendId, 
			@ApiParam(value = "Revision identifier.", required = true)
			@PathVariable("revId") Long revId) {
		IdmIdentityDto originalEntity = getDto(backendId);
		if (originalEntity == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", backendId));
		}
		//
		IdmIdentity revisionIdentity;
		try {
			revisionIdentity = this.auditService.findRevision(IdmIdentity.class, originalEntity.getId(), revId);
		} catch (RevisionDoesNotExistException ex) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND,  ImmutableMap.of("revision", revId), ex);
		}
		// FIXME: to dto
		return new ResponseEntity<>(revisionIdentity, HttpStatus.OK);
	}
	
	@ResponseBody
	@RequestMapping(value = "/{backendId}/revisions", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Identity audit - read all revisions", 
			nickname = "getIdentityRevisions", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") })
				})
	public Resources<?> findRevisions(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable("backendId") String backendId, 
			Pageable pageable) {
		IdmIdentityDto originalEntity = getDto(backendId);
		if (originalEntity == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("identity", backendId));
		}
		// get original entity id
		Page<IdmAuditDto> results = this.auditService.findRevisionsForEntity(IdmIdentity.class.getSimpleName(), originalEntity.getId(), pageable);
		return toResources(results, IdmAuditDto.class);
	}
	
	/**
	 * Returns form definition to given identity.
	 * 
	 * @param backendId
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/{backendId}/form-definitions", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Identity extended attributes form definitions", 
			nickname = "getIdentityFormDefinitions", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.FORM_DEFINITION_AUTOCOMPLETE, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.FORM_DEFINITION_AUTOCOMPLETE, description = "")})
				})
	public ResponseEntity<?> getFormDefinitions(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable @NotNull String backendId) {
		return formDefinitionController.getDefinitions(
				IdmIdentity.class, 
				identityConfiguration.isFormAttributesSecured() ? IdmBasePermission.AUTOCOMPLETE : null);
	}
	
	/**
	 * Returns filled form values
	 * 
	 * @param backendId
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/{backendId}/form-values", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Identity form definition - read values", 
			nickname = "getIdentityFormValues", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "")})
				})
	public Resource<?> getFormValues(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable @NotNull String backendId, 
			@ApiParam(value = "Code of form definition (default will be used if no code is given).", required = false, defaultValue = FormService.DEFAULT_DEFINITION_CODE)
			@RequestParam(name = IdmFormAttributeFilter.PARAMETER_FORM_DEFINITION_CODE, required = false) String definitionCode) {
		IdmFormDefinitionDto formDefinition = formDefinitionController.getDefinition(
				IdmIdentity.class, 
				definitionCode, 
				identityConfiguration.isFormAttributesSecured() ? IdmBasePermission.AUTOCOMPLETE : null);
		//
		IdmIdentityDto dto = getDto(backendId);
		if (dto == null) {		
			// empty form instance with filled form definition
			IdmFormInstanceDto formInstance = new IdmFormInstanceDto();
			formInstance.setFormDefinition(formDefinition);
			// secure attributes
			if (identityConfiguration.isFormAttributesSecured()) {
				formDefinitionController.secureAttributes(formInstance);
			}
			//
			return new Resource<>(formInstance);
		}
		//
		Resource<IdmFormInstanceDto> formValues = formDefinitionController.getFormValues(
				dto,
				formDefinition,
				identityConfiguration.isFormAttributesSecured() ? IdmBasePermission.READ : null);	
		//
		if (!identityConfiguration.isFormAttributesSecured()) {
			// we need to iterate through attributes and make them read only, if identity cannot be updated
			try {
				checkAccess(dto, IdmBasePermission.UPDATE);
			} catch (ForbiddenEntityException ex) {
				formValues.getContent().getFormDefinition().getFormAttributes().forEach(formAttribute -> {
					formAttribute.setReadonly(true);
				});
			}
		}
		//
		return formValues;
	}
	
	/**
	 * Saves connector configuration form values
	 * 
	 * @param backendId
	 * @param formValues
	 * @return
	 */
	@ResponseBody
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_UPDATE + "')"
			+ "or hasAuthority('" + CoreGroupPermission.FORM_VALUE_UPDATE + "')")
	@RequestMapping(value = "/{backendId}/form-values", method = { RequestMethod.POST, RequestMethod.PATCH })
	@ApiOperation(
			value = "Identity form definition - save values", 
			nickname = "postIdentityFormValues", 
			tags = { IdmIdentityController.TAG }, 
			notes = "Only given form attributes by the given values will be saved.",
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_UPDATE, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.FORM_VALUE_UPDATE, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_UPDATE, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.FORM_VALUE_UPDATE, description = "")})
				})
	public Resource<?> saveFormValues(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable @NotNull String backendId,
			@ApiParam(value = "Code of form definition (default will be used if no code is given).", required = false, defaultValue = FormService.DEFAULT_DEFINITION_CODE)
			@RequestParam(name = IdmFormAttributeFilter.PARAMETER_FORM_DEFINITION_CODE, required = false) String definitionCode,
			@ApiParam(value = "Filled form data.", required = true)
			@RequestBody @Valid List<IdmFormValueDto> formValues) {		
		IdmIdentityDto dto = getDto(backendId);
		if (dto == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", backendId));
		}
		if (!identityConfiguration.isFormAttributesSecured()) {
			// if eav form value are not secured by authorization policies => check security by identity
			checkAccess(dto, IdmBasePermission.UPDATE);
		}
		//
		IdmFormDefinitionDto formDefinition = formDefinitionController.getDefinition(
				IdmIdentity.class, 
				definitionCode, 
				identityConfiguration.isFormAttributesSecured() ? IdmBasePermission.AUTOCOMPLETE : null);
		//
		return formDefinitionController.saveFormValues(
				dto, 
				formDefinition, 
				formValues, 
				identityConfiguration.isFormAttributesSecured() ? IdmBasePermission.UPDATE : null);
	}
	
	/**
	 * Save entity's form value
	 * 
	 * @param backendId
	 * @param formValues
	 * @return
	 * @since 9.4.0
	 */
	@ResponseBody
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_UPDATE + "')"
			+ "or hasAuthority('" + CoreGroupPermission.FORM_VALUE_UPDATE + "')")
	@RequestMapping(value = "/{backendId}/form-value", method = { RequestMethod.POST } )
	@ApiOperation(
			value = "Identity form definition - save value", 
			nickname = "postRoleFormValue", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_UPDATE, description = ""),
							@AuthorizationScope(scope = CoreGroupPermission.FORM_VALUE_UPDATE, description = "")}),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_UPDATE, description = ""),
							@AuthorizationScope(scope = CoreGroupPermission.FORM_VALUE_UPDATE, description = "")})
					})
	public Resource<?> saveFormValue(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable @NotNull String backendId,
			@RequestBody @Valid IdmFormValueDto formValue) {		
		IdmIdentityDto dto = getDto(backendId);
		if (dto == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", backendId));
		}
		if (!identityConfiguration.isFormAttributesSecured()) {
			// if eav form value are not secured by authorization policies => check security by identity
			checkAccess(dto, IdmBasePermission.UPDATE);
		}
		//
		return formDefinitionController.saveFormValue(
				dto,
				formValue,
				identityConfiguration.isFormAttributesSecured() ? IdmBasePermission.UPDATE : null);
	}
	
	/**
	 * Returns input stream to attachment saved in given form value.
	 * 
	 * @param backendId
	 * @param formValueId
	 * @return
	 * @since 9.4.0
	 */
	@RequestMapping(value = "/{backendId}/form-values/{formValueId}/download", method = RequestMethod.GET)
	@ResponseBody
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Download form value attachment", 
			nickname = "downloadFormValue",
			tags = { IdmIdentityController.TAG },
			notes = "Returns input stream to attachment saved in given form value.",
			authorizations = {
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") }),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") })
					})
	public ResponseEntity<InputStreamResource> downloadFormValue(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable String backendId,
			@ApiParam(value = "Form value identifier.", required = true)
			@PathVariable String formValueId) {
		IdmIdentityDto dto = getDto(backendId);
		if (dto == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", backendId));
		}
		IdmFormValueDto value = formService.getValue(
				dto, 
				DtoUtils.toUuid(formValueId), 
				identityConfiguration.isFormAttributesSecured() ? IdmBasePermission.READ : null);
		if (value == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, formValueId);
		}
		return formDefinitionController.downloadAttachment(value);
	}
	
	/**
	 * Returns input stream to attachment saved in given form value.
	 * 
	 * @param backendId
	 * @param formValueId
	 * @return
	 * @since 9.4.0
	 */
	@RequestMapping(value = "/{backendId}/form-values/{formValueId}/preview", method = RequestMethod.GET)
	@ResponseBody
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Download form value attachment preview", 
			nickname = "downloadFormValue",
			tags = { IdmIdentityController.TAG },
			notes = "Returns input stream to attachment preview saved in given form value. Preview is supported for the png, jpg and jpeg mime types only",
			authorizations = {
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") }),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "") })
					})
	public ResponseEntity<InputStreamResource> previewFormValue(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable String backendId,
			@ApiParam(value = "Form value identifier.", required = true)
			@PathVariable String formValueId) {
		IdmIdentityDto dto = getDto(backendId);
		if (dto == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", backendId));
		}
		IdmFormValueDto value = formService.getValue(
				dto, 
				DtoUtils.toUuid(formValueId), 
				identityConfiguration.isFormAttributesSecured() ? IdmBasePermission.READ : null);
		if (value == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, formValueId);
		}
		return formDefinitionController.previewAttachment(value);
	}
	
	/**
	 * Returns profile image attachment from identity
	 * 
	 * @return 
	 * @since 9.0.0
	 */
	@RequestMapping(value = "/{backendId}/profile", method = RequestMethod.GET)
	@ResponseBody
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.PROFILE_READ + "')")
	@ApiOperation(
			value = "Profile", 
			nickname = "getProfile",
			tags = { IdmIdentityController.TAG },
			notes = "Returns identity profile.",
			response = IdmProfileDto.class, 
			authorizations = {
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.PROFILE_READ, description = "") }),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.PROFILE_READ, description = "") })
					})
	public ResponseEntity<?> getProfile(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable String backendId) {
		IdmProfileDto profile = profileService.findOneByIdentity(backendId, IdmBasePermission.READ);
		if (profile == null) {
			return new ResponseEntity<InputStreamResource>(HttpStatus.NO_CONTENT);
		}
		return profileController.get(profile.getId().toString());
	}
	
	/**
	 * Returns profile image attachment from identity
	 * 
	 * @return 
	 * @since 9.0.0
	 */
	@RequestMapping(value = "/{backendId}/profile", method = RequestMethod.PATCH)
	@ResponseBody
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.PROFILE_UPDATE + "')")
	@ApiOperation(
			value = "Save profile (create + patch)", 
			nickname = "patchProfile",
			tags = { IdmIdentityController.TAG },
			notes = "Save identity profile. Profile is created, when no profile is found, then is updated (patch).",
			response = IdmProfileDto.class, 
			authorizations = {
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.PROFILE_UPDATE, description = "") }),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.PROFILE_UPDATE, description = "") })
					})
	public ResponseEntity<?> patchProfile(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable String backendId,
			HttpServletRequest nativeRequest) throws HttpMessageNotReadableException {
		IdmProfileDto profile = profileService.findOrCreateByIdentity(backendId, IdmBasePermission.UPDATE);
		//
		return profileController.patch(profile.getId().toString(), nativeRequest);
	}
	
	/**
	 * Returns profile image attachment from identity
	 * 
	 * @return 
	 * @since 9.0.0
	 */
	@RequestMapping(value = "/{backendId}/profile/image", method = RequestMethod.GET)
	@ResponseBody
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.PROFILE_AUTOCOMPLETE + "')")
	@ApiOperation(
			value = "Profile image", 
			nickname = "getProfileImage",
			tags = { IdmIdentityController.TAG },
			notes = "Returns input stream to identity profile image.",
			authorizations = {
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.PROFILE_AUTOCOMPLETE, description = "") }),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.PROFILE_AUTOCOMPLETE, description = "") })
					})
	public ResponseEntity<InputStreamResource> getProfileImage(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable String backendId) {
		IdmProfileDto profile = profileService.findOneByIdentity(backendId, IdmBasePermission.AUTOCOMPLETE);
		if (profile == null) {
			return new ResponseEntity<InputStreamResource>(HttpStatus.NO_CONTENT);
		}
		if (profile.getImage() == null) {
			return new ResponseEntity<InputStreamResource>(HttpStatus.NO_CONTENT);
		}
		IdmAttachmentDto attachment = attachmentManager.get(profile.getImage());
		String mimetype = attachment.getMimetype();
		InputStream is = attachmentManager.getAttachmentData(attachment.getId());
		try {
			BodyBuilder response = ResponseEntity
					.ok()
					.contentLength(is.available())
					.header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", attachment.getName()));
			// append media type, if it's filled
			if (StringUtils.isNotBlank(mimetype)) {
				response = response.contentType(MediaType.valueOf(attachment.getMimetype()));
			}
			//
			return response.body(new InputStreamResource(is));
		} catch (IOException e) {
			throw new ResultCodeException(CoreResultCode.INTERNAL_SERVER_ERROR, e);
		}
	}
	
	/**
	 * Upload new profile picture
	 *
	 * @param backendId
	 * @param fileName
	 * @param data
	 * @return
	 * @throws IOException
	 * @since 9.0.0
	 */
	@ResponseBody
	@RequestMapping(value = "/{backendId}/profile/image", method = RequestMethod.POST)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.PROFILE_UPDATE + "')")
	@ApiOperation(
			value = "Update profile picture", 
			nickname = "postProfilePicture", 
			tags = {
			IdmProfileController.TAG }, 
			notes = "Upload new profile image", 
			authorizations = {
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = {
							@AuthorizationScope(scope = CoreGroupPermission.PROFILE_UPDATE, description = "") }),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = {
							@AuthorizationScope(scope = CoreGroupPermission.PROFILE_UPDATE, description = "") }) })
	public ResponseEntity<?> uploadProfileImage(
			@ApiParam(value = "Identity's uuid identifier or username.", required = false) 
			@PathVariable String backendId,
			@RequestParam(required = true, name = "fileName") @NotNull String fileName,
			@RequestParam(required = true, name = "data") MultipartFile data) {
		IdmProfileDto profile = profileService.findOrCreateByIdentity(backendId, IdmBasePermission.READ, IdmBasePermission.CREATE);
		//
		profile = profileService.uploadImage(profile, data, fileName, IdmBasePermission.UPDATE);
		// refresh with permissions are needed
		IdmProfileFilter context = new IdmProfileFilter();
		context.setAddPermissions(true);
		profile = profileController.getService().get(profile, context, IdmBasePermission.READ);
		//
		return new ResponseEntity<>(profileController.toResource(profile), HttpStatus.OK);
	}
	
	/**
	 * Deletes image attachment from identity
	 * 
	 * @return 
	 * @since 9.0.0
	 */
	@RequestMapping(value = "/{backendId}/profile/image", method = RequestMethod.DELETE)
	@ResponseBody
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.PROFILE_UPDATE + "')")
	@ApiOperation(
			value = "Profile picture", 
			nickname = "deleteProfilePicure",
			tags = { IdmIdentityController.TAG },
			notes = "Deletes profile picture from identity.",
			authorizations = {
					@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.PROFILE_UPDATE, description = "") }),
					@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
							@AuthorizationScope(scope = CoreGroupPermission.PROFILE_UPDATE, description = "") })
					})
	public ResponseEntity<?> deleteProfileImage(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable String backendId) {
		IdmProfileDto profile = profileService.findOneByIdentity(backendId, IdmBasePermission.READ, IdmBasePermission.UPDATE);
		//
		profile = profileService.deleteImage(profile, IdmBasePermission.UPDATE);
		// refresh with permissions are needed
		IdmProfileFilter context = new IdmProfileFilter();
		context.setAddPermissions(true);
		profile = profileController.getService().get(profile, context, IdmBasePermission.READ);
		//
		return new ResponseEntity<>(profileController.toResource(profile), HttpStatus.OK);
	}
	
	/**
	 * Returns profile permissions
	 * 
	 * @return 
	 * @since 9.0.0
	 */
	@ResponseBody
	@RequestMapping(value = "/{backendId}/profile/permissions", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.PROFILE_READ + "')"
			+ " or hasAuthority('" + CoreGroupPermission.PROFILE_AUTOCOMPLETE + "')")
	@ApiOperation(
			value = "What logged identity can do with identity profile", 
			nickname = "getPermissionsOnIdentityProfile", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.PROFILE_READ, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.PROFILE_AUTOCOMPLETE, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.PROFILE_READ, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.PROFILE_AUTOCOMPLETE, description = "")})
				})
	public Set<String> getProfilePermissions(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable @NotNull String backendId) {
		IdmProfileDto profile = profileService.findOneByIdentity(backendId);
		if (profile == null) {
			IdmIdentityDto identity = (IdmIdentityDto) getLookupService().lookupDto(IdmIdentityDto.class, backendId);
			if (identity == null) {
				throw new EntityNotFoundException(IdmIdentity.class, backendId);
			}
			profile = new IdmProfileDto();
			profile.setIdentity(identity.getId());
		}
		//
		// profile can be null (create)
		return profileController.getService().getPermissions(profile);
	}
	
	@ResponseBody
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.PASSWORD_READ + "')")
	@RequestMapping(value = "/{backendId}/password", method = RequestMethod.GET)
	@ApiOperation(
			value = "Get password by identity", 
			nickname = "getIdentityPassword",
			response = IdmPasswordDto.class, 
			tags = { IdmPasswordController.TAG }, 
			authorizations = {
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.PASSWORD_READ, description = "") }),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.PASSWORD_READ, description = "") })
				})
	public ResponseEntity<?> getPassword(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true)
			@PathVariable @NotNull String backendId) {
		IdmIdentityDto dto = getDto(backendId);
		if (dto == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", backendId));
		}
		IdmPasswordDto passwordDto = passwordService.findOneByIdentity(dto.getId());
		if (passwordDto == null) {
			return new ResponseEntity<InputStreamResource>(HttpStatus.NO_CONTENT);
		}
		return new ResponseEntity<>(passwordController.toResource(passwordDto), HttpStatus.OK);
	}
	
	
	/**
	 * Long polling for check unresolved identity role-requests
	 *  
	 * @param backendId - applicant ID
	 * 
	 * @return DeferredResult<OperationResultDto>, where:
	 * 
	 * - EXECUTED = All requests for this identity are resolved,
	 * - RUNNING = Requests are not resolved, but some request was changed (since previous check).
	 * - NOT_EXECUTED = Deferred-result expired
	 * 
	 */
	@ResponseBody
	@RequestMapping(value = "{backendId}/check-unresolved-request", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('" + CoreGroupPermission.ROLE_REQUEST_READ + "')"
			+ " and hasAuthority('" + CoreGroupPermission.IDENTITY_READ + "')")
	@ApiOperation(
			value = "Check changes of unresloved requests for the identity (Long-polling request).", 
			nickname = "checkUnresolvedRequests", 
			tags = { IdmIdentityController.TAG }, 
			authorizations = { 
				@Authorization(value = SwaggerConfig.AUTHENTICATION_BASIC, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLE_REQUEST_READ, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "")}),
				@Authorization(value = SwaggerConfig.AUTHENTICATION_CIDMST, scopes = { 
						@AuthorizationScope(scope = CoreGroupPermission.ROLE_REQUEST_READ, description = ""),
						@AuthorizationScope(scope = CoreGroupPermission.IDENTITY_READ, description = "")})
				})
	public DeferredResult<OperationResultDto> checkUnresolvedRequests(
			@ApiParam(value = "Identity's uuid identifier or username.", required = true) @PathVariable @NotNull String backendId) {
		
		IdmIdentityDto dto = getDto(backendId);
		if (dto == null) {
			throw new ResultCodeException(CoreResultCode.NOT_FOUND, ImmutableMap.of("entity", backendId));
		}
		
		UUID identityId = dto.getId();
		DeferredResultWrapper result = new DeferredResultWrapper( //
				identityId, //
				dto.getClass(),//
				new DeferredResult<OperationResultDto>( //
						30000l, new OperationResultDto(OperationState.NOT_EXECUTED)) //
		); //

		result.onCheckResultCallback(new CheckLongPollingResult() {

			@Override
			public void checkDeferredResult(DeferredResult<OperationResultDto> result,
					LongPollingSubscriber subscriber) {
				checkDeferredRequest(result, subscriber);
			}
		});

		// If isn't long polling enabled, then Blocked response will be sent.
		if (!longPollingManager.isLongPollingEnabled()) {
			result.getResult().setResult(new OperationResultDto(OperationState.BLOCKED));
			return result.getResult();
		}

		longPollingManager.addSuspendedResult(result);
		
		return result.getResult();
	}
	
	@Scheduled(fixedDelay = 2000)
	public synchronized void checkDeferredRequests() {
		longPollingManager.checkDeferredRequests(IdmIdentityDto.class);
	}

	/**
	 * Check deferred result - using default implementation from long-polling-manager.
	 * 
	 * @param deferredResult
	 * @param subscriber
	 */
	private void checkDeferredRequest(DeferredResult<OperationResultDto> deferredResult, LongPollingSubscriber subscriber) {
		Assert.notNull(deferredResult, "Deferred result is required.");
		Assert.notNull(subscriber.getEntityId(), "Subscriber identifier is required.");
				
		IdmRoleRequestFilter filter = new IdmRoleRequestFilter();
		filter.setApplicantId(subscriber.getEntityId());
		
		longPollingManager.baseCheckDeferredResult(deferredResult, subscriber, filter, roleRequestService, true);
	}
	
	@Override
	protected IdmIdentityDto validateDto(IdmIdentityDto dto) {
		dto = super.validateDto(dto);
		//
		// state is read only
		if (!getService().isNew(dto)) {
			IdmIdentityDto previous = getDto(dto.getId());
			dto.setState(previous.getState());
		} else {
			dto.setState(IdentityState.CREATED);
		}
		return dto;
	}
	
	@Override
	protected IdmIdentityFilter toFilter(MultiValueMap<String, Object> parameters) {
		IdmIdentityFilter filter = new IdmIdentityFilter(parameters, getParameterConverter());
		// to entity decorators
		filter.setSubordinatesFor(getParameterConverter().toEntityUuid(parameters, IdmIdentityFilter.PARAMETER_SUBORDINATES_FOR, IdmIdentityDto.class));
		filter.setSubordinatesByTreeType(getParameterConverter().toEntityUuid(parameters, IdmIdentityFilter.PARAMETER_SUBORDINATES_BY_TREE_TYPE, IdmTreeTypeDto.class));
		filter.setManagersFor(getParameterConverter().toEntityUuid(parameters, IdmIdentityFilter.PARAMETER_MANAGERS_FOR, IdmIdentityDto.class));
		filter.setManagersByTreeType(getParameterConverter().toEntityUuid(parameters, IdmIdentityFilter.PARAMETER_MANAGERS_BY_TREE_TYPE, IdmTreeTypeDto.class));
		filter.setFormProjection(getParameterConverter().toEntityUuid(parameters, IdmIdentityFilter.PARAMETER_FORM_PROJECTION, IdmFormProjectionDto.class));
		// OR is supported now
		if (parameters.containsKey(IdmIdentityFilter.PARAMETER_ROLE)) {
			for (Object role : parameters.get(IdmIdentityFilter.PARAMETER_ROLE)) {
				if (role != null) {
					filter.getRoles().add(getParameterConverter().toEntityUuid((String) role, IdmRoleDto.class));
				}
			}
		}
		// different default than in filter ... i don't know why, but change is to dangerous
		filter.setIncludeGuarantees(getParameterConverter().toBoolean(parameters, IdmIdentityFilter.PARAMETER_INCLUDE_GUARANTEES, false)); 
		return filter;
	}
	
}
