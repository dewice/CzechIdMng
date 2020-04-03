package eu.bcvsolutions.idm.core.notification.service.impl;

import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import eu.bcvsolutions.idm.InitTestData;
import eu.bcvsolutions.idm.core.api.dto.IdmIdentityDto;
import eu.bcvsolutions.idm.core.api.exception.ResultCodeException;
import eu.bcvsolutions.idm.core.api.service.IdmIdentityService;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationLevel;
import eu.bcvsolutions.idm.core.notification.api.domain.NotificationState;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmEmailLogDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmMessageDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationLogDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationRecipientDto;
import eu.bcvsolutions.idm.core.notification.api.dto.IdmNotificationTemplateDto;
import eu.bcvsolutions.idm.core.notification.api.dto.NotificationConfigurationDto;
import eu.bcvsolutions.idm.core.notification.api.dto.filter.IdmNotificationFilter;
import eu.bcvsolutions.idm.core.notification.api.dto.filter.IdmNotificationRecipientFilter;
import eu.bcvsolutions.idm.core.notification.api.service.EmailNotificationSender;
import eu.bcvsolutions.idm.core.notification.api.service.IdmEmailLogService;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationConfigurationService;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationLogService;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationRecipientService;
import eu.bcvsolutions.idm.core.notification.api.service.IdmNotificationTemplateService;
import eu.bcvsolutions.idm.core.notification.api.service.NotificationManager;
import eu.bcvsolutions.idm.core.notification.entity.IdmConsoleLog;
import eu.bcvsolutions.idm.core.notification.entity.IdmEmailLog;
import eu.bcvsolutions.idm.core.notification.entity.IdmNotificationConfiguration;
import eu.bcvsolutions.idm.core.notification.entity.IdmNotificationLog;
import eu.bcvsolutions.idm.core.notification.repository.IdmEmailLogRepository;
import eu.bcvsolutions.idm.core.notification.repository.IdmNotificationConfigurationRepository;
import eu.bcvsolutions.idm.core.notification.repository.IdmNotificationLogRepository;
import eu.bcvsolutions.idm.core.security.api.domain.GuardedString;
import eu.bcvsolutions.idm.test.api.AbstractIntegrationTest;

/**
 * Notification service tests
 *
 * TODO: move filters to rest layer
 * FIXME: counts are checked only ...
 * FIXME: rename test by conventions
 *
 * @author Radek Tomiška
 * @author Marek Klement
 *
 */
@Transactional
public class DefaultNotificationServiceIntegrationTest extends AbstractIntegrationTest {

	@Autowired private NotificationManager notificationManager;
	@Autowired private EmailNotificationSender emailService;
	@Autowired private IdmNotificationLogRepository notificationRepository;
	@Autowired private IdmNotificationRecipientService recipientService;
	@Autowired private IdmNotificationConfigurationRepository notificationConfigurationRepository;
	@Autowired private IdmNotificationLogService notificationLogService;
	@Autowired private IdmIdentityService identityService;
	@Autowired private IdmEmailLogRepository emailLogRepository;
	@Autowired private IdmEmailLogService emailLogService;
	@Autowired private IdmNotificationTemplateService notificationTemplateService;
	@Autowired private IdmNotificationConfigurationService notificationConfigurationService;

	@Before
	public void clear() {
		// FIXME: make test stateless!
		emailLogRepository.deleteAll();
		notificationRepository.deleteAll();
	}

