import React from 'react';
import { connect } from 'react-redux';
import { notificationActions } from '../../../../redux/ducks/notification';

import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import KeyValueEditor from '../editComponents/KeyValueEditor';
import DownloadLink from '../DownloadLink';

/**
 * Dialog for entering key/value pairs and downloading a configuration file.
 */
class DownloadDialog extends React.Component {
  downloadButton = React.createRef();
  downloadLink = React.createRef();

  state = {
    attributes: [],
  }

  handleChangeAttributes = (newAttributes) => {
    this.setState({ attributes: newAttributes });
  }

  render() {
    return (
      <Dialog
        header={'Download Configuration File'}
        modal={true}
        visible={this.props.visible}
        onHide={this.props.onHide}
        style={{ 'max-width': '900px' }}
        footer={(
          <div>
            <Button label="Download" ref={this.downloadButton} className="p-button-primary" onClick={this.handleDownload} />
            <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
          </div>
        )}
      >
        <p style={{ 'margin-bottom': '1em' }}>
          Enter key/value pairs to download the correlating agent configuration.
          <br /> You will get a different result depending on the mapping that fits your input.
        </p>
        <KeyValueEditor
          onChange={this.handleChangeAttributes}
          keyValueArray={this.state.attributes}
          maxHeight={`300px`}
        />
        {/** creating reference of DownloadLink, to be able to call onDownload within this.handleDownload */}
        <DownloadLink onRef={ref => this.downloadLink = ref} />
      </Dialog>
    )
  }

  /**
   * triggers download of config file and onHide
   */
  handleDownload = () => {
    const objForDownload = {};
    let hasEmptyKey;
    this.state.attributes.forEach(pair => {
      objForDownload[pair.key || ''] = pair.value || '';
      if (!pair.key) {
        hasEmptyKey = true;
      }
    })

    this.showWarningMsg(hasEmptyKey, objForDownload);

    this.props.onHide();
    this.downloadLink.onDownload(objForDownload);
  }

  // shows a warning msg in case of duplicate or empty keys
  showWarningMsg = (showEmptyKeyMsg, attributeObj) => {
    if (Object.keys(attributeObj).length !== this.state.attributes.length) {
      this.props.showWarningMessage('Invalid Input', 'Some attribute keys were double and have been discarded');
    }
    if (showEmptyKeyMsg) {
      this.props.showWarningMessage('Invalid Input', 'Some attribute keys were double and have been discarded');
    }
  }

  componentDidUpdate(prevProps) {
    if (!prevProps.visible && this.props.visible) {
      this.downloadButton.current.element.focus();
    }
  }
}

const mapDispatchToProps = {
  showWarningMessage: notificationActions.showWarningMessage,
}

export default connect(null, mapDispatchToProps)(DownloadDialog);