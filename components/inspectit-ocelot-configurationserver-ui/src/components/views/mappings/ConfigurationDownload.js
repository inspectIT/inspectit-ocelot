import React from 'react';
import { connect } from 'react-redux';
import { notificationActions } from '../../../redux/ducks/notification';

import axios from '../../../lib/axios-api';
import RandExp from 'randexp';

/**
 * component for downloading configuration files
 * ~ functions can be invoked via reference by parents
 */
class ConfigurationDownload extends React.Component {
  linkRef = React.createRef();

  /**
   * parent component should pass down a function to create a reference
   * [onRef={ref => configDownloadRef = ref}]
   * to be able to call download via configDownloadRef.download()
   */
  componentDidMount() {
    this.props.onRef(this);
  }
  componentWillUnmount() {
    this.props.onRef(undefined);
  }

  download = (attributes) => {
    const requestParams = this.solveRegexValues(attributes);
    if (!requestParams) {
      return;
    }

    axios
      .get('/configuration/agent-configuration', {
        params: { ...requestParams },
      })
      .then((res) => {
        var blob = new Blob([res.data], { type: 'text/x-yaml' });
        const url = window.URL.createObjectURL(blob);

        this.linkRef.current.href = url;
        this.linkRef.current.click();
      })
      .catch(() => {
        this.props.showInfoMessage(
          'Downloading Config File Failed',
          'No file could have been retrieved. There might not be a configuration for the given attributes'
        );
      });
  };

  solveRegexValues = (obj = {}) => {
    try {
      let res = {};
      Object.keys(obj).map((key) => {
        const randexp = new RandExp(obj[key]);
        randexp.max = randexp.min || 0;
        res[key] = randexp.gen();
      });
      return res;
    } catch {
      this.props.showInfoMessage('Downloading Config File Failed', 'The given attribute values are not a regular expression');
      return null;
    }
  };

  render() {
    return <a ref={this.linkRef} download="agent-config.yml" style={{ visibility: 'hidden' }} />;
  }
}

const mapDispatchToProps = {
  showInfoMessage: notificationActions.showInfoMessage,
};

export default connect(null, mapDispatchToProps)(ConfigurationDownload);