	@Test
	public void testReferentialIntegrity() {
		// delete recipients and children
		NotificationConfigurationDto config = createConfig();
		IdmNotificationTemplateDto template = createTestTemplate();
		IdmIdentityDto identityOne = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto identityTwo = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto identitySender = getHelper().createIdentity((GuardedString) null);
		List<IdmNotificationLogDto> notifications = notificationManager.send(
				config.getTopic(),
				new IdmMessageDto.Builder().setTemplate(template).build(),
				identitySender,
				Lists.newArrayList(identityOne, identityTwo)
				);
		Assert.assertEquals(1, notifications.size());
		//
		IdmNotificationLogDto notification = notificationLogService.get(notifications.get(0));
		Assert.assertNotNull(notification);
		Assert.assertEquals(identitySender.getId(), notification.getIdentitySender());
		IdmNotificationFilter notificationFilter = new IdmNotificationFilter();
		notificationFilter.setParent(notification.getId());
		List<IdmEmailLogDto> emails = emailLogService.find(notificationFilter, null).getContent();
		Assert.assertEquals(1, emails.size());
		IdmEmailLogDto emailNotification = emails.get(0);
		Assert.assertEquals(notification.getId(), emailNotification.getParent());
		//
		IdmNotificationRecipientFilter recipientFilter = new IdmNotificationRecipientFilter();
		recipientFilter.setNotification(notification.getId());
		List<IdmNotificationRecipientDto> notificationRecipients = recipientService.find(recipientFilter, null).getContent();
		Assert.assertEquals(2, notificationRecipients.size());
		Assert.assertTrue(notificationRecipients.stream().anyMatch(r -> r.getIdentityRecipient().equals(identityOne.getId())));
		Assert.assertTrue(notificationRecipients.stream().anyMatch(r -> r.getIdentityRecipient().equals(identityTwo.getId())));
		recipientFilter.setNotification(emailNotification.getId());
		List<IdmNotificationRecipientDto> emailRecipients = recipientService.find(recipientFilter, null).getContent();
		Assert.assertEquals(2, emailRecipients.size());
		Assert.assertTrue(emailRecipients.stream().anyMatch(r -> r.getIdentityRecipient().equals(identityOne.getId())));
		Assert.assertTrue(emailRecipients.stream().anyMatch(r -> r.getIdentityRecipient().equals(identityTwo.getId())));
		//
		// sender is removed from notifications when sender identity was deleted
		IdmNotificationFilter senderFilter = new IdmNotificationFilter();
		senderFilter.setIdentitySender(identitySender.getId());
		List<IdmNotificationLogDto> notificationLogDtos = notificationLogService.find(senderFilter, null).getContent();
		Assert.assertFalse(notificationLogDtos.isEmpty());
		notificationLogDtos.forEach(notif -> {
			Assert.assertEquals(identitySender.getId(), notif.getIdentitySender());
		});
		identityService.delete(identitySender);
		Assert.assertNull(identityService.get(identitySender.getId()));
		//shouldn't throw despite the fact that identitySender contained in notificationLogDtos has been deleted
		notificationLogDtos = notificationLogService.find(senderFilter, null).getContent();
		
		// delete parent notification
		notificationLogService.delete(notification);
		//
		// all removed
		Assert.assertNull(notificationLogService.get(notification));
		Assert.assertNull(notificationLogService.get(emailNotification));
		recipientFilter.setNotification(notification.getId());
		Assert.assertTrue(recipientService.find(recipientFilter, null).getContent().isEmpty());
		recipientFilter.setNotification(emailNotification.getId());
		Assert.assertTrue(recipientService.find(recipientFilter, null).getContent().isEmpty());
	}

	@Test
	public void testSendSimple() {
		assertEquals(0, notificationRepository.count());
		NotificationConfigurationDto config = createConfig();

		IdmNotificationTemplateDto template = createTestTemplate();

		IdmIdentityDto identity = identityService.getByUsername(InitTestData.TEST_USER_1);

		notificationManager.send(config.getTopic(), new IdmMessageDto.Builder().setTemplate(template).build(), identity);

		assertEquals(1, notificationRepository.count());
		assertEquals(1, emailLogRepository.count());
	}

	@Test
	public void testFilterByDate() {
		assertEquals(0, notificationRepository.count());
		IdmNotificationTemplateDto template = createTestTemplate();
		IdmIdentityDto identity = identityService.getByUsername(InitTestData.TEST_USER_1);
		NotificationConfigurationDto config = createConfig();
		//
		ZonedDateTime from = ZonedDateTime.now().minusDays(1);
		ZonedDateTime till = ZonedDateTime.now().minusDays(1);
		notificationManager.send(config.getTopic(), new IdmMessageDto.Builder().setTemplate(template).build(), identity);
		notificationManager.send(config.getTopic(), new IdmMessageDto.Builder().setTemplate(template).build(), identity);

		IdmNotificationFilter filter = new IdmNotificationFilter();
		filter.setNotificationType(IdmNotificationLog.class);
		assertEquals(2, notificationLogService.find(filter, null).getTotalElements());

		filter.setFrom(from);
		assertEquals(2, notificationLogService.find(filter, null).getTotalElements());

		filter.setFrom(null);
		filter.setTill(till);
		assertEquals(0, notificationLogService.find(filter, null).getTotalElements());
	}

