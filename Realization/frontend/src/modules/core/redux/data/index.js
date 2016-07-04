'use strict';

import FormManager from './FormManager';
import IdentityManager from './IdentityManager';
import OrganizationManager from './OrganizationManager';
import RoleManager from './RoleManager';
import WorkflowTaskInstanceManager from './WorkflowTaskInstanceManager';
import IdentityRoleManager from './IdentityRoleManager';
import IdentityWorkingPositionManager from './IdentityWorkingPositionManager';
import WorkflowProcessInstanceManager from './WorkflowProcessInstanceManager';
import WorkflowHistoricProcessInstanceManager from './WorkflowHistoricProcessInstanceManager';

const ManagerRoot = {
  FormManager: FormManager,
  IdentityManager: IdentityManager,
  OrganizationManager: OrganizationManager,
  RoleManager: RoleManager,
  WorkflowTaskInstanceManager: WorkflowTaskInstanceManager,
  IdentityRoleManager: IdentityRoleManager,
  IdentityWorkingPositionManager: IdentityWorkingPositionManager,
  WorkflowProcessInstanceManager: WorkflowProcessInstanceManager,
  WorkflowHistoricProcessInstanceManager: WorkflowHistoricProcessInstanceManager
};

ManagerRoot.version = '0.0.1';
module.exports = ManagerRoot;
