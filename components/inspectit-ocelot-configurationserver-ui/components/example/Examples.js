import { connect } from 'react-redux'
import Clock from './Clock'
import Counter from './counter'
import Link from '../basics/Link'

function Examples({ lastUpdate, light }) {
  return (
    <div>
      <Clock lastUpdate={lastUpdate} light={light} />
      <Counter />
      <Link href="/"><a>Index</a></Link>
    </div>
  )
}

function mapStateToProps(state) {  
  const { lastUpdate, light } = state.clock;
  return { lastUpdate, light }
}

export default connect(mapStateToProps)(Examples)