	@Test
	public void testEmailFilterBySender() {
		NotificationConfigurationDto config = createConfig();
		// create templates
		IdmNotificationTemplateDto template = createTestTemplate();

		IdmNotificationFilter filter = new IdmNotificationFilter();

		filter.setSender(InitTestData.TEST_USER_2);
		assertEquals(0, emailLogService.find(filter, null).getTotalElements());
		filter.setSender(InitTestData.TEST_USER_1);
		assertEquals(0, emailLogService.find(filter, null).getTotalElements());

		// send some email
		IdmIdentityDto identity = identityService.getByUsername(InitTestData.TEST_USER_1);
		IdmIdentityDto identity2 = identityService.getByUsername(InitTestData.TEST_USER_2);

		emailService.send(config.getTopic(), new IdmMessageDto.Builder().setTemplate(template).build(), identity);

		filter.setSender(null);
		assertEquals(1, emailLogService.find(filter, null).getTotalElements());
		filter.setSender(identity2.getUsername());
		assertEquals(0, emailLogService.find(filter, null).getTotalElements());
		filter.setSender(null);
		filter.setRecipient(identity.getUsername());
		assertEquals(1, emailLogService.find(filter, null).getTotalElements());
	}

	@Test
	public void testEmailFilterBySent() {
		IdmIdentityDto identity = identityService.getByUsername(InitTestData.TEST_USER_1);

		IdmNotificationFilter filter = new IdmNotificationFilter();
		//
		// create templates
		IdmNotificationTemplateDto template = createTestTemplate();
		//
		emailService.send(new IdmMessageDto.Builder().setTemplate(template).build(), identity);
		filter.setSent(true);
		assertEquals(0, emailLogService.find(filter, null).getTotalElements());

		emailService.send(new IdmMessageDto.Builder().setTemplate(template).build(), identity);
		filter.setSent(false);
		assertEquals(2, emailLogService.find(filter, null).getTotalElements());
	}

	@Test
	public void testTextFilterTest(){
		IdmIdentityDto identity = getHelper().createIdentity((GuardedString) null);
		IdmNotificationFilter filter = new IdmNotificationFilter();
		//
		// create templates
		IdmNotificationTemplateDto template = createTestTemplate();
		IdmNotificationTemplateDto template2 = createTestTemplate();
		//
		emailService.send(new IdmMessageDto.Builder().setTemplate(template).build(), identity);
		emailService.send(new IdmMessageDto.Builder().setTemplate(template2).build(), identity);
		// filter text BODY
		filter.setText(template.getBodyText());
		Page<IdmNotificationLogDto> result = notificationLogService.find(filter, null);
		assertEquals("Wrong text message body",1, result.getTotalElements());
		// filter text HTML
		filter.setText(template2.getBodyHtml());
		result = notificationLogService.find(filter, null);
		assertEquals("Wrong text message html",1, result.getTotalElements());
		// filter text subject
		filter.setText(template2.getSubject());
		result = notificationLogService.find(filter, null);
		assertEquals("Wrong text message html",1, result.getTotalElements());
	}
	
	@Test
	public void testFilterByTopic() {
		IdmNotificationTemplateDto template = createTestTemplate();
		IdmIdentityDto identity = getHelper().createIdentity((GuardedString) null);
		NotificationConfigurationDto config = createConfig();
		//
		notificationManager.send(config.getTopic(), new IdmMessageDto.Builder().setTemplate(template).build(), identity);
		IdmNotificationFilter filter = new IdmNotificationFilter();
		// Test there is just one filtered log record.
		filter.setTopic(config.getTopic());
		List<IdmNotificationLogDto> notifications = notificationLogService.find(filter, null).getContent();
		//
		Assert.assertEquals(2, notifications.size());
		Assert.assertTrue(notifications.stream().anyMatch(n -> n.getType().equals(IdmNotificationLog.NOTIFICATION_TYPE)));
		Assert.assertTrue(notifications.stream().anyMatch(n -> n.getType().equals(IdmEmailLog.NOTIFICATION_TYPE)));
		// Test there is none log record after setting the filter to non-existing value.
		filter.setTopic("nonexistingTopic-147258369");
		assertEquals(0, notificationLogService.find(filter, null).getTotalElements());
	}

