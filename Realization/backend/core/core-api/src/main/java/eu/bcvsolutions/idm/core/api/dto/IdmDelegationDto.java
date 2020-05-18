package eu.bcvsolutions.idm.core.api.dto;

import java.util.UUID;

import org.springframework.hateoas.core.Relation;

import eu.bcvsolutions.idm.core.api.domain.Embedded;

/**
 * Delegation DTO.
 *
 * @author Vít Švanda
 *
 */
@Relation(collectionRelation = "delegations")
public class IdmDelegationDto extends AbstractDto {

	private static final long serialVersionUID = 1L;

	@Embedded(dtoClass = IdmDelegationDefinitionDto.class)
	private UUID definition;
	private UUID ownerId;
	private String ownerType;

	public IdmDelegationDto() {
		super();
	}

	public UUID getDefinition() {
		return definition;
	}

	public void setDefinition(UUID definition) {
		this.definition = definition;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(UUID ownerId) {
		this.ownerId = ownerId;
	}

	public String getOwnerType() {
		return ownerType;
	}

	public void setOwnerType(String ownerType) {
		this.ownerType = ownerType;
	}
}
