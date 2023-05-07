// -----------------------------------------------------------
//
// Dashboard
//
// Manage the dashboard. Root component
//
// -----------------------------------------------------------

import React from 'react';
import {Button, Select, TextInput} from "carbon-components-react";
import {ArrowRepeat, XCircle} from 'react-bootstrap-icons';

import GeneralDashboard from "./GeneralDashboard";
import RunnerDashboard from "./RunnerDashboard";
import RestCallService from "../services/RestCallService";
import ControllerPage from "../component/ControllerPage";

import RunnerChart from "./RunnerChart";


class Dashboard extends React.Component {


  constructor(_props) {
    super();
    this.state = {
      dashboard: {
        details: [],
      },
      display: {
        loading: true,
        orderBy: "nameAsc",
        period: "ONEDAY",
        showActif: true,
        showInactif: true,
        showWorker: true,
        showConnector: true,
        showOnlyError: false,
        showOnlyOverThreshold: false,
        showFrameworkRunner: true,
        filterSearch:""

      },


    };
    this.setPeriod = this.setPeriod.bind(this);
    this.setOrderBy = this.setOrderBy.bind(this)
  }

  componentDidMount() {
    this.refreshDashboard(this.state.display.period, this.state.display.orderBy);
  }


  render() {
    // console.log("dashboard.render display="+JSON.stringify(this.state.display));
    return (<div className={"container"}>
      <GeneralDashboard dashboard={this.state.dashboard} dashboardComponent={this}
                        timestamp={this.state.dashboard.timestamp}/>
      <div className="row">
        <div className="col-md-1">
          <h1 className="title">Details</h1>
        </div>
        <div className="col-md-6">
          <div className="btn-group" role="group" style={{padding: "10px 10px 10px 10px"}}>

            <button className={this.getButtonClass(this.state.display.showActif)}
                    style={{marginLeft: "10px", fontSize: "10px"}}
                    disabled={this.state.display.loading}
                    onClick={() => this.setToggleFilter("showActif")}>
              Actif
            </button>
            <button className={this.getButtonClass(this.state.display.showInactif)}
                    style={{fontSize: "10px"}}
                    disabled={this.state.display.loading}
                    onClick={() => this.setToggleFilter("showInactif")}>
              Inactif
            </button>

            <button className={this.getButtonClass(this.state.display.showWorker)}
                    style={{marginLeft: "10px", fontSize: "10px"}}
                    disabled={this.state.display.loading}
                    onClick={() => this.setToggleFilter("showWorker")}>
              Worker
            </button>
            <button className={this.getButtonClass(this.state.display.showConnector)}
                    style={{fontSize: "10px"}}
                    disabled={this.state.display.loading}
                    onClick={() => this.setToggleFilter("showConnector")}>
              Connector
            </button>

            <button className={this.getButtonClass(this.state.display.showOnlyError)}
                    style={{marginLeft: "10px", fontSize: "10px"}}
                    disabled={this.state.display.loading}
                    onClick={() => this.setToggleFilter("showOnlyError")}>
              <XCircle style={{color:"red"}}/> Only Errors
            </button>
            <button className={this.getButtonClass(this.state.display.showOnlyOverThreshold)}
                    style={{fontSize: "10px"}}
                    disabled={this.state.display.loading}
                    onClick={() => this.setToggleFilter("showOnlyOverThreshold")}>
              Over Threshold
            </button>

            <button className={this.getButtonClass(this.state.display.showFrameworkRunner)}
                    style={{marginLeft: "10px", fontSize: "10px"}}
                    disabled={this.state.display.loading}
                    onClick={() => this.setToggleFilter("showFrameworkRunner")}>
              <img src="/img/cherries.png" width="20" height="20" alt="cherry"/>Framework Runner
            </button>

          </div>
        </div>
        <div className="col-md-1">
          <TextInput labelText="Filter"
                     disabled={this.state.display.loading}
                     value={this.state.display.filterSearch}
                     style={{width: "80px"}}
                     onChange={(event) => this.setSearchFilter( event.target.value)} />
        </div>
        <div className="col-md-2">
          <Select
            value={this.state.display.orderBy}
            labelText="order by"
            disabled={this.state.display.loading}
          onChange={(event) => this.setOrderBy(event.target.value)}>
            <option value="nameAsc">Name (asc)</option>
            <option value="nameDes">Name (desc)</option>
            <option value="execAsc">Execution (asc)</option>
            <option value="execDes">Execution (desc)</option>
            <option value="failAsc">Failure (asc)</option>
            <option value="failDes">Failure (desc)</option>
          </Select>
        </div>
        <div className="col-md-1">
          <Button className="btn btn-success btn-sm"
                  onClick={() => this.refreshDashboard(this.state.display.period, this.state.display.orderBy)}
                  disabled={this.state.display.loading}>
            <ArrowRepeat/> Refresh
          </Button>
        </div>
      </div>

      <div className="row" style={{width: "100%"}}>
        <div className="col-md-12">
          <ControllerPage errorMessage={this.state.error} loading={this.state.display.loading}/>
        </div>
      </div>

      <div className="row" style={{width: "100%"}}>
        <div className="col-md-12">
          <table width="100%">
            {this.state.dashboard ? this.state.dashboard.details.map((runner, _index) =>
              <tr style={this.getStyleRow(runner)}>
                <td style={{verticalAlign: "top"}}>
                  <RunnerDashboard runnerDisplay={runner} timestamp={this.state.dashboard.timestamp}/></td>
                <td style={{verticalAlign: "top"}}>
                  <RunnerChart type="Executions" runnerDisplay={runner} title={false}
                               timestamp={this.state.dashboard.timestamp}/>
                </td>
              </tr>
            ) : <div/>}
          </table>
        </div>
      </div>
    </div>)

  }

