// -----------------------------------------------------------
//
// OperationLog
//
// Access the log of the runtime
//
// -----------------------------------------------------------

import React from 'react';
import {Button} from "carbon-components-react";
import {ArrowRepeat, XCircle} from "react-bootstrap-icons";
import ControllerPage from "../component/ControllerPage";
import RestCallService from "../services/RestCallService";

class OperationLog extends React.Component {


  constructor(_props) {
    super();
    this.state = {
      log: {operations: []},
      display: {
        loading: false,
        showOnlyError: false,
      }
    };
  }

  componentDidMount() {
    this.refreshListLog();
  }

  /*           {JSON.stringify(this.state.runners, null, 2) } */
  render() {
    return (
      <div className={"container"}>
        <div className="row" style={{width: "100%"}}>
          <div className="col-md-4">
            <h1 className="title">Operations Log</h1>
          </div>
          <div className="col-md-6">
            <button className={this.getButtonClass(this.state.display.showOnlyError)}
                    style={{marginLeft: "10px", fontSize: "10px"}}
                    disabled={this.state.display.loading}
                    onClick={() => this.setToggleFilter("showOnlyError")}>
              <XCircle style={{color: "red"}}/> Only Errors
            </button>
          </div>
          <div className="col-md-2">
            <Button className="btn btn-success btn-sm"
                    onClick={() => this.refreshListLog()}
                    disabled={this.state.display.loading}>
              <ArrowRepeat/> Refresh
            </Button>
          </div>
        </div>
        <div className="row" style={{width: "100%"}}>
          <div className="col-md-12">
            <ControllerPage errorMessage={this.state.status} loading={this.state.display.loading}/>
          </div>
        </div>


        <div className="row" style={{width: "100%"}}>
          <div className="col-md-12">

            <table id="runnersTable" className="table is-hoverable is-fullwidth">
              <thead>
              <tr>
                <th>Date</th>
                <th>Runner</th>
                <th>Operation</th>
                <th>Message</th>
                <th>hostname</th>
              </tr>
              </thead>
              <tbody>
              {this.state.log.operations ? this.state.log.operations.map((logitem, _index) =>
                <tr style={this.getStyleRow(logitem)}>
                  <td style={{fontSize: "10px", whiteSpace: "nowrap"}}>
                    {logitem.executionTime}
                  </td>
                  <td>
                    {logitem.runnerType}
                  </td>
                  <td>
                    {logitem.operation}
                  </td>
                  <td  style={{fontSize: "12px"}}>
                    {logitem.message}
                  </td>
                  <td>
                    {logitem.hostname}
                  </td>

                </tr>
              ) : <div/>}
              </tbody>
            </table>

          </div>
        </div>
      </div>
    )
  }

  getButtonClass(active) {
    if (active)
      return "btn btn-primary btn-sm";
    return "btn btn-outline-primary btn-sm"
  }

  setToggleFilter(propertyName) {
    let value = this.state.display[propertyName];
    this.setDisplayProperty(propertyName, !value);
  }

  getStyleRow(logitem) {
    if (this.state.display.showOnlyError && logitem.operation !== "ERROR")
      return {display: "none"};
    if (logitem.operation === "ERROR")
      return {backgroundColor: "#ffd7d9"};
    return {};
  }

  refreshListLog() {
    let uri = 'cherry/api/operationlog/list?';
    console.log("log.refreshListContent http[" + uri + "]");

    this.setDisplayProperty("loading", true);
    this.setState({status: ""});
    var restCallService = RestCallService.getInstance();
    restCallService.getJson(uri, this, this.refreshListLogCallback);
  }

  refreshListLogCallback(httpPayload) {
    this.setDisplayProperty("loading", false);
    if (httpPayload.isError()) {
      console.log("log.refreshListContentCallback: error " + httpPayload.getError());
      this.setState({status: httpPayload.getError()});
    } else {
      this.setState({log: httpPayload.getData()});
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

export default OperationLog;