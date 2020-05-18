package eu.bcvsolutions.idm.core.model.service.impl;

import java.util.List;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.bcvsolutions.idm.core.api.dto.IdmDelegationDefinitionDto;
import eu.bcvsolutions.idm.core.api.dto.filter.IdmDelegationDefinitionFilter;
import eu.bcvsolutions.idm.core.api.service.AbstractEventableDtoService;
import eu.bcvsolutions.idm.core.api.service.EntityEventManager;
import eu.bcvsolutions.idm.core.api.service.IdmDelegationDefinitionService;
import eu.bcvsolutions.idm.core.model.domain.CoreGroupPermission;
import eu.bcvsolutions.idm.core.model.entity.IdmDelegationDefinition;
import eu.bcvsolutions.idm.core.model.entity.IdmDelegationDefinition_;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity_;
import eu.bcvsolutions.idm.core.model.repository.IdmDelegationDefinitionRepository;
import eu.bcvsolutions.idm.core.security.api.dto.AuthorizableType;

/**
 * CRUD service for a definition of delegation.
 *
 * @author Vít Švanda
 *
 */
@Service("delegationDefinitionService")
public class DefaultIdmDelegationDefinitionService extends
		AbstractEventableDtoService<IdmDelegationDefinitionDto, IdmDelegationDefinition, IdmDelegationDefinitionFilter> implements IdmDelegationDefinitionService {

	@Autowired
	public DefaultIdmDelegationDefinitionService(IdmDelegationDefinitionRepository repository, EntityEventManager entityEventManager) {
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
	protected List<Predicate> toPredicates(Root<IdmDelegationDefinition> root, CriteriaQuery<?> query, CriteriaBuilder builder,
			IdmDelegationDefinitionFilter filter) {
		List<Predicate> predicates = super.toPredicates(root, query, builder, filter);

		UUID sourceIdentityId = filter.getSourceIdentity();
		if (sourceIdentityId != null) {
			predicates.add(builder.equal(root.get(IdmDelegationDefinition_.sourceIdentity).get(IdmIdentity_.id), sourceIdentityId));
		}

		UUID targetIdentityId = filter.getTargetIdentity();
		if (targetIdentityId != null) {
			predicates.add(builder.equal(root.get(IdmDelegationDefinition_.targetIdentity).get(IdmIdentity_.id), targetIdentityId));
		}

		String type = filter.getType();
		if (type != null) {
			predicates.add(builder.equal(root.get(IdmDelegationDefinition_.type), type));
		}

		return predicates;
	}

}
