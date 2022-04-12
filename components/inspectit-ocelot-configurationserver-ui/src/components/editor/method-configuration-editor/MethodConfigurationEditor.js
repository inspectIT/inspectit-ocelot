import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { InputSwitch } from 'primereact/inputswitch';
import { Button } from 'primereact/button';
import { v4 as uuid } from 'uuid';
import yaml from 'js-yaml';
import _ from 'lodash';
import SelectionInformation from '../SelectionInformation';
import ErrorInformation from '../ErrorInformation';
import ScopeTypeDisplay from './ScopeTypeDisplay';
import ScopeMethodDisplay from './ScopeMethodDisplay';
import MethodConfigurationEditorFooter from './MethodConfigurationEditorFooter';
import ScopeWizardDialog from '../../views/configuration/scope-wizard/ScopeWizardDialog';
import { selectedFileContentsChanged } from '../../../redux/ducks/configuration/actions';
import { useDispatch } from 'react-redux';

/** data */
import { TOOLTIP_OPTIONS } from '../../../data/constants';

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
  const [currentScope, setCurrentScope] = useState(null);
  const [currentScopeName, setCurrentScopeName] = useState(null);
  const [expandedRows, setExpandedRows] = useState([]);
  const [configurationError, setConfigurationError] = useState(null);
  const [configuration, setConfiguration] = useState([]);
  const [isScopeWizardDialogShown, setIsScopeWizardDialogShown] = useState(false);

  const hideScopeWizardDialog = () => {
    setIsScopeWizardDialogShown(false);
  };

  const onAdd = () => {
    setCurrentScope(null);
    setCurrentScopeName(null);
    setIsScopeWizardDialogShown(true);
  };

  // derived variables
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
   * Converts the given instrumentation information into a JSON format.
   *
   * @param {*} typeMatcher the class instrumentation settings
   * @param {*} methodMatcher the method instrumentation settings
   */
  const prepareScope = (typeMatcher, methodMatcher) => {
    // Prepare class matcher
    const type = typeMatcher.type;
    const classMatcherType = typeMatcher.matcherType;
    const className = typeMatcher.name;

    if (_.isEmpty(type) || _.isEmpty(classMatcherType) || _.isEmpty(className)) {
      throw new Error('Type matcher must be fully specified!');
    }

    const typeMatcherBody = { name: className, 'matcher-mode': classMatcherType };
    const classMatcherScope = { [type]: type === 'interfaces' ? [typeMatcherBody] : typeMatcherBody };

    // Prepare method matcher
    const visibilities = methodMatcher.visibilities;
    const methodMatcherType = methodMatcher.matcherType;
    const isConstructor = methodMatcher.isConstructor;
    const withParameter = methodMatcher.isSelectedParameter;
    const parameters = methodMatcher.parameterList;
    const methodName = methodMatcher.name;

    const constructorBody = { 'is-constructor': true };
    const methodNameBody = methodName && methodMatcherType ? { name: methodName, 'matcher-mode': methodMatcherType } : {};
    const methodMatcherBody = isConstructor ? constructorBody : methodNameBody;
    methodMatcherBody.visibility = visibilities;
    if (withParameter) {
      methodMatcherBody.arguments = parameters;
    } else {
      _.unset(methodMatcherBody, 'arguments');
    }

    // prepare Scope
    const scope = _.merge(classMatcherScope, { methods: [methodMatcherBody] });
    return scope;
  };

  /**
   * Callback which is executed when the add scope button is pressed.
   * Writes the scope with the given instrumentation information in the configuration model.
   *
   * @param {*} typeMatcher the class instrumentation settings
   * @param {*} methodMatcher the method instrumentation settings
   */
  const addScope = (typeMatcher, methodMatcher) => {
    const cloneConfiguration = _.cloneDeep(configuration);
    let scopeName;
    // Check whether an existing scope was modified or a new scope created
    if (currentScopeName) {
      // If currentScopeName is set, an existing scope is being modified
      scopeName = currentScopeName
    } else {
      // If currentScopeName is undefined, a new scope is being created
      // Generate random name for new scope
      scopeName = ('s_gen_scope_' + uuid()).replaceAll('-', '_');
      // Enable Trace and Measure by default for new scope
      _.set(cloneConfiguration, ['inspectit', 'instrumentation', 'rules', 'r_method_configuration_trace', 'scopes', scopeName], true);
      _.set(cloneConfiguration, ['inspectit', 'instrumentation', 'rules', 'r_method_configuration_duration', 'scopes', scopeName], true);
    }

    const preparedScope = prepareScope(typeMatcher, methodMatcher);

    try {
      _.set(cloneConfiguration, ['inspectit', 'instrumentation', 'scopes', scopeName], preparedScope);
      updateConfiguration(cloneConfiguration);
    } catch (error) {
      // todo: proper error handling
      throw new Error(error);
    }
  };

  /**
   * Updates the currently selected configuration file with the given YAML.
   *
   * @param {*} newConfiguration the new configuration
   */
  const updateConfiguration = (newConfiguration) => {
    try {
      const updatedYamlConfiguration = '# {"type": "Method-Configuration"}\n' + yaml.dump(newConfiguration);

      dispatch(selectedFileContentsChanged(updatedYamlConfiguration));
    } catch (error) {
      console.error('Configuration could not been updated.', error);
    }
  };

  /**
   * Updates the current configuration file. Changes the given path to the given value.
   *
   * @param {*} ruleScopePath The path to the scope inside a specific rule which should be changed.
   * @param {*} value The value to be set under the given objectPath.
   */
  const updateConfigurationFile = (ruleScopePath, value) => {
    const cloneConfiguration = _.cloneDeep(configuration);

    if (value === false) {
      _.unset(cloneConfiguration, ruleScopePath, value);
    } else {
      _.set(cloneConfiguration, ruleScopePath, value);
    }

    updateConfiguration(cloneConfiguration);
  };

  /**
   * Deletes the scope with the given name from the configuration model.
   *
   * @param {*} scopeName The name of the scope to be deleted.
   */
  const deleteScope = (scopeName) => {
    const cloneConfiguration = _.cloneDeep(configuration);

    // remove scope
    _.unset(cloneConfiguration, 'inspectit.instrumentation.scopes.' + scopeName);

    // remove scope from all rules
    _.values(SCOPE_STATES_RULES).forEach((ruleName) => {
      _.unset(cloneConfiguration, 'inspectit.instrumentation.rules.' + ruleName + '.scopes.' + scopeName);
    });

    updateConfiguration(cloneConfiguration);
  };

  /**
   * Providing the template body for the row group header.
   */
  const rowGroupHeaderTemplate = ({ scope }) => {
    return <ScopeTypeDisplay scope={scope} />;
  };

  /**
   * Providing the template body for the scopes.
   */
  const scopeDescriptionBodyTemplate = ({ scope }) => {
    return <ScopeMethodDisplay scope={scope} />;
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
  const scopeEditBodyTemplate = ({ scope, name }) => {
    return (
      <div align="right">
        <Button
          icon="pi pi-pencil"
          style={{ marginRight: '0.5rem' }}
          tooltip="Edit Method Configuration"
          tooltipOptions={TOOLTIP_OPTIONS}
          onClick={() => {
            setCurrentScope(scope);
            setCurrentScopeName(name);
            setIsScopeWizardDialogShown(true);
          }}
        />
        <Button
          icon="pi pi-trash"
          tooltip="Remove Method Configuration"
          tooltipOptions={TOOLTIP_OPTIONS}
          onClick={() => {
            deleteScope(name);
          }}
        />
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

        .this :global(.p-datatable tr.p-rowgroup-header) {
          border-top: 1px solid #e7e7e7;
        }

        .this :global(.p-datatable .p-rowgroup-header td) {
          padding: 1rem 0.5rem;
        }

        .this :global(.p-datatable tr.p-rowgroup-footer) {
          height: 1rem;
        }

        .this :global(.p-datatable-row td:first-of-type) {
          padding-left: 2.5rem;
          line-height: 1.4rem;
        }
        .this :global(.p-datatable-row td) {
          padding-top: 0.15rem;
          padding-bottom: 0.15rem;
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
              />
              <Column
                body={({ name }) => scopeStateBodyTemplate(name, SCOPE_STATES_RULES.MEASURING)}
                header="Measure"
                style={{ width: '6rem' }}
              />
              <Column body={scopeEditBodyTemplate} style={{ width: '8rem' }} />
            </DataTable>
          ) : (
            <SelectionInformation hint="The configuration is empty." />
          )}
        </div>
        {!configurationError && <MethodConfigurationEditorFooter onAdd={onAdd} />}
        <ScopeWizardDialog visible={isScopeWizardDialogShown} onHide={hideScopeWizardDialog} onApply={addScope} scope={currentScope} />
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
