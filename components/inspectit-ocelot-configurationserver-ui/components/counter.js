import React, { Component } from 'react'
import { connect } from 'react-redux'
import { clockActions, clockSelectors } from '../redux/ducks/clock'

class Counter extends Component {
  increment = () => {
    this.props.incrementCount();
  }

  incrementFive = () => {
    this.props.incrementCount(5);
  }

  decrement = () => {
    this.props.decrementCount();
  }

  reset = () => {
    this.props.resetCount();
  }

  render() {
    const { count, isNegative } = this.props
    return (
      <div>
        <h1>
          Count: <span>{count}</span>
        </h1>
        <button onClick={this.increment}>+1</button>
        <button onClick={this.incrementFive}>+5</button>
        <button onClick={this.decrement}>-1</button>
        <button onClick={this.reset}>Reset</button>
        Is negative: {isNegative ? "true" : "false"}
      </div>
    )
  }
}

function mapStateToProps(state) {
  const { count } = state.clock;  
  return {
    count,
    isNegative: clockSelectors.isNegativeCount(state)
  }
}

const mapDispatchToProps = {
  incrementCount: clockActions.incrementCount,
  decrementCount: clockActions.decrementCount,
  resetCount: clockActions.resetCount,
}

export default connect(mapStateToProps, mapDispatchToProps)(Counter)
