import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import * as Basic from '../../components/basic';
import { ScriptManager } from '../../redux';
import * as Advanced from '../../components/advanced';

const manager = new ScriptManager();

/**
 * Script's tab panel
 *
 * @author Patrik Stloukal
 */
class Script extends Basic.AbstractContent {

  componentDidMount() {
    const { entityId } = this.props.match.params;
    //
    this.context.store.dispatch(manager.fetchEntityIfNeeded(entityId, null, (entity, error) => {
      this.handleError(error);
    }));
  }

  render() {
    const { entity, showLoading } = this.props;

    return (
      <div>
        <Basic.PageHeader showLoading={!entity && showLoading}>
          <Basic.Icon value="component:script"/>
          {' '}
          { manager.getNiceLabel(entity)} <small> {this.i18n('content.scripts.edit.header') }</small>
        </Basic.PageHeader>

        <Advanced.TabPanel parentId="scripts" match={ this.props.match }>
          { this.getRoutes() }
        </Advanced.TabPanel>
      </div>
    );
  }
}

Script.propTypes = {
  entity: PropTypes.object,
  showLoading: PropTypes.bool
};
Script.defaultProps = {
  entity: null,
  showLoading: false
};

function select(state, component) {
  const { entityId } = component.match.params;
  return {
    entity: manager.getEntity(state, entityId),
    showLoading: manager.isShowLoading(state, null, entityId)
  };
}

export default connect(select)(Script);
