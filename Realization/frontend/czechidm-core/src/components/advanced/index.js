import Table from './Table/Table';
import Column from './Table/Column';
import ColumnLink from './Table/ColumnLink';
import RefreshButton from './Table/RefreshButton';
import CloseButton from './Button/CloseButton';
import IdentityInfo from './IdentityInfo/IdentityInfo';
import Navigation from './Navigation/Navigation';
import TabPanel from './TabPanel/TabPanel';
import Filter from './Filter/Filter';
import DateValue from './DateValue/DateValue';
import Tree from './Tree/Tree';
import TreeNodeSelect from './TreeNodeSelect/TreeNodeSelect';
import DetailButton from './Table/DetailButton';
import ModalProgressBar from './ModalProgressBar/ModalProgressBar';
import EavForm from './Form/EavForm';
import EavContent from './Form/EavContent';
import AbstractFormAttributeRenderer from './Form/AbstractFormAttributeRenderer';
import SelectBoxFormAttributeRenderer from './Form/SelectBoxFormAttributeRenderer';
import BooleanFormAttributeRenderer from './Form/BooleanFormAttributeRenderer';
import PasswordField from './PasswordField/PasswordField';
import ProgressBar from './ProgressBar/ProgressBar';
import RichTextArea from './RichTextArea/RichTextArea';
import AbstractTableContent from './Content/AbstractTableContent';
import EntityInfo from './EntityInfo/EntityInfo';
import AbstractEntityInfo from './EntityInfo/AbstractEntityInfo';
import UuidInfo from './UuidInfo/UuidInfo';
import RoleInfo from './RoleInfo/RoleInfo';
import RoleCatalogueInfo from './RoleCatalogueInfo/RoleCatalogueInfo';
import RoleCatalogueSelect from './RoleCatalogueSelect/RoleCatalogueSelect';
import IdentityContractInfo from './IdentityContractInfo/IdentityContractInfo';
import IdentityRoleInfo from './IdentityRoleInfo/IdentityRoleInfo';
import WorkflowProcessInfo from './WorkflowProcessInfo/WorkflowProcessInfo';
import WorkflowTaskInfo from './WorkflowTaskInfo/WorkflowTaskInfo';
import NotificationTemplateInfo from './NotificationTemplateInfo/NotificationTemplateInfo';
import ScriptArea from './ScriptArea/ScriptArea';
import RoleSelect from './RoleSelect/RoleSelect';
import IdentitySelect from './IdentitySelect/IdentitySelect';
import Recaptcha from './Recaptcha/Recaptcha';
import IdentitiesInfo from './IdentitiesInfo/IdentitiesInfo';
import SchedulerTaskInfo from './SchedulerTaskInfo/SchedulerTaskInfo';
import EntitySelectBox from './EntitySelectBox/EntitySelectBox';
import Dropzone from './Dropzone/Dropzone';
import PasswordChangeComponent from './PasswordChangeComponent/PasswordChangeComponent';
import ValidationMessage from './ValidationMessage/ValidationMessage';
import DynamicTaskDetail from '../../content/task/DynamicTaskDetail';
import DecisionButtons from '../../content/task/DecisionButtons';
import OperationResult from './OperationResult/OperationResult';
import ImageDropzone from './ImageDropzone/ImageDropzone';
import TreeTypeInfo from './TreeTypeInfo/TreeTypeInfo';
import TreeNodeInfo from './TreeNodeInfo/TreeNodeInfo';
import ImageCropper from './ImageCropper/ImageCropper';
import LongRunningTask from './LongRunningTask/LongRunningTask';
import CreatableSelectBox from './CreatableSelectBox/CreatableSelectBox';
import CodeListSelect from './CodeListSelect/CodeListSelect';
import CodeListValue from './CodeListValue/CodeListValue';
import AbstractIcon from './Icon/AbstractIcon';
import Icons from './Icon/Icons';
import AbstractIdentityDashboardButton from './Button/AbstractIdentityDashboardButton';
import RoleRequestInfo from './RoleRequestInfo/RoleRequestInfo';
import PasswordInfo from './PasswordInfo/PasswordInfo';
import AuditableInfo from './EntityInfo/AuditableInfo';
import DetailHeader from './COntent/DetailHeader';

const Components = {
  Table,
  Column,
  ColumnLink,
  RefreshButton,
  CloseButton,
  IdentityInfo,
  Navigation,
  TabPanel,
  Filter,
  _ToogleButton: Filter.ToogleButton,
  _FilterButtons: Filter.FilterButtons,
  DateValue,
  ProgressBar,
  ModalProgressBar,
  Tree,
  TreeNodeSelect,
  DetailButton,
  EavForm,
  EavContent,
  AbstractFormAttributeRenderer,
  SelectBoxFormAttributeRenderer,
  BooleanFormAttributeRenderer,
  PasswordField,
  RichTextArea,
  AbstractTableContent,
  EntityInfo,
  AbstractEntityInfo,
  UuidInfo,
  RoleInfo,
  RoleCatalogueInfo,
  RoleCatalogueSelect,
  IdentityContractInfo,
  IdentityRoleInfo,
  WorkflowProcessInfo,
  NotificationTemplateInfo,
  ScriptArea,
  RoleSelect,
  IdentitySelect,
  Recaptcha,
  IdentitiesInfo,
  SchedulerTaskInfo,
  EntitySelectBox,
  Dropzone,
  PasswordChangeComponent,
  ValidationMessage,
  DecisionButtons,
  DynamicTaskDetail,
  OperationResult,
  ImageDropzone,
  WorkflowTaskInfo,
  TreeTypeInfo,
  TreeNodeInfo,
  ImageCropper,
  LongRunningTask,
  CreatableSelectBox,
  CodeListSelect,
  CodeListValue,
  AbstractIcon,
  Icons,
  AbstractIdentityDashboardButton,
  RoleRequestInfo,
  PasswordInfo,
  AuditableInfo,
  DetailHeader
};

Components.version = '9.4.0';
module.exports = Components;
