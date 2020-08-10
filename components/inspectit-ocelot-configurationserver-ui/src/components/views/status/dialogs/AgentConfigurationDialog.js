import React from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import SyntaxHighlighter from 'react-syntax-highlighter';
import { tomorrowNightBlue } from 'react-syntax-highlighter/dist/cjs/styles/hljs';

/**
 * Dialog shows the agent configuration.
 */
class AgentConfigurationDialog extends React.Component {
  /**
   * Downloading agent configuration.
   */
  download = () => {
    var blob = new Blob([this.props.configurationValue], { type: 'text/x-yaml' });
    this.url = window.URL.createObjectURL(blob);
    return this.url;
  };

  render() {
    return (
      <>
        <style jsx>
          {`
            .highlighter {
              overflow-y: hidden !important;
              overflow-x: hidden !important;
            }
          `}
        </style>
        <Dialog
          style={{ width: '50vw', overflow: 'auto' }}
          header={'Agent Configuration'}
          modal={true}
          visible={this.props.visible}
          onHide={this.props.onHide}
          footer={
            <div>
              <a href={this.download()} download="agent-config.yml">
                <Button icon="pi pi-download" label="Download" className="p-button-primary" />
              </a>
              <Button label="Cancel" className="p-button-secondary" onClick={this.props.onHide} />
            </div>
          }
        >
          <SyntaxHighlighter className="highlighter" language="yaml" style={tomorrowNightBlue}>
            {this.props.configurationValue}
          </SyntaxHighlighter>
        </Dialog>
      </>
    );
  }
}

export default AgentConfigurationDialog;
