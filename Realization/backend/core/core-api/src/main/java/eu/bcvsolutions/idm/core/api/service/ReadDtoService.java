package eu.bcvsolutions.idm.core.api.service;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import eu.bcvsolutions.idm.core.api.dto.BaseDto;
import eu.bcvsolutions.idm.core.api.dto.IdmExportImportDto;
import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.api.exception.ForbiddenEntityException;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;

/**
 * Provide additional methods to retrieve entities using the pagination and
 * sorting abstraction for DTO services.
 * 
 * @param <DTO> {@link BaseDto} type
 * @param <E>   {@link BaseEntity} type
 * @param <F>   {@link BaseFilter} type
 * @author Svanda
 * @author Radek Tomiška
 * 
 * @see Sort
 * @see Pageable
 * @see Page
 * @param <DTO> {@link BaseDto} type
 * @param <F> {@link BaseFilter} type
 */
public interface ReadDtoService<DTO extends BaseDto, F extends BaseFilter>
		extends BaseDtoService<DTO> {

	/**
	 * Returns {@link BaseFilter} type class, which is controlled by this
	 * service
	 * 
	 * @return
	 */
	Class<F> getFilterClass();

	/**
	 * Method controls behavior of methods that convert entities to DTOs.
	 * Beware this method can affect the transformation entities to DTO.
	 *
	 * @return If method return true convert to DTO will be used with filter. If return false method without
	 * will be used for convert entities to DTOs.
	 * @since 9.4.0
	 */
	boolean supportsToDtoWithFilter();
	
	/**
	 * Returns DTO by given id. Returns null, if dto is not exists. Authorization policies are evaluated.
	 *
	 * @param id
	 * @param permission permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	DTO get(Serializable id, BasePermission... permission);
	
	/**
	 * Returns DTO by given id. Returns null, if dto is not exists. Authorization policies are evaluated.
	 * 
	 * @param id
	 * @param context returned DTO structure can be specified.
	 * @param permission permissions to evaluate (AND)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 * @since 9.7.0
	 */
	DTO get(Serializable id, F context, BasePermission... permission);

	/**
	 * Returns page of DTOs.
	 * Never throws {@link ForbiddenEntityException} - returning available dtos by given permissions (AND).
	 * 
	 * @param pageable
	 * @param permission permissions to evaluate
	 * @return
	 */
	Page<DTO> find(Pageable pageable, BasePermission... permission);
	
	/**
	 * Returns page of DTOs by given filter, authorization permission will be evaluated. 
	 * Never throws {@link ForbiddenEntityException} - returning available dtos by given permissions (AND).
	 * 
	 * @param filter
	 * @param pageable
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 */
	Page<DTO> find(F filter, Pageable pageable, BasePermission... permission);
	
	/**
	 * Return page of UUID's. Method works same as {@link ReadDtoService#find(Pageable, BasePermission...)},
	 * but returns only ID's. The behavior with ID is useful for quick receive result.
	 *
	 * @param pageable
	 * @param permission
	 * @return
	 */
	Page<UUID> findIds(Pageable pageable, BasePermission... permission);
	
	/**
	 * Return page of UUID's by given filter, authorization permission will be evaluated.
	 * Method works same as {@link ReadDtoService#find(BaseFilter, Pageable, BasePermission...)},
	 * but returns only ID's. The behavior with ID is useful for quick receive result.
	 *
	 * @param filter
	 * @param pageable
	 * @param permission
	 * @return
	 */
	Page<UUID> findIds(F filter, Pageable pageable, BasePermission... permission);
	
	/**
	 * The number of entities that match the filter.
	 * Never throws {@link ForbiddenEntityException} - returning available dtos by given permissions (AND).
	 * 
	 * @param filter
	 * @param permission base permissions to evaluate (AND)
	 * @return
	 */
	long count(F filter, BasePermission... permission);
	
	/**
	 * Returns whether the given dto is considered to be new.
	 * 
	 * @param dto must never be {@literal null}
	 * @return
	 */
	boolean isNew(DTO dto);
	
	/**
	 * Returns, what currently logged identity can do with given dto
	 * 
	 * @param backendId
	 * @return
	 */
	Set<String> getPermissions(Serializable id);
	
	/**
	 * Returns, what currently logged identity can do with given dto
	 * 
	 * @param backendId
	 * @return
	 */
	Set<String> getPermissions(DTO dto);
	
	/**
	 * Evaluates authorization permission on given dto.
	 *  
	 * @param dto
	 * @param permission base permissions to evaluate (all permission needed)
	 * @return
	 * @throws ForbiddenEntityException if authorization policies doesn't met
	 */
	DTO checkAccess(DTO dto, BasePermission... permission);

	
	/**
	 * Exports DTO. DTO will be added to the given batch. 
	 * 
	 * @param id
	 * @param batch
	 */
	void export(UUID id, IdmExportImportDto batch);
}
