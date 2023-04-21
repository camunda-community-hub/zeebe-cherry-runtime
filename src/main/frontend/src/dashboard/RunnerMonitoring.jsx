// -----------------------------------------------------------
//
// RunnerMonitoring
//
// Monitor a runner
//
// -----------------------------------------------------------

import React from 'react';
import {Button, Tab, Tabs} from "react-bootstrap";

import RestCallService from "../services/RestCallService";
import RunnerChart from "./RunnerChart";

class RunnerMonitoring extends React.Component {


  constructor(props) {
    super();
    this.state = {runner: props.runnerDisplay, timestamp: props.timestamp};
    this.refreshState = this.refreshState.bind(this);
  }

  componentDidMount() {
    console.log("Definition.componentDidMount: BEGIN");
    this.refreshState();
    console.log("Definition.componentDidMount: END");
  }

  componentDidUpdate (prevProps) {
    if (prevProps.timestamp !== this.props.timestamp) {
      // console.log("RunnerDashboard.componentDidUpdate: Change");
      this.setState({
        runner: this.props.runnerDisplay,
        timestamp: this.props.timestamp
      });
    }
  }

  /*           {JSON.stringify(this.state.runners, null, 2) } */
  render() {
    return (<div>

      <Tabs defaultActiveKey="overview">
        <Tab eventKey="overview" title="Overview">
          <div style={{paddingTop: "10px"}}>
            <div>
              <Button className={this.getCssStartButton()}
                      style={{borderRadius: "2px", border: "1px solid"}}
                      disabled={this.state.runner.active}
                      onClick={()=>this.startRunner()}>
                {this.state.labelBtnStart}
              </Button>
              <Button className={this.getCssStopButton() }
                      style={{borderRadius: "2px", border: "1px solid"}}
                      disabled={! this.state.runner.active}
                      onClick={()=>this.stopRunner()}>
                {this.state.labelBtnStop}
              </Button>
            </div>

          </div>
          <div style={{padding: "0px 0px 20px 0px"}}>
            {this.state.runner.statistic.executions} total,
            {this.state.runner.statistic.executionsSucceeded} succeeded,
            {this.state.runner.statistic.executionsFailed} failed,
            {this.state.runner.statistic.executionsBpmnErrors} BPMN Error
          </div>
          <progress class="progress is-small is-primary"
                    value="{this.state.runner.statistic.progressExecutionsSucceeded}"
                    max="{this.state.runner.statistic.progressExecutions}"></progress>
          <div>
            <i>Average {this.state.runner.performance.averageTimeInMs} ms,
              Pic {this.state.runner.performance.picTimeInMs} ms</i>
          </div>

          <div className="container">
            <div className="row" style={{height: "200px", display: "none"}}>
              <div className="col-sm">
                <RunnerChart type="Executions" runnerDisplay={this.state.runner} title={true}/>
              </div>
              <div className="col-sm">
                2eme
                <RunnerChart type="Durations" runnerDisplay={this.state.runner} title={true}/>
              </div>
              <div className="col-sm">
                3eme
                <RunnerChart type="Errors" runnerDisplay={this.state.runner} title={true}/>
              </div>
            </div>
          </div>

        </Tab>

        <Tab eventKey="statistics" title="Statistics">
          Soon the statistics here
        </Tab>
        <Tab eventKey="error" title="Errors">
          <table className="table">
            <thead>
            <tr>
              <th>Date</th>
              <th>Errors</th>
              <th>Explanation</th>
            </tr>
            </thead>
          </table>
        </Tab>
        <Tab eventKey="operation" title="Operations">
          <table className="table">
            <thead>
            <tr>
              <th>Date</th>
              <th>Operation</th>
              <th>Duration</th>
            </tr>
            </thead>
          </table>

        </Tab>
        <Tab eventKey="logs" title="Logs">
          <table className="table">
            <thead>
            <tr>
              <th>Date</th>
              <th>Logs</th>
            </tr>
            </thead>
          </table>
        </Tab>
      </Tabs>


    </div>)
  }


  getCssStartButton() {
    let value="btn btn-light";
    if (this.state.runner.active)
      value= "btn btn-success";
    console.log("getCssStartButton:runneractive="+this.state.runner.active+" CssStartButton="+value);
    return value;
  }

  getCssStopButton() {
    let value="btn btn-light";
    if (! this.state.runner.active)
      value="btn btn-danger";
    console.log("getCssStopButton:runneractive="+this.state.runner.active+" CssStopButton="+value);
    return value;

  }

  stopRunner() {
    console.log("RunnerMonitoring.stopRunner");
    this.setState({labelBtnStop: "Stopping..."});
    var restCallService = RestCallService.getInstance();
    restCallService.putJson('cherry/api/runner/stop?name=' + this.state.runner.name, this, {}, this.operationRunnerCallback);
  }

  startRunner() {
    console.log("RunnerMonitoring.startRunner");
    this.setState({labelBtnStart: "Starting..."});
    var restCallService = RestCallService.getInstance();
    restCallService.putJson('cherry/api/runner/start?name=' + this.state.runner.name, this,{}, this.operationRunnerCallback);
  }

  operationRunnerCallback(httpResponse) {
    let runnerinfo = this.state.runner;
    runnerinfo.active = httpResponse.getData().active;

    this.setState({runner : runnerinfo})
    this.refreshState();
  }

  refreshState() {
    if (this.state.runner.active) {
      this.setState({labelBtnStart: "started", labelBtnStop: "stop"});
    } else {
      this.setState( {labelBtnStart: "start", labelBtnStop: "stopped"});
    }
  }
}

export default RunnerMonitoring;