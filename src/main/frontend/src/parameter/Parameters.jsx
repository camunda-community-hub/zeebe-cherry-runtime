// -----------------------------------------------------------
//
// Parameters
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';

class Parameters extends React.Component {


  constructor(_props) {
    super();
    this.state = {};
  }

  componentDidMount() {
    console.log("Definition.componentDidMount: BEGIN");
    console.log("Definition.componentDidMount: END");
  }

  /*           {JSON.stringify(this.state.runners, null, 2) } */
  render() {
    return(<div>Parameters</div>)
  }

}

export default Parameters;