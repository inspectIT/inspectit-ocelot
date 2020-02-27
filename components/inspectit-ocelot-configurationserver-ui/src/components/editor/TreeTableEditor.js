import { cloneDeep, isEqual, set, unset } from 'lodash';
import { Button } from 'primereact/button';
import { Column } from 'primereact/column';
import { ColumnGroup } from 'primereact/columngroup';
import { InputText } from 'primereact/inputtext';
import { Menubar } from 'primereact/menubar';
import { Message } from 'primereact/message';
import { Row } from 'primereact/row';
import { TreeTable } from 'primereact/treetable';
import PropTypes from 'prop-types';
import React from 'react';

// helper for a schema property type constants
const schemaType = {
  COMPOSITE: 'COMPOSITE',
  STRING: 'STRING',
  INTEGER: 'INTEGER',
  FLOAT: 'FLOAT',
  BOOLEAN: 'BOOLEAN',
  DURATION: 'DURATION',
  ENUM: 'ENUM',
};

const booleanDropdownOptions = [
  { label: 'Yes', value: true },
  { label: 'No', value: false },
];

/**
 * Editor for showing the config file as the table tree.
 *
 * TODO what about duration
 * TODO what about enums (select box, but not used)
 * TODO what about the multiline strings
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

  constructor(props) {
    super(props);

    this.state = {
      data: undefined,
      expandedKeys: { inspectit: true },
      showAll: true,
    };
  }

  componentDidMount() {
    this.regenerateData();
  }

  componentDidUpdate(prevProps) {
    if (!isEqual(prevProps.config, this.props.config)) {
      this.regenerateData();
    }
  }

  /**
   * Processes the config json against the schema and creates the data for the tree table.
   * In addition collects all key ids in order to have them all expanded by default.
   */
  regenerateData = () => {
    const allKeys = {};
    if (this.props.config) {
      const data = this.processKey(this.props.config, [this.props.schema], allKeys);
      this.setState({
        data,
        expandedKeys: allKeys,
      });
    } else {
      this.setState({
        data: undefined,
        expandedKeys: allKeys,
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
      const processedKeys = [];
      // if config exists, process it first
      if (config) {
        // go over all keys of a config object
        Object.keys(config).forEach((congfigKey) => {
          processedKeys.push(congfigKey);

          // resolve value and the matching schema properties
          const configValue = config[congfigKey];
          const schemaProperty = schemaObjects.find((s) => s.propertyName === congfigKey);

          // if we found schema properties create data and push to resulting array
          if (schemaProperty) {
            const isComposite = this.isComposite(schemaProperty);
            const keyIdentifier = this.getKeyIdentifier(schemaProperty.propertyName, parentKeyIdentifier);
            const children =
              (isComposite && this.processKey(configValue, schemaProperty.children, keysCollector, keyIdentifier)) || undefined;
            (isComposite && this.processKey(configValue, schemaProperty.children, keysCollector, keyIdentifier)) || undefined;

            const data = {
              key: keyIdentifier,
              schemaProperty,
              value: configValue,
              nullable: schemaProperty.nullable,
              selectable: !isComposite,
              data: {
                name: schemaProperty.readableName,
                type: this.getReadableDataType(schemaProperty.type),
                value: this.getReabableDataValue(configValue, schemaProperty.type),
                nullable: (!isComposite && this.getBoolanRepresentation(schemaProperty.nullable)) || '',
            },
              children,
            };
            result.push(data);
            keysCollector[keyIdentifier] = true;
          } else {
            const keyIdentifier = this.getKeyIdentifier(congfigKey, parentKeyIdentifier);
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

      // then go over remaining schema objects to fill rest on this level
      if (this.state.showAll) {
        schemaObjects
          .filter((schemaProperty) => processedKeys.indexOf(schemaProperty.propertyName) === -1)
          .forEach((schemaProperty) => {
            const isComposite = this.isComposite(schemaProperty);
            const keyIdentifier = this.getKeyIdentifier(schemaProperty.propertyName, parentKeyIdentifier);
            const children =
              (isComposite && this.processKey(undefined, schemaProperty.children, keysCollector, keyIdentifier)) || undefined;

            const data = {
              key: keyIdentifier,
              schemaProperty,
              nullable: schemaProperty.nullable,
              selectable: false,
              data: {
                name: schemaProperty.readableName,
                type: this.getReadableDataType(schemaProperty.type),
                value: (!isComposite && 'Inherited') || '',
                nullable: (!isComposite && this.getBoolanRepresentation(schemaProperty.nullable)) || '',
              },
              children,
            };
            result.push(data);
          });
      }

    return result.sort((r1, r2) => r1.data.name.localeCompare(r2.data.name));
  };

  getKeyIdentifier = (propertyName, parentKeyIdentifier) =>
    parentKeyIdentifier !== undefined ? parentKeyIdentifier + '.' + propertyName : propertyName;

  isComposite = (schemaProperty) => schemaProperty.type === schemaType.COMPOSITE;

  capitalize = (string) => string.charAt(0).toUpperCase() + string.slice(1).toLowerCase();

  getReadableDataType = (type) => (type !== schemaType.COMPOSITE ? type.charAt(0) + type.slice(1).toLowerCase() : '');

  getDataType = (type) => (type !== schemaType.COMPOSITE ? this.capitalize(type) : '');

  getReabableDataValue = (value, type) => {
    switch (type) {
      case schemaType.BOOLEAN:
        return this.getBoolanRepresentation(value);
      case schemaType.COMPOSITE:
        return '';
      case schemaType.STRING:
        if (value === null) {
          return 'null';
        } else {
          return '"' + value + '"';
        }
      default:
        if (value === null) {
          return 'null';
        } else {
          return value;
    }
  };

  getBoolanRepresentation = (b) => (b === true ? 'Yes' : b === false ? 'No' : b);

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
    {
      label: 'Hide Not Defined',
      icon: 'pi pi-minus',
      disabled: !this.state.showAll,
      command: () => this.setState({ showAll: false }, this.regenerateData),
    },
    {
      label: 'Show All Properties',
      icon: 'pi pi-bars',
      disabled: this.state.showAll,
      command: () => this.setState({ showAll: true }, this.regenerateData),
    },
  ];

  rowClassName = (data) => {
    return {
      'composite-row': !data.selectable,
    };
  };

  wrapWithExtras = (component, { node, defaultSupplier, onPropValueChange, onPropValueRemove }) => {
    const { key, value, nullable } = node;
    const isNull = value === null;
    const isDefined = value !== undefined;

    return (
      <div className="p-inputgroup">
        {!isNull && isDefined && component(isNull, isDefined)}
        {isNull && <div className="value-column edit-text">null</div>}
        {!isDefined && <div className="value-column edit-text composite-row">Inherited</div>}
        {nullable && (
          <>
            {!isNull && <Button label="Null" onClick={() => onPropValueChange(key, null)} autoFocus />}
            {isNull && <Button label="Not null" onClick={() => onPropValueChange(key, defaultSupplier())} />}
          </>
        )}
        {isDefined && <Button label="Unset" className="p-button-danger" onClick={() => onPropValueRemove(key)} />}
        {!isDefined && <Button label="Set" className="p-button-success" onClick={() => onPropValueChange(key, defaultSupplier())} />}
      </div>
    );
  };

  onPropValueChange = (key, value) => {
    this.onPropValueUpdate(key, value, false);
  };

  onPropValueRemove = (key) => {
    this.onPropValueUpdate(key, undefined, true);
  };

  onPropValueUpdate = (key, value, doUnset) => {
    // TODO no fire if no update
    const updated = cloneDeep(this.props.config);
    if (doUnset) {
      unset(updated, key);
    } else {
      set(updated, key, value);
    }
    this.props.onUpdate(updated);
  };

  /** returns editor based on the property to be edited */
  getEditor = (props) => {
    // if we are in read only mode return no-editor
    const { readOnly } = this.props;
    const { node } = props;

    if (readOnly) {
      return <NoEditor node={node} readOnly />;
    }

    const noEditor = <NoEditor node={node} />;

    if (!node.schemaProperty) {
      return noEditor;
    }

    // otherwise return the concrete editor if type is supported
    const { type } = node.schemaProperty;
    switch (type) {
      case schemaType.STRING:
      case schemaType.DURATION:
        return (
          <StringEditor
            node={node}
            onPropValueChange={this.onPropValueChange}
            onPropValueRemove={this.onPropValueRemove}
            wrapWithExtras={this.wrapWithExtras}
          />
        );
      case schemaType.INTEGER:
        return (
          <NumberEditor
            node={node}
            integer
            onPropValueChange={this.onPropValueChange}
            onPropValueRemove={this.onPropValueRemove}
            wrapWithExtras={this.wrapWithExtras}
          />
        );
      case schemaType.FLOAT:
        return (
          <NumberEditor
            node={node}
            onPropValueChange={this.onPropValueChange}
            onPropValueRemove={this.onPropValueRemove}
            wrapWithExtras={this.wrapWithExtras}
          />
        );
      case schemaType.BOOLEAN:
        return (
          <BooleanEditor
            node={node}
            onPropValueChange={this.onPropValueChange}
            onPropValueRemove={this.onPropValueRemove}
            wrapWithExtras={this.wrapWithExtras}
          />
        );
      default:
        return noEditor;
    }
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
            min-width: 760px;
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
          .this :global(.edit-text) {
            align-self: center;
            align-items: center;
            margin-inline-end: 1em;
          }
          .this :global(.p-treetable .p-treetable-tbody > tr > td.p-cell-editing .p-button) {
            width: unset;
            min-width: 96px;
          }
        `}</style>
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
          <Column field="value" className="value-column" editor={this.getEditor} />
          <Column field="nullable" style={{ width: '200px' }} />
          <Column field="type" style={{ width: '200px' }} />
        </TreeTable>
        <Menubar model={this.menuItems()} />
      </div>
    );
  }
}

/** Editor for string values */
const StringEditor = ({ node, onPropValueChange, onPropValueRemove, wrapWithExtras }) => {
  const defaultValue = node.value;

  // fire update on change
  const onChange = (e) => {
    let updateValue = e.target.value;
    onPropValueChange(node.key, updateValue);
  };

  // component to render
  const component = (isNull) => (
    <InputText type="text" defaultValue={defaultValue} onChange={onChange} className="value-column" disabled={isNull} />
  );
  // in case of no wrapping return only component
  // this is useful when combining into other editors
  if (!wrapWithExtras) {
    return component(defaultValue === null);
  } else {
    return wrapWithExtras(component, {
      node,
      defaultSupplier: () => (defaultValue && defaultValue) || '',
      onPropValueChange,
      onPropValueRemove,
    });
  }
};
/** Editor for numbers */
const NumberEditor = ({ node, integer, onPropValueChange, onPropValueRemove, wrapWithExtras }) => {
  const defaultValue = node.value;

  // if we can not parse as int or float, fire the received value
  const onChange = (e) => {
    let updateValue = e.target.value;
    if (updateValue.length === 0) {
      updateValue = 0;
    } else {
      const parsed = integer ? parseInt(updateValue) : parseFloat(updateValue);
      if (!isNaN(parsed)) {
        updateValue = parsed;
      }
    }
    onPropValueChange(node.key, updateValue);
  };

  // component to render
  const component = () => (
    <InputText keyfilter={/^[a-z0-9{}$\.-]+$/} defaultValue={defaultValue} onChange={onChange} className="value-column" />
  );

  if (!wrapWithExtras) {
    return component();
  } else {
    return wrapWithExtras(component, {
      node,
      defaultSupplier: () => (defaultValue && defaultValue) || 0,
      onPropValueChange,
      onPropValueRemove,
    });
  }
};

/** Editor for booleans */
const BooleanEditor = ({ node, onPropValueChange, onPropValueRemove, wrapWithExtras }) => {
  const defaultValue = node.value;
  const custom = defaultValue !== null && defaultValue !== undefined && typeof defaultValue !== 'boolean';

  // component to render
  const component = () => (
    <>
      {!custom && (
        <>
          <Button label="Yes" onClick={() => onPropValueChange(node.key, true)} className={defaultValue !== true && 'p-button-secondary'} />
          <Button
            label="No"
            onClick={() => onPropValueChange(node.key, false)}
            className={defaultValue !== false && 'p-button-secondary'}
          />
          <Button label="Custom" onClick={() => onPropValueChange(node.key, '')} className={'p-button-secondary'} />
        </>
      )}
      {custom && (
        <>
          <StringEditor node={node} onPropValueChange={onPropValueChange} onPropValueRemove={onPropValueRemove} />
          <Button label="Yes/No" onClick={() => onPropValueChange(node.key, true)} className={'p-button-secondary'} />
        </>
      )}
    </>
  );

  if (!wrapWithExtras) {
    return component();
  } else {
    return wrapWithExtras(component, {
      node,
      defaultSupplier: () => (defaultValue && defaultValue) || true,
      onPropValueChange,
      onPropValueRemove,
    });
  }
};

/** No editor - show value only, if not readable then a small warn message is displayed that the data is not editable */
const NoEditor = ({ node, readOnly }) => {
  const value = node.data['value'];

  return (
    <div className="p-grid p-nogutter">
      <div className="p-col">{(value && value) || ''}</div>
      {value && !readOnly && (
        <div className="p-col">
          <Message severity="warn" text="Not editable"></Message>
        </div>
      )}
    </div>
  );
};

TreeTableEditor.propTypes = {
  /** The configuration object */
  config: PropTypes.object,
  /** The config file schema */
  schema: PropTypes.object,
  /** If there is loading in progress */
  loading: PropTypes.bool,
  /** If it's read only */
  readOnly: PropTypes.bool,
  /** Function to invoke for full config update */
  onUpdate: PropTypes.func,
};

TreeTableEditor.defaultProps = {
  loading: false,
};

export default TreeTableEditor;
