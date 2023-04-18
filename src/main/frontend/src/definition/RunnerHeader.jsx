// -----------------------------------------------------------
//
// RunnerHeader
//
// Display the header for a runner
//
// -----------------------------------------------------------

import React from 'react';


class RunnerHeader extends React.Component {


  constructor(props) {
    super();
    this.state = {runner: props.runner};
  }

  componentDidMount() {

  }

  /*           {JSON.stringify(this.state.runners, null, 2) } */
  render() {
    return (

      <table style={{marginBottom: "10px"}}>
        <tbody>
        <tr>
          <td>
            <div style={{border: "1px solid", padding: "5px", textAlign: "center", boxShadow: "5px 5px"}}
                 bis_skin_checked="1">
              <img style={{width: "50px"}} src={this.state.runner.logo} alt="logo"/>
            </div>
          </td>
          <td style={{paddingLeft: "30px"}}>
            {this.state.runner.name}<br/>
            <i>{this.state.runner.label}</i>
          </td>
        </tr>
        </tbody>
      </table>
    );
  }
}
export default RunnerHeader;