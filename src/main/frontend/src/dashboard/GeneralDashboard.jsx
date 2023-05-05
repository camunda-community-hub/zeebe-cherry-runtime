// -----------------------------------------------------------
//
// GeneralDashboard
//
// Display the main dashboard
//
// -----------------------------------------------------------

import React from 'react';
import {Select} from 'carbon-components-react';

class GeneralDashboard extends React.Component {


  constructor(props) {
    super();
    this.state = {
      dashboard: props.dashboard,
      timestamp: props.timestamp,
      period: "ONEDAY"
    };
    this.dashboardComponent = props.dashboardComponent;
    this.setPeriod = this.setPeriod.bind(this);
  }

  componentDidUpdate(prevProps) {
    if (prevProps.timestamp !== this.props.timestamp) {
      // console.log("RunnerDashboard.componentDidUpdate: Change");
      this.setState({
        dashboard: this.props.dashboard,
        timestamp: this.props.timestamp
      });
    }
  }

  render() {
    // console.log("GeneralDashboard.render: period="+this.state.period);
    return (
      <div className="box container">
        <div class="row">
          <div className="col-md-8">
            <h1 className="title is-5"> total jobs in last period </h1>
          </div>
          <div className="col-md-4">
            <Select
              value={this.state.period}
              labelText="Period"
              onChange={(event) => this.setPeriod(event.target.value)}>
              <option value="FOURHOUR">Last 4 Hours</option>
              <option value="ONEDAY">last 24 Hours</option>
              <option value="ONEWEEK">Last week</option>
              <option value="ONEMONTH">Last Month</option>
            </Select>
          </div>
        </div>
        <div class="row">
          <div className="col-md-8">
            <h1 className="title is-5">
              <span className="has-text-primary"
                    style={{paddingRight: "10px"}}>{this.state.dashboard.totalExecutionsSucceeded} Successful</span>
              <span class="has-text-danger"
                    style={{paddingRight: "10px"}}>{this.state.dashboard.totalExecutionsFailed} Failed</span>
              <span className="has-text-danger">{this.state.dashboard.totalExecutionsBpmnErrors} BPMN Errors</span>
            </h1>
          </div>
        </div>

        <div class="row">
          <progress className="progress is-large is-primary" value="1" max="1"></progress>
        </div>
      </div>

    );
  }

  setPeriod(value) {
    console.log("SetPeriod set attribut:" + value);

    this.setState({period: value});

    this.dashboardComponent.setPeriod(value);
  }

}

export default GeneralDashboard;
