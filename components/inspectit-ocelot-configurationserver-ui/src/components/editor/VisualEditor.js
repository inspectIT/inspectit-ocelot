import yaml from 'js-yaml';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import PropTypes from 'prop-types';
import React from 'react';

class VisualEditor extends React.Component {
  constructor() {
    super();

    this.state = {
      isError: false,
      showWarn: false,
    };
  }

  componentDidMount() {
    this.parseAndUpdateState();
  }

  componentDidUpdate(prevProps) {
    if (prevProps.yamlConfig !== this.props.yamlConfig) {
      this.parseAndUpdateState();
    }
  }

  parseAndUpdateState = () => {
    try {
      let config = {};
      let normalized = true;

      if (this.props.yamlConfig && this.props.yamlConfig.length > 0) {
        config = yaml.safeLoad(this.props.yamlConfig);
        normalized = yaml.safeDump(config) === this.props.yamlConfig;
      }

      this.setState({
        isError: false,
        config,
        normalized,
      });
    } catch (error) {
      this.setState({
        isError: true,
        config: undefined,
        normalized: undefined,
      });
    }
  };

  onUpdate = (config) => {
    const updatedYamlConfig = yaml.safeDump(config);
    const doUpdate = () => this.props.onUpdate(updatedYamlConfig);

    if (this.state.normalized) {
      doUpdate();
    } else {
      this.setState({
        showWarn: true,
        pendingConfigUpdate: updatedYamlConfig,
      });
    }
  };

  normalizedDoAccept = () => {
    const { pendingConfigUpdate } = this.state;
    this.setState(
      {
        showWarn: false,
        pendingConfigUpdate: undefined,
      },
      () => this.props.onUpdate(pendingConfigUpdate)
    );
  };

  normalizedDoDecline = () =>
    this.setState({
      showWarn: false,
      pendingConfigUpdate: undefined,
    });

  render() {
    const { config, isError, showWarn } = this.state;

    return (
      <div className="this">
        <style jsx>{`
          .this {
            flex: 1;
            display: flex;
          }
          .errorBox {
            align-self: center;
            justify-content: center;
            flex: 1;
            display: flex;
            flex-direction: column;
            color: #bbb;
            text-align: center;
          }
        `}</style>
        <Dialog
          header="Config file overwrite"
          footer={
            <div>
              <Button label="Yes" icon="pi pi-check" onClick={this.normalizedDoAccept} />
              <Button label="No" icon="pi pi-times" onClick={this.normalizedDoDecline} />
            </div>
          }
          closable={false}
          visible={showWarn}
          style={{ width: '40vw' }}
          modal={true}
          onHide={() => null}
        >
          Your configuration file has comments or other non-yaml data. These will be overwritten if you apply the given update. Do you want
          to continue?
        </Dialog>
        {!isError && this.props.children(this.onUpdate, config)}
        {isError && (
          <div className="errorBox">
            <p>Properties could not be loaded from the YAML content.</p>
          </div>
        )}
      </div>
    );
  }
}

VisualEditor.propTypes = {
  /** yaml config as string */
  yamlConfig: PropTypes.string,
  /** function to update the yaml config */
  onUpdate: PropTypes.function,
};

VisualEditor.defaultProps = {};

export default VisualEditor;
