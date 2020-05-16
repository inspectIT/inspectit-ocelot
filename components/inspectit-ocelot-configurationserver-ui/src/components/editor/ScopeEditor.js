import { Checkbox } from 'primereact/checkbox';
import { Column } from "primereact/column";
import { ColumnGroup } from 'primereact/columngroup';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { Menubar } from 'primereact/menubar';
import { Message } from 'primereact/message';
import { Row } from 'primereact/row';
import { TreeTable } from 'primereact/treetable';
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import lodash from 'lodash';
import { Dropdown } from 'primereact/dropdown';



// helper for a schema property type constants
const schemaType = {
    COMPOSITE: 'COMPOSITE',
    STRING: 'STRING',
    INTEGER: 'INTEGER',
    FLOAT: 'FLOAT',
    BOOLEAN: 'BOOLEAN',
    DURATION: 'DURATION',
    ENUM: 'ENUM'
}


const booleanDropdownOptions = [
    { label: 'Yes', value: true },
    { label: 'No', value: false }
];


/**
 * Editor for showing the config file as the table tree.
 * 
 * TODO what about duration
 * TODO what about enums (select box, but not used)
 * TODO what about the depending props - f.e. ${inspectit.something.something}
 * TODO what about the multiline strings
 */
class ScopeEditor extends React.Component {

    // headerGroup = (
    //     <ColumnGroup>
    //         <Row>
    //             <Column header="Property name" />
    //             <Column header="Value" />
    //             <Column header="Nullable" style={{ width: '200px' }} />
    //             <Column header="Type" style={{ width: '200px' }} />
    //         </Row>
    //     </ColumnGroup>
    // );

    constructor(props) {
        super(props);

        this.state = {
            data: undefined,
            expandedKeys: { 'inspectit': true },
            showAll: true
        }
    }

    // componentDidMount() {
    //     this.regenerateData();
    // }

    // componentDidUpdate(prevProps) {
    //     if (!lodash.isEqual(prevProps.config, this.props.config)) {
    //         this.regenerateData();
    //     }
    // }

    // regenerateData = () => {
    //     const allKeys = {};
    //     if (this.props.config) {
    //         const data = this.processKey(this.props.config, [this.props.schema], allKeys);
    //         this.setState({
    //             data,
    //             expandedKeys: allKeys
    //         });
    //     } else {
    //         this.setState({
    //             data: undefined,
    //             expandedKeys: allKeys
    //         });
    //     }
    // }

    // /**
    //  * Recursive method that returns the array of data to be supplied to the tree table for one config key.
    //  * 
    //  * @param config Currently processed configiguration part (from config YAML)
    //  * @param schemaObjects Array of schema object that could correspond to the given config key
    //  * @param keysCollector Map to add all keys of created data elements
    //  * @param parentKeyIdentifier String identifier of the parent key or undifined for root key
    //  */
    // processKey = (config, schemaObjects, keysCollector, parentKeyIdentifier) => {
    //     const result = [];

    //     // continue if the schema object has elements only
    //     if (schemaObjects && schemaObjects.length > 0) {
    //         const processedKeys = [];

    //         // if config exists, process it first
    //         if (config) {
    //             // go over all keys of a config object
    //             Object.keys(config).forEach(congfigKey => {
    //                 processedKeys.push(congfigKey);

    //                 // resolve value and the matching schema properties
    //                 const configValue = config[congfigKey];
    //                 const schemaProperty = schemaObjects.find(s => s.propertyName === congfigKey);

    //                 // if we found schema properties create data and push to resulting array
    //                 if (schemaProperty) {
    //                     const isComposite = schemaProperty.type === schemaType.COMPOSITE;
    //                     const keyIdentifier = parentKeyIdentifier !== undefined ? parentKeyIdentifier + "." + schemaProperty.propertyName : schemaProperty.propertyName;
    //                     const children = isComposite && this.processKey(configValue, schemaProperty.children, keysCollector, keyIdentifier) || undefined;

