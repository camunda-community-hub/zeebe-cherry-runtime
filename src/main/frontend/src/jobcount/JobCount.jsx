// -----------------------------------------------------------
//
// JobCount
//
// Job count page: same look and feel as the Dashboard, focused on the
// number of pending jobs per runner.
//
// -----------------------------------------------------------

import React from 'react';
import {Button, Select, TextInput} from "carbon-components-react";
import {ArrowRepeat} from 'react-bootstrap-icons';

import RunnerCartouche from "../component/RunnerCartouche";
import RunnerAnalysisTags from "../dashboard/RunnerAnalysisTags";
import RunnerChart from "../dashboard/RunnerChart";
import ControllerPage from "../component/ControllerPage";
import RestCallService from "../services/RestCallService";

class JobCount extends React.Component {

    constructor(_props) {
        super();
        this.state = {
            dashboard: {
                details: [],
            },
            display: {
                loading: true,
                filterValue: "all",
                orderBy: "nameAsc",
                filterSearch: ""
            },
            status: ""
        };
    }

    componentDidMount() {
        this.refreshJobCount();
    }

    render() {
        return (<div className={"container"}>
            <div className="row">
                <div className="col-md-1">
                    <h1 className="title">Job count</h1>
                </div>
                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-12">
                        <ControllerPage errorMessage={this.state.status} loading={this.state.display.loading}/>
                    </div>
                </div>

                <div className="col-md-6">
                    <div className="btn-group" role="group" style={{padding: "10px 10px 10px 10px"}}>
                        <button className={this.getButtonClass(this.state.display.filterValue === "all")}
                                style={{marginLeft: "10px", fontSize: "10px", height: "40px"}}
                                disabled={this.state.display.loading}
                                onClick={() => this.setFilterValue("all")}>
                            All jobs
                        </button>
                    </div>
                </div>
                <div className="col-md-1">
                    <TextInput labelText="Filter"
                               disabled={this.state.display.loading}
                               value={this.state.display.filterSearch}
                               style={{width: "80px"}}
                               onChange={(event) => this.setSearchFilter(event.target.value)}/>
                </div>
                <div className="col-md-2">
                    <Select
                        value={this.state.display.orderBy}
                        labelText="order by"
                        disabled={this.state.display.loading}
                        onChange={(event) => this.setOrderBy(event.target.value)}>
                        <option value="nameAsc">Name (asc)</option>
                        <option value="nameDes">Name (desc)</option>
                        <option value="jobsAsc">Jobs (asc)</option>
                        <option value="jobsDes">Jobs (desc)</option>
                    </Select>
                </div>
                <div className="col-md-1">
                    <Button className="btn btn-success btn-sm"
                            onClick={() => this.refreshJobCount()}
                            disabled={this.state.display.loading}>
                        <ArrowRepeat/> Refresh
                    </Button>
                </div>
            </div>

            <div className="row" style={{width: "100%"}}>
                <div className="col-md-12">
                    <table width="100%" style={{tableLayout: "fixed"}}>
                        <tbody>
                        {this.getSortedFilteredDetails().map((runner, _index) =>
                            <tr key={runner.type}>
                                <td style={{verticalAlign: "top", width: "120px"}}>
                                    <RunnerCartouche runner={runner}/>
                                </td>
                                <td style={{verticalAlign: "top", width: "15%"}}>
                                    {runner.name}
                                </td>
                                <td style={{verticalAlign: "top", width: "15%"}}>
                                    {runner.type}
                                </td>
                                <td style={{verticalAlign: "top", width: "260px"}}>
                                    <RunnerAnalysisTags runnerDisplay={runner} timestamp={this.state.dashboard.timestamp}/>
                                </td>
                                <td style={{verticalAlign: "top"}}>
                                    <RunnerChart type="ExecutionsTopic" runnerDisplay={runner} title={false}
                                                 timestamp={this.state.dashboard.timestamp}/>
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>)
    }

    getButtonClass(active) {
        if (active)
            return "btn btn-primary btn-sm";
        return "btn btn-outline-primary btn-sm"
    }

    setFilterValue(value) {
        let displayObject = this.state.display;
        displayObject.filterValue = value;
        this.setState({display: displayObject});
        this.refreshJobCount();
    }

    setSearchFilter(value) {
        this.setDisplayProperty("filterSearch", value);
    }

    setOrderBy(value) {
        this.setDisplayProperty("orderBy", value);
    }

    /**
     * Sorting/searching is done client side: the backend returns the full "details" list for the
     * selected filter, already enriched with everything needed to display and sort it.
     */
    getSortedFilteredDetails() {
        let listDetails = this.state.dashboard.details || [];
        if (this.state.display.filterSearch) {
            const search = this.state.display.filterSearch.toLowerCase();
            listDetails = listDetails.filter(runner => runner.name && runner.name.toLowerCase().includes(search));
        }
        const comparators = {
            nameAsc: (a, b) => (a.name || "").localeCompare(b.name || ""),
            nameDes: (a, b) => (b.name || "").localeCompare(a.name || ""),
            jobsAsc: (a, b) => (a.topicCount || 0) - (b.topicCount || 0),
            jobsDes: (a, b) => (b.topicCount || 0) - (a.topicCount || 0)
        };
        return [...listDetails].sort(comparators[this.state.display.orderBy]);
    }

    refreshJobCount() {
        let uri = 'cherry/api/jobcount/dashboard?filter=' + this.state.display.filterValue;
        console.log("JobCount.refreshJobCount http[" + uri + "]");

        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson(uri, this, this.refreshJobCountCallback);
    }

    refreshJobCountCallback(httpPayload) {
        this.setDisplayProperty("loading", false);
        if (httpPayload.isError()) {
            console.log("JobCount.refreshJobCountCallback: error " + httpPayload.getError());
            this.setState({status: httpPayload.getError()});
        } else {
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

export default JobCount;
