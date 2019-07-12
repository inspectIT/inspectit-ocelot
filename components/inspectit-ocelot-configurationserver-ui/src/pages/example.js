import React from 'react'
import { connect } from 'react-redux'
import { clockActions } from "../redux/ducks/clock";
import Examples from '../components/example/Examples'

class ExamplePage extends React.Component {

  static getInitialProps({ reduxStore, req }) {
    const isServer = !!req
    // DISPATCH ACTIONS HERE ONLY WITH `reduxStore.dispatch`
    reduxStore.dispatch(clockActions.initClock(isServer))

    return {}
  }

  componentDidMount() {
    // DISPATCH ACTIONS HERE FROM `mapDispatchToProps`
    // TO TICK THE CLOCK
    this.timer = setInterval(() => this.props.tickClock(), 1000)
  }

  componentWillUnmount() {
    clearInterval(this.timer)
  }

  render() {
    return <Examples />
  }
}

const mapDispatchToProps = {
  tickClock: clockActions.tickClock
}

export default connect(null, mapDispatchToProps)(ExamplePage)
