// -----------------------------------------------------------
//
// RunnerMonitoring
//
// Monitor a runner
//
// -----------------------------------------------------------

import React from 'react';
import {Button, Tab, Tabs} from "react-bootstrap";
import {NumberInput} from "carbon-components-react";

import RestCallService from "../services/RestCallService";
import RunnerChart from "./RunnerChart";
import {ArrowRepeat} from "react-bootstrap-icons";
import ControllerPage from "../component/ControllerPage";

class RunnerMonitoring extends React.Component {


  constructor(props) {
    super();
    this.state = {
      runner: props.runnerDisplay,
      timestamp: props.timestamp,
      status: "",
      display: {
        loading: false,
        pageNumberErrors: 0,
        pageNumberEExecutions: 0,
        pageNumberOperations: 0,
      },
      operations: {}
    };
    this.refreshState = this.refreshState.bind(this);
  }

  componentDidMount() {
    console.log("RunnerMonitoring.componentDidMount: BEGIN runner={" + this.state.runner.name);
    this.refreshState();
    this.refreshLoadAllStatistics();

    console.log("RunnerMonitoring.componentDidMount: END");
  }


  componentDidUpdate(prevProps) {
    if (prevProps.timestamp !== this.props.timestamp) {
      // console.log("RunnerMonitoring.componentDidUpdate: Change");
      this.setState({
        runner: this.props.runnerDisplay,
        timestamp: this.props.timestamp
      });
    }
  }

  /*           {JSON.stringify(this.state.runners, null, 2) } */