	@Test
	public void testSenderFilterTest(){
		IdmIdentityDto sender = getHelper().createIdentity();
		IdmNotificationFilter filter = new IdmNotificationFilter();
		IdmNotificationDto notification = new IdmNotificationDto();
		notification.setIdentitySender(sender.getId());
		//
		// create templates
		IdmNotificationTemplateDto template = createTestTemplate();
		IdmMessageDto message = new IdmMessageDto.Builder().setTemplate(template).build();
		notification.setMessage(message);
		notificationManager.send(notification);
		//
		// filter text BODY
		filter.setSender(sender.getUsername());
		Page<IdmNotificationLogDto> result = notificationLogService.find(filter, null);
		assertEquals("Wrong sender",sender.getId(), result.getContent().get(0).getIdentitySender());
	}

	@Test
	public void testParentFilterText(){
		IdmNotificationFilter filter = new IdmNotificationFilter();
		IdmNotificationDto notification = new IdmNotificationDto();
		IdmNotificationDto parentNotification = new IdmNotificationDto();
		// prepare template and message
		IdmNotificationTemplateDto template2 = createTestTemplate();
		IdmMessageDto message2 = new IdmMessageDto.Builder().setTemplate(template2).build();
		// set parent
		parentNotification.setMessage(message2);
		IdmNotificationLogDto logDto = notificationManager.send(parentNotification);
		notification.setParent(logDto.getMessage().getId());
		//
		// send message
		IdmNotificationTemplateDto template = createTestTemplate();
		IdmMessageDto message = new IdmMessageDto.Builder().setTemplate(template).build();
		notification.setMessage(message);
		notificationManager.send(notification);
		// set filter
		filter.setParent(logDto.getId());
		Page<IdmNotificationLogDto> result = notificationLogService.find(filter, null);
		assertEquals("Wrong sender", logDto.getId(), result.getContent().get(0).getParent());
	}

	@Test
	public void testStateFilterTest(){
		IdmIdentityDto identity1 = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto identity2 = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto identity3 = getHelper().createIdentity((GuardedString) null);
		IdmIdentityDto identity4 = getHelper().createIdentity((GuardedString) null);
		List<IdmIdentityDto> identities = Arrays.asList(identity1, identity2, identity3, identity4);
		IdmNotificationTemplateDto template = createTestTemplate();
		IdmMessageDto message = new IdmMessageDto.Builder().setTemplate(template).build();
		notificationManager.send(message, identities);
		//
		IdmNotificationFilter filter = new IdmNotificationFilter();
		filter.setText(template.getSubject());
		filter.setState(NotificationState.ALL);
		Page<IdmNotificationLogDto> result = notificationLogService.find(filter, null);
		assertEquals(1, result.getTotalElements());
	}

	@Test
	public void testSendWildCardsWithoutTemplate() {
		String topic = "testTopic-" + System.currentTimeMillis();
		String text = "testMessageText-" + System.currentTimeMillis();
		//
		IdmIdentityDto identity = getHelper().createIdentity();
		// create config, for email, topic and without level = wildcard
		NotificationConfigurationDto config = new NotificationConfigurationDto();
		config.setTopic(topic); // topic
		config.setNotificationType(IdmEmailLog.NOTIFICATION_TYPE); // email
		config = notificationConfigurationService.save(config);
		//
		// set all text into message
		List<IdmNotificationLogDto> notifications = notificationManager.send(topic,
				new IdmMessageDto.Builder()
				.setLevel(NotificationLevel.SUCCESS) // set level
				.setHtmlMessage(text)
				.setTextMessage(text)
				.setSubject(text)
				.build(),
				identity);
		//
		assertEquals(1, notifications.size());
		//
		IdmNotificationLogDto notification = notifications.get(0);
		assertEquals(text, notification.getMessage().getHtmlMessage());
		assertEquals(text, notification.getMessage().getSubject());
		assertEquals(text, notification.getMessage().getTextMessage());
	}

