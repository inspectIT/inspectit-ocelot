import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { InputSwitch } from 'primereact/inputswitch';
import { Button } from 'primereact/button';
import yaml from 'js-yaml';
import _ from 'lodash';
import SelectionInformation from '../SelectionInformation';
import ErrorInformation from '../ErrorInformation';
import { TOOLTIP_OPTIONS } from '../../../data/constants';
import { selectedFileContentsChanged } from '../../../redux/ducks/configuration/actions';
import { useDispatch } from 'react-redux';

const SCOPE_STATES_RULES = {
  TRACING: 'r_method_configuration_trace',
  MEASURING: 'r_method_configuration_duration',
};

/**
 * GUI editor for creating method/configurations.
 */
const MethodConfigurationEditor = ({ yamlConfiguration }) => {
  const dispatch = useDispatch();

  // state variables
  const [scopes, setScopes] = useState([]);
  const [expandedRows, setExpandedRows] = useState([]);
  const [configurationError, setConfigurationError] = useState(null);
  const [configuration, setConfiguration] = useState([]);

  // derrived variables
  const scopesExist = scopes.length > 0;

  useEffect(() => {
    // parse configuration
    try {
      setConfiguration(yaml.safeLoad(yamlConfiguration));
    } catch (error) {
      setConfigurationError(error);
      setConfiguration(null);
    }
  }, [yamlConfiguration]);

  useEffect(() => {
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
  }, [configuration]);

  /**
   * Updates the current configuration file. Changes the given path to the given value.
   *
   * @param {*} ruleScopePath The path to the scope inside a specific rule which should be changed.
   * @param {*} value The value to be set under the given objectPath.
   */
  const updateConfigurationFile = (ruleScopePath, value) => {
    try {
      const cloneConfiguration = _.cloneDeep(configuration);

      if (value === false) {
        _.unset(cloneConfiguration, ruleScopePath, value);
      } else {
        _.set(cloneConfiguration, ruleScopePath, value);
      }

      const updatedYamlConfiguration = '# {"type": "Method-Configuration"}\n' + yaml.dump(cloneConfiguration);

      dispatch(selectedFileContentsChanged(updatedYamlConfiguration));
    } catch (error) {
      console.error('Configuration could not been updated.', error);
    }
  };

  /**
   * Providing the template body for the row group header.
   */
  const rowGroupHeaderTemplate = ({ typeKey }) => {
    return 'Target: ' + typeKey;
  };

  /**
   * Providing the template body for the scopes.
   */
  const scopeDescriptionBodyTemplate = ({ scope }) => {
    return JSON.stringify(scope);
  };

  /**
   * Providing a generic body template for a scopes state variables (tracing, measuring, ...).
   * @param {*} scopeName the name of the target scope
   * @param {*} stateRule  the rule to update
   */
  const scopeStateBodyTemplate = (scopeName, stateRule) => {
    const ruleScopePath = 'inspectit.instrumentation.rules.' + stateRule + '.scopes.' + scopeName;
    const ruleState = _.get(configuration, ruleScopePath);

    return (
      <InputSwitch
        checked={ruleState}
        onChange={(e) => {
          updateConfigurationFile(ruleScopePath, e.value);
        }}
      />
    );
  };

  /**
   * Providing the template body for the scope's control buttons.
   */
  const scopeEditBodyTemplate = () => {
    return (
      <div align="right">
        <Button
          icon="pi pi-pencil"
          style={{ marginRight: '0.5rem' }}
          tooltip="Edit Method Configuration"
          tooltipOptions={TOOLTIP_OPTIONS}
        />
        <Button icon="pi pi-trash" tooltip="Remove Method Configuration" tooltipOptions={TOOLTIP_OPTIONS} />
      </div>
    );
  };

  return (
    <>
      <style jsx>{`
        .this {
          display: flex;
          flex-direction: column;
          height: 100%;
        }

        .this :global(.p-datatable th) {
          border: none;
          text-align: left;
        }

        .this :global(.p-datatable tr td) {
          border: none;
        }

        .this :global(.p-datatable tr) {
          border-top: 1px solid #e7e7e7;
        }

        .this :global(.p-datatable .p-rowgroup-header td) {
          padding: 1rem 0.5rem;
        }

        .this :global(.p-datatable-row td:first-of-type) {
          padding-left: 2.25rem;
        }

        /* prevent different backgrounds for even and odd row numbers*/
        .this :global(.p-datatable .p-datatable-tbody > tr) {
          background-color: white;
        }

        .no-content-hint {
          background-color: red;
          flex-grow: 1;
        }
      `}</style>
      <div className="this">
        {configurationError ? (
          <ErrorInformation text="Invalid YAML Configuration" error={configurationError} />
        ) : scopesExist ? (
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
            <Column body={scopeDescriptionBodyTemplate} header="Target" />
            <Column
              body={({ name }) => scopeStateBodyTemplate(name, SCOPE_STATES_RULES.TRACING)}
              header="Trace"
              style={{ width: '6rem' }}
            ></Column>
            <Column
              body={({ name }) => scopeStateBodyTemplate(name, SCOPE_STATES_RULES.MEASURING)}
              header="Measure"
              style={{ width: '6rem' }}
            ></Column>
            <Column body={() => scopeEditBodyTemplate()} style={{ width: '8rem' }}></Column>
          </DataTable>
        ) : (
          <SelectionInformation hint="The configuration is empty." />
        )}
      </div>
    </>
  );
};

MethodConfigurationEditor.propTypes = {
  /** The configuration to show in a YAML representation.*/
  yamlConfiguration: PropTypes.string,
};

MethodConfigurationEditor.defaultProps = {
  yamlConfiguration: null,
};

export default MethodConfigurationEditor;