  /* className row does not work height gross and gross */
  render() {
    console.log("RunnerMonitoring.render ");
    return (
      <div>
        <div style={{position: "absolute", top: "0", right: "0"}}>
          <Button className="btn btn-success btn-sm"
                  onClick={() => this.refreshLoadAllStatistics()}
                  disabled={this.state.display.loading}>
            <ArrowRepeat/> Refresh
          </Button>
        </div>
        <Tabs defaultActiveKey="overview">
          <Tab eventKey="overview" title="Overview">
            <ControllerPage errorMessage={this.state.status} loading={this.state.display.loading}/>


            <div className="row" style={{paddingTop: "10px"}}>
              <div>
                <Button className={this.getCssStartButton()}
                        style={{borderRadius: "2px", border: "1px solid"}}
                        disabled={this.state.runner.active}
                        onClick={() => this.startRunner()}>
                  {this.state.labelBtnStart}
                </Button>
                <Button className={this.getCssStopButton()}
                        style={{borderRadius: "2px", border: "1px solid"}}
                        disabled={!this.state.runner.active}
                        onClick={() => this.stopRunner()}>
                  {this.state.labelBtnStop}
                </Button>
              </div>
            </div>
            <div className="row" style={{paddingTop: "10px"}}>
              <div className="col-md-4">
                <NumberInput label="Threshold (in milliseconds)"
                             disabled={this.state.display.loading}
                             readonly="true"
                             value={this.state.runner.threshold}
                             onChange={(event) => {
                               this.setThreshold(event.target.value);
                             }}/>
              </div>
            </div>
            <div className="row" style={{paddingTop: "10px"}}>
              {this.state.runner.statistic.executions} total,
              {this.state.runner.statistic.executionsSucceeded} succeeded,
              {this.state.runner.statistic.executionsFailed} failed,
              {this.state.runner.statistic.executionsBpmnErrors} BPMN Error
            </div>
            <div className="row">
              <progress class="progress is-small is-primary"
                        value="{this.state.runner.statistic.executionsSucceeded}"
                        max="{this.state.runner.statistic.executions}"></progress>

              <div>
                <i>Average {this.state.runner.performance.averageTimeInMs} ms,
                  Pic {this.state.runner.performance.picTimeInMs} ms</i>
              </div>
            </div>


          </Tab>

          <Tab eventKey="graph" title="Graph">
            <div className="container">

              <table style={{width: "100%"}}>
                <tr>
                  <td style={{padding: "20px", maxHeight: "50px"}}>
                    <RunnerChart type="Executions" runnerDisplay={this.state.runner} title={true}/>
                  </td>
                  <td style={{padding: "20px", maxHeight: "50px"}}>
                    <RunnerChart type="Errors" runnerDisplay={this.state.runner} title={true}/>
                  </td>
                </tr>
                <tr>
                </tr>
                <tr>
                  <td style={{padding: "20px", maxHeight: "50px"}}>
                    <RunnerChart type="DurationsAvg" runnerDisplay={this.state.runner} title={true}/>
                  </td>
                  <td style={{padding: "20px", maxHeight: "50px"}}>
                    <RunnerChart type="DurationsPic" runnerDisplay={this.state.runner} title={true}/>
                  </td>
                </tr>

              </table>
            </div>
          </Tab>
          <Tab eventKey="statistics" title="Statistics">
            <h1>Statistics</h1>
            <table className="table">
              <thead>
              <tr>
                <th>Date</th>
                <th>Execution</th>
                <th>Success</th>
                <th>Bpmn Errors</th>
                <th>Fails</th>
                <th>average Time</th>
                <th>Pic time</th>
              </tr>
              </thead>
              {this.state.runner && this.state.runner.performance &&
                this.state.runner.performance.listIntervals.map((interval, _index) =>
                  <tr>
                    <td style={{fontSize: "10px", whiteSpace: "nowrap"}}>{interval.humanTimeSlot}</td>
                    <td style={{textAlign: "right"}}>{interval.executions}</td>
                    <td style={{textAlign: "right"}}>{interval.executionSucceeded}</td>
                    <td style={{textAlign: "right"}}>{interval.executionBpmnErrors}</td>
                    <td style={{textAlign: "right"}}>{interval.executionFailed}</td>
                    <td style={{textAlign: "right"}}>{interval.averageTimeInMs} ms</td>
                    <td style={{textAlign: "right"}}>{interval.picTimeInMs} ms</td>
                  </tr>
                )}
            </table>

          </Tab>
          <Tab eventKey="errors" title="Errors">
            <h1>Errors</h1>
            <table className="table">
              <thead>
              <tr>
                <th>Date</th>
                <th>Status</th>
                <th>Code</th>
                <th>Explanation</th>
              </tr>
              </thead>
              {this.state.operations && this.state.operations.errors &&
                this.state.operations.errors.map((error, _index) =>
                  <tr>
                    <td style={{fontSize: "10px", whiteSpace: "nowrap"}}>{error.executionTime}</td>
                    <td>{error.status}</td>
                    <td>{error.errorCode}</td>
                    <td>{error.errorExplanation}</td>
                  </tr>
                )}
            </table>
          </Tab>

          <Tab eventKey="Executions" title="Executions">
            <table className="table">
              <thead>
              <tr>
                <th>Date</th>
                <th>Status</th>
                <th>Duration</th>
              </tr>
              </thead>
              {this.state.operations && this.state.operations.executions &&
                this.state.operations.executions.map((exec, _index) =>
                  <tr style={this.getStyleRowOperation(exec)}>
                    <td style={{fontSize: "10px", whiteSpace: "nowrap"}}>{exec.executionTime}</td>
                    <td>{exec.status}</td>
                    <td style={{textAlign: "right"}}>{exec.durationms} ms</td>
                  </tr>
                )}
            </table>
          </Tab>

          <Tab eventKey="operation" title="Operations">
            <h1>Operations</h1>
            <table className="table">
              <thead>
              <tr>
                <th>Date</th>
                <th>Operation</th>
                <th>Host name</th>
              </tr>
              </thead>
              {this.state.operations && this.state.operations.operations &&
                this.state.operations.operations.map((operation, _index) =>
                  <tr>
                    <td style={{fontSize: "10px", whiteSpace: "nowrap"}}>{operation.executionTime}</td>
                    <td>{operation.operation}</td>
                    <td>{operation.hostname}</td>

                  </tr>
                )}

            </table>

          </Tab>
        </Tabs>


      </div>)
  }


