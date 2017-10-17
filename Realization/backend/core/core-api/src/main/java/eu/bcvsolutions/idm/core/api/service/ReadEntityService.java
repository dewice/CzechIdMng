package eu.bcvsolutions.idm.core.api.service;

import java.io.Serializable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import eu.bcvsolutions.idm.core.api.dto.filter.BaseFilter;
import eu.bcvsolutions.idm.core.api.entity.BaseEntity;
import eu.bcvsolutions.idm.core.security.api.domain.BasePermission;

/**
 * Provide additional methods to retrieve entities using the pagination and
 * sorting abstraction.
 * 
 * @author Radek Tomiška
 * @see Sort
 * @see Pageable
 * @see Page
 * @deprecated use {@link ReadDtoService}
 */
public interface ReadEntityService<E extends BaseEntity, F extends BaseFilter> extends BaseEntityService<E> {

	/**
	 * Returns {@link BaseFilter} type class, which is controlled by this service
	 * 
	 * @return
	 */
	Class<F> getFilterClass();
	
	default E get(Serializable id) {
		return get(id, (BasePermission[]) null);
	}
	
	/**
	 * Returns entity by given id. Returns null, if entity is not exists.
	 * 
	 * @param id
	 * @return
	 */
	E get(Serializable id, BasePermission... permission);
	
	/**
	 * Returns page of entities
	 * 
	 * @param pageable
	 * @return
	 */
	Page<E> find(Pageable pageable);
	
	/**
	 * Returns page of entities by given filter
	 * 
	 * @param filter
	 * @param pageable
	 * @return
	 */
	Page<E> find(F filter, Pageable pageable);
	
	/**
	 * Returns whether the given entity is considered to be new.
	 * 
	 * @param entity must never be {@literal null}
	 * @return
	 */
	boolean isNew(E entity);
	
	/**
	 * Evaluates authorization permission on given entity
	 *  
	 * @param entity
	 * @param permission base permissions to evaluate
	 * @return
	 */
	E checkAccess(E entity, BasePermission... permission);
}