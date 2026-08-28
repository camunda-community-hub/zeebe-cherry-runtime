// -----------------------------------------------------------
//
// RunnerChart
//
// Manage the main application
//
// -----------------------------------------------------------

import React from 'react';
import Chart from "../component/Chart";


class RunnerChart extends React.Component {


    constructor(props) {
        super();
        this.state = {
            runner: props.runnerDisplay,
            type: props.type,
            title: props.title,
            timestamp: props.timestamp
        };
    }

    componentDidUpdate(prevProps) {

        if (prevProps.timestamp !== this.props.timestamp) {
            // console.log("RunnerChart.componentDidUpdate: Change");
            this.setState({
                runner: this.props.runnerDisplay,
                timestamp: this.props.timestamp
            });
        }
    }

    render() {
        const executionsTopic = (this.state.type === "ExecutionsTopic" && this.state.runner && this.state.runner.performance)
            ? this.getExecutionsTopic() : null;
        return (
            <div style={{border: "1px solid", padding: "5px", width: "100%", height: "150px", boxSizing: "border-box"}}>
                {this.state.type === "Executions" && this.state.runner && this.state.runner.performance &&
                    <Chart type="HorizontalBar" dataList={this.getExecutions()} oneColor={true}
                           options={{
                               title: this.state.title,
                               showXLabel: false,
                               showYLabel: true,
                               width: 400,
                               height: 120,
                               showGrid: false
                           }}
                           title="execution"/>
                }
                {this.state.type === "ExecutionsShort" && this.state.runner && this.state.runner.performance &&
                    <Chart type="HorizontalBar" dataList={this.getExecutions()} oneColor={true}
                           title="Executions"
                           options={{
                               showXLabel: true,
                               showYLabel: true,
                               showGrid: false,
                               showLegend: false,
                               width: 500,
                               height: 120,
                           }}/>
                }
                {this.state.type === "ExecutionsTopic" && this.state.runner && this.state.runner.performance &&
                    <Chart type="HorizontalBar"
                           dataList={executionsTopic.executions}
                           dataList2={executionsTopic.topicCount}
                           label="Executions" label2="Pending Jobs"
                           oneColor={true}
                           options={{
                               showXLabel: false,
                               showYLabel: false,
                               showGrid: false,
                               showLegend: true
                           }}/>
                }
                {this.state.type === "DurationsAvg" && this.state.runner && this.state.runner.performance &&
                    <Chart type="HorizontalBar" dataList={this.getDurationsAvg()} oneColor={true}
                           options={{
                               title: this.state.title,
                               showXLabel: false,
                               showYLabel: true,
                               width: 200,
                               height: 120,
                               showGrid: false
                           }}
                           title="Duration ms (average)"/>
                }
                {this.state.type === "DurationsPic" && this.state.runner && this.state.runner.performance &&
                    <Chart type="HorizontalBar" dataList={this.getDurationsPic()} oneColor={true}
                           options={{
                               title: this.state.title,
                               showXLabel: false,
                               showYLabel: true,
                               width: 200,
                               height: 120,
                               showGrid: false
                           }}
                           title="Duration ms (pic)"/>
                }
                {this.state.type === "Errors" && this.state.runner && this.state.runner.performance &&
                    <Chart type="HorizontalBar" dataList={this.getErrors()} oneColor={true}
                           options={{
                               title: this.state.title,
                               showXLabel: false,
                               showYLabel: true,
                               width: 200,
                               height: 120,
                               showGrid: false
                           }}
                           title="Errors"/>
                }
                {this.state.type === "TopicCount" && this.state.runner && this.state.runner.performance &&
                    <Chart type="HorizontalBar" dataList={this.getTopicCount()} oneColor={true}
                           options={{
                               title: this.state.title,
                               showXLabel: false,
                               showYLabel: true,
                               width: 300,
                               height: 100,
                               showGrid: false
                           }}
                           title="Pending Jobs (Topic Count)"/>
                }
                {this.state.type === "TopicCountShort" && this.state.runner && this.state.runner.performance &&
                    <Chart type="HorizontalBar" dataList={this.getTopicCount()} oneColor={true}
                           options={{
                               showXLabel: true,
                               showYLabel: true,
                               showGrid: false,
                               width: 300,
                               height: 150
                           }}/>
                }
            </div>
        )

    }


    /**
     * Graph expect [{value:123, label="Month"}]
     *
     * runner contains {performance : {listIntervals: [
     *    {slot: "103D00:15", executions: 0, sumOfExecutionTime: 0, executionsSucceeded: 0, executionsFailed: 0,
     *     topicCount: 0, …}
     *    }}
     */
    getExecutions() {
        const result = [];
        this.state.runner.performance.listIntervals.forEach((element, _index, _array) => {
            let record = {label: this.getHourLabel(element.humanTimeSlot), value: element.executions};
            result.push(record);
        });
        return result;
    }

    /**
     * Executions (bar) and topicCount - the pending job count (line) - for the same intervals,
     * so RunnerChart can plot them together on one graph.
     */
    getExecutionsTopic() {
        const executions = [];
        const topicCount = [];
        this.state.runner.performance.listIntervals.forEach((element, _index, _array) => {
            executions.push({label: element.slot, value: element.executions});
            topicCount.push({label: element.slot, value: element.topicCount});
        });
        return {executions, topicCount};
    }

    /**
     * humanTimeSlot is formatted as "yyyy-MM-dd HH:mm" - only the "HH:mm" part is useful on the axis.
     */
    getHourLabel(humanTimeSlot) {
        if (!humanTimeSlot)
            return "";
        const spaceIndex = humanTimeSlot.indexOf(" ");
        return spaceIndex === -1 ? humanTimeSlot : humanTimeSlot.substring(spaceIndex + 1);


    /**
     * humanTimeSlot is formatted as "yyyy-MM-dd HH:mm" - only the "HH:mm" part is useful on the axis.
     */
    getHourLabel(humanTimeSlot) {
        if (!humanTimeSlot)
            return "";
        const spaceIndex = humanTimeSlot.indexOf(" ");
        return spaceIndex === -1 ? humanTimeSlot : humanTimeSlot.substring(spaceIndex + 1);
    }

    getDurationsAvg() {
        const result = [];
        this.state.runner.performance.listIntervals.forEach((element, _index, _array) => {
            let record = {label: element.slot, value: element.averageTimeInMs};
            result.push(record);
        });
        return result;
    }

    getDurationsPic() {
        const result = [];
        this.state.runner.performance.listIntervals.forEach((element, _index, _array) => {
            let record = {label: element.slot, value: element.peakTimeInMs};
            result.push(record);
        });
        return result;
    }

    getErrors() {
        const result = [];
        this.state.runner.performance.listIntervals.forEach((element, _index, _array) => {
            let record = {label: element.slot, value: element.executionsBpmnErrors + element.executionsFailed};
            result.push(record);
        });
        return result;
    }

    getTopicCount() {
        const result = [];
        this.state.runner.performance.listIntervals.forEach((element, _index, _array) => {
            let record = {label: element.slot, value: element.topicCount};
            result.push(record);
        });
        return result;
    }
}

export default RunnerChart;