	@Test
	public void testSendWildCardsWithTemplateAndOwnText() {
		String topic = "testTopic-" + System.currentTimeMillis();
		String textMessage = "testMessageText-" + System.currentTimeMillis();
		String textTemplate = "testMessageTemplate-" + System.currentTimeMillis();
		//
		IdmNotificationTemplateDto template = new IdmNotificationTemplateDto();
		template.setName(textTemplate);
		template.setCode(textTemplate);
		template.setBodyHtml(textTemplate);
		template.setBodyText(textTemplate);
		template.setSubject(textTemplate);
		template = notificationTemplateService.save(template);
		//
		IdmIdentityDto identity = getHelper().createIdentity();
		// create config, for email, topic, template and without level = wildcard
		NotificationConfigurationDto config = new NotificationConfigurationDto();
		config.setTopic(topic); // topic
		config.setTemplate(template.getId()); // template
		config.setNotificationType(IdmEmailLog.NOTIFICATION_TYPE); // email
		config = notificationConfigurationService.save(config);
		//
		// set all text into message
		List<IdmNotificationLogDto> notifications = notificationManager.send(topic,
				new IdmMessageDto.Builder()
				.setLevel(NotificationLevel.SUCCESS) // set level
				.setHtmlMessage(textMessage)
				.setTextMessage(textMessage)
				.setSubject(textMessage)
				.build(),
				identity);
		//
		assertEquals(1, notifications.size());
		//
		IdmNotificationLogDto notification = notifications.get(0);
		// topic has own template, but in message is set text
		assertEquals(textMessage, notification.getMessage().getHtmlMessage());
		assertEquals(textMessage, notification.getMessage().getSubject());
		assertEquals(textMessage, notification.getMessage().getTextMessage());
	}

	@Test
	public void testSendWildCardsWithTemplateWithoutText() {
		String topic = "testTopic-" + System.currentTimeMillis();
		String textTemplate = "testMessageTemplate-" + System.currentTimeMillis();
		//
		IdmNotificationTemplateDto template = new IdmNotificationTemplateDto();
		template.setName(textTemplate);
		template.setCode(textTemplate);
		template.setBodyHtml(textTemplate);
		template.setBodyText(textTemplate);
		template.setSubject(textTemplate);
		template = notificationTemplateService.save(template);
		//
		IdmIdentityDto identity = getHelper().createIdentity();
		// create config, for email, topic, template and without level = wildcard
		NotificationConfigurationDto config = new NotificationConfigurationDto();
		config.setTopic(topic); // topic
		config.setTemplate(template.getId()); // template
		config.setNotificationType(IdmEmailLog.NOTIFICATION_TYPE); // email
		config = notificationConfigurationService.save(config);
		//
		List<IdmNotificationLogDto> notifications = notificationManager.send(topic,
				new IdmMessageDto.Builder()
				.setLevel(NotificationLevel.SUCCESS) // set level
				.build(),
				identity);
		//
		assertEquals(1, notifications.size());
		//
		IdmNotificationLogDto notification = notifications.get(0);
		// topic has own template and in message isnt set text
		assertEquals(textTemplate, notification.getMessage().getHtmlMessage());
		assertEquals(textTemplate, notification.getMessage().getSubject());
		assertEquals(textTemplate, notification.getMessage().getTextMessage());
	}

