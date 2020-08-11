import React, { useState } from 'react';
import { useSelector } from 'react-redux';
import Navigationbar from './Navigationbar';
import contentItem from './ContentItem';

const HistoryView = ({ data }) => {
  let [show, setShow] = useState(false);
  const selection = useSelector((state) => state.configuration.selection);

  const showHistoryView = () => {
    setShow(!show);
  };

  return (
    <>
      <style jsx>
        {`
          .this {
            border: 0;
            border-radius: 0;
            background-color: #eee;
            border-bottom: 1px solid #ddd;
            display: flex;
            flex: 1;
            background-color: white;
          }
          .content {
            border-bottom: 1px solid #dddddd;
            border-left: 1px solid #dddddd;
            overflow-x: hidden;
          }
          .navigationbar {
            background-color: #eeeeee;
            border-left: 1px solid #dddddd;
          }
        `}
      </style>

      <div className="this">
        {show ? (
          <div className="content">
            {data.map((item, index) => (
              <div key={index}> {contentItem(item, selection)} </div>
            ))}
          </div>
        ) : null}
        <div className="navigationbar">
          <Navigationbar showHistoryView={showHistoryView} show={show} />
        </div>
      </div>
    </>
  );
};

export default HistoryView;