    //                     const data = {
    //                         key: keyIdentifier,
    //                         schemaProperty,
    //                         value: configValue,
    //                         nullable: schemaProperty.nullable,
    //                         selectable: !isComposite,
    //                         data: {
    //                             name: schemaProperty.readableName,
    //                             type: this.getReadableDataType(schemaProperty.type),
    //                             value: this.getReabableDataValue(configValue, schemaProperty.type),
    //                             nullable: !isComposite && this.getBoolanRepresentation(schemaProperty.nullable) || ''
    //                         },
    //                         children
    //                     }
    //                     result.push(data);
    //                     keysCollector[keyIdentifier] = true;
    //                 } else {
    //                     const keyIdentifier = parentKeyIdentifier !== undefined ? parentKeyIdentifier + "." + congfigKey : congfigKey;
    //                     const data = {
    //                         key: keyIdentifier,
    //                         selectable: false,
    //                         data: {
    //                             name: this.capitalize(congfigKey),
    //                             type: 'n/a',
    //                             value: 'Not supported',
    //                             nullable: 'n/a'
    //                         },
    //                         children: []
    //                     }
    //                     result.push(data);
    //                 }
    //             });
    //         }

    //         // then go over remaining schema objects to fill rest on this level
    //         if (this.state.showAll) {
    //             schemaObjects
    //                 .filter(schemaProperty => processedKeys.indexOf(schemaProperty.propertyName) === -1)
    //                 .forEach(schemaProperty => {
    //                     const isComposite = schemaProperty.type === schemaType.COMPOSITE;
    //                     const keyIdentifier = parentKeyIdentifier !== undefined ? parentKeyIdentifier + "." + schemaProperty.propertyName : schemaProperty.propertyName;
    //                     const children = isComposite && this.processKey(undefined, schemaProperty.children, keysCollector, keyIdentifier) || undefined;

    //                     const data = {
    //                         key: keyIdentifier,
    //                         schemaProperty,
    //                         nullable: schemaProperty.nullable,
    //                         selectable: false,
    //                         data: {
    //                             name: schemaProperty.readableName,
    //                             type: this.getReadableDataType(schemaProperty.type),
    //                             value: 'Inherited',
    //                             nullable: !isComposite && this.getBoolanRepresentation(schemaProperty.nullable) || ''
    //                         },
    //                         children
    //                     }
    //                     result.push(data);
    //                 });
    //         }
    //     }

    //     return result;
    // }

    // capitalize = (string) => string.charAt(0).toUpperCase() + string.slice(1).toLowerCase();

    // getReadableDataType = (type) => type !== schemaType.COMPOSITE ? type.charAt(0) + type.slice(1).toLowerCase() : "";

    // getDataType = (type) => type !== schemaType.COMPOSITE ? this.capitalize(type) : "";

    // getReabableDataValue = (value, type) => {
    //     switch (type) {
    //         case schemaType.BOOLEAN: return this.getBoolanRepresentation(value);
    //         case schemaType.COMPOSITE: return ""
    //         default:
    //             if (value === null) return 'null'
    //             else return value
    //     }
    // }

    // getBoolanRepresentation = (b) => b === true ? 'Yes' : b === false ? 'No' : b;

    // expandAll = () => {
    //     const expandedKeys = {};
    //     this.expandKeys(this.state.data, expandedKeys);
    //     this.setState({ expandedKeys });
    // }

    // expandKeys = (data, map) => {
    //     if (data) {
    //         data.forEach(d => {
    //             map[d.key] = true;
    //             this.expandKeys(d.children, map);
    //         });
    //     }
    // }

    menuItems = () => [
        {
            label: 'Expand All',
            icon: 'pi pi-chevron-down',
            disabled: this.state.isError,
            command: this.expandAll
        },
        {
            label: 'Collapse All',
            icon: 'pi pi-chevron-up',
            disabled: this.state.isError,
            command: () => this.setState({ expandedKeys: {} })
        },
        {
            label: 'Hide Not Defined',
            icon: 'pi pi-minus',
            disabled: !this.state.showAll,
            command: () => this.setState({ showAll: false }, this.regenerateData)
        },
        {
            label: 'Show All Properties',
            icon: 'pi pi-bars',
            disabled: this.state.showAll,
            command: () => this.setState({ showAll: true }, this.regenerateData)
        }
    ]

