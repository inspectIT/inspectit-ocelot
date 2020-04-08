import yaml from 'js-yaml';
import { Column } from 'primereact/column';
import { ColumnGroup } from 'primereact/columngroup';
import { Row } from 'primereact/row';
import { TreeTable } from 'primereact/treetable';
import PropTypes from 'prop-types';
import React from 'react';

import { Menubar } from 'primereact/menubar';

/**
 * Editor for showing the config file as the table tree.
 */
class TreeTableEditor extends React.Component {
  headerGroup = (
    <ColumnGroup>
      <Row>
        <Column header="Property name" />
        <Column header="Value" />
        <Column header="Nullable" style={{ width: '200px' }} />
        <Column header="Type" style={{ width: '200px' }} />
      </Row>
    </ColumnGroup>
  );

  constructor() {
    super();

    this.state = {
      isError: false,
      data: undefined,
      expandedKeys: { inspectit: true },
    };
  }

  componentDidMount() {
    this.regenerateData();
  }

  componentDidUpdate(prevProps) {
    if (prevProps.value !== this.props.value) {
      this.regenerateData();
    }
  }

  regenerateData = () => {
    try {
      const config = yaml.safeLoad(this.props.value);
      const allKeys = {};
      const data = this.processKey(config, [this.props.schema], allKeys);

      this.setState({
        isError: false,
        data,
        expandedKeys: allKeys,
      });
    } catch (error) {
      this.setState({
        isError: true,
        data: undefined,
      });
    }
  };

  /**
   * Recursive method that returns the array of data to be supplied to the tree table for one config key.
   *
   * @param config Currently processed configiguration part (from config YAML)
   * @param schemaObjects Array of schema object that could correspond to the given config key
   * @param keysCollector Map to add all keys of created data elements
   * @param parentKeyIdentifier String identifier of the parent key or undifined for root key
   */
  processKey = (config, schemaObjects, keysCollector, parentKeyIdentifier) => {
    const result = [];

    // continue if the schema object has elements only
    if (schemaObjects && schemaObjects.length > 0) {
      // go over all keys of a config object
      Object.keys(config).forEach((congfigKey) => {
        // resolve value and the matching schema properties
        const configValue = config[congfigKey];
        const schemaProperty = schemaObjects.find((s) => s.propertyName === congfigKey);

        // if we found schema properties create data and push to resulting array
        if (schemaProperty) {
          const isComposite = schemaProperty.type === 'COMPOSITE';
          const keyIdentifier =
            parentKeyIdentifier !== undefined ? parentKeyIdentifier + '.' + schemaProperty.propertyName : schemaProperty.propertyName;
          const children =
            (isComposite && this.processKey(configValue, schemaProperty.children, keysCollector, keyIdentifier)) || undefined;

          const data = {
            key: keyIdentifier,
            selectable: !isComposite,
            data: {
              name: schemaProperty.readableName,
              type: this.getDataType(schemaProperty.type),
              value: this.getDataValue(configValue, schemaProperty.type),
              nullable: (!isComposite && this.getBoolanRepresentation(schemaProperty.nullable)) || '',
            },
            children,
          };
          result.push(data);
          keysCollector[keyIdentifier] = true;
        } else {
          const keyIdentifier = parentKeyIdentifier !== undefined ? parentKeyIdentifier + '.' + congfigKey : congfigKey;
          const data = {
            key: keyIdentifier,
            selectable: false,
            data: {
              name: this.capitalize(congfigKey),
              type: 'n/a',
              value: 'Not supported',
              nullable: 'n/a',
            },
            children: [],
          };
          result.push(data);
        }
      });
    }

    return result;
  };

  capitalize = (string) => string.charAt(0).toUpperCase() + string.slice(1).toLowerCase();

  getDataType = (type) => (type !== 'COMPOSITE' ? this.capitalize(type) : '');

  getDataValue = (value, type) => {
    switch (type) {
      case 'BOOLEAN':
        return this.getBoolanRepresentation(value);
      case 'COMPOSITE':
        return '';
      default:
        return value ? value : 'null';
    }
  };

  getBoolanRepresentation = (b) => (b ? 'Yes' : 'No');

  expandAll = () => {
    const expandedKeys = {};
    this.expandKeys(this.state.data, expandedKeys);
    this.setState({ expandedKeys });
  };

  expandKeys = (data, map) => {
    if (data) {
      data.forEach((d) => {
        map[d.key] = true;
        this.expandKeys(d.children, map);
      });
    }
  };

  menuItems = () => [
    {
      label: 'Expand All',
      icon: 'pi pi-chevron-down',
      disabled: this.state.isError,
      command: this.expandAll,
    },
    {
      label: 'Collapse All',
      icon: 'pi pi-chevron-up',
      disabled: this.state.isError,
      command: () => this.setState({ expandedKeys: {} }),
    },
  ];

  rowClassName = (data) => {
    return {
      'composite-row': !data.selectable,
    };
  };

  render() {
    const { loading } = this.props;

    return (
      <div className="this">
        <style jsx>{`
          .this {
            flex: 1;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
          }
          .this :global(.p-menubar) {
            background-color: #f4f4f4;
          }
          .this :global(.p-menuitem-text) {
            font-size: smaller;
          }
          .this :global(.p-menuitem-icon) {
            font-size: smaller;
          }
          .errorBox {
            align-self: center;
            justify-content: center;
            flex: 1;
            display: flex;
            flex-direction: column;
            color: #bbb;
          }
          .this :global(.composite-row),
          .this :global(.composite-row) :global(.key-column) {
            color: grey;
            font-weight: normal;
          }
          .this :global(.key-column) {
            color: black;
            font-weight: bold;
          }
          .this :global(.value-column) {
            font-family: monospace;
          }
        `}</style>
        {!this.state.isError && (
          <>
            <TreeTable
              value={this.state.data}
              headerColumnGroup={this.headerGroup}
              autoLayout
              scrollable
              scrollHeight="calc(100vh - 13rem)"
              loading={loading}
              expandedKeys={this.state.expandedKeys}
              onToggle={(e) => this.setState({ expandedKeys: e.value })}
              rowClassName={this.rowClassName}
            >
              <Column field="name" className="key-column" expander />
              <Column field="value" className="value-column" />
              <Column field="nullable" style={{ width: '200px' }} />
              <Column field="type" style={{ width: '200px' }} />
            </TreeTable>
            <Menubar model={this.menuItems()} />
          </>
        )}
        {this.state.isError && (
          <div className="errorBox">
            <p>Properties could not be loaded from the YAML content.</p>
          </div>
        )}
      </div>
    );
  }
}

TreeTableEditor.propTypes = {
  /** The value of the editor (config file) */
  value: PropTypes.string,
  /** The config file schema */
  schema: PropTypes.object,
  /** If there is loading in progress */
  loading: PropTypes.bool,
};

TreeTableEditor.defaultProps = {
  loading: false,
};

export default TreeTableEditor;
