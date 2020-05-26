import yaml from 'js-yaml';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import PropTypes from 'prop-types';
import React from 'react';

/**
 * Caches yaml strings and corresponding parsed representations.
 * This speeds up parsing, dumping is not cached.
 *
 */
class YamlParserCache {
  /**
   * @param {*} size the size of the cache.
   */
  constructor(size) {
    this.cache = new Array(size).fill({
      yamlString: '',
      parsed: {},
    });
    this.writeIndex = 0;
    this.size = size;
  }

  /**
   * Returns null, if the given yamlString is unparseable.
   */
  parse = (yamlString) => {
    let matchingEntry = this.cache.find((entry) => entry.yamlString === yamlString);
    if (matchingEntry) {
      return matchingEntry.parsed;
    } else {
      let parsed;
      try {
        parsed = yaml.safeLoad(yamlString);
      } catch (error) {
        parsed = null;
      }
      this.cache[this.writeIndex] = { yamlString, parsed };
      this.writeIndex = (this.writeIndex + 1) % this.size;
      return parsed;
    }
  };

  dump = (parsed) => {
    let yamlString = yaml.dump(parsed);
    let matchingEntry = this.cache.find((entry) => entry.yamlString === yamlString);
    if (matchingEntry === undefined) {
      this.cache[this.writeIndex] = { yamlString, parsed };
      this.writeIndex = (this.writeIndex + 1) % this.size;
    }
    return yamlString;
  };
}

class YamlParser extends React.Component {
  constructor() {
    super();
    this.state = {
      showWarn: false,
    };
  }

  parser = new YamlParserCache(10);

  isNormalized = () => {
    let parsed = this.parser.parse(this.props.yamlConfig);
    return parsed !== null && this.parser.dump(parsed) === this.props.yamlConfig;
  };

  onUpdate = (config) => {
    const updatedYamlConfig = this.parser.dump(config);
    const oldConfigNormalized = this.isNormalized();

    if (oldConfigNormalized) {
      this.props.onUpdate(updatedYamlConfig);
    } else {
      this.setState({
        showWarn: true,
        pendingConfigUpdate: updatedYamlConfig,
      });
    }
  };

  normalizedDoAccept = () => {
    const { pendingConfigUpdate } = this.state;
    this.setState({
      showWarn: false,
      pendingConfigUpdate: undefined,
    });
    this.props.onUpdate(pendingConfigUpdate);
  };

  normalizedDoDecline = () =>
    this.setState({
      showWarn: false,
      pendingConfigUpdate: undefined,
    });

  render() {
    const { showWarn } = this.state;
    const config = this.parser.parse(this.props.yamlConfig);
    const isError = config === null;

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

YamlParser.propTypes = {
  /** yaml config as string */
  yamlConfig: PropTypes.string,
  /** function to update the yaml config */
  onUpdate: PropTypes.func,
};

YamlParser.defaultProps = {};

export default YamlParser;