    // rowClassName = (data) => {
    //     return {
    //         'composite-row': !data.selectable
    //     }
    // }

    // wrapWithExtras = (component, { node, defaultSupplier, onPropValueChange, onPropValueRemove }) => {
    //     const { key, value, nullable } = node;
    //     const isNull = value === null;
    //     const isDefined = value !== undefined;


    //     return (
    //         <div className="p-inputgroup">
    //             {
    //                 !isNull && isDefined &&
    //                 component(isNull, isDefined)
    //             }
    //             {
    //                 isNull && <div className="value-column edit-text">null</div>
    //             }
    //             {
    //                 !isDefined && <div className="value-column edit-text composite-row">Inherited</div>
    //             }
    //             {
    //                 nullable &&
    //                 <>
    //                     {
    //                         !isNull && <Button label="Null" onClick={() => onPropValueChange(key, null)} autoFocus />
    //                     }
    //                     {
    //                         isNull && <Button label="Not null" onClick={() => onPropValueChange(key, defaultSupplier())} />
    //                     }
    //                 </>
    //             }
    //             {
    //                 isDefined && <Button label="Unset" className="p-button-danger" onClick={() => onPropValueRemove(key)} />
    //             }
    //             {
    //                 !isDefined && <Button label="Set" className="p-button-success" onClick={() => onPropValueChange(key, defaultSupplier())} />
    //             }
    //         </div>
    //     )
    // }

    // /** Editor for string values */
    // StringEditor = ({ node, onPropValueChange, onPropValueRemove }) => {
    //     const defaultValue = node.value;

    //     // when getting empty value in the text input
    //     // we eaither set it to null if possible or use the last valid value
    //     const onChange = (e) => {
    //         let updateValue = e.target.value;
    //         onPropValueChange(node.key, updateValue);
    //     }

    //     return this.wrapWithExtras(
    //         (isNull, _) => (<InputText type="text" defaultValue={defaultValue} onChange={onChange} className="value-column" disabled={isNull} />),
    //         {
    //             node,
    //             defaultSupplier: () => defaultValue && defaultValue || '',
    //             onPropValueChange,
    //             onPropValueRemove
    //         }
    //     );
    // }

    // /** Editor for numbers */
    // NumberEditor = ({ node, integer, onPropValueChange, onPropValueRemove }) => {
    //     const defaultValue = node.value;

    //     // when getting empty value in the text input
    //     // we eaither set it to null if possible or use the last valid value
    //     const onChange = (e) => {
    //         let updateValue = e.target.value;
    //         if (updateValue.length === 0) {
    //             updateValue = 0;
    //         } else {
    //             const parsed = integer ? parseInt(updateValue) : parseFloat(updateValue);
    //             if (!isNaN(parsed)) {
    //                 updateValue = parsed;
    //             }
    //         }
    //         onPropValueChange(node.key, updateValue);
    //     }

    //     return this.wrapWithExtras(
    //         (isNull, _) => (<InputText keyfilter={/^[a-z0-9{}$\.-]+$/} defaultValue={defaultValue} onChange={onChange} className="value-column" />),
    //         {
    //             node,
    //             defaultSupplier: () => defaultValue && defaultValue || 0,
    //             onPropValueChange,
    //             onPropValueRemove
    //         }
    //     );
    // }

    // /** Editor for booleans */
    // BooleanEditor = ({ node, onPropValueChange, onPropValueRemove }) => {
    //     const defaultValue = node.value;
    //     const [value, setValue] = useState(defaultValue);

    //     // if a boolean can be nullable, then we allow the unchecking of the checkbox
    //     const onChange = (e) => {
    //         const value = e.value;
    //         setValue(value);
    //         onPropValueChange(node.key, value);
    //     };

