import React, { Component } from 'react'
import { connect } from 'react-redux'
import { clockActions, clockSelectors } from '../../redux/ducks/clock'

import { Button } from 'primereact/button';
import {Growl} from 'primereact/growl';

class Counter extends Component {
  increment = () => {
    this.props.incrementCount();
    this.growl.show({severity: 'success', summary: 'Increment', detail: 'Incremented counter by 1'});
  }

  incrementFive = () => {
    this.props.incrementCount(5);
  }

  decrement = () => {
    this.props.decrementCount();
  }

  reset = () => {
    this.props.resetCount();
    this.growl.show({severity: 'error', summary: 'Reset', detail: 'Counter has been resetted'});
  }

  render() {
    const { count, isNegative, serverRendered } = this.props
    return (
      <div>
        <Growl ref={(el) => this.growl = el}></Growl>

        <h1>
          Count: <span>{count}</span>
        </h1>
        <Button label="+1" icon="pi pi-caret-up" onClick={this.increment} />
        <Button label="+5" onClick={this.incrementFive} />
        <Button label="-1" onClick={this.decrement} />
        <Button label="Reset" onClick={this.reset} />
        
        Is negative: {isNegative ? "true" : "false"} // Rendered on Server: {serverRendered ?  "true" : "false"}
      </div>
    )
  }
}

function mapStateToProps(state) {
  const { count, serverRendered } = state.clock;  
  return {
    count,
    serverRendered,
    isNegative: clockSelectors.isNegativeCount(state)
  }
}

const mapDispatchToProps = {
  incrementCount: clockActions.incrementCount,
  decrementCount: clockActions.decrementCount,
  resetCount: clockActions.resetCount,
}

export default connect(mapStateToProps, mapDispatchToProps)(Counter)
