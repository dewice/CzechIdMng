

import React, { PropTypes } from 'react';
import _ from 'lodash';
import moment from 'moment';
import uuid from 'uuid';
//
import * as Basic from '../../components/basic';
import * as Advanced from '../../components/advanced';
import { RoleManager, IdentityManager, IdentityContractManager } from '../../redux';
import SearchParameters from '../../domain/SearchParameters';

/**
* Table for keep identity role concept. Input are all current assigned user's permissions
* Designed for use in task detail.
*/

const roleManager = new RoleManager();
const identityManager = new IdentityManager();
const identityContractManager = new IdentityContractManager();

export class RoleConceptTable extends Basic.AbstractContent {

  constructor(props, context) {
    super(props, context);
    this.state = {
      conceptData: {},
      filterOpened: this.props.filterOpened,
      detail: {
        show: false,
        entity: {},
        add: false
      }
    };
  }

  componentDidMount() {
    // We have to create concept from props here, because same instance this component
    //  could be used in past (in this case may be this.prosp and nextProps same)
    this._setConcept(this.props);
  }

  getContentKey() {
    return 'content.task.IdentityRoleConceptTable';
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps && (
      JSON.stringify(nextProps.identityRoles) !== JSON.stringify(this.props.identityRoles) ||
      JSON.stringify(nextProps.addedIdentityRoles) !== JSON.stringify(this.props.addedIdentityRoles) ||
      JSON.stringify(nextProps.removedIdentityRoles) !== JSON.stringify(this.props.removedIdentityRoles) ||
      JSON.stringify(nextProps.changedIdentityRoles) !== JSON.stringify(this.props.changedIdentityRoles)
    )) {
      this._setConcept(nextProps);
    }
  }

  /**
   * Set input arrays (with current, added, removed, changed data) to state (first do clone) and call compile conceptData
   * @param  {array}  identityRoles         Original not modified data
   * @param  {array}  addedIdentityRoles    Added data
   * @param  {array}  removedIdentityRoles  Removed data (ids)
   * @param  {array}  changedIdentityRoles} Changed data (every object in array contains only changed fields and ID)
   */
  _setConcept({ identityRoles, addedIdentityRoles, removedIdentityRoles, changedIdentityRoles}) {
    this.setState({conceptData: this._compileConceptData({ identityRoles, addedIdentityRoles, removedIdentityRoles, changedIdentityRoles})});
  }

  /**
   * Show modal dialog
   * @param  {Object}  entity           Entity show in dialog
   * @param  {Boolean} isEdit = false   If is false then form in dialog will be read only
   */
  _showDetail(entity, isEdit = false, multiAdd = false) {
    const entityFormData = _.merge({}, entity, {
      role: entity._embedded && entity._embedded.role ? entity._embedded.role : null
    });

    this.setState({
      detail: {
        show: true,
        edit: isEdit && !entity.automaticRole,
        entity: entityFormData,
        add: multiAdd
      }
    }, () => {
      this.refs.form.setData(entityFormData);
      this.refs.role.focus();
    });
  }

  /**
   * Close modal dialog
   */
  _closeDetail() {
    this.setState({
      detail: {
        ... this.state.detail,
        show: false,
        add: false
      }
    });
  }

  /**
   * Save added or changed entities to arrays and recompile concept data.
   */
  _saveConcept(event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.refs.form.isFormValid()) {
      return;
    }
    const {identityUsername, createConceptFunc, updateConceptFunc} = this.props;

    const entity = this.refs.form.getData();
    if (entity._added) {
      if (!entity._virtualId && entity.role instanceof Array) {
        for (const roleId of entity.role) {
          const uuidId = uuid.v1();
          const identityRole = _.merge({}, entity, {_virtualId: uuidId, _added: true});
          identityRole._virtualId = uuidId;
          identityRole._embedded = {};
          identityRole._embedded.identity = identityManager.getEntity(this.context.store.getState(), identityUsername);
          identityRole._embedded.role = roleManager.getEntity(this.context.store.getState(), roleId);
          createConceptFunc(identityRole, 'ADD');
        }
      } else {
        const addedIdentityRole = this._findAddedIdentityRoleById(entity.id);
        entity._embedded = {};
        entity._embedded.identity = identityManager.getEntity(this.context.store.getState(), identityUsername);
        entity._embedded.role = roleManager.getEntity(this.context.store.getState(), entity.role);
        if (addedIdentityRole) {
          updateConceptFunc(entity, 'ADD');
        } else {
          createConceptFunc(entity, 'ADD');
        }
      }
    } else {
      const changedIdentityRole = _.merge({}, this._findChangedIdentityRoleById(entity.id));
      let changed = false;
      const resultValidFrom = this._findChange('validFrom', entity);
      const resultValidTill = this._findChange('validTill', entity);

      if (resultValidFrom.changed) {
        changedIdentityRole.validFrom = resultValidFrom.value;
        changed = true;
      }
      if (resultValidTill.changed) {
        changedIdentityRole.validTill = resultValidTill.value;
        changed = true;
      }

      if (changed && changedIdentityRole && changedIdentityRole.id) {
        updateConceptFunc(changedIdentityRole, 'UPDATE');
      } else {
        createConceptFunc(entity, 'UPDATE');
      }
    }
    this.setState({conceptData: this._compileConceptData(this.props)});
    this._closeDetail();
  }

  _findChangedIdentityRoleById(id) {
    const {changedIdentityRoles} = this.props;
    for (const changedIdentityRole of changedIdentityRoles) {
      if (changedIdentityRole.identityRole === id) {
        return changedIdentityRole;
      }
    }
    return null;
  }

  _findAddedIdentityRoleById(virtualId) {
    const {addedIdentityRoles} = this.props;
    for (const addedIdentityRole of addedIdentityRoles) {
      if (addedIdentityRole.id === virtualId) {
        return addedIdentityRole;
      }
    }
    return null;
  }

  /**
   * Find and apply changes to changedIdentityRole array
   */
  _findChange(property, entity) {
    const {conceptData} = this.state;
    const changedPropertyName = '_' + property + 'Changed';
    for (const conceptIdentityRole of conceptData) {
      if (conceptIdentityRole.id === entity.id) {
        let propertyWithNewValue = property;
        if (entity.hasOwnProperty(changedPropertyName)) {
          propertyWithNewValue = changedPropertyName;
        }
        if (entity[propertyWithNewValue] !== conceptIdentityRole[propertyWithNewValue]) {
          return {changed: true, value: entity[propertyWithNewValue]};
        }
      }
    }
    return {changed: false};
  }

  /**
   * Delete operation for added (delete whole object from array added entities),
   * changed (delete changes ... object from array changed entities),
   * removed (remove id from array of deleted entities)
   */
  _deleteConcept(data) {
    // let messageKey;
    // if (data._added) {
    //   messageKey = 'Added';
    // } else if (data._removed) {
    //   messageKey = 'Removed';
    // } else if (data._changed) {
    //   messageKey = 'Changed';
    // } else {
    //   this._internalDeleteConcept(data);
    //   return;
    // }
    // this.refs['confirm-delete'].show(
    //   this.i18n(`action.delete${messageKey}.message`),
    //   this.i18n(`action.delete${messageKey}.header`)
    // ).then(() => {
    // }, () => {
    //   // Rejected
    // });
    this._internalDeleteConcept(data);
  }

  _internalDeleteConcept(data) {
    const {createConceptFunc, removeConceptFunc} = this.props;

    if (data._added) {
      removeConceptFunc(data);
    } else if (data._removed) {
      removeConceptFunc(data.id, 'REMOVE');
    } else if (data._changed) {
      removeConceptFunc(data.id, 'UPDATE');
    } else {
      createConceptFunc(data, 'REMOVE');
      return;
    }
  }

  /**
   * Create final data for concept table by input arrays
   * @param  {array}  identityRoles         Original not modified data
   * @param  {array}  addedIdentityRoles    Added data
   * @param  {array}  removedIdentityRoles  Removed data (ids)
   * @param  {array}  changedIdentityRoles} Changed data (every object in array contains only changed fields and ID)
   * @return {array}  conceptData           Final data for concept table
   */
  _compileConceptData({ identityRoles, addedIdentityRoles, removedIdentityRoles, changedIdentityRoles}) {
    let concepts = _.merge([], identityRoles);
    if (addedIdentityRoles) {
      for (const addedIdentityRole of addedIdentityRoles) {
        addedIdentityRole._added = true;
      }
      concepts = _.concat(concepts, addedIdentityRoles);
    }

    for (const concept of concepts) {
      if (removedIdentityRoles && removedIdentityRoles.includes(concept.id)) {
        concept._removed = true;
      }
      if (changedIdentityRoles) {
        for (const changedIdentityRole of changedIdentityRoles) {
          if (changedIdentityRole.identityRole === concept.id) {
            concept._changed = true;
            for (const property in changedIdentityRole) {
              if (changedIdentityRole.hasOwnProperty(property)) {
                const key = '_' + property + 'Changed';
                concept[key] = changedIdentityRole[property];
              }
            }
          }
        }
      }
    }
    return concepts;
  }

  /**
   * Compute background color row (added, removed, changed)
   */
  _rowClass({rowIndex, data}) {
    if (data[rowIndex]._added) {
      return 'bg-success';
    }
    if (data[rowIndex]._removed) {
      return 'bg-danger';
    }
    if (data[rowIndex]._changed) {
      return 'bg-warning';
    }
    return null;
  }

  /**
   * Generate date cell for concept. If is entity changed, will use 'changed' property. In title show old value.
   */
  _conceptDateCell({rowIndex, data, property}) {
    const changedProperty = '_' + property + 'Changed';
    // Load old value and add to title
    const format = this.i18n('format.date');
    const propertyOldValue = Basic.Cell.getPropertyValue(data[rowIndex], property);
    const dataOldValue = propertyOldValue ? moment(propertyOldValue).format(format) : null;
    const oldValueMessage = dataOldValue ? this.i18n('oldValue', {oldValue: dataOldValue}) : this.i18n('oldValueNotExist');
    const changedPropertyExist = data[rowIndex].hasOwnProperty(changedProperty);
    return (
      <Basic.DateCell
        className={changedPropertyExist ? 'text-danger' : ''}
        property={changedPropertyExist ? changedProperty : property}
        rowIndex={rowIndex}
        data={data}
        title={changedPropertyExist ? oldValueMessage : null}
        format={format}/>
    );
  }

  /**
   * Create new IdentityRoleConcet with virtual ID (UUID)
   */
  _addConcept() {
    const newIdentityRoleConcept = {_added: true};
    this._showDetail(newIdentityRoleConcept, true, true);
  }

  /**
   * Generate cell with actions (buttons)
   */
  _conceptActionsCell({rowIndex, data}) {
    const {readOnly} = this.props;
    const actions = [];
    const value = data[rowIndex];
    const notModificated = !(value._added || value._removed || value._changed);
    const isAutomaticRole = value.automaticRole;

    actions.push(
      <Basic.Button
        level={'danger'}
        onClick={this._deleteConcept.bind(this, data[rowIndex])}
        className="btn-xs"
        disabled={readOnly || isAutomaticRole}
        role="group"
        title={this.i18n('button.delete')}
        titlePlacement="bottom">
        <Basic.Icon icon={notModificated ? 'trash' : 'remove'}/>
      </Basic.Button>
    );
    if (!value._removed) {
      actions.push(
        <Basic.Button
          level={'warning'}
          onClick={this._showDetail.bind(this, data[rowIndex], true, false)}
          className="btn-xs"
          disabled={readOnly || isAutomaticRole}
          role="group"
          title={this.i18n('button.edit')}
          titlePlacement="bottom">
          <Basic.Icon icon={'edit'}/>
        </Basic.Button>
      );
    }
    return (
      <div className="btn-group" role="group">
        {actions}
      </div>
    );
  }

  render() {
    const { showLoading, identityUsername, readOnly, className } = this.props;
    const { conceptData, detail } = this.state;
    //
    return (
      <div>
        <Basic.Confirm ref="confirm-delete" level="danger"/>
        <Basic.Toolbar>
          <div className="pull-right">
            <Basic.Button
              level="success"
              className="btn-xs"
              disabled={readOnly}
              onClick={this._addConcept.bind(this)}>
              <Basic.Icon value="fa:plus"/>
              {' '}
              {this.i18n('button.add')}
            </Basic.Button>
          </div>
          <div className="clearfix"></div>
        </Basic.Toolbar>
        <Basic.Table
          hover={false}
          showLoading={showLoading}
          data={conceptData}
          rowClass={this._rowClass}
          className={className}
          showRowSelection={false}
          noData={this.i18n('component.basic.Table.noData')}>
          <Basic.Column
            header=""
            className="detail-button"
            cell={
              ({ rowIndex, data }) => {
                return (
                  <Advanced.DetailButton
                    title={this.i18n('button.detail')}
                    onClick={this._showDetail.bind(this, data[rowIndex], !data[rowIndex]._removed, false)}/>
                );
              }
            }
            sort={false}/>
          <Basic.Column
            header={this.i18n('entity.IdentityRole.role')}
            property="_embedded.role.name"
            />
          <Basic.Column
            header={this.i18n('entity.IdentityRole.identityContract.title')}
            property="_embedded.identityContract"
            cell={
              ({rowIndex, data}) => {
                const contract = data[rowIndex]._embedded.identityContract;
                return (
                  <Advanced.IdentityContractInfo entityIdentifier={ contract.id } entity={ contract } showIdentity={ false } face="popover" showLink={ false } />
                );
              }
            }/>
          <Basic.Column
            header={this.i18n('entity.Role.description')}
            property="_embedded.role.description"
            rendered={false}
            />
          <Basic.Column
            property="validFrom"
            header={this.i18n('label.validFrom')}
            cell={this._conceptDateCell.bind(this)}/>
          <Basic.Column
            property="validTill"
            header={this.i18n('label.validTill')}
            cell={this._conceptDateCell.bind(this)}/>
          <Basic.Column
            property="automaticRole"
            header={<Basic.Cell className="column-face-bool">{this.i18n('entity.IdentityRole.roleTreeNode.label')}</Basic.Cell>}
            cell={<Basic.BooleanCell className="column-face-bool"/>}/>
          <Basic.Column
            header={this.i18n('label.action')}
            className="action"
            cell={this._conceptActionsCell.bind(this)}/>
        </Basic.Table>

        <Basic.Modal
          bsSize="default"
          show={detail.show}
          onHide={this._closeDetail.bind(this)}
          backdrop="static"
          keyboard={!showLoading}>

          <form onSubmit={this._saveConcept.bind(this)}>
            <Basic.Modal.Header closeButton={!showLoading} text={this.i18n('create.header')} rendered={detail.entity.id === undefined}/>
            <Basic.Modal.Header closeButton={!showLoading} text={this.i18n('edit.header', { role: detail.entity.role })} rendered={detail.entity.id !== undefined}/>
            <Basic.Modal.Body>
              <Basic.AbstractForm ref="form" showLoading={showLoading} readOnly={!detail.edit}>
                <Basic.SelectBox
                  ref="role"
                  manager={roleManager}
                  label={this.i18n('entity.IdentityRole.role')}
                  multiSelect={detail.entity._added && detail.add}
                  readOnly={!detail.entity._added}
                  required/>
                <Basic.SelectBox
                  ref="identityContract"
                  manager={ identityContractManager }
                  forceSearchParameters={ new SearchParameters().setFilter('identity', identityUsername) }
                  label={ this.i18n('entity.IdentityRole.identityContract.label') }
                  placeholder={ this.i18n('entity.IdentityRole.identityContract.placeholder') }
                  helpBlock={ this.i18n('entity.IdentityRole.identityContract.help') }
                  returnProperty={false}
                  readOnly={!detail.entity._added}
                  required
                  useFirst/>
                <Basic.Checkbox
                  ref="automaticRole"
                  label={this.i18n('entity.IdentityRole.roleTreeNode.label')}
                  readOnly
                  hidden={ detail.entity._added }/>

                <Basic.Row>
                  <div className="col-md-6">
                    <Basic.DateTimePicker
                      mode="date"
                      className={detail.entity.hasOwnProperty('_validFromChanged') ? 'text-danger' : null}
                      ref={detail.entity.hasOwnProperty('_validFromChanged') ? '_validFromChanged' : 'validFrom'}
                      label={this.i18n('label.validFrom')}/>
                  </div>
                  <div className="col-md-6">
                    <Basic.DateTimePicker
                      mode="date"
                      className={detail.entity.hasOwnProperty('_validTillChanged') ? 'text-danger' : null}
                      ref={detail.entity.hasOwnProperty('_validTillChanged') ? '_validTillChanged' : 'validTill'}
                      label={this.i18n('label.validTill')}/>
                  </div>
                </Basic.Row>

              </Basic.AbstractForm>
            </Basic.Modal.Body>
            <Basic.Modal.Footer>
              <Basic.Button
                level="link"
                onClick={this._closeDetail.bind(this)}
                showLoading={showLoading}>
                {this.i18n('button.close')}
              </Basic.Button>
              <Basic.Button
                type="submit"
                level="success"
                showLoading={showLoading}
                showLoadingIcon
                rendered={detail.edit}>
                {this.i18n('button.set')}
              </Basic.Button>
            </Basic.Modal.Footer>
          </form>
        </Basic.Modal>
      </div>
    );
  }
}

RoleConceptTable.propTypes = {
  uiKey: PropTypes.string.isRequired,
  identityUsername: PropTypes.string.isRequired,
  className: PropTypes.string
};

RoleConceptTable.defaultProps = {
  filterOpened: false,
  showLoading: false
};


export default RoleConceptTable;