    //     return this.wrapWithExtras(
    //         (isNull, _) => (<Dropdown value={value} options={booleanDropdownOptions} onChange={onChange} editable={true} placeholder="Select the value" disabled={isNull} />),
    //         {
    //             node,
    //             defaultSupplier: () => defaultValue && defaultValue || '',
    //             onPropValueChange,
    //             onPropValueRemove
    //         }
    //     );
    // }

    // /** No editor - show value only, if not readable then a small warn message is displayed that the data is not editable */
    // NoEditor = ({ node, readOnly }) => {
    //     const value = node.data['value'];
    //     return (
    //         <div className="p-grid p-nogutter">
    //             <div className="p-col">{value && value || ''}</div>
    //             {
    //                 value && !readOnly &&
    //                 <div className="p-col"><Message severity="warn" text="Not editable"></Message></div>
    //             }
    //         </div>
    //     )
    // }

    // onPropValueChange = (key, value) => {
    //     this.onPropValueUpdate(key, value, false);
    // }

    // onPropValueRemove = (key) => {
    //     this.onPropValueUpdate(key, undefined, true);
    // }

    // onPropValueUpdate = (key, value, unset) => {
    //     // TODO no fire if no update
    //     // TODO unset option
    //     const updated = lodash.cloneDeep(this.props.config);
    //     if (unset) {
    //         lodash.unset(updated, key)
    //     } else {
    //         lodash.set(updated, key, value);
    //     }
    //     this.props.onUpdate(updated);
    // }

    // /** returns editor based on the property to be edited */
    // getEditor = (props) => {
    //     // if we are in read only mode return no-editor
    //     const { readOnly } = this.props;
    //     const { node } = props;

    //     if (readOnly) {
    //         return <this.NoEditor node={node} readOnly />
    //     }

    //     const noEditor = <this.NoEditor node={node} />;

    //     if (!node.schemaProperty) {
    //         return noEditor;
    //     }

    //     // otherwise return the concrete editor if type is supported
    //     const { type } = node.schemaProperty;
    //     switch (type) {
    //         case schemaType.STRING:
    //         case schemaType.DURATION:
    //             return <this.StringEditor node={node} onPropValueChange={this.onPropValueChange} onPropValueRemove={this.onPropValueRemove} />;
    //         case schemaType.INTEGER: return <this.NumberEditor node={node} integer onPropValueChange={this.onPropValueChange} onPropValueRemove={this.onPropValueRemove} />;
    //         case schemaType.FLOAT: return <this.NumberEditor node={node} onPropValueChange={this.onPropValueChange} onPropValueRemove={this.onPropValueRemove} />;
    //         case schemaType.BOOLEAN: return <this.BooleanEditor node={node} onPropValueChange={this.onPropValueChange} onPropValueRemove={this.onPropValueRemove} />;
    //         default: return noEditor;
    //     }
    // }

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
                .this :global(.p-menubar)  {
                    background-color: #f4f4f4;
                }
                .this :global(.p-menuitem-text)  {
                    font-size: smaller;
                }
                .this :global(.p-menuitem-icon)  {
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
                .this :global(.composite-row), .this :global(.composite-row) :global(.key-column) {
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
                {/* <TreeTable
                    value={this.state.data}
                    headerColumnGroup={this.headerGroup}
                    autoLayout
                    scrollable
                    scrollHeight="calc(100vh - 13rem)"
                    loading={loading}
                    expandedKeys={this.state.expandedKeys}
                    onToggle={e => this.setState({ expandedKeys: e.value })}
                    rowClassName={this.rowClassName}
                >
                    <Column field="name" className="key-column" expander />
                    <Column field="value" className="value-column" editor={this.getEditor} />
                    <Column field="nullable" style={{ width: '200px' }} />
                    <Column field="type" style={{ width: '200px' }} />
                </TreeTable> */}
                <Menubar model={this.menuItems()} />
            </div>
        );

    }

}

ScopeEditor.propTypes = {
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
}

ScopeEditor.defaultProps = {
    loading: false,
};

export default ScopeEditor;
