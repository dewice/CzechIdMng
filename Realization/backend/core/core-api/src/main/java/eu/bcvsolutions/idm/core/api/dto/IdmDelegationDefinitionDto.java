package eu.bcvsolutions.idm.core.api.dto;

import java.util.UUID;

import org.springframework.hateoas.core.Relation;

import eu.bcvsolutions.idm.core.api.domain.Embedded;
import java.time.LocalDate;

/**
 * Definition of a delegation DTO.
 *
 * @author Vít Švanda
 *
 */
@Relation(collectionRelation = "delegationDefs")
public class IdmDelegationDefinitionDto extends AbstractDto {

	private static final long serialVersionUID = 1L;

	@Embedded(dtoClass = IdmIdentityDto.class)
	private UUID sourceIdentity;
	@Embedded(dtoClass = IdmIdentityDto.class)
	private UUID targetIdentity;
	@Embedded(dtoClass = IdmIdentityContractDto.class)
	private UUID sourceContract;
	@Embedded(dtoClass = IdmIdentityContractDto.class)
	private UUID targetContract;
	private String type;
	private LocalDate validFrom;
	private LocalDate validTill;

	public IdmDelegationDefinitionDto() {
		super();
	}

	public UUID getSourceIdentity() {
		return sourceIdentity;
	}

	public void setSourceIdentity(UUID sourceIdentity) {
		this.sourceIdentity = sourceIdentity;
	}

	public UUID getTargetIdentity() {
		return targetIdentity;
	}

	public void setTargetIdentity(UUID targetIdentity) {
		this.targetIdentity = targetIdentity;
	}

	public UUID getSourceContract() {
		return sourceContract;
	}

	public void setSourceContract(UUID sourceContract) {
		this.sourceContract = sourceContract;
	}

	public UUID getTargetContract() {
		return targetContract;
	}

	public void setTargetContract(UUID targetContract) {
		this.targetContract = targetContract;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(LocalDate validFrom) {
		this.validFrom = validFrom;
	}

	public LocalDate getValidTill() {
		return validTill;
	}

	public void setValidTill(LocalDate validTill) {
		this.validTill = validTill;
	}

}
