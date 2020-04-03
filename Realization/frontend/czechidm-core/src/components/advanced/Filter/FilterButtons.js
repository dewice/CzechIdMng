import React from 'react';
import PropTypes from 'prop-types';
//
import * as Basic from '../../basic';

/**
 * Filter buttons (use, clear) mainly for advanced table
 *
 * TODO: condensed with btn-xs (depends on condensed filter too).
 * FIXME: show loading is not propagated - add uiKey as prop + redux.
 *
 * @author Radek Tomiška
 */
export default class FilterButtons extends Basic.AbstractContextComponent {

  _cancelFilter(event) {
    const { cancelFilter } = this.props;
    if (event) {
      event.preventDefault();
    }
    if (cancelFilter) {
      cancelFilter(event);
    }
  }

  _useFilter(event) {
    const { useFilter } = this.props;
    //
    if (useFilter) {
      if (event) {
        event.preventDefault();
      }
      useFilter(event);
    }
  }

  render() {
    const {
      rendered,
      showLoading,
      style,
      className,
      showIcon,
      showText
    } = this.props;
    //
    if (!rendered || showLoading) {
      return null;
    }
    //
    return (
      <span style={ style } className={ className }>
        <Basic.Button
          onClick={ this._cancelFilter.bind(this) }
          style={{ marginRight: 5 }}
          icon={ showIcon ? 'remove' : null }
          title={ this.i18n('button.filter.cancel') }
          titlePlacement="bottom"
          text={ showText ? this.i18n('button.filter.cancel') : null }/>
        <Basic.Button
          level="primary"
          type="submit"
          onClick={ this._useFilter.bind(this) }
          icon={ showIcon ? 'fa:check' : null }
          title={ this.i18n('button.filter.use') }
          titlePlacement="bottom"
          text={ showText ? this.i18n('button.filter.use') : null }/>
      </span>
    );
  }
}

FilterButtons.propTypes = {
  ...Basic.AbstractContextComponent.propTypes,
  /**
   * Callback, when filter is canceled
   */
  cancelFilter: PropTypes.func,
  /**
   * Callback, when filter is used (submited)
   */
  useFilter: PropTypes.func
};
FilterButtons.defaultProps = {
  ...Basic.AbstractContextComponent.defaultProps,
  showIcon: false,
  showText: true
};
