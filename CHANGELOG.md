# Changelog
All notable changes to this project will be documented in this file.

## [10.2.0]

- New method ``ReadWriteDtoService#deleteAll(Iterable<DTO>, BasePermission)`` was added into service layer - shortcut to delete all dtos in one transaction.
- LRT ``CancelProvisioningQueueTaskExecutor`` is deprecated. LRT is not used, use bulk action ``ProvisioningOperationCancelBulkAction`` instead.
- [#2068](https://redmine.czechidm.com/issues/2068) - New menu section for identity (``identity-menu``) was added into module descriptors. This menu is shown from top navigation under logged identity username - this menu is replacement for previously defined **item with id ``identities-profile-system``, which is not configurable in module descriptor (section ``system``) and cannot be disabled now.** Items in this menu is registerable as other items in module descriptor.
- [#1837](https://redmine.czechidm.com/issues/1837) - Long running task rejection policy is implemented. For this reason, default product task executor configuration was changed and this change should be done for projects (but is not required), where default configuration is not used -
``scheduler.task.executor.queueCapacity=50``. With previously configured ``Integer.MAX`` value was ``scheduler.task.executor.maxPoolSize`` configuration ignored => ``scheduler.task.executor.corePoolSize`` was used only.
- [#2107](https://redmine.czechidm.com/issues/2107) - Default method implementation was added into filter interfaces ``CorrelationFilter``, ``FormableFilter``, ``ModifiedFromFilter``, ``ExternalIdentifiableFilter``, ``ModifiedFromFilter``, ``ModifiedTillFilter``, ``CreatedFromFilter``, ``CreatedTillFilter``. Filter interfaces reuse ``DataFilter`` functionality now. If you are using this interface in your custom module, check ``DataFilter`` superclass is used too. All auditable entities can be found by new filters (created and modified form / till - filters are registered automatically).
- [#2105](https://redmine.czechidm.com/issues/2105) - Loading eav attributes with owner entity supports permissions now. If you need to override method ``AbstractFormableService#getFormInstances(DTO)`` in your custom module, then add new parameter with permissions =>
``AbstractFormableService#getFormInstances(DTO, BasePermission...)`` init method definition. Method usage is backward compatible.
- [#2164](https://redmine.czechidm.com/issues/2164) - method ``PrivateIdentityConfiguration#isFormAttributesSecured`` is deprecated. Secured identity attributes will be supported only in future version. This configuration will be removed in the next version.
- [#2164](https://redmine.czechidm.com/issues/2164) - new method ``renderConfirm`` was added to frontend component ``AbstractIdentityDashboardButton``. **Use this method for your custom dashboard button, if button needs to show confirm dialog**. Confirmation dialog cannot be closed otherwise, if it's rendered inside button content.

## [10.1.0]

- [#1711](https://redmine.czechidm.com/issues/1711) - Warning about leading and trailing whitespaces filled in form inputs will be shown automatically.
- [#1986](https://redmine.czechidm.com/issues/1986) - Bulk actions localization for remove entity were renamed from "remove" to "delete".
- [#2023](https://redmine.czechidm.com/issues/2023) - New "empty" option was added to all clearable select boxes used in filters and forms.

## [10.0.0]

- **All changes related to upgrade devstack dependencies can be found in [migration guide](./MIGRATION.md).**
- **Websocket** support removed - Removed all classed for websocket notifications
- [#1924](https://redmine.czechidm.com/issues/1924) - LRT ``PasswordExpiredTaskExecutor`` was scheduled by default to send notification for user after password expired and publish PASSWORD_EXPIRED event.
- **Deprecated classes and methods removed**:
  - ``IdmTreeNodeFilter#setTreeNode(UUID)`` - @deprecated @since 9.4.0 - use ``IdmTreeNodeFilter#setParent(UUID)``.
  - ``IdmTreeNodeFilter#getTreeNode()`` - @deprecated @since 9.4.0 - use ``IdmTreeNodeFilter#getParent()``.
  - ``IdmTreeNodeFilter#PARAMETER_PARENT_TREE_NODE_ID`` - @deprecated @since 9.4.0 - use ``IdmTreeNodeFilter#PARAMETER_PARENT``.
  - ``IdmIdentityContractService#prepareDefaultContract(UUID)`` - @deprecated @since 7.4.0 - use ``IdmIdentityContractService#prepareMainContract(UUID)``.
  - ``IdmIdentityContractRepository#findAllByWorkPosition(IdmTreeNode, RecursionType)`` => parameter changed to ``IdmIdentityContractRepository#findAllByWorkPosition(UUID, RecursionType)`` - previous SpEl expression doesn't work => ``UUID`` is used now as parameter.
  - ``IdmContractPositionRepository#findAllByWorkPosition(IdmTreeNode, RecursionType)`` => parameter changed to ``IdmContractPositionRepository#findAllByWorkPosition(UUID, RecursionType)``
  - ``IdmIdentityContractRepository#countByWorkPosition(IdmTreeNode)`` - @deprecated @since 7.4.0 - use ``IdmIdentityContractRepository#countByWorkPosition_Id(UUID)``.
  - ``IdmIdentityContractRepository#countByWorkPosition_TreeType(IdmTreeType)`` - @deprecated @since 7.4.0 - use ``IdmIdentityContractRepository#countByWorkPosition_TreeType_Id(UUID)``.
  - Unused Hibernate convertors ``LocalDateJpaConverter``, ``LocalDateTimeJpaConverter`` removed.
  - ``WorkflowDefinitionAssembler`` - unused and @deprecated @since 7.8.0.
  - ``CustomParseListener`` - unused and @deprecated @since 7.8.0.
  - ``FilterBuilder#getPredicate(Root, CriteriaQuery, CriteriaBuilder, DataFilter)`` - @deprecated @since 9.7.0 - use ``FilterBuilder#getPredicate(Root, AbstractQuery, CriteriaBuilder, DataFilter)`` - method was deprecated recently, but we want to prevent its usage (related to issue [#1779](https://redmine.czechidm.com/issues/1779)).
  - ``WorkflowFilterDto`` - redundant pageable properties (``pageNumber``, ``pageSize``, ``sortAsc``, ``sortDesc``, ``sortByFields``) removed. @deprecated @since 7.7.0. Use standard ``Pageable`` api instead.
  - ``WorkflowTaskInstanceService#search(WorkflowFilterDto)`` - @deprecated @since 7.7.0 - use ``WorkflowTaskInstanceService#find(WorkflowFilterDto, Pageable)`` - use ``IdmBasePermission.READ`` permission and set currently logged identity into filter, if security has to be evaluated.
  - ``WorkflowHistoricProcessInstanceService#search(WorkflowFilterDto)`` - @deprecated @since 7.7.0 - use ``WorkflowHistoricProcessInstanceService#find(WorkflowFilterDto, Pageable)``.
  - ``WorkflowHistoricTaskInstanceService#search(WorkflowFilterDto)`` - @deprecated @since 7.7.0 - use ``WorkflowHistoricTaskInstanceService#find(WorkflowFilterDto, Pageable)``.
  - ``WorkflowProcessInstanceService#search(WorkflowFilterDto)`` - @deprecated @since 7.7.0 - use ``WorkflowProcessInstanceService#find(WorkflowFilterDto, Pageable)``.
  - ``WorkflowProcessInstanceService#searchInternal(WorkflowFilterDto, boolean)`` - @deprecated @since 7.7.0 - use ``WorkflowProcessInstanceService#find(WorkflowFilterDto, Pageable, BasePermission)`` - parameter ``checkRigts`` is synonymum to ``IdmBasePermission.READ`` permission in standard find method.
  - Classes ``ResourceWrapper``, ``ResourcesWrapper``, ``EmbeddedsWrapper``, ``ResourcePage'`` (all in package ``eu.bcvsolutions.idm.core.api.rest.domain``) - @deprecated @since 7.7.0 - use standard ``find`` methods on workflow services instead.
  - ``IdmAuditFilter#getChangedAttributes()`` - @deprecated @since 9.5.0 - use ``IdmAuditFilter#getChangedAttributesList()``.
  - ``IdmAuditFilter#setChangedAttributes(List)`` - @deprecated @since 9.5.0 - use ``IdmAuditFilter#setChangedAttributesList(List<String>)``.
  - ``IdmAuditService#findEntityWithRelation(Class, MultiValueMap, Pageable)``- @deprecated @since 9.4.1 use ``IdmAuditService#findEntityWithRelation(IdmAuditFilter, Pageable)`` - ``ownerType`` filter can be used. If ``username`` parameter was specified, then use ``ownerCode`` filter).
  - ``ProvisioningService#createAccountsForAllSystems(AbstractDto)`` - @deprecated @since 7.6.0 - use ``ProvisioningService#accountManagement(AbstractDto)``.
  - ``ProvisioningEntityExecutor#createAccountsForAllSystems(DTO)`` - @deprecated @since 7.6.0 - use ``ProvisioningEntityExecutor#accountManagement(DTO)``.
  - ``IdmAuditService#getNameChangedColumns(Class<T>, UUID, Long, T)`` - @deprecated @since 7.8.2 - changed columns are solved directly in audit strategy (``IdmAuditStrategy``).
  - ``AccAccountRepository#countBySystem(SysSystem)`` - @deprecated @since 7.4.0 - use ``AccAccountRepository#countBySystem_Id(UUID)``.
  - ``AccAccountRepository#clearSystemEntity(UUID)`` - @deprecated @since 7.5.1 - use find - update in service instead (audit will not be skipped).
  - ``SysSystemAttributeMappingRepository#clearIsUidAttribute(UUID)`` - @deprecated @since 7.5.0 - use find - delete in service instead (audit will not be skipped).
  - ``AccIdentityAccountRepository#clearRoleSystem(SysRoleSystem)`` - use find - update in service instead (audit will not be skipped).
  - ``AccTreeAccountRepository#clearRoleSystem(SysRoleSystem)`` - use find - update in service instead (audit will not be skipped).
  - ``SysSystemRepository#find(SysSystemFilter, Pageable)`` - @deprecated @since 7.5.2 - use ``SysSystemService#find(SysSystemFilter, Pageable, BasePermission...)``
  - ``SysSystemRepository#clearPasswordPolicy(IdmPasswordPolicy)`` - use find - update in service instead (audit will not be skipped).
  - ``IdmFormDefinitionRepository#clearMain(String, UUID, ZonedDateTime)`` - @deprecated @since 7.4.0 - use find - update in service instead (audit will not be skipped).
  - ``IdmPasswordPolicyRepository#updateDefaultPolicyByType(IdmPasswordPolicyType, UUID)`` - use find - update in service instead (audit will not be skipped).
  - ``SysSyncConfigRepository#clearDefaultLeader(UUID)`` - use find - update instead (audit will not be skipped).
  - ``SysSyncConfigRepository#clearDefaultRole(UUID)`` - use find - update instead (audit will not be skipped).
  - ``SysSyncConfigRepository#clearDefaultTreeNode(UUID)`` - use find - update instead (audit will not be skipped).
  - ``SysSyncConfigRepository#clearDefaultTreeType(UUID)`` - use find - update instead (audit will not be skipped).
  - ``IdmLongRunningTaskRepository#findAllByInstanceIdAndResult_State(String, OperationState)`` - @deprecated @since 7.5.0 - use ``IdmLongRunningTaskRepository#findAllByInstanceIdAndResult_StateOrderByCreatedAsc(String, OperationState)``.
  - ``AccModuleDescriptor#TOPIC_NEW_PASSWORD_ALL_SYSTEMS`` - @deprecated @since 8.0.0 - use ``CoreModuleDescriptor.TOPIC_PASSWORD_CHANGED``.
  - ``SysProvisioningArchiveRepository#deleteBySystem(SysSystem)`` - @deprecated @since 7.4.0 - use ``SysProvisioningArchiveRepository#deleteBySystem_Id(UUID)``.
  - ``SysProvisioningOperationRepository#deleteBySystem_Id(UUID)`` - @deprecated @since 9.2.1 - use ``SysProvisioningOperationRepository#deleteBySystem(UUID)``.
  - ``SysRoleSystemAttributeRepository#deleteByRoleSystem(SysRoleSystem)`` - @deprecated @since 9.5.0 - use find - delete in service instead (method doesn't put merge values in historical controled values.).
  - ``SysProvisioningOperationController#cancelAll(MultiValueMap<String, Object>, )`` - @deprecated @since 9.5.2 - use provided ``ProvisioningOperationCancelBulkAction`` bulk action instead.
  - ``BaseEntityController`` - @deprecated @since 7.1.0 - use ``BaseDtoController``.
  - ``IdmAutomaticRoleAttributeService#prepareAddAutomaticRoles(IdmIdentityContractDto, Set)`` - @deprecated @since 7.8.4 - use ``IdmAutomaticRoleAttributeService#addAutomaticRoles(IdmIdentityContractDto, Set)``.
  - ``IdmAutomaticRoleAttributeService#prepareRemoveAutomaticRoles(IdmIdentityRoleDto, Set)`` - @deprecated @since 7.8.4 - use ``IdmAutomaticRoleAttributeService#removeAutomaticRoles(IdmIdentityRoleDto, Set)``.
  - ``IdmConceptRoleRequestFilter#getRoleTreeNodeId()`` - @deprecated @since 7.7.0 - use ``IdmConceptRoleRequestFilter#getAutomaticRole()``.
  - ``IdmConceptRoleRequestFilter#setRoleTreeNodeId(UUID)`` - @deprecated @since 7.7.0 - use ``IdmConceptRoleRequestFilter#setAutomaticRole(UUID)``.
  - ``IdmEntityEventService#findToExecute(String, ZonedDateTime, PriorityType, Pageable)`` - @deprecated @since 9.4.0 - use ``IdmEntityEventService#findToExecute(String, DateTime, PriorityType, List, Pageable)``.
  - ``IdmIdentityRoleService#findValidRole(UUID, Pageable)`` - @deprecated @since 8.0.0 - use ``IdmIdentityRoleService#findValidRoles(UUID, Pageable)``.
  - ``IdmIdentityService#findAllGuaranteesByRoleId(UUID)`` - @deprecated @since 8.2.0 - use ``IdmIdentityService#findGuaranteesByRoleId(UUID, Pageable)``.
  - ``IdmPasswordService#getSalt(IdmIdentityDto)`` - @deprecated @since 8.0.1 - use ``IdmPasswordService#getSalt()``.
  - ``IdmRoleService#getSubroles(UUID)`` - @deprecated @since 8.0.1 - use ``IdmRoleCompositionService#findDirectSubRoles(UUID)``.
  - ``IdmRoleTreeNodeService#prepareAssignAutomaticRoles(IdmIdentityContractDto, Set)`` - @deprecated @since 7.8.4 - use ``IdmRoleTreeNodeService#addAutomaticRoles(IdmIdentityContractDto, Set)``.
  - ``IdmRoleTreeNodeService#assignAutomaticRoles(IdmIdentityContractDto, Set)`` - @deprecated @since 7.8.4 - use ``IdmRoleTreeNodeService#addAutomaticRoles(IdmIdentityContractDto, Set)``.
  - ``IdmRoleTreeNodeService#prepareRemoveAutomaticRoles(IdmIdentityRoleDto, Set)`` - @deprecated @since 9.5.0 - use ``IdmRoleTreeNodeService#removeAutomaticRoles(IdmIdentityContractDto, Set)``.
  - ``IdmScriptService#getScriptByName(String)`` - @deprecated @since 7.6.0 - use ``IdmScriptService#getByCode(String)``.
  - ``IdmScriptService#getScriptByCode(String)`` - @deprecated @since 7.6.0 - use ``IdmScriptService#getByCode(String)``.
  - ``AuthorizationEvaluator#getParameterNames()`` - @deprecated @since 8.2.0 - use ``AuthorizationEvaluator#getFormDefinition()``.
  - ``IdmProcessedTaskItemService#findAllRefEntityIdsInQueueByScheduledTask(IdmScheduledTaskDto)`` - @deprecated @since 9.3.0 - use ``IdmProcessedTaskItemService#findAllRefEntityIdsInQueueByScheduledTaskId(UUID)``.
  - ``IdmProcessedTaskItemService#createLogItem(DTO, OperationResult, IdmLongRunningTaskDto)`` - @deprecated @since 9.3.0 - use ``IdmProcessedTaskItemService#createLogItem(AbstractDto, OperationResult, UUID)``.
  - ``IdmProcessedTaskItemService#createQueueItem(DTO, OperationResult, IdmScheduledTaskDto)`` - @deprecated @since 9.3.0 - use ``IdmProcessedTaskItemService#createQueueItem(AbstractDto, OperationResult, UUID)``.
  - ``IdmAudit#DELIMITER`` - @deprecated @since 7.8.2 - use ``IdmAuditDto.CHANGED_COLUMNS_DELIMITER``.
  - ``IdmAuditRepository#getPreviousVersion(UUID, Long, Pageable)`` - @deprecated @since 8.0.0 - use ``IdmAuditRepository#getPreviousVersion(UUID, Long)``.
  - ``IdentitySaveBulkAction`` - @deprecated @since 9.4.0 - use concrete bulk actions for execute ACM or provisioning only in acc module.
  - ``AbstractFormValueRepository#findByOwner(O)`` - @deprecated @since 8.2.0 - use ``FormValueService#find(IdmFormValueFilter, Pageable)``.
  - ``AbstractFormValueRepository#findByOwner_Id(Serializable)`` - @deprecated @since 8.2.0 - use ``FormValueService#find(IdmFormValueFilter, Pageable)``.
  - ``AbstractFormValueRepository#findByOwnerAndFormAttribute_FormDefinitionOrderBySeqAsc(O, IdmFormDefinition)`` - @deprecated @since 8.2.0 - use ``FormValueService#find(IdmFormValueFilter, Pageable)``.
  - ``AbstractFormValueRepository#findByOwnerAndFormAttribute_FormDefinition_IdOrderBySeqAsc(O, UUID)`` - @deprecated @since 8.2.0 - use ``FormValueService#find(IdmFormValueFilter, Pageable)``.
  - ``AbstractFormValueRepository#findByOwner_IdAndFormAttribute_FormDefinition_IdOrderBySeqAsc(Serializable, UUID)`` - @deprecated @since 8.2.0 - use ``FormValueService#find(IdmFormValueFilter, Pageable)``.
  - ``AbstractFormValueRepository#findByOwner_IdAndFormAttributeOrderBySeqAsc(Serializable, IdmFormAttribute)`` - @deprecated @since 8.2.0 - use ``FormValueService#find(IdmFormValueFilter, Pageable)``.
  - ``AbstractFormValueRepository#findByOwner_IdAndFormAttribute_IdOrderBySeqAsc(Serializable, UUID)`` - @deprecated @since 8.2.0 - use ``FormValueService#find(IdmFormValueFilter, Pageable)``.
  - ``IdmIdentityRoleRepository#countByRole(IdmRole)`` - @deprecated @since 7.4.0 - use ``IdmIdentityRoleRepository#countByRole_Id(UUID)``.
  - ``IdmRoleCatalogueRoleRepository#find(IdmRoleCatalogueRoleFilter, Pageable)`` - @deprecated @since 8.1.4 - use ``IdmRoleCatalogueRoleService#find(IdmRoleCatalogueRoleFilter, Pageable)``.
  - ``IdmRoleCatalogueRoleRepository#findAllByRole_Id(UUID)`` - @deprecated @since 8.1.4 - use ``IdmRoleCatalogueRoleService#find(IdmRoleCatalogueRoleFilter, Pageable)``.
  - ``IdmRoleCatalogueRoleRepository#findAllByRoleCatalogue_Id(UUID)`` - @deprecated @since 8.1.4 - use ``IdmRoleCatalogueRoleService#find(IdmRoleCatalogueRoleFilter, Pageable)``.
  - ``IdmRoleCatalogueRoleRepository#deleteAllByRole_Id(UUID)`` - @deprecated @since 8.1.4 - use ``IdmRoleCatalogueRoleService#find(IdmRoleCatalogueRoleFilter, Pageable)`` and then delete.
  - ``IdmRoleCatalogueRoleRepository#deleteAllByRoleCatalogue_Id(UUID)`` - @deprecated @since 8.1.4 - use ``IdmRoleCatalogueRoleService#find(IdmRoleCatalogueRoleFilter, Pageable)`` and then delete.
  - ``IdmMessage#DEFAULT_LEVEL`` - @deprecated @since 7.6.0 - use ``IdmMessageDto.DEFAULT_LEVEL``.
  - ``IdmNotificationConfigurationRepository#findTypes(String, NotificationLevel)`` - @deprecated @since 9.2.0 - use ``IdmNotificationConfigurationRepository#findAllByTopicAndWildcardLevel(String, NotificationLevel)``.
  - ``IdmIdentityController#roles(String)`` - @deprecated @since 9.4.0 - use ``IdmIdentityRoleController#find(MultiValueMap, Pageable)`` with filter by identity.
  - ``AbstractAutomaticRoleTaskExecutor#setRoleTreeNodeId(UUID)`` - @deprecated @since 7.6.0 - use ``AbstractAutomaticRoleTaskExecutor#setAutomaticRoleId(UUID)``.
  - ``AbstractAutomaticRoleTaskExecutor#getRoleTreeNodeId()`` - @deprecated @since 7.6.0 - use ``AbstractAutomaticRoleTaskExecutor#getAutomaticRoleId(UUID)``.
  - ``AddNewAutomaticRoleForPositionTaskExecutor#setRoleTreeNodeId(UUID)`` - @deprecated @since 7.6.0 - use ``AbstractAutomaticRoleTaskExecutor#setAutomaticRoleId(UUID)``.
  - ``AddNewAutomaticRoleForPositionTaskExecutor#getRoleTreeNodeId()`` - @deprecated @since 7.6.0 - use ``AbstractAutomaticRoleTaskExecutor#getAutomaticRoleId(UUID)``.
  - ``AddNewAutomaticRoleTaskExecutor#setRoleTreeNodeId(UUID)`` - @deprecated @since 7.6.0 - use ``AbstractAutomaticRoleTaskExecutor#setAutomaticRoleId(UUID)``.
  - ``AddNewAutomaticRoleTaskExecutor#getRoleTreeNodeId()`` - @deprecated @since 7.6.0 - use ``AbstractAutomaticRoleTaskExecutor#getAutomaticRoleId(UUID)``.
  - ``RemoveAutomaticRoleTaskExecutor#setRoleTreeNodeId(UUID)`` - @deprecated @since 7.6.0 - use ``AbstractAutomaticRoleTaskExecutor#setAutomaticRoleId(UUID)``.
  - ``RemoveAutomaticRoleTaskExecutor#getRoleTreeNodeId()`` - @deprecated @since 7.6.0 - use ``AbstractAutomaticRoleTaskExecutor#getAutomaticRoleId(UUID)``.
- Default database name configured for the all ``dev`` profiles was renamed to ``bcv_idm_10`` to prevent update (by flyway) old database on the background - **old database can be used for LTS version 9.7.x development**, so clone database is needed.

## [9.7.15]

- [#2042](https://redmine.czechidm.com/issues/2042) - LRT ``AccountProtectionExpirationTaskExecutor `` was scheduled by default.

## [9.7.12]

- [#1917](https://redmine.czechidm.com/issues/1917) - **Base permissions for roles, which can be requested, is used in copy assigned roles feature**. Configure this permissions (``CANBEREQUESTED``) to identity roles, which can be copied. Previously configured permissions ``READ`` on identity role will be not sufficient to copy roles in role request detail. **New authorization policy evaluator ``IdentityRoleByRoleEvaluator`` can be configured to add this permission to identity roles by role definition.** If you want to enable copying all assigned roles (the same behavior as before), then add ``BasePermissionEvaluator`` with permission ``CANBEREQUESTED`` for all assigned roles (``IdmIdentityRole`` entity).
- [#1932](https://redmine.czechidm.com/issues/1932) - LRT ``DeleteLongRunningTaskExecutor`` was created and scheduled by default to remove old executed long running tasks (``EXECUTED`` lrt older than 90 days will be removed by default schedule). Delete long running task is possible now (new permission ``LONGRUNNINGTASK_DELETE`` is needed) => **if you are using long running task reference in entity in your custom module, then custom processor (processing ``DELETE`` event with ``IdmLongRunningTaskDto`` content) has to be implemented (ensure referential integrity)**.
- [#1933](https://redmine.czechidm.com/issues/1933) - LRT ``DeleteNotificationTaskExecutor`` was created and scheduled by default to remove old sent notifications (``sent`` notifications older than 180 days will be removed by default schedule). Delete notificationa is possible now (new permission ``NOTIFICATION_DELETE`` is needed).
- [#1934](https://redmine.czechidm.com/issues/1934) - LRT ``DeleteSynchronizationLogTaskExecutor`` was created and scheduled by default to remove old synchronization logs (logs older than 180 days will be removed by default schedule).
- [#1413](https://redmine.czechidm.com/issues/1413) - Implemented merging of EAVs from slices to contract. Beware, only EAVs with changed value will be updated now.

## [9.7.9]

- [#1894](https://redmine.czechidm.com/issues/1894) - Virtual system request - Note for implementer was added. New method in service VsRequestService was added `realize(request, reason)`. From the FE is call only new method `realize(request, reason)` now! If some project overridden old method `realize(request)`, then you may be need to change it to the new one.

## [9.7.8]

- [#1887](https://redmine.czechidm.com/issues/1887) - Filling the value of ``validTill`` automatically for newly created assigned roles in bulk action for role assignment (``IdentityAddRoleBulkAction``) and in synchronization (default role creation) by related contract validity was removed.

## [9.7.7]

- Services ``IdmEntityEventService``, ``IdmEntityStateService`` and managers ``EntityEventManager``, ``EntityStateManager`` are usable in scripts now (``ScriptEnabled`` added).
- [#1851](https://redmine.czechidm.com/issues/1851) - Bulk action for recalculate accounts and provision (``RoleAccountManagementBulkAction``) on the role table works the same way as the same bulk action on the identity table - account management and provisioning is called synchronously in LRT, which is asynchronous itself (and event queue is not overused now).

## [9.7.5]

- [#1798](https://redmine.czechidm.com/issues/1798) - **New base permissions for roles, which can be requested, was added**. Configure this new permissions (``CANBEREQUESTED``) to role, which enables role requests for users. Previously configured permissions ``AUTOCOMPLETE`` on role will be not sufficient to see roles in role request detail (and in bulk actions for assign / remove assigned roles). ``AUTOCOMPLETE`` permission can be used  widely for select boxes. **If you have ``RoleCanBeRequestedEvaluator`` configured in you project already, just switch permission from ``AUTOCOMPLETE`` to ``CANBEREQUESTED``**.

## [9.7.3]

- [#1794](https://redmine.czechidm.com/issues/1794) Notification template with code ``vs:vsRequestCreated`` was changed. Please redeploy manually this notification template.
- [#1798](https://redmine.czechidm.com/issues/1798) - **New base permissions for disable / enable identity manually was added**. Configure this new permissions (``MANUALLYDISABLE``, ``MANUALLYENABLE``) to role, which enables bulk action and dashboard button for activate / deactivate user manually. Previously configured permissions ``UPDATE`` on identity will be not sufficient to execute this function.

## [9.7.0]
- Deprecated class (since 7.3.0) ``AuditableListener`` was removed. Use class ``AuditableEntityListener`` instead.
- Deprecated classes (since 7.7.0) for long running tasks placed in core ``impl`` package was removed. Used the same classes from core ``api``.
- [#1692](https://redmine.czechidm.com/issues/1692) - **The table of user roles changes has been redesigned**. All logic was move from frontend to backend. This new table (RequestIdentityRoleTable) is pageable, sorting, filtering on backend now.
   - For this table was created new DTO ``IdmRequestIdentityRoleDto``, representing change for specific identity-role entity.
   - For this table was created new REST endpoint too ``/request-identity-roles``. This endpoint can be calling with standard CRUD operations. Logic in this REST (service ``IdmRequestIdentityRoleService``) will be automatically creates role-concepts.
   - The finding method in this serivce compiles assigned identity-roles and role-concepts together and returns list of ``IdmRequestIdentityRoleDto``.
   - Sorting works separatly for identity-roles and concepts. For this reason are first returns concepts adding new assign roles. Then are returns currently assigned identity-roles. If for returns identity-role exists remove or update concept, then is identity-role marked as changed. Role-concepts and identity-roles are always returns as ``IdmRequestIdentityRoleDto``.
   - **For performance reasons, the REST endpoint for role-request, returns request without concepts** (from this version)! For same reason **``IdmRoleRequestService`` doesn't returns concepts in the request from now**. If you need to get request with a concepts, you can use this:  
    ```javascript
    roleRequestService.get(requestId, new IdmRoleRequestFilter(true));
    ```
- ``eslint 5.3.0`` library in version is used for FE - use new ``.eslintrc`` configuration file in your custom module (new rules was reused from ``airbnb``).
- History of workflow processes created (and synchronously ended) in synchronization will be delete from now (for prevent of too many records in activiti database).
- **NPM version 6.x.x or higher is required** for support of NPM lockfile (package-lock.json).
- ``FilterBuilder`` supports creating predicates for sub queries now. Use new ``getPredicate`` method with ``AbstractQuery`` parameter instead (method with ``CriteriaQuery`` parameter is deprecated and will be removed). All filters in core was updated to use ``AbstractQuery`` parameter - make sure you override this method in your custom module.
- Deprecated module ``gui`` was removed from product. Prevent to pull target folder, if you are product developer.
- Email templates ``changeIdentityRoleImplementer`` and ``changeIdentityRole`` were redesigned within task ``system state in request``
 [#1736](https://redmine.czechidm.com/issues/1736). **Please don't forget upgrade them (manually) in your projects.**
- [Filters](https://wiki.czechidm.com/devel/documentation/architecture/dev/filters#defaultcontractbymanagerfilter) by manager for subordinate contracts were added.


## [9.6.3]
- [#1668](https://redmine.czechidm.com/issues/1668) - ``EntityEventDeleteExecutedProcessor`` was removed. LRT ``DeleteExecutedEventTaskExecutor`` was created and scheduled by default to cover this functionality (executed events older than 3 days will be removed by default schedule).
- [#1393](https://redmine.czechidm.com/issues/1393) - Provisioned attributes (schema connector attribute names) are logged for provisioning operations in queue and archive. Is possible to filter by attributes used in provisioning context (updated, removed and empty provisioning). This feature is available for the new records in provisioning queue and archive. **Older records** (created before version 9.6.3 was installed) **will be filtered as empty provisioning without attributes** (attributes are empty).
- [#1647](https://redmine.czechidm.com/issues/1647) - LRT ``DeleteProvisioningArchiveTaskExecutor`` was created and scheduled by default to remove old provisioning archives (archived EXECUTED operations older than 90 days will be removed by default schedule).

## [9.6.0]
- [#1534](https://redmine.czechidm.com/issues/1534) - Incremental account management and provisioning:
  - Change/update/delete of the assigned roles (identity-roles) invoke account management only for changed identity-roles. No more for all identity-roles of the identity. If you need to execute account management for entire identity, then you can use bulk action "Recalculate accounts and provision".
  - Change/update/delete of the assigned roles (identity-roles) invoke provisioning only for touched accounts.
  - Return type for method ``AccAccountManagementService.deleteIdentityAccount`` was changed from ``void`` to ``List<UUID>``.
  - New methods (``resolveNewIdentityRoles``, ``resolveUpdatedIdentityRoles``) for incremental account management were added to service ``AccAccountManagementService``.
- [#1543](https://redmine.czechidm.com/issues/1543) - Realization of the role-request is executed asynchronously now. Processor ``RoleRequestRealizationProcessor`` catches event for type ``RoleRequestEventType.NOTIFY`` now (previously ``RoleRequestEventType.EXECUTION``).
- [#1543](https://redmine.czechidm.com/issues/1543) - **Parent event is not closed as ``EXECUTED`` automatically** - not needed anymore (used for asynchronous business roles in role request). Prevent to use second asynchronous processing in your modules (Request is processed asynchronously => it's not needed to process sub events asynchronously).
- [#514](https://redmine.czechidm.com/issues/514) - Records are saved in new transaction into provisioning queue - event if some connector throws unexpected exception (which doesn't generalize ``ProvisioningException``), then log with exception is available in queue. If you are using custom processor for event with ``SysProvisioningOperationDto`` content, don't forget to save this provisioning operation in new transaction too - use prepared ``SysProvisioningOperationService#saveOperation`` method.
- [#514](https://redmine.czechidm.com/issues/514) - **Notification after successful provisioning operation is not sent any more**. Information is available in application log under level ``INFO`` only for now. It is the same behavior as original default notification configuration for topic ``acc:provisioning`` with ``SUCCESS`` level. Notification about failed provisioning operation is still available (the same topic ``acc:provisioning`` with level ``ERROR``) and can be configured to email (sent to application log by default notification configuration).
- [#1185](https://redmine.czechidm.com/issues/1185) and [#1384](https://redmine.czechidm.com/issues/1384). Password metadata is now filled only by controller not service. Password chnage form has now different url path than before. Original: **/identity/:entityId/password** new **/identity/:entityId/password/change**. Please check all overriden forms and components for possible issues.
- Deprecated methods (since 7.7.0) ``SysProvisioningBatchRepository#findBatch`` were removed. Use method ``SysProvisioningBatchRepository#findAllBySystemEntity_IdOrderByCreatedAsc`` instead.
- Deprecated method (since 7.7.0) ``IdmNotificationTemplateService#getTemplateByCode`` was removed. Use method ``IdmNotificationTemplateService#getByCode`` instead.
- Deprecated method (since 7.8.0) ``IdmIdentityRepository#findAllByRole`` was removed. Use method ``IdmIdentityService#findAllByRole`` instead.
- Deprecated method (since 7.8.0) ``JwtAuthenticationMapper#getDTOAuthorities`` was removed. Use method ``JwtAuthenticationMapper#getDtoAuthorities`` instead.
- Deprecated method (since 8.2.0) ``SysProvisioningBatchService#findBatch(UUID, UUID, UUID)`` was removed. Use method ``SysProvisioningBatchService#findBatch(UUID)`` instead.


## [9.4.0]
- [#1372](https://redmine.czechidm.com/issues/1372) - HTTP status 206 for long running task supports download result from them.
- Deprecated static properties ``PARAMETER_PROPERTY`` and ``PARAMETER_VALUE`` defined in ``IdmTreeNodeFilter`` were removed (deprecated @since 8.2.0). Use static properties ``PARAMETER_CORRELATION_PROPERTY`` and ``PARAMETER_CORRELATION_VALUE`` instead.
- [#1378](https://redmine.czechidm.com/issues/1378) - Dashboard content was redesigned and split into two component - dashboard (system info) and identity dashboard (identity info). ``Index.js``, ``layout`` in frontend ``czechidm-app`` module was updated - don't forget to **update this module**, if project specific app module is used. Changes are backward compatible - previously created dashboards will be rendered in new dashboard content. New dashboard components with type ``identity-dashboard`` can be registered.
- Confidential storage agenda was hidden in menu. Confidential storage is still available in router on url ``<server>/confidential-storage`` and on rest api.
- Font Awesome library was upgraded to version 5. Backward compatibility with the old version 4 icon names was configured, but will be removed in release CzechIdM 10.x. Use new icon names - see the [documentation](https://fontawesome.com/how-to-use/on-the-web/setup/upgrading-from-version-4#name-changes).
- Deprecated unused field **``parameters`` was removed from ``AuthorizationEvaluatorDto``** (deprecated since CzechIdM 8.2.0). Use form definition instead (field ``formDefinition``).
- Services for extended attributes definition ``IdmFormAttributeService`` and ``IdmFormDefinitionService`` support events now (constructors was changed).
- [#1474](https://redmine.czechidm.com/issues/1474) - Changing persistent type of form attribute is possible only if no values are persisted.
- [#1050](https://redmine.czechidm.com/issues/1050) - **Shortext as default** - When **EAV attribute** from mapped attribute was created, then (for string schema attributes) **PersistentType.TEXT** type was used as default. TEXT type cannot be indexed, so this type is not useful for searching. For this reason will be since version 9.4.0 as default type sets **PersistentType.SHORTTEXT**.
**Important**: Shortext is limmeted on max **2000** characters! Existing values (with old type) will be not modifed.
- [#1405](https://redmine.czechidm.com/issues/1405) - Code of workflow **Synchronization - Roles from LDAP** was changed from **'syncRoleAd'** to **'syncRoleLdap'**. More info in the ticket.



## [9.3.0]
- [#1290](https://redmine.czechidm.com/issues/1290) - ``SchedulableStatefulExecutor`` supports separating processing of items, read more in [documentation](https://wiki.czechidm.com/devel/documentation/application_configuration/dev/scheduled_tasks/task-scheduler#stateful_task_executors). Use new properties ``continueOnException``, ``requireNewTransaction`` in your custom LRT, when the same behavior is needed.
- [#1285](https://redmine.czechidm.com/issues/1285) - Password is now transformed via transformation script to resource. Beware all password including \_\_PASSWORD\_\_ must be marked as password attribute (attribute mapping detail). All password attributes **can't be overridden in role mapping**. Transformation script obtain password as GuardedString. **GuardedString or null must be returned by the script**. Password generation during create new account works same as before.
- [#1358](https://redmine.czechidm.com/issues/1358) - Frontend ``Tree`` component was redesigned from the scratch. I hope no one used original ``Tree`` component in custom modules (original was ugly and buged, I'm apologize :(), but if yes, check please your contents on frontend and use new properties respecting the component properties or the component's ``readme.txt`` - the main change is about removed header decorator and added ``onSelect``, ``onDetail`` callbacks instead. New tree component is used in forms (e.g. for creating new contract or position) - tree structure can be selected with tree usage now.
- [#1323](https://redmine.czechidm.com/issues/1323) - Provisioning ``Merge`` was redesigned. Controlled values in IdM are computed from transformations on role-system-attribute now. Provisioning archive is no longer used for merge. All system attributes was marked with 'evictControlledValuesCache = true'. We need ensure recalculation for all merge attribute (resolves state when somewho could set parent attribute strategy on different value than Merge). All merge attribute caches will be recalculated on first provisioning.
- [#1370](https://redmine.czechidm.com/issues/1370) - ``environment`` and ``baseCode`` attributes were added into ``IdmRole`` entity and dto:
  - Unique role's attribute ``code`` is combined from ``baseCode``, ``environment`` and with separator (``|`` by default).
  - It's possible to synchronize (or create) roles with the same ``baseCode`` and different ``environment``. ``Environment`` attribute is needed to use in synchronization mapping and correlation (e.g. attribute is filled by constant and this constant has to be used in correlation to e.g. ``<baseCode>|<environment>``. Change script was provided for switching current attribute mappings in synchronization for role's ``code`` attribute => ``baseCode`` is used now.
  - ``baseCode`` and ``code`` attribute is filled automatically on background, if only one is given, ``code`` can be still used (for maintain backward compatibility) => **no change is needed on your projects, if new ``environment`` attribute is not used**, otherwise ``baseCode`` should be used (exception will be thrown, if ``code`` and ``environment`` will be trying to change together - this is the reason, why change script for synchronization mapping was provided - it's not needed to change mapping for role's code (=> switched to ``baseCode``), just add mapping for the added environment).
- [#1225](https://redmine.czechidm.com/issues/1225) - Synchronization of identities now supports linking accounts to protection mode or not linking them at all, if the owner of the account doesn't have any valid contract. Synchronization also removes the flag "wish" from system entities if possible, so IdM has correct information about existing accounts in the target system.
- [#1361](https://redmine.czechidm.com/issues/1361) - Email notification can send attachments. Sent attachments are not persisted automatically and it's possible to send them from backend only. Attachments (``IdmAttachmentDto``) can be instanced (``inputData`` will be used, if it's not {@code null}), or persisted before - data will be loaded by ``AttachmentManager``, when email is send (see ``DefaultEmailer``).

## [9.2.2]
- [#1322](https://redmine.czechidm.com/issues/1322) - Preferred language and collapsed navigation is persisted to identity profile now and is loaded after login. Selecting locale doesn't refresh whole page now - add listening redux property ``i18nReady: state.config.get('i18nReady')`` if you content is not refreshed automatically and use localization.
- ``Index.js`` in frontend ``czechidm-app`` module was updated - don't forget to **update this module**, if project specific app module is used.

## [9.2.0]
- [#1261](https://redmine.czechidm.com/issues/1261) - Internal processing of events in ``AbstractEntityEventProcessor`` was improved. It's possible to create processor for super classes, e.g. one processor can handle all BaseDto generalizations. ``AbstractEntityEventProcessor#onApplicationEvent`` method intercepts ``ApplicationEvent`` now - this method is not in ``EntityEventProcessor`` interface, but if you overrided her in custom processor, refactor your processors to new input parameter.
- [#1267](https://redmine.czechidm.com/issues/1267) - Eav attribute values can be saved together with owner - e.g. save identity with filled eavs. ``FormableDto`` super class was added and has to be defined for all dtos with eav support - change generalization tor your custom dtos with eav attribute support from ``AbstractDto`` to new ``FormableDto``.
- [#1272](https://redmine.czechidm.com/issues/1272) - Default value is not used for new extended attributes on entity details. Make sure your custom component implementing ``AbstractFormAttributeRenderer`` behaves the same way, if ``#toInputValue`` method was overrided. Sending read only (~disabled) extended attribute values from FE to BE was removed - read only attributes can be secured and this check worked only if value is not changed in the meantime (update and security check was skipped). Now it will be more transparent.
- [#1259](https://redmine.czechidm.com/issues/1259) - New agenda for generated values was created. Now is possible generate for example username, email and eavs. Agenda now supports only ``IdmIdentity`` and their eavs.
- [#1281](https://redmine.czechidm.com/issues/1281) - Websocket notifications are no longer supported. Configured websocket notifications are sent to configured global default notification sender (console by default). Reconfigure your custom topics, which uses websocket, to other notification type (e.g. email, console). Websocket sender and service (FE+BE) is marked as deprecated and they are not initialized by default - move senders to your project if needed (don't forget to copy and rename (or create) the new table for saving websocket notification logs in your database - table ``idm_notification_websocket`` will be removed in next major release). ``Index.js`` in frontend ``czechidm-app`` module was updated - don't forget to **update this module**, if project specific app module is used.
- [#1287](https://redmine.czechidm.com/issues/1287 - New generate type for passwords - prefixes and suffixes) Is possible generated passwords with prefixes and suffixes. Both strings are optional and can be setup only for generated password policy. Prefix and suffix are not counted into maximum and minimum length.
- [#1158](https://redmine.czechidm.com/issues/1158) - Long running task implement Configurable interface now. Form definition can be used for LRT configuration.


## [9.1.0]
- Authentication is not needed for using Spring Data queries in repositories (fixed mainly for test purposes).
- Role code is used in role select boxes (code is shown, only if name is different than code).
- [#1145](https://redmine.czechidm.com/issues/1145) - Other contract positions can be configured. Other positions are used for assign automatic roles by tree nodes.
- ``Index.js`` in frontend ``czechidm-app`` module was updated - don't forget to **update this module**, if project specific app module is used.

## [9.0.0]
- [#1200](https://redmine.czechidm.com/issues/1200) - Business roles:
  - ``code`` unique attribute added to ``IdmRole`` entity - filled by ``name`` values by default . ``name`` attribute is not unique anymore - **use code attribute in your modules** for lookup roles. ``name`` is used as user friendly role name on frontend. Both fields ``name`` and ``code`` are required on controller layer.
  - Deprecated ``IdmRoleService#getByName`` method was removed - use ``IdmRoleService#getByCode`` method.
  - Deprecated lazy lists were removed from ``IdmRole``, ``IdmRoleDto`` - ``subRoles``, ``superiorRoles``, ``guarantees``, ``roleCatalogues``. Check usage of this lists in your modules and replace them with appropriate services.
  - Make sure all your workflow processes are correctly ended - ``IdmRole``, ``IdmRoleDto``, ``IdmIdentityRole`, ``IdmIdentityRoleDto``, ``IdmConceptRoleRequestDto`` structure changed.
   - All Lazy lists from  `IdmRole``, ``IdmRoleDto`` were removed - see above.
   - ``roleTreeNode`` attribute was renamed to ``automaticRole`` (previous ``automaticRole`` boolean flag was removed) in dtos ``IdmIdentityRoleDto``, ``IdmConceptRoleRequestDto``.Check attribute ``automaticRole`` (for automatic role) or ``directRole`` (for sub roles of business role) - this assigned roles cannot be removed directly. ``IdmAutomaticRoleAttributeService#ROLE_TREE_NODE_ATTRIBUTE_NAME`` was removed (used just for compatibility issues, which is not needed anymore) - use standard ``IdmIdentityRole_.automaticRole`` metamodel.
  - Obsolete, deprecated and unused methods ``IdmRoleRepository#findOneByName``,``IdmRoleRepository#getPersistedRole`` and ``IdmRoleRepository#getSubroles`` removed. Use ``IdmRoleService`` instead (with filter usage).
  - ``RoleGuaranteeEvaluator`` - evaluator supports guarantees configured by identity and by role now.
  - **Role guarantees, business roles (composition), role catalogues, tree nodes supports authorization policies now - configure [authorization policies](https://wiki.czechidm.com/devel/documentation/security/dev/authorization#default_settings_of_permissions_for_a_role_detail).**
- Event mechanism was improved:
  - ``EntityEventManager`` constants for event properties were moved directly into ``EntityEvent``. Use event getters and setters to use property values.
  - Parent event can be propagated, when sub event is created by the parent event. Executing account management was improved thanks to this mechanism - is called after change role request is executed (not for the all single assigned roles). When role is assigned outside the request (directly in some backend business logic), then account management s executed the same way as before - for each assigned role.
  - child events are deleted automatically - remove events from event queue will be easier.
- Method ``IdmAuthorizationPolicyService#getEnabledPersistedRoleAuthorities`` was removed - use previously loaded dto in event processing.
- [#1204](https://redmine.czechidm.com/issues/1204) - Reschedule HR long running task  ``HrEnableContractProcess`` - processed item queue should be cleared

## [8.2.0]

- [#1125](https://redmine.czechidm.com/issues/1125) - Identity extended attributes supports authorization policies, read [more](https://wiki.czechidm.com/devel/documentation/security/dev/authorization#secure_identity_form_extended_attribute_values).
- [#1121](https://redmine.czechidm.com/issues/1121) - Execute synchronization asynchronously
  - SynchronizationService is no longer LRT (Long Running Task).
  - Event types and processors to start and cancel sync has been removed.
- [#636](https://redmine.czechidm.com/issues/636) - JWT token are persisted now. **JWT token doesn't contain authorities now - use ''LoginDto.authorities'' instead**. JWT contains only authentication data. Logout feature is supported now. ``AuthenticationTestUtils`` was removed - this utility used SecurityContext directly and this was dangerous (identity id was not filled and authorization policies was skipped). ``IdmAuthorityChange`` entity and repository was removed at all - ``IdmTokenDto`` and ``DefaultTokenManager`` can be used now.
- [#1163](https://redmine.czechidm.com/issues/1163) - ``ConfidentialStorage`` service supports ``AbstractDto`` as value owner now. Check your module, where ``AbstractDto`` as owner for ``ConfidentialStorage`` was used before this update and provide appropriate change script if needed (see ``V8_02_006__confidential-storage-support-identifiable-owner.sql``).

## [8.1.7]
- ``Index.js`` in frontend ``czechidm-app`` module was updated - don't forget to **update this module**, if project specific app module is used.

## [8.1.3]

- ``Configurable`` interface contains form definition now. ``Configurable`` instances can be configured by eav form on frontend (the richer ui). **Methods ``getFormDefinition`` and ``getFormAttributes`` were moved to ``Configurable`` interface from ``AbstractReportExecutor`` super class. Make sure you are using ``public`` modifier on this methods in your reports**.
- [#1153](https://redmine.czechidm.com/issues/1153) - Add event support for ``IdmPasswordDto`` (create, update, delete)

## [8.1.2]

- [#1137](https://redmine.czechidm.com/issues/1137) - Sync of slices does not use contract EAV definition

## [8.1.1]

- [#1128](https://redmine.czechidm.com/issues/1128) - Event processing - check parent event type for duplicates.
- [#1129](https://redmine.czechidm.com/issues/1129) - Boolean eav attribute with default value.
- [#1135](https://redmine.czechidm.com/issues/1135) - Bulk actions evaluates permission on frontend in select box. Problem with filtering and selecting identities was fixed (permission are now evaluated correctly). AbstractBulkAction is now part of api, AbstractIdentityBulkAction was removed from impl package and functionality was moved to api. For all bulk action is now used Enabled annotation for check if action is enabled/disabled.


## [[8.1.0](https://github.com/bcvsolutions/CzechIdMng/releases/tag/8.1.0)]

- Bulk action for identities - More about bulk backend actions you can find [here](https://wiki.czechidm.com/devel/documentation/bulk_actions).
- Time slices of contracts - More about time slices you can find [here](https://wiki.czechidm.com/devel/documentation/identities#time_slices_of_the_contractual_relationship).
- Password history - More about password history you can find [here](https://wiki.czechidm.com/8.0/documentation/security/dev/password-policies#standard_policy_for_validation).
- [#1076](https://redmine.czechidm.com/issues/1076) -  Extended form value - added support for ``org.joda.time.LocalDate`` and string in ISO 8601 format as value for DATE or DATETIME persistent type. Persistent type INT, LONG, BOOLEAN - supports setting string value now (default conversions from string representation added ~ the same meaning as``#valueOf(String)``).
- [#853](https://redmine.czechidm.com/issues/853) - Extended form values without filled values (``null``) are not saved.

## [8.0.0]

- [#938](https://redmine.czechidm.com/issues/938) - Requests for automatic assign roles (by tree node or attributes) was implemented.
 - Creating, updating, deleting of the any automatic role **must use** this requests. Requests are approval by "same" workflows as request for roles uses.
 - Create, update, delete methods from `IdmAutomaticRoleAttributeController` was **removed**. Method `/delete-via-request/` was **added** to this controller. This method creates request for delete given automatic role.
- Detail of user - frontend validation that required at least 3 characters for the user name was removed.
- [#468](https://redmine.czechidm.com/issues/468) - Added asynchronous event processing. Added asynchronous event type ``NOTIFY`` on identities, identity roles, roles, contracts, contract guarantees, role catalogues, tree nodes - notify about entity was changed. Processors could listen and process this new event asynchronously. Read more in [documentation](https://redmine.czechidm.com/issues/468). Account management and process automatic roles was switched to asynchronous processing by default (instead ``DELETE`` events - removing identity, their role or contact will be still synchronous). Check please your custom processors for automatic roles and account management - make sure they will work with new ``NOTIFY`` event. Make sure source file **``czechidm-app/src/Index.js``** on frontend is updated - flash message with localized processors was added.
- [#979](https://redmine.czechidm.com/issues/979) - Attribute modified from ``AbstractEntity`` isn't audited anymore. Modified date can be found in ``IdmAudit`` as revision ``timestamp``.
- **``Marked``** library was removed from frontend. Use standard localizations for writing inline help and documentation.

## [7.8.5]

- [#1037](https://redmine.czechidm.com/issues/1037) - Fix pageable in automatic role recalculation.

## [7.8.3]
- [#1005](https://redmine.czechidm.com/issues/1005) - Dry run is available for long running task which supports it.
- [#1000](https://redmine.czechidm.com/issues/1000) - Persistent type ``SHORTTEXT`` was added to eav forms. Change script for all CzechIdM tables for persist eav values is provided  - provide the same change script in your module, if extended attributes was added to custom entity. ``SHORTTEXT`` and ``UUID`` values are indexed now.
- [#980](https://redmine.czechidm.com/issues/980) - Improved performance with recalculation automatic roles and skip recalculation during synchronization.

## [7.8.0]

- [#857](https://redmine.czechidm.com/issues/857) - Filters - added support for multiple filter parameters, `MultiValueMap` is used.
- [#274](https://redmine.czechidm.com/issues/274) - Filters for entity event processors was added.


## [7.7.0]

### Added

  - [#920](https://redmine.czechidm.com/issues/920) - Event processor - support conditional processing. New method ``EntityEventProcessor#conditional``.
  - [#922](https://redmine.czechidm.com/issues/922) - ``IdmIdentityService#findValidByRole`` method was added - return valid identities with assigned valid role (valid contract and identity role relation).
  - [#842](https://redmine.czechidm.com/issues/842) - Add new automatic roles by attribute and refactor current automatic role by tree structure. Backward compatibility of current automatic role and assigned roles is guaranteed.
  - Workflow task instances has permissions now. You should add ``WORKFLOWTASK_READ`` permission for default user role.

### Changed

  - All workflow services and controllers now using DTO layer, for more information read [#529](https://redmine.czechidm.com/issues/529).

## [7.6.1]

### Added

- [#870](https://redmine.czechidm.com/issues/870) - Cannot save 0x00 to Postgres column - Provisioning operation.
- [#621](https://redmine.czechidm.com/issues/621) - add dry run mode and long running task detail with processed items.
- [#784](https://redmine.czechidm.com/issues/784) - notification template info card improvements.


## [7.6.0]

### Removed

##### Role request
- Method ``startRequestNewTransactional(UUID requestId, boolean checkRight)`` was removed from interface IdmRoleRequestService (now is using only in implementation).

##### Evaluator 'IdentityAccountByIdentityEvaluator'
- Evaluator 'IdentityAccountByIdentityEvaluator' was removed and replaced by 'IdentityAccountByAccountEvaluator'.

##### AccAccountService
- Deprecated method 'delete(AccAccountDto account, boolean deleteTargetAccount, UUID entityId)' was removed. AccAccountService supported events now, parameters 'deleteTargetAccount' and 'entityId' were transformed to properties of delete event.

##### ProvisioningOperation
- In the task #798, we solved problem with not updated UID value in Provisioning operation. This occurred when was two and more provisioning operations in the queue and when first provisioning (traged system) changed UID of the account. Next provisionings in the queue was wrong (old) value of UID. This was fixed (provisioning operation has relation on the system entity now).
- Beware - before update we recommand solve/cancel all active provisioning operations. We need to create new column "system_entity_id" and remove old "system_entity_uid". In the change script we will try transform data in the provisioning operation table (from string uid to the ID), but it may not be fully sucessfuly.

### Changed

- Method ``saveAndFlush`` was added into ``BaseEntityRepository`` and this method is used now for saving all dtos - see ``AbstractReadWriteDtoService#saveInternal``. Auditable dto's metadata (e.g. ``modifier``, ``modified``) are available now after dto is saved by ``IdentitySevaProcessor``, for more information read [#834](https://redmine.czechidm.com/issues/834).

##### Sync
- Since version **7.6 (in identity synchronization)**, the default contractual relationship (when creating a new identity) is not created, for more information read [#867](https://redmine.czechidm.com/issues/867)!

##### Identity
- Identity's last name attribute is optional, change script was provided. Make sure you check identity's last name for ``null`` values, in your project.

##### Script

- Methods ``IdmScriptService#getScriptByName``, ``IdmScriptService#getScriptByCode`` are deprecated and will be removed - use ``IdmScriptService#getByCode`` method instead.

##### Long running task
- Abstract tasks in package `eu.bcvsolutions.idm.core.scheduler.service.impl` are deprecated and will be removed, use new tasks in api:
  - `eu.bcvsolutions.idm.core.scheduler.api.service.AbstractLongRunningTaskExecutor`
  - `eu.bcvsolutions.idm.core.scheduler.api.service.AbstractSchedulableStatefulExecutor`
  - `eu.bcvsolutions.idm.core.scheduler.api.service.AbstractSchedulableTaskExecutor`
  - `eu.bcvsolutions.idm.core.scheduler.api.service.StatelessAsynchronousTask`

### Added

- [#780](https://redmine.czechidm.com/issues/780) - Path with resources - support multiple locations for scripts, notification templates and workflow definitions.

##### Identity

- [#815](https://redmine.czechidm.com/issues/815) - Identity state was added. When identity starts to be valid, then new password is generated for all their accounts - one password is set on all identity's accounts and in CzechIdM. Identity can be disabled (and enabled) manually through rest endpoint. Other states are controlled by system and by identity contract states.

##### Report module
- Reports are available now, read more in documentation.

##### Attachment manager
- Saving binary files to filesystem is supported now, read more in documentation.

##### Common forms
- `CommonFormService` is used for saving report filter and configuration for long running taskc
- `IdmFormDefinitionService#updateDefinition` is used for updating form definitions for common forms automatically (see compatible vs incompatible changes).


## [7.5.2]

#### Acc module

- [#797](https://redmine.czechidm.com/issues/797) - target system security was hidden by provisioning break (fixed). ``SysSystemRepository#find`` method is deprecated and will be removed - use ``SysSystemService#find`` method instead.

## [7.5.1]

##### Contractual relation

- Long running tasks (LRT) for HR processes were restored - they are useful for pre-production checks (disable processors + show what LRT wants to process).

## [7.5.0]

### Added

#### Core module

##### Scheduler

- Added ``DependentTaskTrigger`` - execute task, when other task successfully ended. Dependent tasks are executed by ``LongRunningTaskExecuteDependentProcessor``.

#### Acc module

##### System
- Add three new attributes for block system provisioning operation (createOperation, updateOperation, deleteOperation). All these attributes has default value false.


##### Provisioning

- After add, remove or update ``IdmContractGuaranteeDto`` is execute provisioning for identity that own this contract.
- It's possible to send additional attributes, when password is changed (e.g. password expiration in extended attribute). New flag ``sendOnPasswordChange`` was added to system attribute mapping - attribute with this flag checked will be send together with changed password to provisioning. Two ways for provisioning additional attributes are implemented:
  - send additional attributes together with new password in one provisioning operation
  - send additional attributes after password is changed in another provisioning operation
    - two ways are be configurable by application configuration ``idm.sec.acc.provisioning.sendPasswordAttributesTogether`` (is effective for all target systems):
    - ``true``: additional password attributes will be send in one provisioning operation together with password
    - ``false``: additional password attributes will be send in new provisioning operation, after password change operation
- Add provisioning break for basic operations (create, update, delete), provisioning break can be set per system or globally for all systems.

### Changed

##### Identity

- Warning notifications about expired password and before password expiration are not send to disabled identities.
- Base permission ``CHANGEPERMISSION`` was added for identities. This permission is used for evaluating rights to identity's role requests - changing identity permissions. Add this new permission to your configured authorization policies (e.g for SelfIdentityEvaluator, SubordinatesEvaluator).

##### Contractual relation

- Service ``IdmContractGuaranteeService`` now implements EventableDtoService for method save and delete.
- Attribute ``state`` was added. Attribute disabled was removed from ``IdmIdentityContractDto`` as redundant to new state. Previously disabled contracts is enabled with state ``ECXLUDED`` - change script is provided. Contract with state ``DISABLED`` is invalid now - all processed work with this state (automatic roles, end of contract process).
- Contract processors were moved into ``eu.bcvsolutions.idm.core.model.event.processor.contract`` package:
  - ``IdentityContractCreateByAutomaticRoleProcessor``
  - ``IdentityContractDeleteProcessor``
  - ``IdentityContractSaveProcessor``
  - ``IdentityContractUpdateByAutomaticRoleProcessor``
- Business logic from HR long running tasks (LRT) for HR processes was moved into processors. Enable / disable identity with no valid or excluded contracts is executed immediately after contract is changed. HR processors are using almost the same workflow as LRT (LRT variables were removed only), but its configurable now. Long running task were removed and will be unscheduled automatically after new version will be installed:
  - ``AbstractWorkflowEventProcessor``
  - ``HrContractExclusionProcess``
  - ``HrEnableContractProcess``
  - ``HrEndContractProcess``

### Removed

#### Core module

##### Identity

- Password reset functionality was removed from core module. Password reset will be standalone module.

##### Contractual relation

- ``IdmIdentityContractRepository#deleteByIdentity`` was removed. It was skipping audit (bug).

## [7.4.0]

### Requirements

#### All workflow instances (task) have to be closed (resolved / canceled) before update. Some services were refactored and new workflow definitions (core) will be installed automatically.

### Added

#### Core module

##### Long running task

- New long running task ExecuteScriptTaskExecutor, this task can execute any of scripts in DEFAULT category.
- new long running task RemoveOldLogsTaskExecutor for remove logging event, logging event exception and logging event property.
- ``@DisallowConcurrentExecution`` can be used for prevent to execute task concurrently.

### Changed

- CGLIB for creating proxies are enforced by default. Now is possible to use annotations on methods, which is not defined in service interface. Prevent to use some logic in service constructors and always define annotations in implementation class, [read more](https://www.credera.com/blog/technology-insights/open-source-technology-insights/aspect-oriented-programming-in-spring-boot-part-2-spring-jdk-proxies-vs-cglib-vs-aspectj/). Make sure you have ``spring.aop.proxy-target-class=true`` property configured in your project.

#### Core module
- Application property **idm.pub.core.notification.template.folder** was changed to **idm.sec.core.notification.template.folder**.
- Application property **idm.pub.core.notification.template.fileSuffix** was changed to **idm.sec.core.notification.template.fileSuffix**.
- Application property **idm.pub.core.script.folder** was changed to **idm.sec.core.script.folder**.
- Application property **idm.pub.core.script.fileSuffix** was changed to **idm.sec.core.script.fileSuffix**.
- **All service interface were moved into core api module**. Check service imports in your project:
  - ``IdmAuthorizationPolicyService``
  - ``IdmConceptRoleRequestService``
  - ``IdmContractGuaranteeService``
  - ``IdmConfigurationService``
  - ``IdmIdentityContractService``
  - ``IdmIdentityRoleValidRequestService``
  - ``IdmIdentityRoleService``
  - ``IdmIdentityService``
  - ``IdmPasswordPolicyService``
  - ``IdmPasswordService``
  - ``IdmRoleCatalogueRoleService``
  - ``IdmRoleCatalogueService``
  - ``IdmRoleGuaranteeService``
  - ``IdmRoleRequestService``
  - ``IdmRoleService``
  - ``IdmRoleTreeNodeService``
  - ``IdmScriptAuthorityService``
  - ``IdmScriptService``
  - ``IdmTreeNodeService``
  - ``IdmTreeTypeService``
  - ``ConsoleNotificationSender``
  - ``Emailer``
  - ``EmailNotificationSender``
  - ``IdmConsoleLogService``
  - ``IdmEmailLogService``
  - ``IdmNotificationConfigurationService``
  - ``IdmNotificationLogService``
  - ``IdmNotificationRecipientService``
  - ``IdmNotificationTemplateService``
  - ``IdmSmsLogService``
  - ``IdmWebsocketLogService``
  - ``NotificationManager``
  - ``NotificationSender``
  - ``WebsocketNotificationSender``
  - ``IdmLongRunningTaskService``
  - ``IdmProcessedTaskItemService``
  - ``IdmScheduledTaskService``
  - ``IdmAuditService``
  - ``IdmLoggingEventExceptionService``
  - ``IdmLoggingEventPropertyService``
  - ``IdmLoggingEventService``
  - ``GrantedAuthoritiesFactory``
  - ``JwtAuthenticationService``
  - ``LoginService``
  - ``AuthenticationFilter``

- **Idm** prefix was added to all core service filters and all filters were moved to copre api module. Check filters usage in you project:
  - ``RoleFilter`` was renamed to ``IdmRoleFilter`` and moved to core api module
  - ``TreeNodeFilter`` was renamed to ``IdmTreeNodeFilter`` and moved to core api module
  - ``TreeNodeFilter`` was renamed to ``IdmTreeNodeFilter`` and moved to core api module
  - ``FormDefinitionFilter`` was renamed to ``IdmFormDefinitionFilter`` and moved to core api module
  - ``FormAttributeFilter`` was renamed to ``IdmFormAttributeFilter`` and moved to core api module
  - ``FormValueFilter`` was renamed to ``IdmFormValueFilter`` and moved to core api module
  - ``AuditFilter`` was renamed to ``IdmAuditFilter`` and moved to core api module
  - ``AuthorizationPolicyFilter`` was renamed to ``IdmAuthorizationPolicyFilter``
  - ``ConceptRoleRequestFilter`` was renamed to ``IdmConceptRoleRequestFilter``
  - ``ContractGuaranteeFilter`` was renamed to ``IdmContractGuaranteeFilter``
  - ``IdentityContractFilter`` was renamed to ``IdmIdentityContractFilter``
  - ``IdentityFilter`` was renamed to ``IdmIdentityFilter``
  - ``IdentityRoleFilter`` was renamed to ``IdmIdentityRoleFilter``
  - ``LoggingEventExceptionFilter`` was renamed to ``IdmLoggingEventExceptionFilter`` and moved to core api module
  - ``LoggingEventFilter`` was renamed to ``IdmLoggingEventFilter`` and moved to core api module
  - ``LoggingEventPropertyFilter`` was renamed to ``IdmLoggingEventPropertyFilter`` and moved to core api module
  - ``PasswordFilter`` was renamed to ``IdmPasswordFilter``
  - ``PasswordPolicyFilter`` was renamed to ``IdmPasswordPolicyFilter``
  - ``RoleCatalogueFilter`` was renamed to ``IdmRoleCatalogueFilter``
  - ``RoleCatalogueRoleFilter`` was renamed to ``IdmRoleCatalogueRoleFilter``
  - ``RoleGuaranteeFilter`` was renamed to ``IdmRoleGuaranteeFilter``
  - ``RoleRequestFilter`` was renamed to ``IdmRoleRequestFilter``
  - ``RoleTreeNodeFilter`` was renamed to ``IdmRoleTreeNodeFilter``
  - ``ScriptAuthorityFilter`` was renamed to ``IdmScriptAuthorityFilter``
  - ``ScriptFilter`` was renamed to ``IdmScriptFilter``
  - ``LongRunningTaskFilter`` was renamed to ``IdmLongRunningTaskFilter``
  - ``IdmProcessedTaskItemFilter` was moved to core api module.
  - ``NotificationFilter`` was renamed to ``IdmNotificationFilter`` and moved to core api module
  - ``NotificationRecipientFilter`` was renamed to ``IdmNotificationRecipientFilter`` and moved to core api module
  - ``NotificationTemplateFilter`` was renamed to ``IdmNotificationTemplateFilter`` and moved to core api module
  - ```` was renamed to ````

- ``EnabledAcpect`` class was renamed to ``EnabledAspect``
- FE: improved external frentend configuration. All config properties can be changed without application build. Check external configuration in your project and move ``serverUrl`` property into config object.


##### Identity

- Deprecated method ``IdmIdentityService#saveIdentity`` was removed. Use ``save``or ``publish`` method instead.
- Method ``IdmIdentityService#passwordChange`` don't modify given password change dto - results was added.  

##### Event processiong

- Added ``AbstractEventableDtoService`` for simplify services working with events. ``CoreEvent`` with ``CoreEventType`` is published by default now for save and delete event for all domain types (e.g. ``IdmIdentityDto``, ``IdmIdentityContractDto``, ``IdmRoleCatalogueDto``, ``IdmIdentityRoleDto`` ...). **Check [appropriate event type comparison](https://wiki.czechidm.com/devel/dev/architecture/events#event_types)** in your project.

##### Password policy

- Entity IdmPasswordPolicy was transformed to DTO. Update all places where call from your project service IdmPasswordPolicyService. The service now uses DTO only. Also check your current scripts and their authorities. If you're overloaded any of frontend component which is related IdmPasswordPolicy, check her functionality.
- The attributes of IdmPasswordPolicy are now Integer (in past int). Please Check your password policy settings and remove redundant zeros in settings. The zero in the setting is already taken as a valid setting value.

##### Script

- Script with category DEFAULT can be executed from executor with another category.

##### Confidential storage

- The encryption of the confidential agenda can now be done using the key from application properties, also file with key may be defined in application properties. Backward compatibility with key defined in resource **isn't maintained**. If your project using key directly saved in **.war** on path  **<war>/lib/idm-core-impl-<version>.jar/eu/bcvsolutions/idm/confidential/key.key**, move your key outside the **.war** and configure application property ``cipher.crypt.secret.keyPath``.

##### Notifications

- Api ``NotificationSender`` was changed. Now are returned all notifications that is sent. Also mechanism of sending with topic was fixed. Check your project notification configuration.


##### Role

- Role agenda was refactored to dto usage. Search **``IdmRoleService``** usage in your project - **all methods works with dto**, entity service methods were removed (e.q. ``findSecured``). **In workflow ``roleSevice.get(roleId, null)`` method has to be used**.
- **``GrantedAuthoritiesFactory#getActiveRoleAuthorities(UUID identityId, IdmRoleDto role)``** - method using role dto as parameter.
- **``IdmRoleDto`` is used as event content - change all entity event processors from template ``IdmRole`` to ``IdmRoleDto`` in your project.** ``RoleEvent`` uses ``IdmRoleDto`` content too.
- Rest endpoint - changed role lists structure (``IdmRoleGuaranteeDto, IdmRoleCatalogueDto`` are used) - uuid only is needed, when relation is created and updated.
- ``IdmRoleGuaranteeRepository`` - removed methods ``deleteByGuarantee_Id``, ``deleteByRole`` - service layer has to be used for create audit records.
- **``RoleFilter``** was **moved to core api** module and renamed to ``IdmRoleFilter`` - we will add ``Idm`` prefix to all core filters.
- **``IdmRoleFilter``** - fields are ``UUID`` now - **roleCatalogue => roleCatalogueId**, **guarantee => guaranteeId**

##### Role request

- Added authorization policies support. New [authorization policy evaluators](https://wiki.czechidm.com/devel/dev/security/change-user-permissions#security) has to be [configured](https://wiki.czechidm.com/devel/dev/security/authorization#default_settings_of_permissions_for_an_identity_profile) to add permission for role requests. Added new permission group ``ROLEREQUEST``.
- Don't use **``IdmRoleRequestRepository#find()``** and **``IdmConceptRoleRequestRepository#find()``** methods directly => use service layer (methods are using criteria api now).
- Role request navigation item was moved to audit and enabled for all logged users - can read their own role requests history.

##### Tree structure

- Tree type and node agendas were refactored to dto usage. Search **``IdmTreeTypeService``** usage in your project - **all methods works with dto**. Search **``IdmTreeNodeService``** usage in your project - **all methods works with dto**.
- **``IdmTreeTypeService``** supports events (``CREATE``, ``UPDATE``, ``DELETE``).
- **``IdmTreeTypeService#getConfigurations(UUID)`` uses uuid as parameter**.
- **``IdmTreeTypeRepository#clearDefaultTreeType()``** was removed - configuration service is used for persist default tree type and node.
- **``IdmTreeTypeRepository#clearDefaultTreeNode()``** was removed - configuration service is used for persist default tree type and node.
- **``IdmTreeTypeRepository#findOneByDefaultTreeTypeIsTrue()``** was removed - configuration service is used for persist default tree type and node.
- **``IdmTreeTypeService#clearDefaultTreeNode()``** was removed - configuration service is used for persist default tree type and node.
- ``TreeConfiguration`` was added - provide default tree node and type. This configuration is used in ``DefaultIdmIdentityContractService``, ``DeafultIdmTreeNodeService`` constructors.
- **``IdmTreeTypeFilter``** is used as filter in ``IdmTreeTypeService`` - its possible to find types by code from rest api.
- **``TreeNodeFilter``** was renamed to **``IdmTreeNodeFilter``**. Its possible to find node by type and code from rest api.
- Don't use **``IdmTreeTypeRepository#find()``** method directly => use service layer (methods are using criteria api now).
- **``IdmTreeNodeService#rebuildIndexes(UUID)``** uses uuid as parameter**.
- **Tree processors were moved to package ``eu.bcvsolutions.idm.core.model.event.processor.tree``**.
- **``IdmTreeNodeDto`` is used as event content - change all entity event processors from template ``IdmTreeNode`` to ``IdmTreeNodeDto`` in your project.** ``TreeNodeEvent`` uses ``IdmTreeNodeDto`` content too.
- **``IdmTreeTypeDto`` is used as event content - change all entity event processors from template ``IdmTreeType`` to ``IdmTreeTypeDto`` in your project.** ``TreeTypeEvent`` uses ``IdmTreeTypeDto`` content too.
- **Added authorization policies support.** Authorization policies has to be [configured](https://wiki.czechidm.com/devel/dev/security/authorization#default_settings_of_permissions_for_an_identity_profile) to add permission for tree types and nodes. ``BasePermissionEvaluator`` can be used.

##### Dynamic forms (eav)

- Form definitions was refactored to dto usage. Search **``IdmFormDefinitionService``** usage in your project - **all methods works with dto** and interface was moved to core api module.
- Form attributes was refactored to dto usage. Search **``IdmFormAttributeService``** usage in your project - **all methods works with dto** and interface was moved to core api module.
- Don't use **``IdmFormDefinitionRepository#find()``**, **``IdmFormAttributeRepository#find()``** and **``AbstractFormValueRepository#find()``** methods directly => use service layer (methods are using criteria api now).
- **``FormAttributeFilter``** -  **formDefinition**, **formDefinitionId** fields were removed. Use **definitionId** field. **``FormAttributeFilter``** was renamed to **``IdmFormAttributeFilter``** and moved into core api package
- **``FormValueFilter``** - - fields are ``UUID`` now - **formDefinition => definitionId**, **formAttribute => attributeId**.  **``FormValueFilter``** was renamed to **``IdmFormValueFilter``** and moved into core api package
- **``IdmFormDefinitonFilter``** was added - form definitions can be found by ``type``, ``code``, ``name`` and ``main``.
- Form values was refactored to dto usage. **``IdmFormValueDto``** is used for all form values => its not needed to know owner type. Search **``FormValueService``** usage in your project - **all methods works with dto**. Owner can be entity and dto now.
- **``IdmFormInstanceDto``** was added and its used as **value holder for form service api** - contains form definition + form values by owner. Rest endpoints for save and get form values use **``IdmFormInstanceDto``** insted raw values
- **``FormService``** was refactored to dto usage and moved to core api module. Search **``FormService``** usage in your project - **all methods works with dto** (definitions, attributes, instances). Helper methods ``toValueMap``, ``toPersistentValueMap``, ``toPersistentValues``, ``toSinglePersistentValue`` was moved to ``IdmFormDefinitionDto`` class.
- **``AbstractFormableService``** as refactored to dto usage. Generalize ``AbstractEventableDtoService`` now and automatically clear form values, when owner (dto) is deleted. Service interface was moved to core api module.
- **``FormValueService``** uses ``IdmFormDefinitionDto`` and ``IdmFormAttributeDto`` as parameters now.  Search **``FormValueService``** usage in your project. **``FormValueService``** was moved to core api module.
- **``AbstractFormValueRepository``** finds owner by attribute methods using attribute uuid as parameter. Use new methods for find values by owner id - methods for find values by owner are deprecated - needs persisted owner and this could be confusing.
- **``IdmFormDefinitionController#getOwnerTypes()``** rest endpoint method returns simple string owner types without resource wrapper.
- **Added authorization policies support.** Authorization policies has to be configured to add permission for form definition and attribute administration. ``BasePermissionEvaluator`` can be used. Permission groups were renamed to ``FORMDEFINITION`` and ``FORMATTRIBUTE``.
- **Confidential values are stored under form attribute identifier now.** Form attribute's code can be changed and stored confidential values were identified by code - can be lost after code is changed. Change script ``V1_00_028__eav-change-confidential-storage-key.sql`` is provided - provide the same change script in your module, if extended attributes was added to custom entity.
- Form attribute has new field ``faceType`` with contains information about attribute rendering on FE. Persistent types ``TEXTAREA``, ``RICHTEXTAREA``, ``CURRENCY`` were removed and appropriate face types were added. ``Change script V1_00_029__eav-add-face-type.sql`` is provided - provide the same change script in your module, if extended attributes was added to custom entity. Face type can be added in custom module for change (add) attribute rendered - its used mainly for select boxes.
- Persistent type ``UUID`` was added - entity identifiers can be saved into form value.

##### Audit

- ``IdmLoggingEventDto`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto`` package.
- ``IdmLoggingEventExceptionDto`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto`` package.
- ``IdmLoggingEventPropertyDto`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto`` package.
- ``IdmAuditDto`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto`` package.
- ``IdmAuditDiffDto`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto`` package.
- ``IdmAuditFilter`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto.filter`` package.
- ``IdmLoggingEventExceptionFilter`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto.filter`` package.
- ``IdmLoggingEventFilter`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto.filter`` package.
- ``IdmLoggingEventPropertyFilter`` was moved to ``eu.bcvsolutions.idm.core.api.audit.dto.filter`` package.

##### TestHelper

- **``TestHelper``** interface and **``DefaultTestHelper``** implementation was moved to core test api module. TestHelper is reused in acc module now.
- Role entity usage was removed - dto or role id (uuid) is used now.
- Tree type and node entities usage was removed - dto or id (uuid) is used now.

#### Acc module

- **Provisioning and synchronization was refactored to dto usage.** Check all ``ProvisioningEntityExecutor``, ``SynchronizationEntityExecutor`` usage in your project.

- **Acc** prefix was added to all acc service filters. Check filters usage in you project:
- ``AccountFilter`` was renamed to ``AccAccountFilter``
- ``IdentityAccountFilter`` was renamed to ``AccIdentityAccountFilter``
- ``ProvisioningOperationFilter`` was renamed to ``SysProvisioningOperationFilter``
- ``ProvisioningRequestFilter`` was renamed to ``SysProvisioningRequestFilter``
- ``RoleAccountFilter`` was renamed to ``AccRoleAccountFilter``
- ``RoleCatalogueAccountFilter`` was renamed to ``AccRoleCatalogueAccountFilter``
- ``RoleSystemAttributeFilter`` was renamed to ``SysRoleSystemAttributeFilter``
- ``RoleSystemFilter`` was renamed to ``SysRoleSystemFilter``
- ``SchemaAttributeFilter`` was renamed to ``SysSchemaAttributeFilter``
- ``SchemaObjectClassFilter`` was renamed to ``SysSchemaObjectClassFilter``
- ``SyncActionLogFilter`` was renamed to ``SysSyncActionLogFilter``
- ``SynchronizationConfigFilter`` was renamed to ``SysSyncConfigFilter``
- ``SynchronizationLogFilter`` was renamed to ``SysSyncLogFilter``
- ``SyncItemLogFilter`` was renamed to ``SysSyncItemLogFilter``
- ``SystemAttributeMappingFilter`` was renamed to ``SysSystemAttributeMappingFilter``
- ``SystemEntityFilter`` was renamed to ``SysSystemEntityFilter``
- ``SystemMappingFilter`` was renamed to ``SysSystemMappingFilter``
- ``TreeAccountFilter`` was renamed to ``AccTreeAccountFilter``

##### System service
- Method **readObject** was replaced with **readConnectorObject**
  - **Old**: IcConnectorObject readObject(SysSystem system, SysSystemMappingDto systemMapping, IcUidAttribute uidAttribute);
  - **New**: IcConnectorObject readConnectorObject(UUID systemId, String uid, IcObjectClass objectClass);


##### Synchronization

- Entity **SysSyncLog was transformed to SysSyncLogDto**. Update all places where call from your project SysSyncLogService. The service now uses only DTO. Backward compatibility of all synchronization logs is guaranteed.
- Entity **SysSyncItemLog was transformed to SysSyncItemLogDto**. Update all places where call from your project SysSyncItemLogService. The service now uses only DTO. Backward compatibility of all synchronization item logs is guaranteed.
- Entity **SysSyncActionLog was transformed to SysSyncActionLogDto**. Update all places where call from your project SysSyncActionLogService. The service now uses only DTO. Backward compatibility of all synchronization action logs is guaranteed.
- Entity **SysSyncConfig was transformed to SysSyncConfigDto**. Update all places where call from your project SysSyncConfigService. The service now uses only DTO. Backward compatibility of synchronization config is guaranteed.

##### Attribute mapping
- Entity **SysRoleSystemAttribute was transformed to SysRoleSystemAttributeDto**. Update all places where call from your project [SysRoleSystemAttributeService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysRoleSystemAttributeService.java). The service now uses only DTO.
- Entity **SysSchemaAttribute was transformed to SysSchemaAttributeDto**. Update all places where call from your project [SysSchemaAttributeService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysSchemaAttributeService.java). The service now uses only DTO.
- Entity **SysSchemaObjectClass was transformed to SysSchemaObjectClassDto**. Update all places where call from your project [SysSchemaObjectClassService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysSchemaObjectClassService.java). The service now uses only DTO.
- Entity **SysSystemAttributeMapping was transformed to SysSystemAttributeMappingDto**. Update all places where call from your project [SysSystemAttributeMappingService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysSystemAttributeMappingService.java). The service now uses only DTO.
- Entity **SysSystemMapping was transformed to SysSystemMappingDto**. Update all places where call from your project [SysSystemMappingService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysSystemMappingService.java). The service now uses only DTO.
- Into method getAuthenticationAttribute in [SysSystemAttributeMappingService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysSystemAttributeMappingService.java) was added new parameter SystemEntityType, please check usage this method.

##### Role system
- Entity **SysRoleSystem was transformed to SysRoleSystemDto**. Update all places where call from your project [SysRoleSystemService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysRoleSystemService.java). The service now uses only DTO.


##### System
- Entity **SysSystem was transformed to SysSystemDto**. Update all places where call from your project [SysSystemService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysSystemService.java). The service now uses only DTO.

##### Account management

- Added authorization policies support for identity accounts. Added new permission group ``IDENTITYACCOUNT`` with new evaluator [IdentityAccountByIdentityEvaluator](https://wiki.czechidm.com/devel/dev/security/authorization#identityaccountbyidentityevaluator).
- Don't use **AccIdentityAccountRepository#find()** method directly => use service layer (methods are using criteria api now).
- Added authorization policies support for role accounts. Added new tab to role detail with assigned role accounts. Added new permission group ``ROLEACCOUNT``. with new evaluator [RoleAccountByRoleEvaluator](https://wiki.czechidm.com/devel/dev/security/authorization#roleaccountbyroleevaluator).
- Don't use **AccRoleAccountRepository#find()** method directly => use service layer (methods are using criteria api now).
- Added authorization policies support for tree accounts. Added new tab to tree node detail with assigned tree accounts. Added new permission group ``TREEACCOUNT``.
- Don't use **AccTreeAccountRepository#find()** method directly => use service layer (methods are using criteria api now).
- ``AccAccountManagementService`` uses dto - check service usage in your project.

##### Provisioning

- Entity **SysProvisioningBatch was transformed to SysProvisioningBatchDto**. Update all places where call from your project [SysProvisioningBatchService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysProvisioningBatchService.java). The service now uses only DTO.
- Entity **SysProvisioningArchive was transformed to SysProvisioningArchiveDto**. Update all places where call from your project [SysProvisioningArchiveService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysProvisioningArchiveService.java). The service now uses only DTO.
- Entity **SysProvisioningOperation was transformed to SysProvisioningOperationDto**. Update all places where call from your project [SysProvisioningOperationService](https://github.com/bcvsolutions/CzechIdMng/blob/develop/Realization/backend/acc/src/main/java/eu/bcvsolutions/idm/acc/service/api/SysProvisioningOperationService.java). The service now uses only DTO.
- Entity **SysProvisioningRequest was removed** - use ``SysProvisioningOperation`` and ``SysProvisioningOperationDto``.
- Method **SysProvisioningBatch findBatch(SysProvisioningOperation operation)** from service ``SysProvisioningBatchService`` was moved into service ``SysProvisioningOperationService``, update all places where you call method **findBatch** to new service.
- Password provisioning was improved - operation results are returned now, without input password dto is changed.
- Provisioning operation table is sorted by created date descended.

### Removed

#### Core module

- Removed support for entity services - use dto services in your project. Removed classes:
  - ``DefaultReadWriteEntityController`` - use ``DefaultReadWriteDtoController``
  - ``AbstractReadWriteEntityController`` - use ``DefaultReadWriteDtoController``
  - ``AbstractReadEntityController`` - use ``AbstractReadWriteDtoController``
  - ``AbstractReadWriteEntityService`` - use ``AbstractReadWriteDtoService``
  - ``AbstractReadEntityService`` - use ``AbstractReadDtoService``
  - ``ReadWriteEntityService`` - use ``ReadWriteDtoService``
  - ``ReadEntityService`` - use ``ReadDtoService``
  - ``AuthorizableEntityService`` was removed - ``AuthorizableService`` is used for adding authorization policies support for dto services.
- Use ``@RestController`` for your controllers. ``@RepositoryRestController`` support from Spring Data project was removed.
- ``BaseFilter`` template and ``find(F filter, Pageable pageable)`` method was removed from ``BaseEntityRepository``. Use service layer with criteria api for find dtos.

##### Long running tasks
- From ``AbstractLongRunningTaskExecutor`` was removed deprecated method ``getParameterNames``, replace this method from your project with method ``getPropertyNames``.
- From ``AbstractScheduledTaskInitializer`` was removed method ``getLOG()``.
- From ``LongRunningTaskManager``   was removed internal method ``scheduleProcessCreated()``.
