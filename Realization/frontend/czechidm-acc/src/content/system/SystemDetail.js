import React from 'react';
import PropTypes from 'prop-types';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
//
import { Basic, Managers, Utils, Enums } from 'czechidm-core';
import { SystemManager } from '../../redux';

const systemManager = new SystemManager();

/**
 * Target system detail content
 *
 * @author Radek Tomiška
 */
class SystemDetail extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.passwordPolicyManager = new Managers.PasswordPolicyManager();
    //
    let showConfigurationRemoteServer = false;
    if (props.entity) {
      showConfigurationRemoteServer = props.entity.remote;
    }

    this.state = {
      _showLoading: false,
      showConfigurationRemoteServer
    };
  }

  getContentKey() {
    return 'acc:content.system.detail';
  }

  componentDidMount() {
    const { entity } = this.props;
    this._initForm(entity);
  }

  // @Deprecated - since V10 ... replaced by dynamic key in Route
  // UNSAFE_componentWillReceiveProps(nextProps) {
  //   const { entity } = this.props;
  //   if (entity && nextProps.entity && entity.id !== nextProps.entity.id) {
  //     this._initForm(nextProps.entity);
  //   }
  // }

  _initForm(entity) {
    let data = {
      ...entity,
    };

    if (entity) {
      if (entity._embedded && entity._embedded.passwordPolicyGenerate) {
        data.passwordPolicyGenerate = entity._embedded.passwordPolicyGenerate;
      }
      if (entity._embedded && entity._embedded.passwordPolicyValidate) {
        data.passwordPolicyValidate = entity._embedded.passwordPolicyValidate;
      }
      if (entity.disabledProvisioning) {
        if (entity.disabled) {
          data.stateEnum = 'disabledProvisioning';
        } else {
          data.stateEnum = 'readonlyDisabledProvisioning';
        }
      } else if (entity.disabled) {
        data.stateEnum = 'disabled';
      } else if (entity.readonly) {
        data.stateEnum = 'readonly';
      } else {
        data.stateEnum = null;
      }

      // connector is part of entity, not embedded
      if (entity.connectorServer) {
        // set data for connector server
        data = {
          ...data,
          host: entity.connectorServer.host,
          port: entity.connectorServer.port,
          useSsl: entity.connectorServer.useSsl,
          password: entity.connectorServer.password,
          timeout: entity.connectorServer.timeout
        };
      }

      if (entity.blockedOperation) {
        data = {
          ...data,
          createOperation: entity.blockedOperation.createOperation,
          updateOperation: entity.blockedOperation.updateOperation,
          deleteOperation: entity.blockedOperation.deleteOperation
        };
      }
    }
    //
    this.refs.form.setData(data);
    this.refs.name.focus();
  }

  /**
   * In this method validate bouth forms - remote connector server and/or system detail
   * after validate save bouth details - system detail, remote connector
   * After complete previous steps, save system
   */
  save(afterAction, event) {
    if (event) {
      event.preventDefault();
    }
    const entity = this.refs.form.getData();

    if (!this.refs.form.isFormValid()) {
      return;
    }
    const { uiKey } = this.props;

    this.setState({
      _showLoading: true
    }, () => {
      const saveEntity = {
        ...entity,
        connectorServer: {
          host: entity.host,
          password: entity.password,
          port: entity.port,
          timeout: entity.timeout,
          useSsl: entity.useSsl
        },
        blockedOperation: {
          createOperation: entity.createOperation,
          updateOperation: entity.updateOperation,
          deleteOperation: entity.deleteOperation
        },
        readonly: entity.stateEnum === 'readonlyDisabledProvisioning' || entity.stateEnum === 'readonly',
        disabled: entity.stateEnum === 'disabledProvisioning' || entity.stateEnum === 'disabled',
        disabledProvisioning: entity.stateEnum === 'readonlyDisabledProvisioning' || entity.stateEnum === 'disabledProvisioning'
      };
      //
      if (Utils.Entity.isNew(saveEntity)) {
        this.context.store.dispatch(systemManager.createEntity(saveEntity, `${uiKey}-detail`, (createdEntity, newError) => {
          this._afterSave(createdEntity, newError, afterAction);
        }));
      } else {
        this.context.store.dispatch(systemManager.updateEntity(saveEntity, `${uiKey}-detail`, (patchedEntity, newError) => {
          this._afterSave(patchedEntity, newError, afterAction);
        }));
      }
    });
  }

  /**
   * Method show form for remote server configuration
   */
  _setRemoteServer(event) {
    this.setState({
      showConfigurationRemoteServer: event.currentTarget.checked
    });
  }

  _afterSave(entity, error, afterAction = 'CLOSE') {
    this.setState({
      _showLoading: false
    }, () => {
      if (error) {
        this.addError(error);
        return;
      }

      this.addMessage({ message: this.i18n('save.success', { name: entity.name }) });
      //
      if (afterAction === 'CLOSE') {
        // reload options with remote connectors

        this.context.history.replace(`/systems`);
      } else {
        this._initForm(entity);
        // set again confidential to password
        this.refs.form.getComponent('password').openConfidential(false);
        this.context.history.replace(`/system/${entity.id}/detail`);
      }
    });
  }

  render() {
    const { uiKey, entity } = this.props;
    const { _showLoading, showConfigurationRemoteServer } = this.state;
    //
    const blockedOperationLabels = [];
    if (entity && entity.blockedOperation) {
      if (entity.blockedOperation.createOperation) {
        blockedOperationLabels.push(this.i18n('acc:entity.BlockedOperation.createOperation.short'));
      }
      if (entity.blockedOperation.updateOperation) {
        blockedOperationLabels.push(this.i18n('acc:entity.BlockedOperation.updateOperation.short'));
      }
      if (entity.blockedOperation.deleteOperation) {
        blockedOperationLabels.push(this.i18n('acc:entity.BlockedOperation.deleteOperation.short'));
      }
    }

    return (
      <div>
        <Helmet title={Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('edit.title')} />
        <form onSubmit={this.save.bind(this, 'CONTINUE')}>
          <Basic.Panel className={Utils.Entity.isNew(entity) ? '' : 'no-border last'}>
            <Basic.PanelHeader text={Utils.Entity.isNew(entity) ? this.i18n('create.header') : this.i18n('basic')} />

            <Basic.PanelBody
              style={ Utils.Entity.isNew(entity) ? { paddingTop: 0, paddingBottom: 0 } : { padding: 0 } }
              showLoading={ _showLoading } >
              <Basic.AbstractForm
                ref="form"
                uiKey={ uiKey}
                readOnly={
                  Utils.Entity.isNew(entity)
                  ?
                  !Managers.SecurityManager.hasAuthority('SYSTEM_CREATE')
                  :
                  !Managers.SecurityManager.hasAuthority('SYSTEM_UPDATE') }>
                <Basic.Alert
                  level="warning"
                  icon="exclamation-sign"
                  rendered={ blockedOperationLabels.length > 0 }
                  text={ this.i18n('blockedOperationInfo', { operations: blockedOperationLabels.join(', ') }) }
                  className="no-margin"/>

                <Basic.TextField
                  ref="name"
                  label={this.i18n('acc:entity.System.name')}
                  required
                  max={255}/>
                <Basic.Checkbox
                  ref="remote"
                  onChange={this._setRemoteServer.bind(this)}
                  label={this.i18n('acc:entity.System.remoteConnector.label')}
                  helpBlock={this.i18n('acc:entity.System.remoteConnector.help')}/>
                {/* definition for remote connector server */}
                <Basic.TextField
                  ref="host"
                  label={this.i18n('acc:entity.ConnectorServer.host')}
                  hidden={!showConfigurationRemoteServer}
                  required={showConfigurationRemoteServer}
                  max={255}/>
                <Basic.Checkbox
                  ref="useSsl"
                  label={this.i18n('acc:entity.ConnectorServer.useSsl')}
                  hidden={!showConfigurationRemoteServer}/>
                <Basic.TextField
                  ref="port"
                  label={this.i18n('acc:entity.ConnectorServer.port')}
                  hidden={!showConfigurationRemoteServer}/>
                <Basic.TextField
                  ref="password"
                  type="password"
                  confidential
                  label={this.i18n('acc:entity.ConnectorServer.password')}
                  hidden={!showConfigurationRemoteServer}/>
                <Basic.TextField
                  ref="timeout"
                  label={this.i18n('acc:entity.ConnectorServer.timeout')}
                  hidden={!showConfigurationRemoteServer}/>
                {/* end for connector server definition */}
                <Basic.SelectBox
                  ref="passwordPolicyValidate"
                  label={this.i18n('acc:entity.System.passwordPolicyValidate')}
                  placeholder={this.i18n('acc:entity.System.passwordPolicyValidate')}
                  manager={this.passwordPolicyManager}
                  forceSearchParameters={this.passwordPolicyManager.getDefaultSearchParameters()
                    .setFilter('type', Enums.PasswordPolicyTypeEnum.findKeyBySymbol(Enums.PasswordPolicyTypeEnum.VALIDATE))}/>
                <Basic.SelectBox
                  ref="passwordPolicyGenerate"
                  label={this.i18n('acc:entity.System.passwordPolicyGenerate')}
                  placeholder={this.i18n('acc:entity.System.passwordPolicyGenerate')}
                  manager={this.passwordPolicyManager}
                  forceSearchParameters={this.passwordPolicyManager.getDefaultSearchParameters()
                    .setFilter('type', Enums.PasswordPolicyTypeEnum.findKeyBySymbol(Enums.PasswordPolicyTypeEnum.GENERATE))}/>
                <Basic.TextArea
                  ref="description"
                  label={this.i18n('acc:entity.System.description')}
                  max={255}/>
                <Basic.EnumSelectBox
                  ref="stateEnum"
                  label={ this.i18n('acc:entity.System.state.label', { escape: false }) }
                  placeholder={ this.i18n('acc:entity.System.state.placeholder') }
                  helpBlock={ this.i18n('acc:entity.System.state.help') }
                  emptyOptionLabel={ this.i18n('acc:entity.System.state.active.label', { escape: false }) }
                  options={[
                    {
                      value: 'readonly',
                      niceLabel: this.i18n('acc:entity.System.readonly.label', { escape: false }),
                      description: this.i18n('acc:entity.System.readonly.help', { escape: false })
                    },
                    {
                      value: 'readonlyDisabledProvisioning',
                      niceLabel: this.i18n('acc:entity.System.readonlyDisabledProvisioning.label', { escape: false }),
                      description: this.i18n('acc:entity.System.readonlyDisabledProvisioning.help', { escape: false })
                    },
                    {
                      value: 'disabled',
                      niceLabel: this.i18n('acc:entity.System.disabled.label', { escape: false }),
                      description: this.i18n('acc:entity.System.disabled.help', { escape: false })
                    },
                    {
                      value: 'disabledProvisioning',
                      niceLabel: this.i18n('acc:entity.System.disabledProvisioning.label', { escape: false }),
                      description: this.i18n('acc:entity.System.disabledProvisioning.help', { escape: false })
                    },
                  ]}/>
                <Basic.Checkbox
                  ref="queue"
                  label={this.i18n('acc:entity.System.queue.label')}
                  helpBlock={this.i18n('acc:entity.System.queue.help')}/>
                <Basic.Checkbox
                  ref="createOperation"
                  label={this.i18n('acc:entity.BlockedOperation.createOperation.label')}
                  helpBlock={this.i18n('acc:entity.BlockedOperation.createOperation.help')}/>
                <Basic.Checkbox
                  ref="updateOperation"
                  label={ this.i18n('acc:entity.BlockedOperation.updateOperation.label') }
                  helpBlock={ this.i18n('acc:entity.BlockedOperation.updateOperation.help') }/>
                <Basic.Checkbox
                  ref="deleteOperation"
                  label={ this.i18n('acc:entity.BlockedOperation.deleteOperation.label') }
                  helpBlock={ this.i18n('acc:entity.BlockedOperation.deleteOperation.help') }/>
              </Basic.AbstractForm>
            </Basic.PanelBody>

            <Basic.PanelFooter>
              <Basic.Button type="button" level="link" onClick={this.context.history.goBack}>{this.i18n('button.back')}</Basic.Button>

              <Basic.SplitButton
                level="success"
                title={this.i18n('button.saveAndContinue')}
                onClick={this.save.bind(this, 'CONTINUE')}
                showLoading={_showLoading}
                showLoadingIcon
                showLoadingText={this.i18n('button.saving')}
                rendered={
                  Utils.Entity.isNew(entity)
                  ?
                  Managers.SecurityManager.hasAuthority('SYSTEM_CREATE')
                  :
                  Managers.SecurityManager.hasAuthority('SYSTEM_UPDATE')
                }
                pullRight
                dropup>
                <Basic.MenuItem eventKey="1" onClick={this.save.bind(this, 'CLOSE')}>{this.i18n('button.saveAndClose')}</Basic.MenuItem>
              </Basic.SplitButton>
            </Basic.PanelFooter>
          </Basic.Panel>
          {/* onEnter action - is needed because SplitButton is used instead standard submit button */}
          <input type="submit" className="hidden"/>
        </form>
      </div>
    );
  }
}

SystemDetail.propTypes = {
  entity: PropTypes.object,
  uiKey: PropTypes.string.isRequired,
  _permissions: PropTypes.arrayOf(PropTypes.string)
};
SystemDetail.defaultProps = {
  _permissions: null
};

function select(state, component) {
  if (!component.entity) {
    return {};
  }
  return {
    _permissions: systemManager.getPermissions(state, null, component.entity.id)
  };
}

export default connect(select)(SystemDetail);
