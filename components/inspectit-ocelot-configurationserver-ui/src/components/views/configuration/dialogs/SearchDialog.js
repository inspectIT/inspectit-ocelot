import React, { useState } from 'react';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { ToggleButton } from 'primereact/togglebutton';
import { Button } from 'primereact/button';
import { ListBox } from 'primereact/listbox';
import _ from 'lodash';

const SearchResultTemplate = ({ filename, lineNumber, line, matches }) => {
  // split matches
  let index = 0;
  const split = [];
  _.each(matches, (match) => {
    const { start, end } = match;
    if (start > index) {
      const previousText = line.substring(index, start);
      split.push({ text: previousText, highlight: false });
    }
    const matchText = line.substring(start, end);
    split.push({ text: matchText, highlight: true });
    index = end;
  });

  if (index < line.length) {
    const endingText = line.substring(index);
    split.push({ text: endingText, highlight: false });
  }

  split[0].text = split[0].text.replace(/^\s+/, ''); // trim leading spaces
  console.log(split);

  return (
    <>
      <style jsx>{`
        .item {
          display: flex;
        }
        .match {
          flex-grow: 1;
          font-family: monospace;
          overflow: hidden;
          white-space: nowrap;
          text-overflow: ellipsis;
        }
        .source {
          color: #9e9e9e;
          margin-left: 2rem;
          white-space: nowrap;
        }
        .source span {
          color: black;
        }
        :global(.p-highlight) .source,
        :global(.p-highlight) .source span {
          color: inherit;
        }
        .highlight {
          background-color: #ffa726;
          padding: 0 0.25rem;
          border-radius: 4px;
        }
      `}</style>

      <div className="item">
        <div className="match">
          {split.map((match, index) =>
            match.highlight ? (
              <span key={index} className="highlight">
                {match.text}
              </span>
            ) : (
              match.text
            )
          )}
        </div>
        <div className="source">
          {filename} <span>{lineNumber}</span>
        </div>
      </div>
    </>
  );
};

