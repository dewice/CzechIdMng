package eu.bcvsolutions.idm.core.model.service.impl;


import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableMap;

import eu.bcvsolutions.idm.core.api.domain.CoreResultCode;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.AbstractReadWriteEntityService;
import eu.bcvsolutions.idm.core.model.dto.PasswordChangeDto;
import eu.bcvsolutions.idm.core.model.dto.filter.PasswordFilter;
import eu.bcvsolutions.idm.core.model.entity.IdmIdentity;
import eu.bcvsolutions.idm.core.model.entity.IdmPassword;
import eu.bcvsolutions.idm.core.model.repository.IdmPasswordRepository;
import eu.bcvsolutions.idm.core.model.service.api.IdmPasswordService;
import eu.bcvsolutions.idm.security.api.domain.GuardedString;

/**
 * 
 * 
 * @author Ondrej Kopr <kopr@xyxy.cz>
 * 
 * TODO: password valid till and valid from!!
 */

@Service
public class DefaultIdmPasswordService extends AbstractReadWriteEntityService<IdmPassword, PasswordFilter> implements IdmPasswordService {
	
	private static final String ALGORITHM = "PBKDF2WithHmacSHA512";
	
	private static final int ITERATION_COUNT = 512;
	
	private static final int DERIVED_KEY_LENGTH = 256;
	
	private IdmPasswordRepository identityPasswordRepository;
	
	@Autowired
	public DefaultIdmPasswordService(
			IdmPasswordRepository identityPasswordRepository) {
		super(identityPasswordRepository);
		//
		Assert.notNull(identityPasswordRepository);
		//
		this.identityPasswordRepository = identityPasswordRepository;
	}

	@Override
	public IdmPassword save(IdmIdentity identity, PasswordChangeDto passwordDto) {
		Assert.notNull(identity);
		Assert.notNull(passwordDto);
		Assert.notNull(passwordDto.getNewPassword());
		GuardedString password = passwordDto.getNewPassword();
		//
		IdmPassword passwordEntity = getPasswordByIdentity(identity);
		//
		if (passwordEntity == null) {
			// identity has no password yet
			passwordEntity = new IdmPassword();
			passwordEntity.setIdentity(identity);
		}
		//
		if (passwordDto.getMaxPasswordAge() != null) {
			passwordEntity.setValidTill(passwordDto.getMaxPasswordAge().toLocalDate());
		}
		// set valid from now
		passwordEntity.setValidFrom(new LocalDate());
		//
		passwordEntity.setPassword(this.generateHash(password, getSalt(identity)));
		//
		// set must change password to false
		passwordEntity.setMustChange(false);
		//
		return identityPasswordRepository.save(passwordEntity);
	}

	@Override
	public void delete(IdmIdentity identity) {
		IdmPassword passwordEntity = getPasswordByIdentity(identity);
		if (passwordEntity != null) {
			this.identityPasswordRepository.delete(passwordEntity);
		}
	}
	
	@Override
	public IdmPassword get(IdmIdentity identity) {
		return this.getPasswordByIdentity(identity);
	}
	
	@Override
	public boolean checkPassword(GuardedString passwordToCheck, IdmPassword password) {
		byte[] newPassword = generateHash(passwordToCheck, this.getSalt(password.getIdentity()));
		return Arrays.equals(newPassword, password.getPassword());
	}

	@Override
	public byte[] generateHash(GuardedString password, byte[] salt) {
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
			PBEKeySpec keySpec = new PBEKeySpec(password.asString().toCharArray(), salt, ITERATION_COUNT, DERIVED_KEY_LENGTH);
			SecretKey key = factory.generateSecret(keySpec);
			return key.getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new ResultCodeException(CoreResultCode.PASSWORD_CHANGE_FAILED, ImmutableMap.of("error", e.getMessage()), e);
		}
	}
	
	@Override
	public byte[] getSalt(IdmIdentity identity) {
		UUID id = identity.getId();
		return ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array();
	}
	
	/**
	 * Method get IdmIdentityPassword by identity.
	 * 
	 * @param identity
	 * @return Object IdmIdentityPassword when password for identity was founded otherwise null.
	 */
	private IdmPassword getPasswordByIdentity(IdmIdentity identity) {
		Assert.notNull(identity);
		//
		return this.identityPasswordRepository.findOneByIdentity(identity);
	}
}
