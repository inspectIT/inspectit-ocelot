import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { Dropdown } from 'primereact/dropdown';
import { Checkbox } from 'primereact/checkbox';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import ConfigDocumentation from './ConfigDocumentation';
import useFetchData from '../../../../hooks/use-fetch-data';
import _ from 'lodash';
import useDebounce from '../../../../hooks/use-debounce';

/**
 * The sidebar panel for showing existing versions of the configuration files.
 */
const DocumentationView = () => {
  // global state variables
  const configurationUpdateDate = useSelector((state) => state.configuration.updateDate);

  // local state
  const [selectedAgentMapping, setSelectedAgentMapping] = useState(null);
  const [includeDefault, setIncludeDefault] = useState(true);
  const [filterValue, setFilterValue] = useState('');
  const [configurationDocs, setConfigurationDocs] = useState(null);

  // debounced filter value for preventing filtering on each input change
  const filterValueDebounced = useDebounce(filterValue, 500);

  // fetch required data
  const [{ data: agentMappings }, refreshAgentMappings] = useFetchData('mappings', {}, []);
  const [{ data: fullConfigurationDocs, isLoading: isLoadingDocs, isError: isDocsError, error }, refreshConfigurationDocs] = useFetchData(
    'configuration/documentation',
    {
      'agent-mapping': selectedAgentMapping,
      'include-default': includeDefault,
    }
  );

  // derived variables
  const agentMappingOptions = _(agentMappings)
    .sortBy((mapping) => mapping.name.toLowerCase())
    .map((mapping) => {
      return { label: mapping.name, value: mapping.name };
    })
    .value();

  // initially load the agent mappings
  useEffect(() => {
    refreshAgentMappings();
  }, []);

  // refresh the configuration docs when parameters or configuration changed
  useEffect(() => {
    if (selectedAgentMapping) {
      refreshConfigurationDocs();
    }
  }, [selectedAgentMapping, includeDefault, configurationUpdateDate]);

  // filter documentation for display
  useEffect(() => {
    if (fullConfigurationDocs) {
      const filter = filterValueDebounced.toLowerCase();
      const filteredData = {
        actions: _.filter(fullConfigurationDocs.actions, (e) => e.name.toLowerCase().includes(filter)),
        scopes: _.filter(fullConfigurationDocs.scopes, (e) => e.name.toLowerCase().includes(filter)),
        rules: _.filter(fullConfigurationDocs.rules, (e) => e.name.toLowerCase().includes(filter)),
        metrics: _.filter(fullConfigurationDocs.metrics, (e) => e.name.toLowerCase().includes(filter)),
      };
      setConfigurationDocs(filteredData);
    }
  }, [filterValueDebounced, fullConfigurationDocs]);

  return (
    <>
      <style jsx>
        {`
          .documentation {
            background-color: #eeeeee;
            border-right: 1px solid #ddd;
            display: flex;
            flex-direction: column;
            width: 35rem;
            overflow-y: auto;
            height: 100%;
            box-shadow: -5px 0px 5px 0px #0000001c;
          }
          .head {
            padding: 1rem 0;
            border-bottom: 1px solid #9e9e9e;
          }
          .headline {
            color: #111;
            border-bottom: 1px solid #9e9e9e;
            margin-bottom: 0.5rem;
            padding: 0.5rem 1rem 0.5rem;
            font-size: 1rem;
          }
          .input-row {
            display: flex;
            align-items: center;
            margin-bottom: 0.25rem;
            padding: 0 1rem;
          }
          .input-row label {
            width: 13rem;
          }
          .input-row :global(.p-dropdown) {
            width: 15rem;
          }
          .note {
            flex: 1;
            padding: 1rem;
          }
          .note .error {
            margin-top: 0.5rem;
            color: #333;
            border-left: 4px solid #c1c1c1;
            padding: 0.5rem;
            font-family: monospace;
          }
          .filter {
            padding: 0.5rem 1rem;
            border-bottom: 1px solid #9e9e9e;
          }
        `}
      </style>
      <div className="documentation p-component">
        <div className="head">
          <div className="headline">Configuration Docs</div>

          <div className="input-row">
            <label htmlFor="agentMappingDd">Select Agent Mapping:</label>
            <Dropdown
              id="agentMappingDd"
              placeholder="Select an Agent Mapping"
              options={agentMappingOptions}
              value={selectedAgentMapping}
              onChange={(e) => setSelectedAgentMapping(e.value)}
              filter={true}
            />
          </div>

          <div className="input-row">
            <label htmlFor="defaultConfigurationCb">Include Default Configuration:</label>
            <Checkbox id="defaultConfigurationCb" checked={includeDefault} onChange={(e) => setIncludeDefault(e.checked)} />
          </div>
        </div>

        <div className="filter p-inputgroup">
          <span className="pi p-inputgroup-addon pi-filter" />

          <InputText
            style={{ flex: 1 }}
            value={filterValue}
            placeholder={'Filter Documentation'}
            onChange={(e) => setFilterValue(e.target.value)}
            disabled={!(configurationDocs && !isDocsError)}
          />

          <Button icon="pi pi-times" disabled={!filterValue} onClick={() => setFilterValue('')} />
        </div>

        {selectedAgentMapping ? (
          configurationDocs && !isDocsError ? (
            <ConfigDocumentation configurationDocs={configurationDocs} selectedAgentMapping={selectedAgentMapping} />
          ) : isLoadingDocs ? (
            <div className="note">Loading documentation...</div>
          ) : (
            <div className="note">
              The documentation for the selected Agent Mapping could not be loaded.
              <div className="error">
                Error: <span>{error?.message}</span>
              </div>
            </div>
          )
        ) : (
          <div className="note">Select the Agent Mapping whose documentation should be displayed.</div>
        )}
      </div>
    </>
  );
};

export default DocumentationView;
