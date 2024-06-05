// -----------------------------------------------------------
//
// ControllerPage
//
// Manage the control on the page: display "loading in progress", "error"
//
// -----------------------------------------------------------

import React from 'react';
import {InlineLoading} from "carbon-components-react";

class ControllerPage extends React.Component {


  constructor(_props) {
    super();
    this.state = {
      errorMessage: "",
      loading: false
    };
  }

  componentDidUpdate(prevProps) {
    if (prevProps.errorMessage !== this.props.errorMessage) {
      // console.log("RunnerDashboard.componentDidUpdate: Change");
      this.setState({
        errorMessage: this.props.errorMessage
      });
    }
    if (prevProps.loading !== this.props.loading) {
      this.setState({
        loading: this.props.loading
      });
    }

  }

  /*           {JSON.stringify(this.state.runners, null, 2) } */
  render() {
    return (
      <div style={{height: "50px"}}>
        {this.state.loading &&
          <table>
            <tr>
              <td><InlineLoading></InlineLoading></td>
              <td>Loading</td>
            </tr>
          </table>}

        {this.state.errorMessage &&
          <div className="alert alert-danger" style={{margin: "10px 10px 10px 10px"}}>
            Error:{this.state.errorMessage}
          </div>}
      </div>
    )
  }

}

export default ControllerPage;