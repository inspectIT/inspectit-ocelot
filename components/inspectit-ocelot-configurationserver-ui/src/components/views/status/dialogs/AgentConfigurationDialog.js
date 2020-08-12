import React from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import SyntaxHighlighter from 'react-syntax-highlighter';
import { tomorrowNightBlue } from 'react-syntax-highlighter/dist/cjs/styles/hljs';
import { ProgressBar } from 'primereact/progressbar';
/**
 * Dialog shows the agent configuration.
 */
const AgentConfigurationDialog = ({ visible, onHide, configurationValue, error, isLoading }) => {
  /**
   * Downloading agent configuration.
   */
  const download = () => {
    const blob = new Blob([configurationValue], { type: 'text/x-yaml' });
    const url = window.URL.createObjectURL(blob);
    return url;
  };
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
        visible={visible}
        onHide={onHide}
        footer={
          <div>
            <a href={!error ? download() : null} download="agent-config.yml">
              <Button icon="pi pi-download" label="Download" className="p-button-primary" disabled={error} />
            </a>
            <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
          </div>
        }
      >
        {isLoading ? (
          <ProgressBar mode="indeterminate" />
        ) : (
          <SyntaxHighlighter className="highlighter" language="yaml" style={tomorrowNightBlue}>
            {!error ? configurationValue : 'error'}
          </SyntaxHighlighter>
        )}
      </Dialog>
    </>
  );
};

export default AgentConfigurationDialog;