  getCssStartButton() {
    let value = "btn btn-light";
    if (this.state.runner.active)
      value = "btn btn-success";
    return value;
  }

  getCssStopButton() {
    let value = "btn btn-light";
    if (!this.state.runner.active)
      value = "btn btn-danger";
    return value;

  }

  getStyleRowOperation(exec) {
    if (exec.status === "ERROR")
      return {backgroundColor: "#ffd7d9"};
    return {};

  }

  stopRunner() {
    console.log("RunnerMonitoring.stopRunner");
    this.setState({labelBtnStop: "Stopping...", status: ""});
    var restCallService = RestCallService.getInstance();
    restCallService.putJson('cherry/api/runner/stop?runnertype=' + this.state.runner.type, {}, this, this.operationRunnerCallback);
  }

  startRunner() {
    console.log("RunnerMonitoring.startRunner");
    this.setState({labelBtnStart: "Starting...", status: ""});
    var restCallService = RestCallService.getInstance();
    restCallService.putJson('cherry/api/runner/start?runnertype=' + this.state.runner.type, {}, this, this.operationRunnerCallback);
  }


  setThreshold(value) {
    let runnerinfo = this.state.runner;
    runnerinfo.threshold = value;
    this.setState({runner: runnerinfo})
  }

  operationRunnerCallback(httpResponse) {

    if (httpResponse.isError()) {
      console.log("RunnerMonitoring.operationRunnerCallback: error " + httpResponse.getError());
      this.setState({status: httpResponse.getError()});
    } else {
      let runnerinfo = this.state.runner;
      runnerinfo.active = httpResponse.getData().active;
      this.setState({runner: runnerinfo})
    }
    this.refreshState();
  }


  refreshState() {
    if (this.state.runner.active) {
      this.setState({labelBtnStart: "Started", labelBtnStop: "Stop"});
    } else {
      this.setState({labelBtnStart: "Start", labelBtnStop: "Stopped"});
    }
  }

  refreshLoadAllStatistics() {
    this.loadStatistics("ERRORS", 24, this.state.display.pageNumberErrors);
    this.loadStatistics("EXECUTIONS", 24, this.state.display.pageNumberEExecutions);
    this.loadStatistics("OPERATIONS", 24, this.state.display.pageNumberOperations);


  }

  loadStatistics(operationtype, numberOfMonitoring, pageNumber) {
    let uri = 'cherry/api/runner/operations?runnertype=' + this.state.runner.type
      + "&operationtype=" + operationtype
      + "&nbhoursmonitoring=" + numberOfMonitoring
      + "&pagenumber=" + pageNumber
      + "&rowsperpage=20"
      + "&timezoneoffset=" + (new Date()).getTimezoneOffset();
    console.log("RunnerMonitoring.loadStatisticsCallbackCallback http[" + uri + "]");

    this.setDisplayProperty("loading", true);
    this.setState({status: ""});
    var restCallService = RestCallService.getInstance();
    restCallService.getJson(uri, this, this.loadStatisticsCallback);
  }

  loadStatisticsCallback(httpPayload) {
    console.log("DashBoard.loadStatisticsCallbackCallback");
    this.setDisplayProperty("loading", false);
    if (httpPayload.isError()) {
      console.log("Dashboard.refreshDashboardCallback: error " + httpPayload.getError());
      this.setState({status: "Error"});
    } else {

      let operationsContent = this.state.operations;
      if (!operationsContent) {
        operationsContent = {};
      }
      // Complete the variable
      let operationsServer = httpPayload.getData();
      for (var i in operationsServer) {
        operationsContent[i] = operationsServer[i];
      }
      this.setState({operations: operationsContent});

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

export default RunnerMonitoring;