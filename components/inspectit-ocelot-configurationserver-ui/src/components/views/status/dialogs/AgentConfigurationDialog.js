import React from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import SyntaxHighlighter from 'react-syntax-highlighter';
import { tomorrowNightBlue } from 'react-syntax-highlighter/dist/cjs/styles/hljs';
import axios from '../../../../lib/axios-api';


/**
 * Dialog shows the agent configuration.
 */
class AgentConfigurationDialog extends React.Component {
  state = {
    configurationValue: '',
  };

  componentDidUpdate(prevProps) {
    if (prevProps.attributes !== this.props.attributes) {
      this.getConfiguration(this.props.attributes);
    }
  }

  /**
   * Downloading agent configuration.
   */
  download = () => {
    var blob = new Blob([this.props.configurationValue], { type: 'text/x-yaml' });
    this.url = window.URL.createObjectURL(blob);
    return this.url;
  };

   /**
   * Closing dialog.
   */
  handleClose = (success = true) => {
    if (success) {
      this.props.onHide();
    }
  };

  render() {
    return (
      <Dialog style={{ width: '50vw' }}
        header={'Agent Configuration'}
        modal={true}
        visible={this.props.visible}
        onHide={this.props.onHide}
        footer={
          <div>
            <a href={this.download()} download="agent-config.yml">
              <Button label="Download" className="p-button-primary" />
            </a>
            <Button label="Cancel" className="p-button-secondary" onClick={this.handleClose} />
          </div>
        }
      >
        <SyntaxHighlighter language="yaml" style={tomorrowNightBlue}>
          {this.props.configurationValue}
        </SyntaxHighlighter>
      </Dialog>
    );
  }
}

export default AgentConfigurationDialog;