	@Test
	public void testSendTwoWildCardsWithDifferentTemplate() {
		String topic = "testTopic-" + System.currentTimeMillis();
		String textTemplate1 = "testMessageTemplate1-" + System.currentTimeMillis();
		String textTemplate2 = "testMessageTemplate2-" + System.currentTimeMillis();
		//
		IdmNotificationTemplateDto template1 = new IdmNotificationTemplateDto();
		template1.setName(textTemplate1);
		template1.setCode(textTemplate1);
		template1.setBodyHtml(textTemplate1);
		template1.setBodyText(textTemplate1);
		template1.setSubject(textTemplate1);
		template1 = notificationTemplateService.save(template1);
		//
		IdmNotificationTemplateDto template2 = new IdmNotificationTemplateDto();
		template2.setName(textTemplate2);
		template2.setCode(textTemplate2);
		template2.setBodyHtml(textTemplate2);
		template2.setBodyText(textTemplate2);
		template2.setSubject(textTemplate2);
		template2 = notificationTemplateService.save(template2);
		//
		IdmIdentityDto identity = getHelper().createIdentity();
		// create config, for email, topic, template and without level = wildcard
		NotificationConfigurationDto config1 = new NotificationConfigurationDto();
		config1.setTopic(topic); // topic
		config1.setTemplate(template1.getId()); // template
		config1.setNotificationType(IdmEmailLog.NOTIFICATION_TYPE); // email
		config1 = notificationConfigurationService.save(config1);
		//
		// create second config, for console, topic, template and without level = wildcard
		NotificationConfigurationDto config = new NotificationConfigurationDto();
		config.setTopic(topic); // same topic
		config.setTemplate(template2.getId()); // different template
		config.setNotificationType(IdmConsoleLog.NOTIFICATION_TYPE); // console
		config = notificationConfigurationService.save(config);
		//
		List<IdmNotificationLogDto> notifications = notificationManager.send(topic,
				new IdmMessageDto.Builder()
				.setLevel(NotificationLevel.SUCCESS) // set level
				.build(),
				identity);
		//
		assertEquals(2, notifications.size());
		//
		//
		int orderConsole = 0;
		int orderEmail = 1;
		// we didn't know order
		if (!notifications.get(0).getMessage().getHtmlMessage().equals(textTemplate2)) {
			// email first
			orderConsole = 1;
			orderEmail = 0;
		}
		//
		// Notification has different text
		IdmNotificationLogDto notificationConsole = notifications.get(orderConsole);
		assertEquals(textTemplate2, notificationConsole.getMessage().getHtmlMessage());
		assertEquals(textTemplate2, notificationConsole.getMessage().getSubject());
		assertEquals(textTemplate2, notificationConsole.getMessage().getTextMessage());
		//
		IdmNotificationLogDto notificationEmail= notifications.get(orderEmail);
		assertEquals(textTemplate1, notificationEmail.getMessage().getHtmlMessage());
		assertEquals(textTemplate1, notificationEmail.getMessage().getSubject());
		assertEquals(textTemplate1, notificationEmail.getMessage().getTextMessage());
	}

	@Test
	public void testSendTwoWildCardsWithOwnMessage() {
		String topic = "testTopic-" + System.currentTimeMillis();
		String textTemplate1 = "testMessageTemplate1-" + System.currentTimeMillis();
		String textTemplate2 = "testMessageTemplate2-" + System.currentTimeMillis();
		String textMessage = "testMessageText-" + System.currentTimeMillis();
		//
		IdmNotificationTemplateDto template1 = new IdmNotificationTemplateDto();
		template1.setName(textTemplate1);
		template1.setCode(textTemplate1);
		template1.setBodyHtml(textTemplate1);
		template1.setBodyText(textTemplate1);
		template1.setSubject(textTemplate1);
		template1 = notificationTemplateService.save(template1);
		//
		IdmNotificationTemplateDto template2 = new IdmNotificationTemplateDto();
		template2.setName(textTemplate2);
		template2.setCode(textTemplate2);
		template2.setBodyHtml(textTemplate2);
		template2.setBodyText(textTemplate2);
		template2.setSubject(textTemplate2);
		template2 = notificationTemplateService.save(template2);
		//
		IdmIdentityDto identity = getHelper().createIdentity();
		// create config, for email, topic, template and without level = wildcard
		NotificationConfigurationDto config1 = new NotificationConfigurationDto();
		config1.setTopic(topic); // topic
		config1.setTemplate(template1.getId()); // template
		config1.setNotificationType(IdmEmailLog.NOTIFICATION_TYPE); // email
		config1 = notificationConfigurationService.save(config1);
		//
		// create second config, for console, topic, template and without level = wildcard
		NotificationConfigurationDto config = new NotificationConfigurationDto();
		config.setTopic(topic); // same topic
		config.setTemplate(template2.getId()); // different template
		config.setNotificationType(IdmConsoleLog.NOTIFICATION_TYPE); // console
		config = notificationConfigurationService.save(config);
		//
		List<IdmNotificationLogDto> notifications = notificationManager.send(topic,
				new IdmMessageDto.Builder()
				.setLevel(NotificationLevel.SUCCESS) // set level
				.setHtmlMessage(textMessage)
				.setTextMessage(textMessage)
				.setSubject(textMessage)
				.build(),
				identity);
		//
		assertEquals(2, notifications.size());
		//
		// bouth notifacations has same text
		IdmNotificationLogDto notification1 = notifications.get(0);
		IdmNotificationLogDto notification2 = notifications.get(1);
		//
		assertEquals(textMessage, notification1.getMessage().getHtmlMessage());
		assertEquals(textMessage, notification1.getMessage().getSubject());
		assertEquals(textMessage, notification1.getMessage().getTextMessage());
		//
		assertEquals(textMessage, notification2.getMessage().getHtmlMessage());
		assertEquals(textMessage, notification2.getMessage().getSubject());
		assertEquals(textMessage, notification2.getMessage().getTextMessage());
	}

