import React from 'react'
import PropTypes from 'prop-types'
import { Button } from 'primereact/button'

const SettingsElement = (props) => {
  return (
    <div className={`this ${props.line ? 'line': ''}`}>
      <style jsx>{`
        .this{
          margin: 1rem;
        }
        .header{
          margin: 0;
          margin-bottom: 1rem;
          background: #ddd;
          padding-left: 0.5rem;
          height: 2rem;

        }
        .title{
          font-size: 1rem;
          font-weight: normal;
          position: relative;
          top: 0.25rem;
        }
        .content{
          padding-left: 1rem;
          padding-right: 1rem;
          padding-bottom: 1rem;
          display: flex;
          justify-content: center;
        }
        .settingsEbtn{
          text-align: right;
          margin-bottom: 1rem;
        }
        .line:after{
          content: "";
          display: block;
          border-radius: 25px;
          height: 0.1rem;
          background: #ddd;
          position: relative;
          width: 95%;
          left: calc(5% / 2)
      `}</style>
      {props.fullHeader ? <div className='header'>{props.fullHeader}</div> : props.title ? <div className='header'><h4 className='title'>{props.title}</h4></div> : ''}
      <div className='content'>{props.children}</div>
      {props.btn ? <div className={`settingsEbtn`}><Button label={props.btnLabel} icon={props.btnIcon} onClick={props.btnOnClick}/></div> : ''}
    </div>
  )
}

SettingsElement.propTypes = {
  /** header component which will be displayed instead of the basic header */
  fullHeader: PropTypes.element,
  /** title which will be displayed in the basic header */
  title: PropTypes.string,
  /** defines whether a button should be displayed in the lower right corner below content */
  btn: PropTypes.bool,
  /** label of shown button */
  btnLabel: PropTypes.string,
  /** primereact icon code */
  btnIcon: PropTypes.string,
  /** function to dispatch when button is clicked */
  btnOnClick: PropTypes.func,
  /** defines whether a line should be displayed after the component to seperate from other settingsElements */
  line: PropTypes.bool
}

export default SettingsElement