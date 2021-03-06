import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import Helmet from 'react-helmet';
//
import * as Basic from '../../components/basic';
import { LongRunningTaskManager } from '../../redux';
import * as Advanced from '../../components/advanced';
import * as Utils from '../../utils';

const manager = new LongRunningTaskManager();

/**
 * Long running task tab panel
 *
 * @author Radek Tomiška
 */
class LongRunningTaskRoute extends Basic.AbstractContent {

  componentDidMount() {
    const { entityId } = this.props.match.params;
    //
    this.context.store.dispatch(manager.fetchEntityIfNeeded(entityId, null, (entity, error) => {
      this.handleError(error);
    }));
  }

  getContentKey() {
    return 'content.scheduler.all-tasks';
  }

  render() {
    const { entity, showLoading } = this.props;
    //
    return (
      <div>
        <Helmet title={this.i18n('title')} />

        <Advanced.DetailHeader
          showLoading={ showLoading }
          entity={ entity }
          back="/scheduler/all-tasks">
          { entity ? Utils.Ui.getSimpleJavaType(entity.taskType) : null }<small> { this.i18n('detail.header') }</small>
        </Advanced.DetailHeader>

        <Advanced.TabPanel parentId="scheduler-all-tasks" match={ this.props.match }>
          { this.getRoutes() }
        </Advanced.TabPanel>
      </div>
    );
  }
}

LongRunningTaskRoute.propTypes = {
  entity: PropTypes.object,
  showLoading: PropTypes.bool
};
LongRunningTaskRoute.defaultProps = {
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

export default connect(select)(LongRunningTaskRoute);