	@Test
	public void testSendNofificationToConsoleIfTopicNotFound() {
		String topic = getHelper().createName();
		// create config, for email, topic, template and without level = wildcard
		NotificationConfigurationDto config = new NotificationConfigurationDto();
		config.setTopic(topic); // topic
		config.setLevel(NotificationLevel.INFO);
		config.setNotificationType(IdmEmailLog.NOTIFICATION_TYPE); // email
		config = notificationConfigurationService.save(config);
		//
		IdmIdentityDto identity = getHelper().createIdentity();
		List<IdmNotificationLogDto> notifications = notificationManager.send(
				topic,
				new IdmMessageDto
					.Builder()
					.setLevel(NotificationLevel.SUCCESS) // set level
					.setMessage("message")
					.setSubject("subject")
					.build(),
				identity);
		//
		assertEquals(1, notifications.size());
		//
		// console channel is expected, because topic configuration is wrong
		IdmNotificationLogDto notification = notifications.get(0);
		//
		Assert.assertEquals(IdmConsoleLog.NOTIFICATION_TYPE, notification.getType());
	}

	@Test(expected = ResultCodeException.class)
	public void testSendRedirectNotificationWithEmptyRecipients() {
		IdmNotificationConfiguration config = new IdmNotificationConfiguration();
		config.setTopic(getHelper().createName());
		config.setNotificationType(IdmConsoleLog.NOTIFICATION_TYPE);
		config.setRedirect(true);
		config = notificationConfigurationRepository.save(config);
		//
		notificationManager.send(
				config.getTopic(),
				new IdmMessageDto
					.Builder()
					.setLevel(NotificationLevel.SUCCESS)
					.setMessage("message")
					.setSubject("subject")
					.build(),
				getHelper().createIdentity((GuardedString) null));
	}

	@Test
	public void testSendNotificationToAlias() {
		final IdmIdentityDto originalRecipient = getHelper().createIdentity((GuardedString) null);
		NotificationConfigurationDto config = new NotificationConfigurationDto();
		config.setTopic(getHelper().createName());
		config.setNotificationType(IdmConsoleLog.NOTIFICATION_TYPE);
		config.setRecipients("one,two,two,two");
		config.setRedirect(false);
		//
		// we are using repository directly - validations are on the service layer
		config = notificationConfigurationService.save(config);
		//
		String subject = getHelper().createName();
		List<IdmNotificationLogDto> notifications = notificationManager.send(
				config.getTopic(),
				new IdmMessageDto
					.Builder()
					.setLevel(NotificationLevel.SUCCESS)
					.setMessage("message")
					.setSubject(subject)
					.build(),
					originalRecipient);
		//
		Assert.assertEquals(2, notifications.size());
		Assert.assertTrue(notifications.stream().anyMatch(n -> {
			return n.getRecipients().size() == 1 && originalRecipient.getId().equals(n.getRecipients().get(0).getIdentityRecipient());
		}));
		Assert.assertTrue(notifications.stream().anyMatch(n -> {
			return n.getRecipients().size() == 2
					&& n.getRecipients().stream().anyMatch(r -> "one".equals(r.getRealRecipient()))
					&& n.getRecipients().stream().anyMatch(r -> "two".equals(r.getRealRecipient()));
		}));
		//
		// test by filter
		IdmNotificationFilter filter = new IdmNotificationFilter();
		filter.setText(subject);
		filter.setNotificationType(IdmNotificationLog.class); // envelope only
		notifications = notificationLogService.find(filter, null).getContent();
		//
		Assert.assertEquals(2, notifications.size());
		Assert.assertTrue(notifications.stream().anyMatch(n -> {
			return n.getRecipients().size() == 1 && originalRecipient.getId().equals(n.getRecipients().get(0).getIdentityRecipient());
		}));
		Assert.assertTrue(notifications.stream().anyMatch(n -> {
			return n.getRecipients().size() == 2
					&& n.getRecipients().stream().anyMatch(r -> "one".equals(r.getRealRecipient()))
					&& n.getRecipients().stream().anyMatch(r -> "two".equals(r.getRealRecipient()));
		}));
		//
		filter.setNotificationType(IdmConsoleLog.class); // target sender
		notifications = notificationLogService.find(filter, null).getContent();
		//
		Assert.assertEquals(2, notifications.size());
		Assert.assertTrue(notifications.stream().anyMatch(n -> {
			return n.getRecipients().size() == 1 && originalRecipient.getId().equals(n.getRecipients().get(0).getIdentityRecipient());
		}));
		Assert.assertTrue(notifications.stream().anyMatch(n -> {
			return n.getRecipients().size() == 2
					&& n.getRecipients().stream().allMatch(r -> IdmConsoleLog.NOTIFICATION_TYPE.equals(r.getRealRecipient()));
		}));
	}

