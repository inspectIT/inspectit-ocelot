import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { InputSwitch } from 'primereact/inputswitch';
import { Button } from 'primereact/button';
import yaml from 'js-yaml';
import _ from 'lodash';

/**
 * GUI editor for creating method/configurations.
 */
const MethodConfigurationEditor = ({ yamlConfiguration }) => {
  // state variables
  const [scopes, setScopes] = useState([]);
  const [expandedRows, setExpandedRows] = useState([]);
  const [scopeStates, setScopeStates] = useState({});

  useEffect(() => {
    // parse configuration
    const configuration = yaml.safeLoad(yamlConfiguration);
    
    // collect existing scopes from the configuration
    const scopeObjects = _.get(configuration, 'inspectit.instrumentation.scopes');
    const currentScopes = _.map(scopeObjects, (value, key) => {
      const { type, superclass, interfaces } = value;
      return {
        typeKey: JSON.stringify(type) + '|' + JSON.stringify(superclass) + '|' + JSON.stringify(interfaces),
        name: key,
        scope: value,
      };
    });
    setScopes(currentScopes);

    // expand all rows by default
    const initialExpandedRows = _.reduce(
      currentScopes,
      (result, { typeKey }) => {
        result[typeKey] = true;
        return result;
      },
      {}
    );
    setExpandedRows(initialExpandedRows);
  }, []);

  const setScopeStateAttribute = (scopeName, attribute, value) => {
    const scopeState = _.get(scopeStates, scopeName, {});
    setScopeStates({
      ...scopeStates,
      [scopeName]: {
        ...scopeState,
        [attribute]: value,
      },
    });
  };

  const rowGroupHeaderTemplate = ({ typeKey }) => {
    return <>{typeKey}</>;
  };

  const scopeDescriptionBodyTemplate = ({ scope }) => {
    return <>{JSON.stringify(scope)}</>;
  };

  const scopeStateBodyTemplate = (scopeName, stateAttribute) => {
    const scopeState = _.get(scopeStates, scopeName, {});

    return (
      <InputSwitch
        checked={scopeState[stateAttribute]}
        onChange={(e) => {
          setScopeStateAttribute(scopeName, stateAttribute, e.value);
        }}
      />
    );
  };

  const scopeEditBodyTemplate = ({ name }) => {
    return (
      <div align="right">
        <Button icon="pi pi-pencil" />
        <Button icon="pi pi-trash" />
      </div>
    );
  };

  return (
    <>
      <style jsx>{`
        .this :global(.p-datatable) :global(th) {
          border: 0 none;
          text-align: left;
        }
      `}</style>
      <div className="this">
        <DataTable
          value={scopes}
          rowHover
          dataKey="typeKey"
          sortField="typeKey"
          sortOrder={1}
          groupField="typeKey"
          expandableRowGroups={true}
          expandedRows={expandedRows}
          onRowToggle={(e) => setExpandedRows(e.data)}
          rowGroupHeaderTemplate={rowGroupHeaderTemplate}
          rowGroupFooterTemplate={() => <></>}
          rowGroupMode="subheader"
        >
          <Column body={scopeDescriptionBodyTemplate} />
          <Column body={({ name }) => scopeStateBodyTemplate(name, 'tracing')} header="Trace" style={{ width: '6rem' }}></Column>
          <Column body={({ name }) => scopeStateBodyTemplate(name, 'measure')} header="Measure" style={{ width: '6rem' }}></Column>
          <Column body={scopeEditBodyTemplate} style={{ width: '8rem' }}></Column>
        </DataTable>
      </div>
    </>
  );
};

MethodConfigurationEditor.propTypes = {
  yamlConfiguration: PropTypes.string,
};

MethodConfigurationEditor.defaultProps = {
  yamlConfiguration: null,
};

export default MethodConfigurationEditor;
