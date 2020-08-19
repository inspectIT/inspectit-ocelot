import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { configurationActions } from '../../../../redux/ducks/configuration'
import Navigationbar from './Navigationbar';
import contentItem from './ContentItem';

const HistoryView = ({ versionSelection, versionSelectionChange }) => {

  const dispatch = useDispatch();

  // state variables
  let [show, setShow] = useState(false);

  // global state variables
  const selection = useSelector((state) => state.configuration.selection);
  const versions = useSelector((state) => state.configuration.versions)

  const showHistoryView = () => {
    setShow(!show);
  };

  useEffect(() => {
    if (show) {
      if (versions.length == 0) {
        dispatch(configurationActions.fetchVersions());
      }
    }
  }, [show])

  const selectVersion = (item, index) => {
    versionSelectionChange(index);
    dispatch(configurationActions.fetchFilesWithId(item.id));
  }

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
            height: 60em;
          }
          .version-selected {
            color: white;
            background: #007AD9;
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
            {versions.map((item, index) => (
              <div className={versionSelection == index ? "version-selected" : null} key={index} onClick={() => selectVersion(item, index)} >
                {contentItem(item, (versions.length - index), selection)}
              </div>
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
