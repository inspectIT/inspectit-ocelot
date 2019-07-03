import { connect } from 'react-redux'
import Clock from './clock'
import Counter from './counter'
import Link from '../components/basics/Link'

function Examples({ lastUpdate, light }) {
  return (
    <div>
      <Clock lastUpdate={lastUpdate} light={light} />
      <Counter />
      <Link href="/abc"><a>abc</a></Link>
    </div>
  )
}

function mapStateToProps(state) {
  const { lastUpdate, light } = state
  return { lastUpdate, light }
}

export default connect(mapStateToProps)(Examples)
