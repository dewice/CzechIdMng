package eu.bcvsolutions.idm.core.model.service.impl;

import java.util.List;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.bcvsolutions.idm.core.api.dto.IdmDelegationDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmDelegationFilter;
import eu.bcvsolutions.idm.core.api.service.AbstractEventableDtoService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmDelegationService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmDelegation;
import eu.bcvsolutions.idm.core.model.entity.IdmDelegation_;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity_;
import eu.bcvsolutions.idm.core.model.repository.IdmDelegationRepository;
import eu.bcvsolutions.idm.core.security.api.dto.AuthorizableType;

/**
 * CRUD service for delegations.
 *
 * @author Vít Švanda
 *
 */
@Service("delegationService")
public class DefaultIdmDelegationService extends
		AbstractEventableDtoService<IdmDelegationDto, IdmDelegation, IdmDelegationFilter> implements IdmDelegationService {

	@Autowired
	public DefaultIdmDelegationService(IdmDelegationRepository repository, EntityEventManager entityEventManager) {
		super(repository, entityEventManager);
	}

	@Override
	public AuthorizableType getAuthorizableType() {
		return new AuthorizableType(CoreGroupPermission.DELEGATIONDEFINITION, getEntityClass());
	}

	@Override
	public boolean supportsToDtoWithFilter() {
		return true;
	}

	@Override
	protected List<Predicate> toPredicates(Root<IdmDelegation> root, CriteriaQuery<?> query, CriteriaBuilder builder,
			IdmDelegationFilter filter) {
		List<Predicate> predicates = super.toPredicates(root, query, builder, filter);

		UUID sourceIdentityId = filter.getSourceIdentity();
		if (sourceIdentityId != null) {
			predicates.add(builder.equal(root.get(IdmDelegation_.sourceIdentity).get(IdmIdentity_.id), sourceIdentityId));
		}

		UUID targetIdentityId = filter.getTargetIdentity();
		if (targetIdentityId != null) {
			predicates.add(builder.equal(root.get(IdmDelegation_.targetIdentity).get(IdmIdentity_.id), targetIdentityId));
		}

		String ownerType = filter.getOwnerType();
		if (ownerType != null) {
			predicates.add(builder.equal(root.get(IdmDelegation_.ownerType), ownerType));
		}

		UUID ownerId = filter.getOwnerId();
		if (ownerId != null) {
			predicates.add(builder.equal(root.get(IdmDelegation_.ownerId), ownerId));
		}

		return predicates;
	}
}
