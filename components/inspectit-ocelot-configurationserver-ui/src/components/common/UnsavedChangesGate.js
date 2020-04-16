import React from 'react';
import { connect } from 'react-redux';
import { configurationSelectors } from '../../redux/ducks/configuration';

/**
 * Prevents exiting the web page in case there are unsaved changes available.
 */
class UnsavedChangesGate extends React.Component {
  componentDidMount() {
    window.onbeforeunload = () => {
      if (this.hasUnsavedChanges()) {
        // blocks navigation
        return true;
      } else {
        return null;
      }
    };
  }

  componentWillUnmount() {
    window.onbeforeunload = null;
  }

  render() {
    return <>{this.props.children}</>;
  }

  hasUnsavedChanges = () => {
    return this.props.unsavedConfigChanges;
  };
}

function mapStateToProps(state) {
  return {
    unsavedConfigChanges: configurationSelectors.hasUnsavedChanges(state)
  };
}

export default connect(mapStateToProps)(UnsavedChangesGate);