const _result = [
  {
    file: 'apache/apache-actions.yml',
    firstLine: '      # All actions are designed to be applied on Closeab  HttpClient.doExecute(HttpHost, HttpRequest, HttpContext)',
    startLine: 4,
    startColumn: 59,
    endLine: 4,
    endColumn: 63,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: '      # All actions are designed to be applied on CloseableHttpClient.doExecute(HttpHost, HttpRequest, HttpContext)',
    startLine: 4,
    startColumn: 80,
    endLine: 4,
    endColumn: 84,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: '      # All actions are designed to be applied on CloseableHttpClient.doExecute(HttpHost, HttpRequest, HttpContext)',
    startLine: 4,
    startColumn: 90,
    endLine: 4,
    endColumn: 94,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: '      # All actions are designed to be applied on CloseableHttpClient.doExecute(HttpHost, HttpRequest, HttpContext)',
    startLine: 4,
    startColumn: 103,
    endLine: 4,
    endColumn: 107,
  } /*
  {
    file: 'apache/apache-actions.yml',
    firstLine: '      # of the Apache HttpClient (https://hc.apache.org/httpcomponents-client-ga/)',
    startLine: 5,
    startColumn: 22,
    endLine: 5,
    endColumn: 26,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: '      # of the Apache HttpClient (https://hc.apache.org/httpcomponents-client-ga/)',
    startLine: 5,
    startColumn: 34,
    endLine: 5,
    endColumn: 38,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: '      # of the Apache HttpClient (https://hc.apache.org/httpcomponents-client-ga/)',
    startLine: 5,
    startColumn: 56,
    endLine: 5,
    endColumn: 60,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: "          - 'org.apache.http'",
    startLine: 12,
    startColumn: 24,
    endLine: 12,
    endColumn: 28,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: "          _arg0: 'HttpHost'",
    startLine: 14,
    startColumn: 18,
    endLine: 14,
    endColumn: 22,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: '      # Extracts the target http path',
    startLine: 17,
    startColumn: 28,
    endLine: 17,
    endColumn: 32,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: "          - 'org.apache.http'",
    startLine: 21,
    startColumn: 24,
    endLine: 21,
    endColumn: 28,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: "          _arg1: 'HttpRequest'",
    startLine: 23,
    startColumn: 18,
    endLine: 23,
    endColumn: 22,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: '      # Extracts the HTTP Method of the request, e.g. GET or POST',
    startLine: 26,
    startColumn: 21,
    endLine: 26,
    endColumn: 25,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: "          - 'org.apache.http'",
    startLine: 29,
    startColumn: 24,
    endLine: 29,
    endColumn: 28,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: "          _arg1: 'HttpRequest'",
    startLine: 31,
    startColumn: 18,
    endLine: 31,
    endColumn: 22,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: "          - 'org.apache.http'",
    startLine: 38,
    startColumn: 24,
    endLine: 38,
    endColumn: 28,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: "          _returnValue: 'HttpResponse'",
    startLine: 40,
    startColumn: 25,
    endLine: 40,
    endColumn: 29,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: '      # Writes down-propagated context data to the HTTP Headers',
    startLine: 49,
    startColumn: 51,
    endLine: 49,
    endColumn: 55,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: "          - 'org.apache.http'",
    startLine: 54,
    startColumn: 24,
    endLine: 54,
    endColumn: 28,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: "          _arg1: 'HttpMessage'",
    startLine: 56,
    startColumn: 18,
    endLine: 56,
    endColumn: 22,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: '      # Reads up-propagated context data from the HTTP Headers',
    startLine: 66,
    startColumn: 50,
    endLine: 66,
    endColumn: 54,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: "          - 'org.apache.http'",
    startLine: 71,
    startColumn: 24,
    endLine: 71,
    endColumn: 28,
  },
  {
    file: 'apache/apache-actions.yml',
    firstLine: "          _returnValue: 'HttpMessage'",
    startLine: 73,
    startColumn: 25,
    endLine: 73,
    endColumn: 29,
  },
  {
    file: 'apache/apache-scope.yml',
    firstLine: "          name: 'org.apache.http.impl.client.CloseableHttpClient'",
    startLine: 6,
    startColumn: 28,
    endLine: 6,
    endColumn: 32,
  },
  {
    file: 'apache/apache-scope.yml',
    firstLine: "          name: 'org.apache.http.impl.client.CloseableHttpClient'",
    startLine: 6,
    startColumn: 54,
    endLine: 6,
    endColumn: 58,
  },
  {
    file: 'asd - Copy (3)/example.yml',
    firstLine:
      '      # settings for the prometheus exporter (https://github.com/census-instrumentation/opencensus-java/tree/master/exporters/stats/prometheus)',
    startLine: 8,
    startColumn: 46,
    endLine: 8,
    endColumn: 50,
  },
  {
    file: 'asd - Copy (3)/example.yml',
    firstLine:
      '      # settings for the zipkin exporter (https://github.com/census-instrumentation/opencensus-java/tree/master/exporters/trace/zipkin)',
    startLine: 36,
    startColumn: 42,
    endLine: 36,
    endColumn: 46,
  },
  {
    file: 'asd - Copy (3)/example.yml',
    firstLine: '        # the v2 Url under which the ZipKin server can be accessed, e.g. http://127.0.0.1:9411/api/v2/spans',
    startLine: 40,
    startColumn: 73,
    endLine: 40,
    endColumn: 77,
  },
  {
    file: 'asd - Copy (3)/example.yml',
    firstLine:
      '      # settings for the jaeger exporter (https://github.com/census-instrumentation/opencensus-java/tree/master/exporters/trace/jaeger)',
    startLine: 44,
    startColumn: 42,
    endLine: 44,
    endColumn: 46,
  },
  {
    file: 'asd - Copy (3)/example.yml',
    firstLine: '        # the URL under which the jaeger thrift server can be accessed, e.g. http://127.0.0.1:14268/api/traces',
    startLine: 49,
    startColumn: 77,
    endLine: 49,
    endColumn: 81,
  },
  {
    file: 'asd - Copy (3)/example.yml',
    firstLine:
      '      # settings for the OpenCensus Agent Trace exporter (https://opencensus.io/exporters/supported-exporters/java/ocagent/)',
    startLine: 54,
    startColumn: 58,
    endLine: 54,
    endColumn: 62,
  },
  {
    file: 'git.yml',
    firstLine: '    http:',
    startLine: 2,
    startColumn: 4,
    endLine: 2,
    endColumn: 8,
  },*/,
];

