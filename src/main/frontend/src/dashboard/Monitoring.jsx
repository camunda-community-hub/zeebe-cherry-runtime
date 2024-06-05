// -----------------------------------------------------------
//
// Monitoring
//
// Monitor the Cherry Engine
//
// -----------------------------------------------------------

import React from 'react';
import RestCallService from "../services/RestCallService";


class Monitoring extends React.Component {


  constructor(_props) {
    super();
    this.state = {
      dashboard: {
        details: [],
      },
      display: {
        message: "",
        status: "OK"
      },


    };
  }

  componentDidMount() {
    this.refreshMonitoring();
  }


  render() {
    // console.log("dashboard.render display="+JSON.stringify(this.state.display));
    return (<div className={"container"}>

      <div className="row">
        <div className="col-md-2">
          {this.state.status === "OK" && <div>OK</div>}
          {this.state.status === "REFRESH" && <div>Refreshing</div>}
          {this.state.status === "ERROR" && <div>Error</div>}
        </div>
        <div className="col-md-2">
          {this.state.message}
        </div>
      </div>


    </div>)

  }


  refreshMonitoring(period, orderBy) {
    let uri = 'cherry/api/monitoring/status';
    console.log("Monitoring.refreshMonitoring http[" + uri + "]");

    this.setDisplayProperty("loading", true);
    this.setState({status: "REFRESH"});
    var restCallService = RestCallService.getInstance();
    restCallService.getJson(uri, this, this.refreshMonitoringCallback);
  }

  refreshMonitoringCallback(httpPayload) {
    this.setDisplayProperty("loading", false);
    if (httpPayload.isError()) {
      console.log("Monitoring.refreshMonitoringCallback: error " + httpPayload.getError());
      this.setState({status: "ERROR"});
    } else {
      let firstRunner = httpPayload.getData().details[0];
      console.log("Monitoring: RESTCALLBACK first is [" + JSON.stringify(firstRunner.name) + "]");
      this.setState({status: httpPayload.getData().status, message: httpPayload.getData().message});
    }
  }

  /**
   * Set the display property
   * @param propertyName name of the property
   * @param propertyValue the value
   */
  setDisplayProperty(propertyName, propertyValue) {
    let displayObject = this.state.display;
    displayObject[propertyName] = propertyValue;
    this.setState({display: displayObject});
  }
}

export default Monitoring;