	@Test
	public void testSendNotificationToAliasWithRedirect() {
		final IdmIdentityDto originalRecipient = getHelper().createIdentity((GuardedString) null);
		NotificationConfigurationDto config = new NotificationConfigurationDto();
		config.setTopic(getHelper().createName());
		config.setNotificationType(IdmConsoleLog.NOTIFICATION_TYPE);
		config.setRecipients("one,two,two,two");
		config.setRedirect(true);
		//
		// we are using repository directly - validations are on the service layer
		config = notificationConfigurationService.save(config);
		//
		String subject = getHelper().createName();
		List<IdmNotificationLogDto> notifications = notificationManager.send(
				config.getTopic(),
				new IdmMessageDto
					.Builder()
					.setLevel(NotificationLevel.SUCCESS)
					.setMessage("message")
					.setSubject(subject)
					.build(),
					originalRecipient);
		//
		Assert.assertEquals(1, notifications.size());
		Assert.assertTrue(notifications.stream().anyMatch(n -> {
			return n.getRecipients().size() == 2
					&& n.getRecipients().stream().anyMatch(r -> "one".equals(r.getRealRecipient()))
					&& n.getRecipients().stream().anyMatch(r -> "two".equals(r.getRealRecipient()));
		}));
		//
		// test by filter
		IdmNotificationFilter filter = new IdmNotificationFilter();
		filter.setText(subject);
		filter.setNotificationType(IdmNotificationLog.class); // envelope only
		notifications = notificationLogService.find(filter, null).getContent();
		//
		Assert.assertEquals(1, notifications.size());
		Assert.assertTrue(notifications.stream().anyMatch(n -> {
			return n.getRecipients().size() == 2
					&& n.getRecipients().stream().anyMatch(r -> "one".equals(r.getRealRecipient()))
					&& n.getRecipients().stream().anyMatch(r -> "two".equals(r.getRealRecipient()));
		}));
		//
		filter.setNotificationType(IdmConsoleLog.class); // target sender
		notifications = notificationLogService.find(filter, null).getContent();
		//
		Assert.assertEquals(1, notifications.size());
		Assert.assertTrue(notifications.stream().anyMatch(n -> {
			return n.getRecipients().size() == 2
					&& n.getRecipients().stream().anyMatch(r -> IdmConsoleLog.NOTIFICATION_TYPE.equals(r.getRealRecipient()));
		}));
	}

	private IdmNotificationTemplateDto createTestTemplate() {
		// create templates
		IdmNotificationTemplateDto template = new IdmNotificationTemplateDto();
		template.setName(getHelper().createName());
		template.setBodyHtml(getHelper().createName());
		template.setBodyText(template.getBodyHtml());
		template.setCode(template.getName());
		template.setSubject(getHelper().createName());
		return notificationTemplateService.save(template);
	}

	/**
	 * TODO: move to helper?
	 *
	 * @return
	 */
	private NotificationConfigurationDto createConfig() {
		NotificationConfigurationDto config = new NotificationConfigurationDto();
		config.setTopic(getHelper().createName());
		config.setNotificationType(IdmEmailLog.NOTIFICATION_TYPE);
		//
		return  notificationConfigurationService.save(config);
	}

}
