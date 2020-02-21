import yaml from 'js-yaml';
import { Column } from "primereact/column";
import { ColumnGroup } from 'primereact/columngroup';
import { Row } from 'primereact/row';
import { TreeTable } from 'primereact/treetable';
import PropTypes from 'prop-types';
import React from 'react';

import {Menubar} from 'primereact/menubar';

/**
 * Editor for showing the config file as the table tree.
 */
class TreeTableEditor extends React.Component {

    constructor() {
        super();

        this.state = {
            isError: false,
            data: undefined,
            expandedKeys: { 'inspectit': true }
        }
    }

    componentDidMount() {
        this.regenerateData();
    }

    componentDidUpdate(prevProps) {
        console.log(this.state.expandedKeys);
        if (prevProps.value !== this.props.value) {
            this.regenerateData();
        }
    }

    regenerateData = () => {
        try {
            const config = yaml.safeLoad(this.props.value);
            const data = this.processKey(config, [this.props.schema]);
            this.setState({ 
                isError: false,
                data
            })
        } catch (error) {
            this.setState({ 
                isError: true,
                data: undefined
            })
        }
    }

    processKey = (configKey, schemaObjects, parentKey) => {
        const result = [];

        // continue if the schema object has elements only
        if (schemaObjects && schemaObjects.length > 0) {

            // go over all keys of a config object
            Object.keys(configKey).forEach(k => {
                // resolve value and the matching schema properties
                const v = configKey[k];
                const schemaProperties =  schemaObjects.find(s => s.propertyName === k);

                // if we found schema properties create data and push to resulting array
                if (schemaProperties) {
                    const isComposite = schemaProperties.type === "COMPOSITE";
                    const key = parentKey !== undefined ? parentKey + "." + schemaProperties.propertyName : schemaProperties.propertyName;
                    const children = isComposite && this.processKey(v, schemaProperties.children, key) || undefined;

                    const data = {
                        key,
                        selectable: !isComposite,
                        data: {
                            name: schemaProperties.readableName,
                            type: this.getDataType(schemaProperties.type),
                            value: this.getDataValue(v, schemaProperties.type),
                            nullable: !isComposite && this.getBoolanRepresentation(schemaProperties.nullable) || ''
                        },
                        children
                    }
                    result.push(data);
                }
            });
        }

        return result;
    }

    getDataType = (type) => type !== "COMPOSITE" ? type.charAt(0) + type.slice(1).toLowerCase() : "";

    getDataValue = (value, type) => {
        switch(type){
            case "BOOLEAN": return this.getBoolanRepresentation(value);
            case "COMPOSITE": return ""
            default: return value ? value : 'null'
        }
    }

    getBoolanRepresentation = (b) => b ? 'Yes' : 'No';

    expandAll = () => {
        const expandedKeys = {};
        this.expandKeys(this.state.data, expandedKeys);
        this.setState({ expandedKeys });
    }

    expandKeys = (data, map) => {
        if (data) {
            data.forEach(d => {
                map[d.key] = true;
                this.expandKeys(d.children, map);
            });
        }
    }

    headerGroup = (
        <ColumnGroup>
            <Row>
                <Column header="Property name" />
                <Column header="Value" />
                <Column header="Nullable?" />
                <Column header="Type" />
            </Row>
        </ColumnGroup>
    );

    menuItems = () => [
        {
            label:'Expand All',
            icon:'pi pi-chevron-down',
            disabled: this.state.isError,
            command: this.expandAll
        },
        {
            label:'Collapse All',
            icon:'pi pi-chevron-up',
            disabled: this.state.isError,
            command: () => this.setState({ expandedKeys: {}})
        }
    ]

    render() {
        const { loading } = this.props;

        return (
            <div className="this">
                <style jsx>{`
                .this {
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
                `}</style>
                {
                    !this.state.isError &&
                    <>
                        <TreeTable 
                            value={this.state.data} 
                            headerColumnGroup={this.headerGroup} 
                            scrollable 
                            scrollHeight="40vh" 
                            autoLayout
                            loading={loading}
                            expandedKeys={this.state.expandedKeys}
                            onToggle={e => this.setState({ expandedKeys: e.value })}
                        >
                            <Column field="name" expander />
                            <Column field="value" />
                            <Column field="nullable" />
                            <Column field="type" />
                        </TreeTable>
                        <Menubar model={this.menuItems()} />
                    </>
                }
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
}

TreeTableEditor.defaultProps = {
    loading: false,
};

export default TreeTableEditor;


