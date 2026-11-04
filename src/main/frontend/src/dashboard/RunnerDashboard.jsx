// -----------------------------------------------------------
//
// RunnerDashboard
//
// Dashboard for a runner: show overciew, logs, errors, statistics
//
// -----------------------------------------------------------

import React from 'react';
import RunnerCartouche from "../component/RunnerCartouche";
import {Tag} from "carbon-components-react";

class RunnerDashboard extends React.Component {


    constructor(props) {
        super();

        this.state = {
            runner: props.runnerDisplay,
            timestamp: props.timestamp
        };
    }

    componentDidUpdate(prevProps) {
        if (prevProps.timestamp !== this.props.timestamp) {
            this.setState({
                runner: this.props.runnerDisplay,
                timestamp: this.props.timestamp
            });
        }
    }

    render() {
        return (
            <div>
                <div bis_skin_checked="1">
                    <table style={{width: "100%"}}>
                        <tbody>
                        <tr>
                            <td style={{padding: "10px", margin: "20px"}}>
                                <div className="column is-two-thirds" style={{padding: "0px 0px 20px 0px"}}
                                     bis_skin_checked="1">
                                    <h1 className="title is-5 job-type is-hoverable">
                                        <table>
                                            <tbody>
                                            <tr>
                                                <td>
                                                    <RunnerCartouche runner={this.state.runner}/>
                                                </td>
                                                <td style={{padding: "5px 0px 0px 25px"}}> {this.state.runner.name}
                                                    {this.state.runner.statistic && this.state.runner.statistic.executionsFailed > 0 &&
                                                        <Tag type="red"
                                                             title="Fails"> {this.state.runner.statistic.executionsFailed} Fails </Tag>}
                                                    {this.state.runner.statistic && this.state.runner.statistic.executionsBpmnErrors > 0 &&
                                                        <Tag type="red"
                                                             title="BPMN ControllerPage"> {this.state.runner.statistic.executionsBpmnErrors} BPMN
                                                            Errors </Tag>}
                                                    {this.state.runner.nboverthreshold > 0 && <Tag type="purple"
                                                                                                   title="Over threshold"> {this.state.runner.nboverthreshold} Errors </Tag>}
                                                    <br/>
                                                    <br/>
                                                    {this.state.runner.statistic && this.state.runner.statistic.executionsSucceeded} Successful,
                                                    {this.state.runner.statistic && this.state.runner.statistic.executionsFailed} failed,
                                                    {this.state.runner.statistic && this.state.runner.statistic.executionsBpmnErrors} BPMN
                                                    Errors,
                                                    {this.state.runner.topicCount || 0} Jobs waiting<br/>

                                                    <span style={{
                                                        fontStyle: "italic",
                                                        fontSize: "small"
                                                    }}>{this.state.runner.collectionname}&nbsp;({this.state.runner.classrunner})</span>

                                                </td>
                                            </tr>

                                            </tbody>
                                        </table>
                                    </h1>
                                    <h1>
                                        <progress className="progress is-small is-primary" value="1" max="1"></progress>
                                    </h1>
                                    <h2 style={{padding: "5px 0px 0px 25px", fontSize: "70%"}}>
                                        <i>Average {this.state.runner.performance.averageTimeInMs} ms
                                            Peak {this.state.runner.performance.peakTimeInMs} ms </i></h2></div>
                            </td>
                        </tr>
                        </tbody>
                    </table>

                </div>
            </div>);
    }

}

export default RunnerDashboard;
