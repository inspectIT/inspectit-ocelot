import React from 'react';
import dateformat from 'dateformat';
import { Toolbar } from 'primereact/toolbar';
import { Button } from 'primereact/button';

const contentItem = (item, fileTreeSelected) => {
  let commitDisable = true;
  if (fileTreeSelected != '') {
    for (let i = 0; i < item.changes.length; i++) {
      const itemChanges = item.changes[i].slice(0, fileTreeSelected.length);
      if (itemChanges === fileTreeSelected) {
        commitDisable = false;
      }
    }
  } else {
    commitDisable = false;
  }

  return (
    <>
      <style jsx>
        {`
          .this {
            border: 1px solid #dddddd;
            background: transparent;
            color: black;
            height: 4.5em;
          }
          .this-disable {
            border: 1px solid #dddddd;
            background: #cccccc;
            color: #7f7f7f;
            height: 4.5em;
          }
          .table {
            padding-top: 1em;
          }
          .td {
            width: 12em;
          }
          .name {
            padding-left: 1em;
            padding-top: 1em;
            font-weight: bold;
          }
          .id {
            padding-left: 0.2em;
            font-size: 0.78em;
            font-style: italic;
          }
          .date {
            padding-right: 1em;
            display: block;
            text-align: right;
            font-weight: bold;
          }
          .author {
            display: table;
            margin: auto;
            font-weight: bold;
          }
        `}
      </style>
      <div className={!commitDisable ? 'this' : 'this-disable'}>
        <div>
          <table className="table">
            <tbody>
              <tr>
                <td className="td">
                  <label className="name">{item.name}</label>
                  <label className="id">({item.id})</label>
                </td>
                <td className="td">
                  <label className="date">
                    <label>{dateformat(item.date, 'yyyy-mm-dd HH:MM')}</label>
                  </label>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div className="author">
          <label>{item.author}</label>
        </div>
      </div>
    </>
  );
};

class HistoryView extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      visibleLeft: true,
      selectedCommit: this.props.data[0],
      show: false,
    };
  }

  showHistoryView = () => {
    this.setState({
      show: !this.state.show,
    });
  };

  render() {
    return (
      <>
        <style jsx>
          {`
            .this :global(.p-toolbar) {
              border: 0;
              border-radius: 0;
              background-color: #eee;
              border-bottom: 1px solid #ddd;
            }
            .content {
              border-bottom: 1px solid #dddddd;
              border-left: 1px solid #dddddd;
              height: 95%;
              overflow-x: hidden;
            }
            .template {
              padding-left: 2em;
              padding-right: 2em;
              padding-top: 0.5em;
              padding-bottom: 0.5em;
            }
          `}
        </style>
        <div className="this">
          <Toolbar>
            <Button onClick={() => this.showHistoryView()} icon="pi pi-bars"></Button>
          </Toolbar>
          {this.state.show ? (
            <div className="content">
              {this.props.data.map((item, index) => {
                return (
                  <div className="template" key={index}>
                    {' '}
                    {contentItem(item, this.props.fileTreeSelected)}{' '}
                  </div>
                );
              })}
            </div>
          ) : null}
        </div>
      </>
    );
  }
}

export default HistoryView;
