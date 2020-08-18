import React from 'react';
import dateformat from 'dateformat';

const contentItem = (item, version, fileTreeSelected) => {

  // Is needed to display changes of the selected file in the different versions.
  let containsChanges = false;
  /*
  if (fileTreeSelected != null) {
    for (let i = 0; i < item.changes.length; i++) {
      const itemChanges = item.changes[i].slice(0, fileTreeSelected.length);
      if (itemChanges === fileTreeSelected) {
        containsChanges = true;
      }
    }
  }
*/

  return (
    <>
      <style jsx>
        {`
          .this {
            border-bottom: 1px solid #dddddd;
            background: transparent;
            height: 4.5em;
          }
          .container {
            display: flex;
            padding-top: 1em;
          }
          .section1 {
            flex-grow: 1;
            padding-left: 1em;
            padding-right: 1em;
          }
          .section2 {
            flex-grow: 1;
            padding-left: 1em;
            padding-right: 1em;
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
          }
          .author {
            display: table;
            margin: auto;
          }
        `}
      </style>
      <div className="this">
        <div>
          <div className="container">
            <div className="section1">
              <label className="name">
                {containsChanges ? '* ' : null}
                Version {version}
              </label>
              <label className="id">{item.id.substring(0, 6)}</label>
            </div>
            <div className="section2">
              <label className="date">
                <label>
                  {dateformat((item.date * 1000), 'yyyy-mm-dd HH:MM:ss')}</label>
              </label>
            </div>
          </div>
        </div>
        <div className="author">
          <label>{item.author}</label>
        </div>
      </div>
    </>
  );
};

export default contentItem;
