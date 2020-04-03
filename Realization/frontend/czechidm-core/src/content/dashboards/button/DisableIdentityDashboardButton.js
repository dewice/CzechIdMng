import React from 'react';
import { connect } from 'react-redux';
//
import * as Basic from '../../../components/basic';
import * as Advanced from '../../../components/advanced';
import * as Utils from '../../../utils';
import { IdentityManager } from '../../../redux';

const identityManager = new IdentityManager();

/**
 * Quick button to disable identity manually.
 *
 * @author Radek Tomiška
 * @since 9.6.0
 */
class DisableIdentityDashboardButton extends Advanced.AbstractIdentityDashboardButton {

  getIcon() {
    return this.i18n('eav.bulk-action.identity-disable-bulk-action.icon');
  }

  isRendered() {
    const { identity, permissions, userContext } = this.props;
    //
    return !identity.disabled
      && userContext.id !== identity.id
      && Utils.Permission.hasPermission(permissions, 'MANUALLYDISABLE');
  }

  getLabel() {
    return this.i18n('eav.bulk-action.identity-disable-bulk-action.label');
  }

  getTitle() {
    return this.i18n('eav.bulk-action.identity-disable-bulk-action.title');
  }

  onClick() {
    const { identity } = this.props;
    //
    this.refs['confirm-disable'].show(
      this.i18n(`content.identities.action.deactivate.message`, { count: 1, record: identityManager.getNiceLabel(identity) }),
      this.i18n(`content.identities.action.deactivate.header`, { count: 1 })
    ).then(() => {
      this.setState({
        showLoading: true
      }, () => {
        identityManager
          .getService()
          .deactivate(identity.id)
          .then(json => {
            // new entity to redux trimmed store
            this.context.store.dispatch(identityManager.receiveEntity(identity.id, json));
            //
            this.setState({
              showLoading: false
            });
          });
      });
    }, () => {
      // nothing
    });
  }

  renderConfirm() {
    return (
      <Basic.Confirm ref="confirm-disable" level="danger" />
    );
  }

  getLevel() {
    return 'danger';
  }
}

function select(state) {
  return {
    i18nReady: state.config.get('i18nReady'), // required
    userContext: state.security.userContext // required
  };
}

export default connect(select)(DisableIdentityDashboardButton);
