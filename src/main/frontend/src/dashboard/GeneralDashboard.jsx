// -----------------------------------------------------------
//
// GeneralDashboard
//
// Display the main dashboard
//
// -----------------------------------------------------------

import React from 'react';

class GeneralDashboard extends React.Component {


    constructor(props) {
        super();
        this.state = {
            dashboard: props.dashboard,
            timestamp: props.timestamp
        };
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
        return (
            <div className="box container">
                <div className="row">
                    <div className="col-md-12">
                        <h1 className="title is-5"> total jobs in last period </h1>
                    </div>
                </div>
                <div className="row">
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

                <div className="row">
                    <progress className="progress is-large is-primary" value={this.state.dashboard.totalExecutions}
                              max={this.state.dashboard.totalExecutionsSucceeded}></progress>
                </div>
            </div>

        );
    }

}

export default GeneralDashboard;
