import React from 'react';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import SyntaxHighlighter from 'react-syntax-highlighter';
import { tomorrowNightBlue } from 'react-syntax-highlighter/dist/cjs/styles/hljs';
import { ProgressBar } from 'primereact/progressbar';
import PropTypes from 'prop-types';

/**
 * Dialog shows the agent configuration.
 */
const AgentConfigurationDialog = ({ visible, onHide, configurationValue, error, loading, agentName, fileName }) => {
  const downloadFilename = 'agent-config' + (agentName ? '_' + agentName.replace(/[^a-z0-9-]/gi, '_').toLowerCase() : '') + '.yml';

  // downloading the agent configuration
  const download = () => {
    const blob = new Blob([configurationValue], { type: 'text/x-yaml' });
    return window.URL.createObjectURL(blob);
  };

  const dialogHeader = agentName
    ? 'Agent Configuration' + (agentName ? " of Agent '" + agentName + "'" : '')
    : 'Configuration' + (fileName ? " of File '" + fileName + "'" : '');

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
        header={dialogHeader}
        modal={true}
        visible={visible}
        onHide={onHide}
        footer={
          <div>
            <a href={error || loading ? null : download()} download={downloadFilename}>
              <Button icon="pi pi-download" label="Download" className="p-button-primary" disabled={loading || error} />
            </a>
            <Button label="Cancel" className="p-button-secondary" onClick={onHide} />
          </div>
        }
      >
        {loading ? (
          <ProgressBar mode="indeterminate" />
        ) : error ? (
          <div>The agent configuration could not been loaded due to an unexpected error.</div>
        ) : (
          <SyntaxHighlighter className="highlighter" language="yaml" style={tomorrowNightBlue}>
            {configurationValue}
          </SyntaxHighlighter>
        )}
      </Dialog>
    </>
  );
};

AgentConfigurationDialog.propTypes = {
  /** The content of the configuration */
  configurationValue: PropTypes.string,
  /** Whether a error is thrown */
  error: PropTypes.bool,
  /** Whether the dialog is visible */
  visible: PropTypes.bool,
  /** Whether the file is loading */
  loading: PropTypes.bool,
  /** Callback on dialog hide */
  onHide: PropTypes.func,
  /** The agent name of the configuration */
  agentName: PropTypes.string,
  /** The file name of the configuration */
  fileName: PropTypes.string,
};

AgentConfigurationDialog.defaultProps = {
  error: false,
  visible: true,
  onHide: () => {},
};

export default AgentConfigurationDialog;
