import React from 'react'
import Link from '../components/basics/Link'
import { Button } from 'primereact/button';
import { Panel } from 'primereact/panel';

class IndexPage extends React.Component {

  render() {
    return (
      <Panel header="Examples">
        <Link href="/example">
          <Button label="Redux Example" />
        </Link>
      </Panel>
    )
  }

}

export default IndexPage;