const SearchDialog = () => {
  // state variables
  const [searchTarget, setSearchTarget] = useState(0); // the current selected file name
  const [resultSelection, setResultSelection] = useState(null);
  const [searchResults, setSearchResults] = useState([]);

  const citySelectItems = [
    { label: 'New York', value: 'NY' },
    { label: 'Rome', value: 'RM' },
    { label: 'London', value: 'LDN' },
    { label: 'Istanbul', value: 'IST' },
    { label: 'Paris', value: 'PRS' },
  ];

  const executeSearch = () => {
    const result = _(_result)
      .groupBy((element) => element.file + ':' + element.startLine)
      .map((elements, key) => {
        const matches = _.map(elements, (element) => {
          return {
            start: element.startColumn,
            end: element.endColumn,
          };
        });

        return {
          key,
          filename: elements[0].file, // always the same because we group for this
          lineNumber: elements[0].startLine, // always the same because we group for this
          line: elements[0].firstLine, // must be the same for all elements
          matches,
        };
      })

      .value();
    console.log(result);

    setSearchResults(result);
  };

  const footer = (
    <div>
      <Button label="Open File" icon="pi pi-external-link" /*onClick={this.onHide}*/ />
    </div>
  );

  return (
    <>
      <style jsx>{`
        .this :global(.p-dialog-content) {
          padding: 0;
          display: flex;
          flex-direction: column;
          align-items: stretch;
          height: 75vh;
        }
        .input-container {
          margin: 0.5rem;
        }
        .target :global(.p-togglebutton) {
          margin-right: 0.5rem;
        }
        .query {
          margin-bottom: 0.5rem;
        }
        .query :global(.query-input) {
          flex-grow: 1;
        }
        .this :global(.p-listbox) {
          width: 100%;
          border: 0;
          border-radius: 0;
          overflow-y: auto;
        }
      `}</style>

      <div className="this">
        <Dialog
          className="search-dialog"
          header="Search in Configuration Files"
          visible={true}
          style={{ width: '50vw', minWidth: '50rem' }}
          //onHide={() => this.onHide('displayBlockScroll')}
          blockScroll
          footer={footer}
        >
          <div className="input-container">
            <div className="query p-inputgroup">
              <span className="p-inputgroup-addon">
                <i className="pi pi-search"></i>
              </span>
              <InputText className="query-input" placeholder="" />
              <Button icon="pi pi-search" onClick={executeSearch} />
            </div>

            <div className="target">
              <ToggleButton
                onLabel="In Workspace"
                offLabel="In Workspace"
                checked={searchTarget == 0}
                onChange={() => setSearchTarget(0)}
              />
              <ToggleButton
                onLabel="In Directory"
                offLabel="In Directory"
                checked={searchTarget == 1}
                onChange={() => setSearchTarget(1)}
                disabled={true}
              />
              <ToggleButton
                onLabel="In Agent Mapping"
                offLabel="In Agent Mapping"
                checked={searchTarget == 2}
                onChange={() => setSearchTarget(2)}
                disabled={true}
              />
            </div>
          </div>

          <ListBox
            optionLabel="key"
            value={resultSelection}
            options={searchResults}
            onChange={(e) => setResultSelection(e.value)}
            itemTemplate={SearchResultTemplate}
          />
        </Dialog>
      </div>
    </>
  );
};

export default SearchDialog;