  getStyleRow(runner) {
    if (!this.state.display.showActif && runner.active)
      return {display: "none"};
    if (!this.state.display.showInactif && !runner.active)
      return {display: "none"};
    if (!this.state.display.showWorker && runner.classrunner === "worker")
      return {display: "none"};
    if (!this.state.display.showConnector && runner.classrunner === "connector")
      return {display: "none"};
    if (this.state.display.showOnlyError && runner.nbfail === 0)
      return {display: "none"};
    if (this.state.display.showOnlyOverThreshold && runner.nboverthreshold === 0)
      return {display: "none"};
    if (!this.state.display.showFrameworkRunner && runner.frameworkrunner === "true")
      return {display: "none"};

    if (this.state.display.filterSearch
      && !runner.name.toLowerCase().includes(this.state.display.filterSearch.toLowerCase())) {
    return {display: "none"};
  }


    return {};
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

  setPeriod(value) {
    console.log("DashBoard.setperiod: " + value);
    this.setDisplayProperty("period", value);
    this.refreshDashboard(value, this.state.display.orderBy);
  }

  setOrderBy(value) {
    // console.log("DashBoard.setOrderBy: " + value);
    this.setDisplayProperty("orderBy", value);
    this.refreshDashboard(this.state.display.period, value);

  }

  setSearchFilter(value) {
    this.setDisplayProperty("filterSearch", value);
  }

  refreshDashboard(period, orderBy) {
    let uri = 'cherry/api/runner/dashboard?period=' + period + "&orderBy=" + orderBy;
    console.log("DashBoard.refreshDashboard http[" + uri + "]");

    this.setDisplayProperty("loading", true);
    this.setState({status: ""});
    var restCallService = RestCallService.getInstance();
    restCallService.getJson(uri, this, this.refreshDashboardCallback);
  }

  refreshDashboardCallback(httpPayload) {
    this.setDisplayProperty("loading", false);
    if (httpPayload.isError()) {
      console.log("Dashboard.refreshDashboardCallback: error " + httpPayload.getError());
      this.setState({status: "Error"});
    } else {
      let firstRunner = httpPayload.getData().details[0];
      console.log("dashboard: RESTCALLBACK first is [" + JSON.stringify(firstRunner.name) + "]");
      this.setState({dashboard: httpPayload.getData()});

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

export default Dashboard;
