import React from 'react';
import dateformat from 'dateformat';

const contentItem = (item) => {

  return (
    <>
      <style jsx>
        {`
          .this {
            border-bottom: 1px solid #dddddd;
            background: transparent;
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
          .id {
            padding-left: 1em;
            padding-top: 1em;
            font-weight: bold;
          }
          .date {
            padding-right: 1em;
            display: block;
            text-align: right;
          }
          .author {
            display: table;
            margin: auto;
            padding-top: 0.2em;
            padding-bottom: 0.7em;
          }
        `}
      </style>

      <div className="this">
        <div>
          <div className="container">
            <div className="section1">
              <label className="id">
                {item.id.substring(0, 6)}
              </label>
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
