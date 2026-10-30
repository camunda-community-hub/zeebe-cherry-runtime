// -----------------------------------------------------------
//
// RunnerAnalysisTags
//
// Vertical stack of tags built from runner.analysis (see RunnerThroughput.Analysis)
//
// -----------------------------------------------------------

import React from 'react';
import {Tag} from "carbon-components-react";

class RunnerAnalysisTags extends React.Component {

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
            <div style={{display: "flex", flexDirection: "column", gap: "4px"}}>
                {this.getAnalysisTags().map((tag, index) =>
                    <Tag key={index} type={tag.type} size="sm">{tag.label}</Tag>
                )}
            </div>
        )
    }

    /**
     * Carbon Tag has no "orange" type, so the MEDIUM/INCREASE (caution) level uses "magenta" instead.
     */
    getThroughputTag(value, name) {
        switch (value) {
            case "LOW":
                return {label: "Low " + name, type: "green"};
            case "MEDIUM":
                return {label: "Medium " + name, type: "magenta"};
            case "HIGH":
                return {label: "High " + name, type: "red"};
            default:
                return null;
        }
    }

    getOrientationTag(value, name) {
        switch (value) {
            case "HIGHDECREASE":
                return {label: "High decrease " + name, type: "green"};
            case "DECREASE":
                return {label: "Decrease " + name, type: "green"};
            case "STABLE":
                return {label: "Stable " + name, type: "blue"};
            case "INCREASE":
                return {label: "Increase " + name, type: "magenta"};
            case "HIGHINCREASE":
                return {label: "High increase " + name, type: "red"};
            default:
                return null;
        }
    }

    /**
     * Build the vertical tag list from runner.analysis, in the same field order as
     * RunnerThroughput.Analysis: throughput, backlog, execution, failure, errors, backLogHuman.
     */
    getAnalysisTags() {
        const analysis = this.state.runner && this.state.runner.analysis;
        if (!analysis)
            return [];

        const tags = [
            this.getThroughputTag(analysis.throughput, "throughput"),
            this.getOrientationTag(analysis.backlog, "backlog"),
            this.getOrientationTag(analysis.execution, "execution"),
            this.getThroughputTag(analysis.failure, "failure"),
            this.getThroughputTag(analysis.errors, "errors"),
            analysis.backLogHuman ? {label: "Backlog: " + analysis.backLogHuman, type: "gray"} : null
        ];
        return tags.filter(tag => tag !== null);
    }
}

export default RunnerAnalysisTags;
